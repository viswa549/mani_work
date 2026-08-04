package com.ecvs.overrideload.service;

import com.ecvs.overrideload.config.OverrideLoadProperties;
import com.ecvs.overrideload.dto.OverrideLoadRequest;
import com.ecvs.overrideload.dto.OverrideLoadResponse;
import com.ecvs.overrideload.entity.CisException;
import com.ecvs.overrideload.repository.CisExceptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OverrideLoadServiceTest {

    @Mock
    private BlobContentProvider blobContentProvider;

    @Mock
    private CisExceptionRepository cisExceptionRepository;

    private OverrideLoadService service;

    @BeforeEach
    void setUp() {
        OverrideLoadProperties properties = new OverrideLoadProperties();
        properties.setBatchSize(100);
        properties.setJobName("OVERRIDE_LOAD");
        service = new OverrideLoadService(
                blobContentProvider,
                new AaiExceptionCsvParser(),
                cisExceptionRepository,
                properties);
    }

    @Test
    void deletesExistingDataThenLoadsCsvAndReturnsReconciliation() {
        String csv = """
                AAI_COID,AAI_CUST_NUM,AAI_LINKED_PROD,AAI_LINKED_ACCOUNT,AAI_LINKED_SUBPC,AAI_LINKED_CUAC,AAI_EXC_ACAC,AAI_STD_ACAC,AAI_CREATE_DATE,AAI_CHANGE_DATE,AAI_DELETED_FLAG
                96,19116873,DDA,401026748,J7,TRS,WAH,WAA,6/26/2025,6/26/2025,N
                96,19117386,DDA,9888041127,E5,EXC,WAE,WAA,5/9/2026,5/19/2026,N
                """;

        when(blobContentProvider.resolveBlobName(any())).thenReturn("AAI Exception File 6-26-26.csv");
        when(blobContentProvider.openBlobStream(isNull(), anyString()))
                .thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        when(cisExceptionRepository.deleteAllInLanding()).thenReturn(10);
        when(cisExceptionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        OverrideLoadResponse response = service.loadOverrides(new OverrideLoadRequest());

        assertThat(response.getJobStatus()).isEqualTo("SUCCESS");
        assertThat(response.getHttpStatus()).isEqualTo(200);
        assertThat(response.getDeletedCount()).isEqualTo(10);
        assertThat(response.getTotalRecordCount()).isEqualTo(2);
        assertThat(response.getSuccessCount()).isEqualTo(2);
        assertThat(response.getExceptionCount()).isEqualTo(0);
        assertThat(response.getError()).isEmpty();
        assertThat(response.getJobName()).isEqualTo("OVERRIDE_LOAD");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CisException>> captor = ArgumentCaptor.forClass(List.class);
        verify(cisExceptionRepository).deleteAllInLanding();
        verify(cisExceptionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void returnsPartialSuccessWhenSomeRowsAreInvalid() {
        String csv = """
                AAI_COID,AAI_CUST_NUM,AAI_LINKED_PROD,AAI_LINKED_ACCOUNT,AAI_LINKED_SUBPC,AAI_LINKED_CUAC,AAI_EXC_ACAC,AAI_STD_ACAC,AAI_CREATE_DATE,AAI_CHANGE_DATE,AAI_DELETED_FLAG
                bad,19116873,DDA,401026748,J7,TRS,WAH,WAA,6/26/2025,6/26/2025,N
                96,19117386,DDA,9888041127,E5,EXC,WAE,WAA,5/9/2026,5/19/2026,N
                """;

        when(blobContentProvider.resolveBlobName(any())).thenReturn("file.csv");
        when(blobContentProvider.openBlobStream(isNull(), anyString()))
                .thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        when(cisExceptionRepository.deleteAllInLanding()).thenReturn(0);
        when(cisExceptionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        OverrideLoadResponse response = service.loadOverrides(null);

        assertThat(response.getJobStatus()).isEqualTo("PARTIAL_SUCCESS");
        assertThat(response.getHttpStatus()).isEqualTo(207);
        assertThat(response.getTotalRecordCount()).isEqualTo(2);
        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getExceptionCount()).isEqualTo(1);
        assertThat(response.getError()).hasSize(1);
    }
}
