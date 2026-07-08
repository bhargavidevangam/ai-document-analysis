package com.bhargavi.ai.serviceImpl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.bhargavi.ai.payload.DocumentSummaryResponse;
import com.bhargavi.ai.service.DocumentAnalysisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DocumentAnalysisServiceImpl implements DocumentAnalysisService {

    private static final int MAX_SUMMARY_CHARS = 420;
    private static final int MAX_GEMINI_INPUT_CHARS = 12_000;
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1/models/";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "xlsx", "doc", "docx");

    private final Tika tika = new Tika();
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.model}")
    private String geminiModel;

    @Value("${ocr.tesseract.command}")
    private String tesseractCommand;

    @Value("${ocr.tesseract.language}")
    private String tesseractLanguage;

    @Override
    public DocumentSummaryResponse analyze(MultipartFile file, String prompt) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must not be empty");
        }

        String extension = getFileExtension(file);
        String extractedText = extractText(file, extension).replaceAll("\\s+", " ").trim();
        System.out.println("Extracted Text: " + extractedText);
        if (extractedText.isBlank()) {
            throw new IllegalArgumentException("Could not extract readable text from uploaded file");
        }

        String summary = summarize(extractedText, prompt);
        int wordCount = extractedText.split("\\s+").length;

        return new DocumentSummaryResponse(
                file.getOriginalFilename(),
                file.getContentType(),
                    summary,
                wordCount);
    }

    private String getFileExtension(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("File name must include one of: pdf, xlsx, doc, docx");
        }

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported file type. Allowed types: pdf, xlsx, doc, docx");
        }

        return extension;
    }

    private String extractText(MultipartFile file, String extension) {
        String parsedText = extractTextWithTika(file);
        if (!parsedText.isBlank() || !"pdf".equals(extension)) {
            return parsedText;
        }
        System.out.println("No text extracted with Tika for PDF. Attempting OCR for scanned PDF.");
        // For scanned PDFs with no selectable text, fallback to OCR.
        return extractTextFromScannedPdf(file);
    }

    private String extractTextWithTika(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return tika.parseToString(inputStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded file", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported or unreadable document format", e);
        }
    }

    private String extractTextFromScannedPdf(MultipartFile file) {
        final byte[] pdfBytes;
        try {
            pdfBytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded PDF for OCR", e);
        }

        if (pdfBytes.length == 0) {
            throw new IllegalArgumentException("Uploaded PDF is empty and cannot be processed with OCR");
        }

        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder ocrText = new StringBuilder();

            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, 300);
                String pageText = runTesseract(image).replaceAll("\\s+", " ").trim();
                if (pageText.isBlank()) {
                    continue;
                }
                if (!ocrText.isEmpty()) {
                    ocrText.append(' ');
                }
                ocrText.append(pageText);
            }

            if (ocrText.isEmpty()) {
                throw new IllegalArgumentException("OCR completed but no readable text was found in the PDF pages");
            }

            return ocrText.toString();
        } catch (IllegalArgumentException e) {
            // Keep detailed OCR/PDF validation message for API response.
            throw e;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read scanned PDF pages for OCR: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Failed during OCR rendering/execution: " + e.getMessage(), e);
        }
    }

    private String runTesseract(BufferedImage image) {
        Path imageFile = null;
        Path outputPrefix = null;
        try {
            imageFile = Files.createTempFile("ocr-page-", ".png");
            outputPrefix = Files.createTempFile("ocr-output-", "");
            Files.deleteIfExists(outputPrefix);

            ImageIO.write(image, "png", imageFile.toFile());

            ProcessBuilder processBuilder = new ProcessBuilder(
                    tesseractCommand,
                    imageFile.toString(),
                    outputPrefix.toString(),
                    "-l",
                    tesseractLanguage,
                    "--dpi",
                    "300");
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String errorOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalArgumentException("OCR failed. Verify Tesseract installation/configuration. " + errorOutput);
            }

            Path outputTextFile = Path.of(outputPrefix.toString() + ".txt");
            if (!Files.exists(outputTextFile)) {
                return "";
            }

            return Files.readString(outputTextFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Failed to run OCR for scanned PDF. Configure a valid ocr.tesseract.command (current: "
                            + tesseractCommand + "). Cause: " + e.getMessage(),
                    e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("OCR process was interrupted", e);
        } finally {
            tryDelete(imageFile);
            tryDelete(outputPrefix);
            if (outputPrefix != null) {
                tryDelete(Path.of(outputPrefix.toString() + ".txt"));
            }
        }
    }

    private void tryDelete(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup for OCR temp files.
        }
    }

    private String summarize(String text, String prompt) {
        String geminiSummary = summarizeWithGemini(text, prompt);

        System.out.println("Gemini Summary: " + geminiSummary);
        if (geminiSummary != null && !geminiSummary.isBlank()) {
            System.out.println("Using Gemini API for summarization.");
            return geminiSummary;
        }
        System.out.println("Using LOCAL for summarization.");
        return summarizeLocally(text);
    }

    private String summarizeWithGemini(String text, String prompt) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return null;
        }

        String model = (geminiModel == null || geminiModel.isBlank()) ? "gemini-2.5-pro" : geminiModel;

        String truncatedInput = text.length() > MAX_GEMINI_INPUT_CHARS
                ? text.substring(0, MAX_GEMINI_INPUT_CHARS)
                : text;

        String defaultPrompt = prompt + truncatedInput;

        if (defaultPrompt == null || defaultPrompt.isBlank()) {
            defaultPrompt = """
                    Summarize the following document in plain text.
                    Rules:
                    - Use at most 3 sentences.
                    - Keep the summary under 420 characters.
                    - Focus on the main purpose, key facts, and outcome.

                    Document:
                    %s
                    """.formatted(truncatedInput);
        }

        System.out.println("Gemini API Prompt: " + prompt);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", defaultPrompt)))));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-goog-api-key", geminiApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        String endpoint = GEMINI_BASE_URL + model + ":generateContent";

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entity, String.class);
            return extractGeminiSummary(response.getBody());
        } catch (RestClientException | IllegalArgumentException e) {
            return null;
        }
    }

    private String extractGeminiSummary(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }

        try {
            JsonNode partsNode = objectMapper.readTree(responseBody)
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts");

            if (!partsNode.isArray() || partsNode.isEmpty()) {
                return null;
            }

            StringBuilder combined = new StringBuilder();
            for (JsonNode part : partsNode) {
                String piece = part.path("text").asText("");
                if (piece.isBlank()) {
                    continue;
                }
                if (!combined.isEmpty()) {
                    combined.append(' ');
                }
                combined.append(piece.trim());
            }

            String summary = combined.toString().replaceAll("\\s+", " ").trim();
            if (summary.isBlank()) {
                return null;
            }

            if (summary.length() <= MAX_SUMMARY_CHARS) {
                return summary;
            }

            return summary.substring(0, MAX_SUMMARY_CHARS) + "...";
        } catch (IOException e) {
            return null;
        }
    }

    private String summarizeLocally(String text) {
        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder summary = new StringBuilder();

        for (String sentence : sentences) {
            if (sentence.isBlank()) {
                continue;
            }

            if (summary.length() + sentence.length() + 1 > MAX_SUMMARY_CHARS) {
                break;
            }

            if (!summary.isEmpty()) {
                summary.append(' ');
            }
            summary.append(sentence.trim());

            if (summary.length() >= MAX_SUMMARY_CHARS || countSentences(summary.toString()) >= 3) {
                break;
            }
        }

        if (summary.isEmpty()) {
            int end = Math.min(text.length(), MAX_SUMMARY_CHARS);
            return text.substring(0, end) + (text.length() > end ? "..." : "");
        }

        return summary.toString();
    }

    private int countSentences(String value) {
        return value.split("(?<=[.!?])\\s+").length;
    }
}
