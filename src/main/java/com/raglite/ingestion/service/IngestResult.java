package com.raglite.ingestion.service;

public record IngestResult(
        String docId,
        int chunksStored,
        int chunksFailed
) {
}
