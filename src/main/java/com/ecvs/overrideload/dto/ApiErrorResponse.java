package com.ecvs.overrideload.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Error response shape for 4xx/5xx override-load failures. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private Instant timestamp;
    private String error;
    private int status;
    private String path;

    @Builder.Default
    private List<String> additionalDetails = new ArrayList<>();

    @Builder.Default
    private List<OverrideLoadResponse.ProcessStat> processStats = new ArrayList<>();
}
