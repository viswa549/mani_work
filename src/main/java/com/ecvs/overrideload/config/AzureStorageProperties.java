package com.ecvs.overrideload.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "azure.storage")
public class AzureStorageProperties {

    private String accountName;
    private String containerName;
    private String blobName;
    /** Optional; when set, used instead of DefaultAzureCredential (local/dev). */
    private String connectionString;
}
