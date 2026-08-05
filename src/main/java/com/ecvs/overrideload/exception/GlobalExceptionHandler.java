package com.ecvs.overrideload.exception;

import com.ecvs.overrideload.dto.ApiErrorResponse;
import com.ecvs.overrideload.dto.OverrideLoadResponse.ProcessStat;
import com.ecvs.overrideload.dto.OverrideLoadResponse.StageStatistics;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(OverrideLoadException.class)
    public ResponseEntity<ApiErrorResponse> handleOverrideLoadException(
            OverrideLoadException ex, HttpServletRequest request) {
        log.error("Override load failed [{}]: {}", ex.getErrorCode(), ex.getMessage(), ex);
        return ResponseEntity.status(ex.getHttpStatus()).body(ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .error(ex.getMessage())
                .status(ex.getHttpStatus())
                .path(request.getRequestURI())
                .additionalDetails(ex.getAdditionalDetails())
                .processStats(ex.getProcessStats())
                .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .error("Validation failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .additionalDetails(details)
                .processStats(List.of())
                .build());
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDataAccess(
            DataAccessException ex, HttpServletRequest request) {
        log.error("Database connectivity/query failure", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .error("Database connectivity or query failure: " + ex.getMostSpecificCause().getMessage())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .path(request.getRequestURI())
                .additionalDetails(List.of(ErrorCodes.DB_CONNECTIVITY))
                .processStats(List.of(failedLoadStat()))
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception during override load", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .error("Unexpected server error: " + ex.getMessage())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getRequestURI())
                .additionalDetails(List.of(ErrorCodes.UNEXPECTED))
                .processStats(List.of(failedLoadStat()))
                .build());
    }

    private ProcessStat failedLoadStat() {
        Instant now = Instant.now();
        return ProcessStat.builder()
                .stage("LOAD")
                .status("FAILED")
                .startTime(now)
                .endTime(now)
                .statistics(StageStatistics.builder()
                        .totalEntitlementsProcessed(0)
                        .totalEntitlementsSuccessful(0)
                        .newEntitlements(null)
                        .modifiedEntitlements(null)
                        .deletedEntitlements(null)
                        .totalEntitlementsFailed(0)
                        .build())
                .build();
    }
}
