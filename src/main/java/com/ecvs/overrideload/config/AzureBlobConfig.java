package com.ecvs.overrideload.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Azure Blob client. Prefers connection string for local/dev; otherwise uses
 * DefaultAzureCredential (AKS workload identity / managed identity).
 */
@Configuration
public class AzureBlobConfig {

    @Bean
    @ConditionalOnProperty(prefix = "azure.storage", name = "account-name", matchIfMissing = false)
    public BlobServiceClient blobServiceClient(AzureStorageProperties properties) {
        if (!StringUtils.hasText(properties.getAccountName())
                && !StringUtils.hasText(properties.getConnectionString())) {
            throw new IllegalStateException(
                    "Configure azure.storage.account-name (AKS workload identity) "
                            + "or azure.storage.connection-string");
        }

        if (StringUtils.hasText(properties.getConnectionString())) {
            return new BlobServiceClientBuilder()
                    .connectionString(properties.getConnectionString())
                    .buildClient();
        }

        String endpoint = "https://%s.blob.core.windows.net".formatted(properties.getAccountName());
        return new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }
}
