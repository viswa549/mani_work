package com.ecvs.overrideload.service;

import com.ecvs.overrideload.config.OverrideLoadProperties;
import com.ecvs.overrideload.dto.OverrideLoadRequest;
import com.ecvs.overrideload.dto.OverrideLoadResponse;
import com.ecvs.overrideload.dto.OverrideLoadResponse.ErrorDetail;
import com.ecvs.overrideload.entity.CisException;
import com.ecvs.overrideload.exception.ErrorCodes;
import com.ecvs.overrideload.exception.OverrideLoadException;
import com.ecvs.overrideload.repository.CisExceptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * ECVS-1240: delete landing.cis_exception, then load AAI Exception CSV from Blob.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OverrideLoadService {

    private final BlobContentProvider blobStorageService;
    private final AaiExceptionCsvParser csvParser;
    private final CisExceptionRepository cisExceptionRepository;
    private final OverrideLoadProperties loadProperties;

    @Transactional
    public OverrideLoadResponse loadOverrides(OverrideLoadRequest request) {
        Instant startedAt = Instant.now();
        String blobName = blobStorageService.resolveBlobName(
                request != null ? request.getBlobName() : null);
        String containerName = request != null ? request.getContainerName() : null;

        List<ErrorDetail> errors = new ArrayList<>();
        long deletedCount = 0;
        long successCount = 0;
        long totalRecordCount = 0;
        long exceptionCount = 0;

        try {
            deletedCount = deleteExistingData();

            AaiExceptionCsvParser.ParseResult parseResult;
            try (InputStream stream = blobStorageService.openBlobStream(containerName, blobName)) {
                parseResult = csvParser.parse(stream);
            } catch (IOException ex) {
                throw new OverrideLoadException(
                        ErrorCodes.CSV_PARSE_FAILURE,
                        "Failed to parse AAI Exception CSV: " + ex.getMessage(),
                        HttpStatus.BAD_REQUEST.value(),
                        ex);
            }

            totalRecordCount = parseResult.getTotalRecordCount();
            errors.addAll(parseResult.getErrors());
            exceptionCount = parseResult.getErrors().size();

            successCount = persistInBatches(parseResult.getValidRecords());

            String jobStatus = exceptionCount == 0 ? "SUCCESS" : "PARTIAL_SUCCESS";
            int httpStatus = exceptionCount == 0
                    ? HttpStatus.OK.value()
                    : HttpStatus.MULTI_STATUS.value();

            return OverrideLoadResponse.builder()
                    .jobName(loadProperties.getJobName())
                    .jobStatus(jobStatus)
                    .httpStatus(httpStatus)
                    .message("Override load completed")
                    .blobName(blobName)
                    .startedAt(startedAt)
                    .completedAt(Instant.now())
                    .totalRecordCount(totalRecordCount)
                    .successCount(successCount)
                    .exceptionCount(exceptionCount)
                    .deletedCount(deletedCount)
                    .error(errors)
                    .build();

        } catch (OverrideLoadException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new OverrideLoadException(
                    ErrorCodes.DB_INSERT_FAILURE,
                    "Database failure during override load: " + rootMessage(ex),
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    ex);
        } catch (RuntimeException ex) {
            throw new OverrideLoadException(
                    ErrorCodes.UNEXPECTED,
                    "Unexpected failure during override load: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    ex);
        }
    }

    private int deleteExistingData() {
        try {
            int deleted = cisExceptionRepository.deleteAllInLanding();
            log.info("Deleted {} existing rows from landing.cis_exception", deleted);
            return deleted;
        } catch (DataAccessException ex) {
            throw new OverrideLoadException(
                    ErrorCodes.DB_DELETE_FAILURE,
                    "Failed to delete existing landing.cis_exception data: " + rootMessage(ex),
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    ex);
        }
    }

    private long persistInBatches(List<CisException> records) {
        if (records.isEmpty()) {
            return 0;
        }

        int batchSize = Math.max(1, loadProperties.getBatchSize());
        long saved = 0;
        try {
            for (int i = 0; i < records.size(); i += batchSize) {
                int end = Math.min(i + batchSize, records.size());
                List<CisException> batch = records.subList(i, end);
                cisExceptionRepository.saveAll(batch);
                cisExceptionRepository.flush();
                saved += batch.size();
            }
            log.info("Persisted {} cis_exception rows", saved);
            return saved;
        } catch (DataAccessException ex) {
            throw new OverrideLoadException(
                    ErrorCodes.DB_INSERT_FAILURE,
                    "Failed to insert override records: " + rootMessage(ex),
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    ex);
        }
    }

    private String rootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : ex.getMessage();
    }
}
