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
 * ECVS-1240 Override Load API.
 * <p>
 * Deletes existing landing.cis_exception rows and reloads from AAI Exception CSV in Blob storage.
 */
@RestController
@RequestMapping("/api/v1/override-load")
@RequiredArgsConstructor
public class OverrideLoadController {

    private final OverrideLoadService overrideLoadService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OverrideLoadResponse> loadOverrides(
            @Valid @RequestBody(required = false) OverrideLoadRequest request) {

        OverrideLoadResponse response = overrideLoadService.loadOverrides(
                request != null ? request : new OverrideLoadRequest());

        return ResponseEntity.status(response.getHttpStatus()).body(response);
    }
}
