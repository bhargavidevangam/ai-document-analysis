package com.bhargavi.ai.service;

import org.springframework.web.multipart.MultipartFile;

import com.bhargavi.ai.payload.DocumentSummaryResponse;

public interface DocumentAnalysisService {

    DocumentSummaryResponse analyze(MultipartFile file,String prompt);
}
