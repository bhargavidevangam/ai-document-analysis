# AI Document Analysis

AI Document Analysis is a Spring Boot backend application that allows users to upload documents and generate intelligent summaries using Google Gemini AI.

The application demonstrates AI integration with Java by combining document processing and large language models to extract meaningful insights from uploaded files.

## Overview

This service accepts uploaded documents, extracts readable text, and returns concise summaries suitable for downstream systems.

It includes:
- intelligent summarization via Google Gemini,
- local summarization fallback when Gemini is unavailable,
- OCR fallback for scanned PDFs,
- OpenAPI/Swagger documentation for easy API testing.

## Tech Stack

- Java 21
- Spring Boot 4.1.x
- Spring Web MVC
- Spring Validation
- Spring Data JPA
- PostgreSQL (configured in `application.properties`)
- Apache Tika (document text extraction)
- Apache PDFBox (PDF page rendering for OCR)
- Tesseract OCR (for scanned PDFs)
- Google Gemini API (LLM summarization)
- springdoc-openapi (Swagger UI + OpenAPI)
- Maven Wrapper (`mvnw`, `mvnw.cmd`)

## Key Features

- Upload and analyze documents (`pdf`, `xlsx`, `doc`, `docx`)
- Automatic text extraction from supported file formats
- OCR fallback for image-based/scanned PDFs
- Optional prompt-based custom summaries
- Safe fallback to local summarization if Gemini is not configured or unavailable
- Standardized API error handling
- Interactive API docs via Swagger UI

## Prerequisites

Before running locally, make sure you have:
- JDK 21
- Maven (optional, wrapper is included)
- PostgreSQL (if your runtime path touches DB features/config)
- Tesseract OCR installed (recommended for scanned PDF support)
- Gemini API key (optional but recommended for AI summaries)

## Configuration

Main configuration file: `src/main/resources/application.properties`

### Core Properties

- `server.port=8008`
- `server.servlet.context-path=/aidocs`
- `spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/courtdb}`
- `spring.datasource.username=${DB_USERNAME:postgres}`
- `spring.datasource.password=${DB_PASSWORD:password}`

### AI + OCR Properties

- `gemini.api.key=${GEMINI_API_KEY}`
- `gemini.model=${GEMINI_MODEL}`
- `ocr.tesseract.command=${TESSERACT_CMD:/usr/bin/tesseract}`
- `ocr.tesseract.language=eng`

### Windows Example (Environment Variables)

```cmd
set GEMINI_API_KEY=your_api_key_here
set GEMINI_MODEL=gemini-2.5-pro
set TESSERACT_CMD=C:\Program Files\Tesseract-OCR\tesseract.exe
```

## API Endpoint

- **Method:** `POST`
- **Path:** `/aidocs/api/document-analysis/summary`
- **Content-Type:** `multipart/form-data`
- **Form fields:**
  - `file` (required): uploaded document
  - `prompt` (optional): custom summarization instruction

## Request Examples

### Default Summary

```cmd
curl -X POST "http://localhost:8008/aidocs/api/document-analysis/summary" ^
  -F "file=@D:\path\to\sample.pdf"
```

### Prompt-Based Summary

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

## Documentation

- Swagger UI: `http://localhost:8008/aidocs/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8008/aidocs/v3/api-docs`

## Project Structure

```text
ai-document-analysis/
|-- pom.xml
|-- README.md
`-- src/
    |-- main/
    |   |-- java/com/bhargavi/ai/
    |   |   |-- controller/
    |   |   |-- service/
    |   |   |-- serviceImpl/
    |   |   |-- payload/
    |   |   |-- exception/
    |   |   `-- config/
    |   `-- resources/
    |       `-- application.properties
    `-- test/
        `-- java/
```

## Run Locally

```cmd
cd /d D:\Users\test\Downloads\ai-document-analysis
mvnw.cmd spring-boot:run
```

## Run Tests

```cmd
cd /d D:\Users\test\Downloads\ai-document-analysis
mvnw.cmd test
```

## Professional Notes

- If Gemini configuration is missing or a remote call fails, the service automatically falls back to local summarization.
- OCR runs only when PDF text extraction returns empty content.
- Keep secrets (like API keys) in environment variables, not in source control.
- Validate uploaded files at client side as well for better user experience.

## Contributing

1. Fork or clone the repository.
2. Create a feature branch.
3. Add or update tests for your changes.
4. Open a pull request with a clear description.

## License

This project is licensed under the Apache License 2.0. See `LICENSE` for details.