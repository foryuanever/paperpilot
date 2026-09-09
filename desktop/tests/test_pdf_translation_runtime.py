"""Run with the offline runtime Python; no external translation calls."""
import importlib.util
import os
from pathlib import Path
import sys
import tempfile
import types
import unittest
from unittest.mock import patch

import pymupdf
import tenacity

ROOT = Path(__file__).resolve().parents[1]


class TranslationRuntimeTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        with patch.dict(os.environ, {"PAPER_SOLVER_DEPENDENCY_WORKDIR": self.temp.name}):
            spec = importlib.util.spec_from_file_location("pdf_service_test", ROOT / "dependency-service/pdf/server.py")
            self.service = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(self.service)

    def test_converter_retry_is_finite(self):
        converter = types.SimpleNamespace(retry=tenacity.retry)
        translator = types.SimpleNamespace(BaseTranslator=object)
        package = types.ModuleType("pdf2zh")
        package.converter, package.translator = converter, translator
        with patch.dict(sys.modules, {"pdf2zh": package}), patch.dict(os.environ, {"PAPER_SOLVER_DESKTOP_TRANSLATE_URL": "http://127.0.0.1/test"}):
            self.service.install_papersolver_google_bridge()
        calls = []
        @converter.retry(wait=tenacity.wait_none())
        def broken_paragraph():
            calls.append(1)
            raise RuntimeError("unavailable")
        with self.assertRaisesRegex(RuntimeError, "unavailable"):
            broken_paragraph()
        self.assertEqual(len(calls), 2)

    def test_fifteen_page_progress_and_pdf_validation(self):
        doc = pymupdf.open()
        for _ in range(15):
            doc.new_page()
        pdf = doc.tobytes()
        doc.close()
        directory = Path(self.temp.name)
        (directory / "input.pdf").write_bytes(pdf)
        self.service.tasks["paper"] = dict(input=str(directory / "input.pdf"),
            mono=str(directory / "mono.pdf"), dual=str(directory / "dual.pdf"), state="PENDING")
        seen = []
        def translate_stream(**kwargs):
            self.assertEqual(kwargs["thread"], 4)
            for page in range(1, 16):
                kwargs["callback"](types.SimpleNamespace(n=page, total=15))
                seen.append(self.service.task_state("paper")["progress"])
            return pdf, pdf
        package = types.ModuleType("pdf2zh")
        package.translate_stream = translate_stream
        with patch.dict(sys.modules, {"pdf2zh": package}), \
             patch.object(self.service, "install_papersolver_google_bridge"), \
             patch.object(self.service, "get_layout_model"), \
             patch.object(self.service, "watch_translate_task"):
            self.service.run_translate_task("paper", {})
        self.assertEqual(seen, sorted(seen))
        self.assertLess(max(seen), 98)
        self.assertEqual(self.service.task_state("paper")["state"], "SUCCESS")
        self.assertTrue((directory / "dual.pdf").exists())


if __name__ == "__main__":
    unittest.main()
