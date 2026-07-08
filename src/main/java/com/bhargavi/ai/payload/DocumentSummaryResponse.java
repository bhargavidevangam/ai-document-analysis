package com.bhargavi.ai.payload;

public record DocumentSummaryResponse(
        String fileName,
        String contentType,
        String summary,
        int wordCount) {
}
