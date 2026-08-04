package com.ecvs.overrideload.exception;

import com.ecvs.overrideload.config.OverrideLoadProperties;
import com.ecvs.overrideload.dto.OverrideLoadResponse;
import com.ecvs.overrideload.dto.OverrideLoadResponse.ErrorDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final OverrideLoadProperties loadProperties;

    @ExceptionHandler(OverrideLoadException.class)
    public ResponseEntity<OverrideLoadResponse> handleOverrideLoadException(OverrideLoadException ex) {
        log.error("Override load failed [{}]: {}", ex.getErrorCode(), ex.getMessage(), ex);
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus(), "FAILED");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<OverrideLoadResponse> handleDataAccess(DataAccessException ex) {
        log.error("Database connectivity/query failure", ex);
        return buildErrorResponse(
                ErrorCodes.DB_CONNECTIVITY,
                "Database connectivity or query failure: " + ex.getMostSpecificCause().getMessage(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "FAILED");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<OverrideLoadResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception during override load", ex);
        return buildErrorResponse(
                ErrorCodes.UNEXPECTED,
                "Unexpected server error: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "FAILED");
    }

    private ResponseEntity<OverrideLoadResponse> buildErrorResponse(
            String errorCode, String description, int httpStatus, String jobStatus) {

        OverrideLoadResponse body = OverrideLoadResponse.builder()
                .jobName(loadProperties.getJobName())
                .jobStatus(jobStatus)
                .httpStatus(httpStatus)
                .message(description)
                .startedAt(Instant.now())
                .completedAt(Instant.now())
                .totalRecordCount(0)
                .successCount(0)
                .exceptionCount(1)
                .deletedCount(0)
                .error(List.of(ErrorDetail.builder()
                        .errorCode(errorCode)
                        .errorDescription(description)
                        .build()))
                .build();

        return ResponseEntity.status(httpStatus).body(body);
    }
}
