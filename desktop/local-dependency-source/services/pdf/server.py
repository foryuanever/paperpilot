#!/usr/bin/env python3
import argparse
from difflib import SequenceMatcher
import io
import json
import os
import re
import tempfile
import threading
import time
import traceback
import uuid
from pathlib import Path

import requests
from flask import Flask, jsonify, request, send_file

app = Flask(__name__)
tasks = {}
tasks_lock = threading.Lock()
translation_context = threading.local()
translation_activity_lock = threading.Lock()
translation_activity_counts = {}
# A single large PDF can legitimately keep the serialized desktop bridge busy
# for far longer than the legacy 15-minute absolute timeout. Stop only when
# the task goes quiet, while retaining a finite upper bound for bad jobs.
TRANSLATION_MAX_DURATION_SECONDS = max(900, int(os.environ.get("PAPER_SOLVER_PDF_TRANSLATION_TIMEOUT_SECONDS", "2700")))
TRANSLATION_STALL_TIMEOUT_SECONDS = max(120, int(os.environ.get("PAPER_SOLVER_PDF_TRANSLATION_STALL_TIMEOUT_SECONDS", "180")))
layout_model = None
layout_model_lock = threading.Lock()
work_root = Path(os.environ.get("PAPER_SOLVER_DEPENDENCY_WORKDIR", Path.home() / "Library" / "Application Support" / "PaperSolver" / "dependency-work"))
work_root.mkdir(parents=True, exist_ok=True)


@app.get("/")
def health():
    return jsonify({
        "ok": True,
        "name": "PaperSolver Local Dependency",
        "service": "pdf",
        "paperSolverBridge": True,
        "googleBridge": bool(os.environ.get("PAPER_SOLVER_DESKTOP_TRANSLATE_URL", "").strip()),
        "time": int(time.time())
    })


@app.get("/health")
def health_alias():
    return health()


@app.post("/v1/translate")
def translate():
    upload = request.files.get("file")
    if upload is None:
        return jsonify({"state": "FAILURE", "message": "missing pdf file"}), 400
    data_raw = request.form.get("data") or "{}"
    try:
        data = json.loads(data_raw)
    except Exception:
        data = {}
    task_id = uuid.uuid4().hex
    task_dir = work_root / task_id
    task_dir.mkdir(parents=True, exist_ok=True)
    input_path = task_dir / safe_pdf_name(upload.filename or "paper.pdf")
    upload.save(input_path)
    if not looks_like_pdf(input_path):
        return jsonify({"state": "FAILURE", "message": "uploaded file is not a valid pdf"}), 400
    state = {
        "id": task_id,
        "state": "PENDING",
        "progress": 8,
        "message": "task accepted",
        "createdAt": time.time(),
        "lastActivityAt": time.time(),
        "input": str(input_path),
        "dual": str(task_dir / "dual.pdf"),
        "mono": str(task_dir / "mono.pdf"),
        "error": ""
    }
    with tasks_lock:
        tasks[task_id] = state
    worker = threading.Thread(target=run_translate_task, args=(task_id, data), daemon=True)
    worker.start()
    return jsonify({"id": task_id, "state": "PENDING", "progress": 8})


@app.get("/v1/translate/<task_id>")
def status(task_id):
    state = task_state(task_id)
    if state is None:
        return jsonify({"state": "FAILURE", "message": "task not found"}), 404
    return jsonify(public_state(state))


@app.get("/v1/translate/<task_id>/dual")
def dual(task_id):
    state = task_state(task_id)
    if state is None:
        return jsonify({"state": "FAILURE", "message": "task not found"}), 404
    if state.get("state") != "SUCCESS":
        return jsonify(public_state(state)), 409
    dual_path = Path(state.get("dual") or "")
    if not dual_path.exists():
        return jsonify({"state": "FAILURE", "message": "dual pdf not found"}), 404
    return send_file(dual_path, mimetype="application/pdf", as_attachment=False, download_name="dual.pdf")


def run_translate_task(task_id, data):
    with translation_activity_lock:
        translation_activity_counts[task_id] = 0
    update_task(task_id, state="RUNNING", progress=18, message="正在加载本机翻译引擎")
    watchdog = threading.Thread(target=watch_translate_task, args=(task_id,), daemon=True)
    watchdog.start()
    try:
        translation_context.task_id = task_id
        install_papersolver_google_bridge()
        from pdf2zh import translate_stream

        input_path = Path(task_state(task_id)["input"])
        service = str(data.get("service") or "google")
        # Do not assume English. pdf2zh forwards this value to the translator
        # bridge, which supports automatic detection for multilingual PDFs.
        lang_in = str(data.get("lang_in") or "auto")
        lang_out = str(data.get("lang_out") or "zh")
        # Two workers shorten long documents while the desktop bridge keeps a
        # strict shared concurrency cap for the selected provider.
        try:
            thread = max(1, min(4, int(data.get("thread") or 4)))
        except (TypeError, ValueError):
            thread = 4

        update_task(task_id, progress=32, message="正在逐段翻译并重建页面")
        def page_progress(progress):
            if task_failed(task_id):
                raise RuntimeError("翻译任务已停止")
            total = max(1, int(progress.total or 1))
            completed = max(0, int(progress.n or 0) - 1)
            update_task(task_id, progress=32 + int(60 * completed / total),
                        lastActivityAt=time.time(),
                        message=f"正在翻译第 {completed + 1}/{total} 页（已完成 {completed} 页）")
        pdf_bytes = input_path.read_bytes()
        translated, dual = translate_stream(
            stream=pdf_bytes,
            pages=None,
            lang_in=lang_in,
            lang_out=lang_out,
            service=service,
            thread=thread,
            callback=page_progress,
            model=get_layout_model(),
            skip_subset_fonts=bool(data.get("skip_subset_fonts", True))
        )
        state = task_state(task_id)
        mono_bytes = validate_pdf_bytes(translated, "单语 PDF")
        dual_bytes = validate_pdf_bytes(dual, "双语 PDF")
        Path(state["mono"]).write_bytes(mono_bytes)
        Path(state["dual"]).write_bytes(dual_bytes)
        if not task_failed(task_id):
            update_task(task_id, state="SUCCESS", progress=100, message="双语 PDF 已生成")
    except Exception as error:
        if not task_failed(task_id):
            update_task(
                task_id,
                state="FAILURE",
                progress=100,
                message=str(error) or "translation failed",
                error=traceback.format_exc(limit=8)
            )
    finally:
        with translation_activity_lock:
            translation_activity_counts.pop(task_id, None)
        translation_context.task_id = ""


def install_papersolver_google_bridge():
    bridge_url = os.environ.get("PAPER_SOLVER_DESKTOP_TRANSLATE_URL", "").strip()
    if not bridge_url:
        return
    try:
        from pdf2zh import converter as converter_module
        from pdf2zh import translator as translator_module
    except Exception:
        return
    if getattr(translator_module, "_papersolver_google_bridge_installed", False):
        return

    class PaperSolverGoogleTranslator(translator_module.BaseTranslator):
        name = "google"
        lang_map = {"zh": "zh-CN"}

        def __init__(self, lang_in, lang_out, model, ignore_cache=False, **kwargs):
            super().__init__(lang_in, lang_out, model, ignore_cache)
            self.task_id = str(getattr(translation_context, "task_id", "") or "")
            self.endpoint = bridge_url
            self.bibliography_mode = False

        def do_translate(self, text):
            translation_context.task_id = self.task_id
            original = str(text or "")
            if self.bibliography_mode:
                return original
            if is_bibliography_heading(original):
                self.bibliography_mode = True
                return original
            if is_protected_bibliography_text(original):
                return original
            # pdf2zh also emits invisible layout placeholders and formula-only
            # fragments. Google rejects some of these with HTTP 400, while
            # preserving them verbatim is the correct rendering behaviour.
            text, protected_layout_tokens = protect_layout_tokens(clean_bridge_text(original))
            if not has_translatable_text(text):
                return original
            bridge_source_lang = guess_source_language(original, self.lang_in)
            touch_translate_task("正在逐段翻译并重建页面")
            response = requests.post(
                self.endpoint,
                json={
                    "provider": bridge_provider_name(),
                    "text": text,
                    "sourceLang": bridge_source_lang,
                    "targetLang": self.lang_out,
                    "pdfTranslationBridge": True,
                },
                timeout=(3, 90),
            )
            if not response.ok:
                detail = response.text.replace("\n", " ").strip()[:240]
                message = f"桌面翻译桥接不可用（HTTP {response.status_code}）：{detail or '没有返回错误详情'}"
                mark_bridge_failure(message)
                raise RuntimeError(message)
            payload = response.json()
            touch_translate_task("正在逐段翻译并重建页面")
            result = payload.get("translatedText") or payload.get("translated_text") or ""
            if not result:
                message = "桌面翻译桥接没有返回译文"
                mark_bridge_failure(message)
                raise RuntimeError(message)
            translated = sanitize_bridge_translation(original, str(result), protected_layout_tokens, bridge_source_lang)
            return translator_module.remove_control_characters(translated)

    translator_module.GoogleTranslator = PaperSolverGoogleTranslator
    converter_module.GoogleTranslator = PaperSolverGoogleTranslator
    translator_module._papersolver_google_bridge_installed = True

def bridge_provider_name():
    provider_file = os.environ.get("PAPER_SOLVER_TRANSLATION_PROVIDER_FILE", "").strip()
    if provider_file:
        try:
            payload = json.loads(Path(provider_file).read_text(encoding="utf-8"))
            provider = str(payload.get("provider") or "").strip().lower()
            if provider in {"google", "google-web", "tencent-transmart", "youdao"}:
                return provider
        except Exception:
            pass
    return "tencent-transmart"


def clean_bridge_text(value):
    text = str(value or "")
    text = re.sub(r"[\ue000-\uf8ff\ufffd\u25a0\u25aa\u25cf\u25c6\u25b2\u25b6\uf0a7\uf0b7]", " ", text)
    text = re.sub(r"[\[【(（]\s*20\d{2}[-/]\d{1,2}[-/]\d{1,2}(?:\s+\d{1,2}:\d{2}(?::\d{2})?)?\s*[\]】)）]", " ", text)
    text = re.sub(r"[\[【(（]\s*\d{3}\s*[\]】)）]", " ", text)
    text = re.sub(r"\b20\d{2}[-/]\d{1,2}[-/]\d{1,2}\s+\d{1,2}:\d{2}(?::\d{2})?\b", " ", text)
    cleaned = "".join(ch for ch in text if ch >= " " and ch != "\x7f")
    lines = [re.sub(r"[ \t]+", " ", line).strip() for line in cleaned.splitlines()]
    return "\n".join(lines).strip()[:5000]


def protect_layout_tokens(value):
    """Keep pdf2zh inline layout markers out of external translation APIs."""
    tokens = []

    def replace(match):
        tokens.append(match.group(0))
        return f"PAPERSOLVER_LAYOUT_TOKEN_{len(tokens) - 1}_END"

    protected = re.sub(r"<\s*[se]\s*:\s*\d+\s*>", replace, str(value or ""), flags=re.IGNORECASE)
    return protected, tokens


def restore_layout_tokens(value, tokens):
    restored = str(value or "")
    for index, token in enumerate(tokens):
        pattern = rf"PAPERSOLVER[\s_-]*LAYOUT[\s_-]*TOKEN[\s_-]*{index}[\s_-]*END"
        restored = re.sub(pattern, token, restored, flags=re.IGNORECASE)
    return restored


def sanitize_bridge_translation(original, value, tokens, source_lang):
    translated = str(value or "")
    if not translated.strip():
        raise RuntimeError("翻译服务返回空内容，已停止重建 PDF")
    if re.search(r"(?:Traceback|Error invoking|Internal Server Error|<html|<!doctype)", translated, re.IGNORECASE):
        raise RuntimeError("翻译服务返回错误页，已停止重建 PDF")
    marker_matches = re.findall(r"<\s*[se]\s*:\s*\d+\s*>", translated, re.IGNORECASE)
    layout_matches = re.findall(r"PAPERSOLVER[\s_-]*LAYOUT[\s_-]*TOKEN[\s_-]*(\d+)[\s_-]*END", translated, re.IGNORECASE)
    expected = [str(index) for index in range(len(tokens))]
    if marker_matches or sorted(layout_matches, key=int) != expected:
        raise RuntimeError("翻译服务破坏了 PDF 版面标记，已停止重建 PDF")

    translated = re.sub(r"[\[【(（]\s*20\d{2}[-/]\d{1,2}[-/]\d{1,2}(?:\s+\d{1,2}:\d{2}(?::\d{2})?)?\s*[\]】)）]", "", translated)
    translated = re.sub(r"\b20\d{2}[-/]\d{1,2}[-/]\d{1,2}\s+\d{1,2}:\d{2}(?::\d{2})?\b", "", translated)
    translated = re.sub(r"[\[【(（]\s*\d{3}\s*[\]】)）]", "", translated)
    translated = re.sub(r"[\ue000-\uf8ff\ufffd\u25a0\u25aa\u25cf\u25c6\u25b2\u25b6\uf0a7\uf0b7]", " ", translated)
    translated = re.sub(r"[ \t]{2,}", " ", translated).strip()

    if re.search(r"(.{16,100})(?:\s*\1){2,}", translated, re.DOTALL):
        raise RuntimeError("翻译服务返回了重复内容，已停止重建 PDF")
    if len(translated) > max(len(original) * 5 + 800, 12000):
        raise RuntimeError("翻译服务返回内容异常膨胀，已停止重建 PDF")
    source = re.sub(r"\s+", " ", str(original or "")).strip().casefold()
    result = re.sub(r"\s+", " ", translated).strip().casefold()
    source_cmp = re.sub(r"<\s*[se]\s*:\s*\d+\s*>", " ", source, flags=re.IGNORECASE)
    source_cmp = re.sub(r"papersolver[\s_-]*layout[\s_-]*token[\s_-]*\d+[\s_-]*end", " ", source_cmp, flags=re.IGNORECASE)
    result_cmp = re.sub(r"papersolver[\s_-]*layout[\s_-]*token[\s_-]*\d+[\s_-]*end", " ", result, flags=re.IGNORECASE)
    non_latin_source = bool(re.search(r"[A-Za-zÀ-ÿ]", source)) and not bool(re.search(r"[\u3400-\u9fff]", source))
    if source_lang not in {"", "auto"} and non_latin_source and len(source_cmp) >= 24:
        similarity = SequenceMatcher(None, source_cmp, result_cmp).ratio()
        if source_cmp == result_cmp or (len(source_cmp) >= 80 and similarity >= 0.86):
            raise RuntimeError("翻译服务返回原文或大段原文回流，已停止重建 PDF")
        # A provider can prepend a short Chinese sentence while leaving a
        # whole source paragraph untouched. Catch that before layout rebuild.
        source_fragments = re.findall(r"[A-Za-zÀ-ÿ][A-Za-zÀ-ÿ0-9 ,.;:'\"()\-]{79,}", source_cmp)
        if any(fragment.strip() in result_cmp for fragment in source_fragments):
            raise RuntimeError("翻译服务混入大段原文，已停止重建 PDF")
    return restore_layout_tokens(translated, tokens)


def has_translatable_text(value):
    return any(ch.isalpha() or ch.isdigit() for ch in str(value or ""))


def guess_source_language(value, requested):
    requested = str(requested or "auto").lower()
    if requested not in {"", "auto"}:
        return requested
    text = str(value or "").lower()
    if re.search(r"[áéíóúüñ¿¡]", text) or sum(len(re.findall(rf"\b{word}\b", text)) for word in ("el", "los", "las", "una", "que", "para", "con", "por")) >= 2:
        return "es"
    if re.search(r"[àâçéèêëîïôùûüÿœæ]", text) or sum(len(re.findall(rf"\b{word}\b", text)) for word in ("les", "des", "une", "dans", "pour", "avec", "est")) >= 2:
        return "fr"
    if re.search(r"[äöüß]", text) or sum(len(re.findall(rf"\b{word}\b", text)) for word in ("der", "die", "das", "und", "für", "mit")) >= 2:
        return "de"
    if re.search(r"[çğıöşüİı]", text) or sum(len(re.findall(rf"\b{word}\b", text)) for word in ("ve", "bir", "için", "ile", "olan", "bu")) >= 2:
        return "tr"
    if sum(len(re.findall(rf"\b{word}\b", text)) for word in ("il", "gli", "una", "che", "per", "con")) >= 2:
        return "it"
    if re.search(r"[ãõáéíóúâêôç]", text) or sum(len(re.findall(rf"\b{word}\b", text)) for word in ("uma", "dos", "das", "para", "com", "não")) >= 2:
        return "pt"
    return "auto"


def is_protected_bibliography_text(value):
    """Keep section labels and bibliography-like fragments verbatim."""
    text = re.sub(r"\s+", " ", str(value or "")).strip()
    if not text:
        return True
    compact = re.sub(r"[\s:：.。\-_]+", "", text).lower()
    if compact in {
        "abstract", "摘要", "references", "reference", "参考文献", "bibliography",
        "workscited", "literaturecited", "referencesandnotes", "参考资料"
    }:
        return True
    if bool(re.match(r"^(?:\[\d+\]|\d+\.|\([A-Za-z]+\s*,\s*\d{4}\))\s+[A-Z]", text)):
        return True
    if bool(re.search(r"(?:https?://|doi\.org/|\bdoi\s*:|arxiv:\d|\bet\s+al\b|\bvol\.\s*\d+|\bpp\.\s*\d+-\d+|\bpages?\s+\d+-\d+|\bed\.\s*\d+)", text, re.IGNORECASE)):
        return True
    return False


def is_bibliography_heading(value):
    raw = re.sub(r"\s+", " ", str(value or "")).strip().lower()
    text = re.sub(r"[\s:：.。\-_]+", "", raw)
    if text in {"references", "reference", "参考文献", "bibliography", "workscited", "literaturecited", "referencesandnotes", "参考资料"}:
        return True
    return bool(re.match(r"^(?:\d+\.?\s*)?(?:references?|bibliography|works\s+cited|literature\s+cited|参考文献|参考资料|references\s*and\s*notes)(?:\s*[:：.。]|\s+|$)", raw, re.IGNORECASE))


def get_layout_model():
    global layout_model
    with layout_model_lock:
        if layout_model is None:
            from pdf2zh.doclayout import OnnxModel
            layout_model = OnnxModel.from_pretrained()
        return layout_model


def bytes_from_pdf_result(value):
    if value is None:
        raise RuntimeError("empty pdf result")
    if isinstance(value, bytes):
        return value
    if isinstance(value, bytearray):
        return bytes(value)
    if hasattr(value, "getvalue"):
        return value.getvalue()
    if hasattr(value, "read"):
        pos = value.tell() if hasattr(value, "tell") else None
        if hasattr(value, "seek"):
          value.seek(0)
        data = value.read()
        if pos is not None and hasattr(value, "seek"):
          value.seek(pos)
        return data
    raise RuntimeError("unsupported pdf result")


def validate_pdf_bytes(value, label):
    data = bytes_from_pdf_result(value)
    if len(data) < 1024 or not data.startswith(b"%PDF"):
        raise RuntimeError(f"{label}生成失败：输出不是有效 PDF，已阻止保存异常文件")
    error_markers = (
        "论文结构化解析失败".encode("utf-8"),
        b"Fatal Python error",
        b"Traceback",
        b"Error invoking",
    )
    if any(marker in data for marker in error_markers):
        raise RuntimeError(f"{label}包含解析错误页，已阻止保存异常文件")
    return data


def safe_pdf_name(name):
    base = "".join(ch if ch.isalnum() or ch in "._-" else "_" for ch in name).strip("._")
    if not base.lower().endswith(".pdf"):
        base += ".pdf"
    return base[:90] or "paper.pdf"


def looks_like_pdf(path):
    with open(path, "rb") as handle:
        return handle.read(4) == b"%PDF"


def task_state(task_id):
    with tasks_lock:
        state = tasks.get(task_id)
        return dict(state) if state else None


def update_task(task_id, **updates):
    with tasks_lock:
        state = tasks.get(task_id)
        if not state:
            return
        state.update(updates)
        state["updatedAt"] = time.time()


def touch_translate_task(message=None):
    task_id = str(getattr(translation_context, "task_id", "") or "")
    if not task_id:
        return
    with translation_activity_lock:
        translation_activity_counts[task_id] = translation_activity_counts.get(task_id, 0) + 1
        count = translation_activity_counts[task_id]
    updates = {
        "lastActivityAt": time.time(),
        "progress": min(92, 32 + min(60, max(1, int(count ** 0.62))))
    }
    if message:
        updates["message"] = message
    else:
        updates["message"] = f"正在逐段翻译并重建页面（已处理 {count} 个片段）"
    update_task(task_id, **updates)


def watch_translate_task(task_id):
    while True:
        time.sleep(5)
        state = task_state(task_id)
        if state is None or state.get("state") in {"SUCCESS", "FAILURE"}:
            return
        now = time.time()
        created_at = float(state.get("createdAt") or now)
        last_activity_at = float(state.get("lastActivityAt") or created_at)
        if now - created_at >= TRANSLATION_MAX_DURATION_SECONDS:
            expire_translate_task(task_id, "translation watchdog expired; maximum translation duration reached")
            return
        if now - last_activity_at >= TRANSLATION_STALL_TIMEOUT_SECONDS:
            expire_translate_task(task_id, "translation watchdog expired; desktop translation bridge did not make progress in time")
            return


def expire_translate_task(task_id, message="translation watchdog expired; desktop translation bridge did not complete in time"):
    state = task_state(task_id)
    if state is None or state.get("state") in {"SUCCESS", "FAILURE"}:
        return
    update_task(
        task_id,
        state="FAILURE",
        progress=100,
        message=message,
        error="PaperSolver stopped this translation instead of leaving it in progress indefinitely."
    )


def task_expired(task_id):
    state = task_state(task_id)
    return bool(state and state.get("state") == "FAILURE" and "translation watchdog expired" in str(state.get("message") or ""))


def task_failed(task_id):
    state = task_state(task_id)
    return bool(state and state.get("state") == "FAILURE")


def mark_bridge_failure(message):
    task_id = str(getattr(translation_context, "task_id", "") or "")
    if not task_id or task_failed(task_id):
        return
    update_task(
        task_id,
        state="FAILURE",
        progress=100,
        message=message,
        error="PaperSolver stopped the task after the desktop translation bridge failed."
    )


def public_state(state):
    return {
        "id": state.get("id"),
        "state": state.get("state") or "RUNNING",
        "progress": int(state.get("progress") or 20),
        "message": state.get("message") or "",
        "error": state.get("error") or "",
        "createdAt": state.get("createdAt"),
        "lastActivityAt": state.get("lastActivityAt")
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=11008)
    args = parser.parse_args()
    app.run(host=args.host, port=args.port, threaded=True, use_reloader=False)


if __name__ == "__main__":
    main()
