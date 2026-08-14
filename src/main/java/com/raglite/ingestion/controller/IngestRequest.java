package com.raglite.ingestion.controller;

import jakarta.validation.constraints.NotBlank;

public record IngestRequest(
        @NotBlank String docId,
        @NotBlank String text
) {
}
