package com.raglite.ingestion.controller;

import com.raglite.ingestion.service.IngestResult;
import com.raglite.ingestion.service.IngestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    public IngestResult ingest(@Valid @RequestBody IngestRequest request) {
        return ingestionService.ingest(request.docId(), request.text());
    }
}
