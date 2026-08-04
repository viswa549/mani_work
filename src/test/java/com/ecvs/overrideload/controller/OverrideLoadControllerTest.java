package com.ecvs.overrideload.controller;

import com.ecvs.overrideload.config.OverrideLoadProperties;
import com.ecvs.overrideload.dto.OverrideLoadRequest;
import com.ecvs.overrideload.dto.OverrideLoadResponse;
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

    @Test
    void returnsReconciliationPayloadOnSuccess() throws Exception {
        OverrideLoadResponse response = OverrideLoadResponse.builder()
                .jobName("OVERRIDE_LOAD")
                .jobStatus("SUCCESS")
                .httpStatus(200)
                .message("Override load completed")
                .blobName("AAI Exception File 6-26-26.csv")
                .startedAt(Instant.parse("2026-06-26T12:00:00Z"))
                .completedAt(Instant.parse("2026-06-26T12:00:05Z"))
                .totalRecordCount(30397)
                .successCount(30397)
                .exceptionCount(0)
                .deletedCount(100)
                .error(List.of())
                .build();

        when(overrideLoadService.loadOverrides(any(OverrideLoadRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/override-load")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job_status").value("SUCCESS"))
                .andExpect(jsonPath("$.total_record_count").value(30397))
                .andExpect(jsonPath("$.success_count").value(30397))
                .andExpect(jsonPath("$.exception_count").value(0))
                .andExpect(jsonPath("$.http_status").value(200));
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
                        .content("{\"blobName\":\"missing.csv\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.job_status").value("FAILED"))
                .andExpect(jsonPath("$.error[0].error_code").value(ErrorCodes.BLOB_CONNECTIVITY))
                .andExpect(jsonPath("$.error[0].error_description").value("Azure Blob connectivity failure"));
    }
}
