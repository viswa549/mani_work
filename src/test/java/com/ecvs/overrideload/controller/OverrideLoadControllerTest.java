package com.ecvs.overrideload.controller;

import com.ecvs.overrideload.config.OverrideLoadProperties;
import com.ecvs.overrideload.dto.OverrideLoadRequest;
import com.ecvs.overrideload.dto.OverrideLoadResponse;
import com.ecvs.overrideload.dto.OverrideLoadResponse.ProcessStat;
import com.ecvs.overrideload.dto.OverrideLoadResponse.StageStatistics;
import com.ecvs.overrideload.exception.ErrorCodes;
import com.ecvs.overrideload.exception.GlobalExceptionHandler;
import com.ecvs.overrideload.exception.OverrideLoadException;
import com.ecvs.overrideload.service.OverrideLoadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OverrideLoadController.class)
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(OverrideLoadProperties.class)
class OverrideLoadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OverrideLoadService overrideLoadService;

    private final UUID batchId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @Test
    void returnsProcessStatsOnSuccess() throws Exception {
        OverrideLoadResponse response = OverrideLoadResponse.builder()
                .batchId(batchId)
                .currentStatus("SUCCESS")
                .processStats(List.of(ProcessStat.builder()
                        .stage("LOAD")
                        .status("SUCCESS")
                        .startTime(Instant.parse("2026-06-30T12:00:00Z"))
                        .endTime(Instant.parse("2026-06-30T12:00:05Z"))
                        .statistics(StageStatistics.builder()
                                .totalEntitlementsProcessed(30397)
                                .totalEntitlementsSuccessful(30397)
                                .newEntitlements(30397)
                                .modifiedEntitlements(null)
                                .deletedEntitlements(100)
                                .totalEntitlementsFailed(0)
                                .build())
                        .build()))
                .build();

        when(overrideLoadService.loadOverrides(any(OverrideLoadRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/override-load")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value(batchId.toString()))
                .andExpect(jsonPath("$.currentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.processStats[0].stage").value("LOAD"))
                .andExpect(jsonPath("$.processStats[0].statistics.totalEntitlementsProcessed").value(30397))
                .andExpect(jsonPath("$.processStats[0].statistics.totalEntitlementsSuccessful").value(30397))
                .andExpect(jsonPath("$.processStats[0].statistics.totalEntitlementsFailed").value(0));
    }

    @Test
    void returnsStructuredErrorWhenBlobUnavailable() throws Exception {
        when(overrideLoadService.loadOverrides(any()))
                .thenThrow(new OverrideLoadException(
                        ErrorCodes.BLOB_CONNECTIVITY,
                        "Azure Blob connectivity failure",
                        HttpStatus.SERVICE_UNAVAILABLE.value()));

        mockMvc.perform(post("/api/v1/override-load")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Azure Blob connectivity failure"))
                .andExpect(jsonPath("$.path").value("/api/v1/override-load"));
    }

    @Test
    void returnsBadRequestWhenBatchIdMissing() throws Exception {
        mockMvc.perform(post("/api/v1/override-load")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation failed"));
    }
}
