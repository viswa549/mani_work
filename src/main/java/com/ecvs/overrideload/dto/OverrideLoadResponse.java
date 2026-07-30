package com.ecvs.overrideload.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * API response shaped for Automic job status mapping (ECVS-1240).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OverrideLoadResponse {

    @JsonProperty("job_name")
    private String jobName;

    @JsonProperty("job_status")
    private String jobStatus;

    @JsonProperty("http_status")
    private int httpStatus;

    @JsonProperty("message")
    private String message;

    @JsonProperty("blob_name")
    private String blobName;

    @JsonProperty("started_at")
    private Instant startedAt;

    @JsonProperty("completed_at")
    private Instant completedAt;

    @JsonProperty("total_record_count")
    private long totalRecordCount;

    @JsonProperty("success_count")
    private long successCount;

    @JsonProperty("exception_count")
    private long exceptionCount;

    @JsonProperty("deleted_count")
    private long deletedCount;

    @JsonProperty("error")
    @Builder.Default
    private List<ErrorDetail> error = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {

        @JsonProperty("error_code")
        private String errorCode;

        @JsonProperty("error_description")
        private String errorDescription;
    }
}
