package com.ecvs.overrideload.service;

import com.ecvs.overrideload.entity.CisException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AaiExceptionCsvParserTest {

    private AaiExceptionCsvParser parser;
    private final UUID batchId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @BeforeEach
    void setUp() {
        parser = new AaiExceptionCsvParser();
    }

    @Test
    void parsesValidAaiExceptionRows() throws Exception {
        String csv = """
                AAI_COID,AAI_CUST_NUM,AAI_LINKED_PROD,AAI_LINKED_ACCOUNT,AAI_LINKED_SUBPC,AAI_LINKED_CUAC,AAI_EXC_ACAC,AAI_STD_ACAC,AAI_CREATE_DATE,AAI_CHANGE_DATE,AAI_DELETED_FLAG
                96,19116873,DDA,00000000401026748,J7,TRS,WAH,WAA,6/26/2025,6/26/2025,N
                96,19117386,DDA,9888041127,E5,EXC,WAE,WAA,5/9/2026,5/19/2026,
                """;

        AaiExceptionCsvParser.ParseResult result = parser.parse(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), batchId);

        assertThat(result.getTotalRecordCount()).isEqualTo(2);
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValidRecords()).hasSize(2);

        CisException first = result.getValidRecords().getFirst();
        assertThat(first.getBatchId()).isEqualTo(batchId);
        assertThat(first.getCoid()).isEqualTo((short) 96);
        assertThat(first.getCustomerNumber()).isEqualTo(19116873L);
        assertThat(first.getLinkedProductCode()).isEqualTo("DDA");
        assertThat(first.getLinkedAccountNumber()).isEqualTo(401026748L);
        assertThat(first.getLinkedSubProductCode()).isEqualTo("J7");
        assertThat(first.getLinkedCuacCode()).isEqualTo("TRS");
        assertThat(first.getExcAcac()).isEqualTo("WAH");
        assertThat(first.getStdAcac()).isEqualTo("WAA");
        assertThat(first.getCreateDate()).isEqualTo(LocalDate.of(2025, 6, 26));
        assertThat(first.getDeletedFlag()).isEqualTo("N");
    }

    @Test
    void collectsRowLevelExceptionsForInvalidData() throws Exception {
        String csv = """
                AAI_COID,AAI_CUST_NUM,AAI_LINKED_PROD,AAI_LINKED_ACCOUNT,AAI_LINKED_SUBPC,AAI_LINKED_CUAC,AAI_EXC_ACAC,AAI_STD_ACAC,AAI_CREATE_DATE,AAI_CHANGE_DATE,AAI_DELETED_FLAG
                XX,19116873,DDA,401026748,J7,TRS,WAH,WAA,6/26/2025,6/26/2025,N
                96,19117386,DDA,9888041127,E5,EXC,WAE,WAA,5/9/2026,5/19/2026,N
                """;

        AaiExceptionCsvParser.ParseResult result = parser.parse(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), batchId);

        assertThat(result.getTotalRecordCount()).isEqualTo(2);
        assertThat(result.getValidRecords()).hasSize(1);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().getFirst()).contains("OL-CSV-002");
    }
}
