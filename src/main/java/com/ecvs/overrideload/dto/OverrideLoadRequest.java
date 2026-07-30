package com.ecvs.overrideload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional request body to override default blob location for a load run.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverrideLoadRequest {

    /** Blob name within the configured container. If blank, uses application default. */
    private String blobName;

    /** Optional container override. */
    private String containerName;
}
