package com.ecvs.overrideload.service;

import java.io.InputStream;

/**
 * Abstraction over Azure Blob download so the load flow can be tested without Azure.
 */
public interface BlobContentProvider {

    InputStream openBlobStream(String containerName, String blobName);

    String resolveBlobName(String blobName);
}
