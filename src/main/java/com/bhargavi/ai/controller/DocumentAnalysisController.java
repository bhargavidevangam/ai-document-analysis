package com.bhargavi.ai.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.bhargavi.ai.payload.DocumentSummaryResponse;
import com.bhargavi.ai.service.DocumentAnalysisService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/document-analysis")
@Tag(name = "Document Analysis", description = "Endpoints for analyzing uploaded documents")
public class DocumentAnalysisController {

    private final DocumentAnalysisService documentAnalysisService;

    public DocumentAnalysisController(DocumentAnalysisService documentAnalysisService) {
        this.documentAnalysisService = documentAnalysisService;
    }

    @PostMapping(value = "/summary", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Generate a document summary", description = "Accepts a file upload and returns extracted text and a short summary")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Summary generated successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DocumentSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input file")
    })
    public ResponseEntity<DocumentSummaryResponse> summarizeDocument(
            @Parameter(description = "Document file to analyze (pdf, xlsx, doc, docx)", required = true, schema = @Schema(type = "string", format = "binary"))
            @RequestPart("file") MultipartFile file,@RequestParam(required = false) String prompt) {
        try {
            return ResponseEntity.ok(documentAnalysisService.analyze(file,prompt));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage(), e);
        }
    }
}