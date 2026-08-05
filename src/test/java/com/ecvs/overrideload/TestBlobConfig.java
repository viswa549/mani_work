package com.ecvs.overrideload;

import com.ecvs.overrideload.service.BlobContentProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@TestConfiguration
public class TestBlobConfig {

    @Bean
    @Primary
    BlobContentProvider blobContentProvider() {
        return new BlobContentProvider() {
            @Override
            public ByteArrayInputStream openCsvForBatch(UUID batchId) {
                String csv = """
                        AAI_COID,AAI_CUST_NUM,AAI_LINKED_PROD,AAI_LINKED_ACCOUNT,AAI_LINKED_SUBPC,AAI_LINKED_CUAC,AAI_EXC_ACAC,AAI_STD_ACAC,AAI_CREATE_DATE,AAI_CHANGE_DATE,AAI_DELETED_FLAG
                        96,19116873,DDA,401026748,J7,TRS,WAH,WAA,6/26/2025,6/26/2025,N
                        """;
                return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public String resolveBlobName(UUID batchId) {
                return batchId + ".csv";
            }
        };
    }
}
