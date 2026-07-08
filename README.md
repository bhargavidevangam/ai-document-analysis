# AI Document Analysis API

Production-style Spring Boot API for AI-based document summarization.

It supports:
- automatic text extraction from uploaded documents,
- default AI summary generation when no prompt is provided,
- prompt-based AI summary generation when a prompt is provided,
- OCR fallback for scanned PDFs.

## What This Service Does

1. Accepts a document upload (`pdf`, `xlsx`, `doc`, `docx`).
2. Extracts text using Apache Tika.
3. If a PDF has no selectable text, runs OCR (Tesseract) on rendered pages.
4. Generates a summary:
   - **Default summary** when `prompt` is empty.
   - **Prompt-based summary** when `prompt` is provided.
5. Returns metadata and the final summary response.

## API Endpoint

- **Method:** `POST`
- **Path:** `/aidocs/api/document-analysis/summary`
- **Content-Type:** `multipart/form-data`
- **Form fields:**
  - `file` (required): document file
  - `prompt` (optional): custom instruction for summary style/output

## Request Examples

### 1) Default summary (no prompt)

```cmd
curl -X POST "http://localhost:8008/aidocs/api/document-analysis/summary" ^
  -F "file=@D:\path\to\sample.pdf"
```

### 2) Prompt-based summary

```cmd
curl -X POST "http://localhost:8008/aidocs/api/document-analysis/summary" ^
  -F "file=@D:\path\to\sample.pdf" ^
  -F "prompt=Summarize in 3 bullet points with action items."
```

## Example Response

```json
{
  "fileName": "sample.pdf",
  "contentType": "application/pdf",
  "summary": "The document outlines...",
  "wordCount": 1234
}
```

## OCR Support for Scanned PDFs

- OCR is used only when PDF text extraction returns empty text.
- Tesseract must be installed and configured.
- Configure command path with `TESSERACT_CMD`.

### Windows example

```cmd
set TESSERACT_CMD=C:\Program Files\Tesseract-OCR\tesseract.exe
```

## Configuration

Main properties are in `src/main/resources/application.properties`.

- `server.port=8008`
- `server.servlet.context-path=/aidocs`
- `gemini.api.key=${GEMINI_API_KEY}`
- `gemini.model=${GEMINI_MODEL}`
- `ocr.tesseract.command=${TESSERACT_CMD:/usr/bin/tesseract}`
- `ocr.tesseract.language=eng`

## Run Locally

```cmd
cd /d D:\Users\test\Downloads\ai-document-analysis
mvnw.cmd spring-boot:run
```

## API Documentation

- Swagger UI: `http://localhost:8008/aidocs/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8008/aidocs/v3/api-docs`

## Notes

- If Gemini API credentials are not available or external AI fails, the service falls back to local summarization.
- Summary output is intentionally concise for API consumers.