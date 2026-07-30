#!/usr/bin/env python3
import argparse
import io
import json
import os
import tempfile
import threading
import time
import traceback
import uuid
from pathlib import Path

from flask import Flask, jsonify, request, send_file

app = Flask(__name__)
tasks = {}
tasks_lock = threading.Lock()
work_root = Path(os.environ.get("PAPER_SOLVER_DEPENDENCY_WORKDIR", Path.home() / "Library" / "Application Support" / "PaperSolver" / "dependency-work"))
work_root.mkdir(parents=True, exist_ok=True)


@app.get("/")
def health():
    return jsonify({
        "ok": True,
        "name": "PaperSolver Local Dependency",
        "service": "pdf",
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
    update_task(task_id, state="RUNNING", progress=18, message="loading local translator")
    try:
        from pdf2zh import translate_stream

        input_path = Path(task_state(task_id)["input"])
        service = str(data.get("service") or "google")
        lang_in = str(data.get("lang_in") or "en")
        lang_out = str(data.get("lang_out") or "zh")
        thread = int(data.get("thread") or 4)

        update_task(task_id, progress=32, message="translating pdf")
        pdf_bytes = input_path.read_bytes()
        translated, dual = translate_stream(
            stream=pdf_bytes,
            pages=None,
            lang_in=lang_in,
            lang_out=lang_out,
            service=service,
            thread=thread,
            skip_subset_fonts=bool(data.get("skip_subset_fonts", True))
        )
        state = task_state(task_id)
        Path(state["mono"]).write_bytes(bytes_from_pdf_result(translated))
        Path(state["dual"]).write_bytes(bytes_from_pdf_result(dual))
        update_task(task_id, state="SUCCESS", progress=100, message="dual pdf generated")
    except Exception as error:
        update_task(
            task_id,
            state="FAILURE",
            progress=100,
            message=str(error) or "translation failed",
            error=traceback.format_exc(limit=8)
        )


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


def public_state(state):
    return {
        "id": state.get("id"),
        "state": state.get("state") or "RUNNING",
        "progress": int(state.get("progress") or 20),
        "message": state.get("message") or "",
        "error": state.get("error") or ""
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=11008)
    args = parser.parse_args()
    app.run(host=args.host, port=args.port, threaded=True)


if __name__ == "__main__":
    main()
