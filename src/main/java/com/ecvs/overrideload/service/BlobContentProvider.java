package com.ecvs.overrideload.service;

import java.io.InputStream;
import java.util.UUID;

/**
 * Abstraction over Azure Blob download so the load flow can be tested without Azure.
 */
public interface BlobContentProvider {

    InputStream openCsvForBatch(UUID batchId);

    String resolveBlobName(UUID batchId);
}
