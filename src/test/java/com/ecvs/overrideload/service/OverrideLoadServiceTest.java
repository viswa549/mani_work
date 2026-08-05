package com.ecvs.overrideload.service;

import com.ecvs.overrideload.config.OverrideLoadProperties;
import com.ecvs.overrideload.dto.OverrideLoadRequest;
import com.ecvs.overrideload.dto.OverrideLoadResponse;
import com.ecvs.overrideload.entity.CisException;
import com.ecvs.overrideload.repository.CisExceptionJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OverrideLoadServiceTest {

    @Mock
    private BlobContentProvider blobContentProvider;

    @Mock
    private CisExceptionJdbcRepository jdbcRepository;

    private OverrideLoadService service;

    private final UUID batchId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @BeforeEach
    void setUp() {
        OverrideLoadProperties properties = new OverrideLoadProperties();
        properties.setBatchSize(100);
        properties.setMaxErrorDetails(50);
        properties.setJobName("OVERRIDE_LOAD");
        service = new OverrideLoadService(
                blobContentProvider,
                new AaiExceptionCsvParser(),
                jdbcRepository,
                properties);
    }

    @Test
    void clearsStagingThenJdbcBatchLoadsAndReturnsProcessStats() {
        String csv = """
                AAI_COID,AAI_CUST_NUM,AAI_LINKED_PROD,AAI_LINKED_ACCOUNT,AAI_LINKED_SUBPC,AAI_LINKED_CUAC,AAI_EXC_ACAC,AAI_STD_ACAC,AAI_CREATE_DATE,AAI_CHANGE_DATE,AAI_DELETED_FLAG
                96,19116873,DDA,401026748,J7,TRS,WAH,WAA,6/26/2025,6/26/2025,N
                96,19117386,DDA,9888041127,E5,EXC,WAE,WAA,5/9/2026,5/19/2026,N
                """;

        when(blobContentProvider.openCsvForBatch(batchId))
                .thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        when(jdbcRepository.clearLandingTable()).thenReturn(10);
        when(jdbcRepository.batchInsert(anyList(), anyInt())).thenAnswer(inv -> {
            List<?> rows = inv.getArgument(0);
            return rows.size();
        });

        OverrideLoadResponse response = service.loadOverrides(
                OverrideLoadRequest.builder().batchId(batchId).build());

        assertThat(response.getBatchId()).isEqualTo(batchId);
        assertThat(response.getCurrentStatus()).isEqualTo("SUCCESS");
        assertThat(response.getProcessStats()).hasSize(1);

        OverrideLoadResponse.ProcessStat stat = response.getProcessStats().getFirst();
        assertThat(stat.getStage()).isEqualTo("LOAD");
        assertThat(stat.getStatus()).isEqualTo("SUCCESS");
        assertThat(stat.getStatistics().getTotalEntitlementsProcessed()).isEqualTo(2);
        assertThat(stat.getStatistics().getTotalEntitlementsSuccessful()).isEqualTo(2);
        assertThat(stat.getStatistics().getNewEntitlements()).isEqualTo(2);
        assertThat(stat.getStatistics().getDeletedEntitlements()).isEqualTo(10);
        assertThat(stat.getStatistics().getTotalEntitlementsFailed()).isEqualTo(0);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CisException>> captor = ArgumentCaptor.forClass(List.class);
        verify(jdbcRepository).clearLandingTable();
        verify(jdbcRepository).batchInsert(captor.capture(), anyInt());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().getFirst().getBatchId()).isEqualTo(batchId);
    }

    @Test
    void returnsPartialWhenSomeRowsAreInvalid() {
        String csv = """
                AAI_COID,AAI_CUST_NUM,AAI_LINKED_PROD,AAI_LINKED_ACCOUNT,AAI_LINKED_SUBPC,AAI_LINKED_CUAC,AAI_EXC_ACAC,AAI_STD_ACAC,AAI_CREATE_DATE,AAI_CHANGE_DATE,AAI_DELETED_FLAG
                bad,19116873,DDA,401026748,J7,TRS,WAH,WAA,6/26/2025,6/26/2025,N
                96,19117386,DDA,9888041127,E5,EXC,WAE,WAA,5/9/2026,5/19/2026,N
                """;

        when(blobContentProvider.openCsvForBatch(any()))
                .thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        when(jdbcRepository.clearLandingTable()).thenReturn(0);
        when(jdbcRepository.batchInsert(anyList(), anyInt())).thenAnswer(inv -> {
            List<?> rows = inv.getArgument(0);
            return rows.size();
        });

        OverrideLoadResponse response = service.loadOverrides(
                OverrideLoadRequest.builder().batchId(batchId).build());

        assertThat(response.getCurrentStatus()).isEqualTo("PARTIAL");
        OverrideLoadResponse.ProcessStat stat = response.getProcessStats().getFirst();
        assertThat(stat.getStatus()).isEqualTo("PARTIAL_SUCCESS");
        assertThat(stat.getStatistics().getTotalEntitlementsProcessed()).isEqualTo(2);
        assertThat(stat.getStatistics().getTotalEntitlementsSuccessful()).isEqualTo(1);
        assertThat(stat.getStatistics().getTotalEntitlementsFailed()).isEqualTo(1);
    }
}
