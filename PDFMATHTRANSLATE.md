# PDFMathTranslate integration

PaperSolver 的“双栏翻译”直接调用 PDFMathTranslate HTTP API，并展示其生成的 `dual.pdf`。

```bash
docker compose -f docker-compose.pdfmathtranslate.yml up -d
```

默认服务地址为 `http://127.0.0.1:11008`，可通过 `PDFMATH_TRANSLATE_URL` 修改。

Upstream: https://github.com/PDFMathTranslate/PDFMathTranslate  
License: AGPL-3.0
