package com.ecvs.overrideload.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "azure.storage")
public class AzureStorageProperties {

    private String accountName;
    private String containerName = "aai-exception";
    /** Legacy default blob name (optional fallback). */
    private String blobName;
    /** Pattern with one %s for batchId, e.g. "%s.csv" or "override/%s.csv". */
    private String blobNamePattern = "%s.csv";
    /** Optional; when set, used instead of DefaultAzureCredential (local/dev). */
    private String connectionString;
}
