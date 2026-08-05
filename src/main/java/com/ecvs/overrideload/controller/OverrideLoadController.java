package com.ecvs.overrideload.controller;

import com.ecvs.overrideload.dto.OverrideLoadRequest;
import com.ecvs.overrideload.dto.OverrideLoadResponse;
import com.ecvs.overrideload.service.OverrideLoadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /api/v1/override-load
 * Reads CSV from Azure Blob by batchId and JDBC-bulk-loads into Postgres staging.
 */
@RestController
@RequestMapping("/api/v1/override-load")
@RequiredArgsConstructor
public class OverrideLoadController {

    private final OverrideLoadService overrideLoadService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OverrideLoadResponse> loadOverrides(
            @Valid @RequestBody OverrideLoadRequest request) {

        OverrideLoadResponse response = overrideLoadService.loadOverrides(request);
        return ResponseEntity.ok(response);
    }
}
