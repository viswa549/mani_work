package com.ecvs.overrideload.service;

import com.ecvs.overrideload.config.OverrideLoadProperties;
import com.ecvs.overrideload.dto.OverrideLoadRequest;
import com.ecvs.overrideload.dto.OverrideLoadResponse;
import com.ecvs.overrideload.dto.OverrideLoadResponse.ProcessStat;
import com.ecvs.overrideload.dto.OverrideLoadResponse.StageStatistics;
import com.ecvs.overrideload.exception.ErrorCodes;
import com.ecvs.overrideload.exception.OverrideLoadException;
import com.ecvs.overrideload.repository.CisExceptionJdbcRepository;
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
import java.util.UUID;

/**
 * ECVS-1240 Override Load:
 * stream CSV from Blob by batchId → validate → JDBC bulk insert into landing.cis_exception.
 * <p>
 * JPA remains available for other requirements; this path uses JDBC batch for throughput.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OverrideLoadService {

    public static final String STAGE_LOAD = "LOAD";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_PARTIAL_SUCCESS = "PARTIAL_SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    private final BlobContentProvider blobContentProvider;
    private final AaiExceptionCsvParser csvParser;
    private final CisExceptionJdbcRepository jdbcRepository;
    private final OverrideLoadProperties loadProperties;

    @Transactional
    public OverrideLoadResponse loadOverrides(OverrideLoadRequest request) {
        UUID batchId = request.getBatchId();
        Instant start = Instant.now();

        int deletedCount = 0;
        int processed = 0;
        int successful = 0;
        int failed = 0;
        List<String> rowErrors = new ArrayList<>();

        try {
            deletedCount = clearExistingData();

            AaiExceptionCsvParser.ParseResult parseResult;
            try (InputStream stream = blobContentProvider.openCsvForBatch(batchId)) {
                parseResult = csvParser.parse(stream, batchId);
            } catch (IOException ex) {
                throw fail(batchId, start, ErrorCodes.CSV_PARSE_FAILURE,
                        "Failed to parse AAI Exception CSV: " + ex.getMessage(),
                        HttpStatus.BAD_REQUEST.value(),
                        deletedCount, 0, 0, 0, List.of(), ex);
            }

            processed = parseResult.getTotalRecordCount();
            failed = parseResult.getErrors().size();
            rowErrors = capErrors(parseResult.getErrors());

            try {
                successful = jdbcRepository.batchInsert(
                        parseResult.getValidRecords(), loadProperties.getBatchSize());
            } catch (DataAccessException ex) {
                throw fail(batchId, start, ErrorCodes.DB_INSERT_FAILURE,
                        "Failed to insert override records: " + rootMessage(ex),
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        deletedCount, processed, 0, failed, rowErrors, ex);
            }

            Instant end = Instant.now();
            String stageStatus = failed == 0 ? STATUS_SUCCESS : STATUS_PARTIAL_SUCCESS;
            String currentStatus = failed == 0 ? STATUS_SUCCESS : STATUS_PARTIAL;

            ProcessStat loadStat = ProcessStat.builder()
                    .stage(STAGE_LOAD)
                    .status(stageStatus)
                    .startTime(start)
                    .endTime(end)
                    .statistics(StageStatistics.builder()
                            .totalEntitlementsProcessed(processed)
                            .totalEntitlementsSuccessful(successful)
                            .newEntitlements(successful)
                            .modifiedEntitlements(null)
                            .deletedEntitlements(deletedCount)
                            .totalEntitlementsFailed(failed)
                            .build())
                    .build();

            log.info("Override load complete batchId={} processed={} ok={} failed={} deleted={}",
                    batchId, processed, successful, failed, deletedCount);

            return OverrideLoadResponse.builder()
                    .batchId(batchId)
                    .currentStatus(currentStatus)
                    .processStats(List.of(loadStat))
                    .build();

        } catch (OverrideLoadException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw fail(batchId, start, ErrorCodes.DB_INSERT_FAILURE,
                    "Database failure during override load: " + rootMessage(ex),
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    deletedCount, processed, successful, failed, rowErrors, ex);
        } catch (RuntimeException ex) {
            throw fail(batchId, start, ErrorCodes.UNEXPECTED,
                    "Unexpected failure during override load: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    deletedCount, processed, successful, failed, rowErrors, ex);
        }
    }

    private int clearExistingData() {
        try {
            return jdbcRepository.clearLandingTable();
        } catch (DataAccessException ex) {
            throw new OverrideLoadException(
                    ErrorCodes.DB_DELETE_FAILURE,
                    "Failed to clear existing landing.cis_exception data: " + rootMessage(ex),
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    ex);
        }
    }

    private OverrideLoadException fail(
            UUID batchId,
            Instant start,
            String errorCode,
            String message,
            int httpStatus,
            int deleted,
            int processed,
            int successful,
            int failed,
            List<String> details,
            Throwable cause) {

        ProcessStat loadStat = ProcessStat.builder()
                .stage(STAGE_LOAD)
                .status(STATUS_FAILED)
                .startTime(start)
                .endTime(Instant.now())
                .statistics(StageStatistics.builder()
                        .totalEntitlementsProcessed(processed)
                        .totalEntitlementsSuccessful(successful)
                        .newEntitlements(null)
                        .modifiedEntitlements(null)
                        .deletedEntitlements(deleted)
                        .totalEntitlementsFailed(failed)
                        .build())
                .build();

        List<String> additional = new ArrayList<>();
        additional.add("batchId=" + batchId);
        additional.add("errorCode=" + errorCode);
        if (details != null) {
            additional.addAll(details);
        }

        return new OverrideLoadException(
                errorCode, message, httpStatus, additional, List.of(loadStat), cause);
    }

    private List<String> capErrors(List<String> errors) {
        int max = Math.max(0, loadProperties.getMaxErrorDetails());
        if (errors.size() <= max) {
            return new ArrayList<>(errors);
        }
        List<String> capped = new ArrayList<>(errors.subList(0, max));
        capped.add("Additional %d row error(s) omitted".formatted(errors.size() - max));
        return capped;
    }

    private String rootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : ex.getMessage();
    }
}
