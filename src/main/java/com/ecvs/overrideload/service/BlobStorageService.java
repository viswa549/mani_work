package com.ecvs.overrideload.service;

import com.azure.core.exception.AzureException;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.ecvs.overrideload.config.AzureStorageProperties;
import com.ecvs.overrideload.exception.ErrorCodes;
import com.ecvs.overrideload.exception.OverrideLoadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Service
@ConditionalOnBean(BlobServiceClient.class)
@RequiredArgsConstructor
@Slf4j
public class BlobStorageService implements BlobContentProvider {

    private final BlobServiceClient blobServiceClient;
    private final AzureStorageProperties storageProperties;

    @Override
    public InputStream openCsvForBatch(UUID batchId) {
        String blobName = resolveBlobName(batchId);
        String container = storageProperties.getContainerName();

        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(container);
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            if (!blobClient.exists()) {
                throw new OverrideLoadException(
                        ErrorCodes.BLOB_NOT_FOUND,
                        "Blob not found: container=%s, blob=%s".formatted(container, blobName),
                        HttpStatus.NOT_FOUND.value());
            }

            log.info("Streaming blob {}/{} for batchId={}", container, blobName, batchId);
            return blobClient.openInputStream();
        } catch (OverrideLoadException ex) {
            throw ex;
        } catch (BlobStorageException ex) {
            if (ex.getStatusCode() == 404) {
                throw new OverrideLoadException(
                        ErrorCodes.BLOB_NOT_FOUND,
                        "Blob not found: container=%s, blob=%s".formatted(container, blobName),
                        HttpStatus.NOT_FOUND.value(),
                        ex);
            }
            throw new OverrideLoadException(
                    ErrorCodes.BLOB_READ_FAILURE,
                    "Failed to read blob %s/%s: %s".formatted(container, blobName, ex.getMessage()),
                    HttpStatus.BAD_GATEWAY.value(),
                    ex);
        } catch (AzureException | IllegalArgumentException | IllegalStateException ex) {
            throw new OverrideLoadException(
                    ErrorCodes.BLOB_CONNECTIVITY,
                    "Azure Blob connectivity failure: " + ex.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    ex);
        }
    }

    @Override
    public String resolveBlobName(UUID batchId) {
        return storageProperties.getBlobNamePattern().formatted(batchId);
    }
}
