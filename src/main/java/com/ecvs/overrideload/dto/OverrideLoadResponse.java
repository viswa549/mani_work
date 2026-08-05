package com.ecvs.overrideload.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sync/async-compatible response for override-load.
 *
 * currentStatus: SUCCESS | IN_PROGRESS | FAILED | PARTIAL
 * processStats[].status: SUCCESS | IN_PROGRESS | FAILED | PARTIAL_SUCCESS
 * processStats[].stage: EXTRACT | LOAD | ARCHIVE | TRANSFORM
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OverrideLoadResponse {

    private UUID batchId;
    private String currentStatus;

    @Builder.Default
    private List<ProcessStat> processStats = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProcessStat {
        private String stage;
        private String status;
        private Instant startTime;
        private Instant endTime;
        private StageStatistics statistics;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class StageStatistics {
        private Integer totalEntitlementsProcessed;
        private Integer totalEntitlementsSuccessful;
        private Integer newEntitlements;
        private Integer modifiedEntitlements;
        private Integer deletedEntitlements;
        private Integer totalEntitlementsFailed;
    }
}
