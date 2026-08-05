package com.ecvs.overrideload.repository;

import com.ecvs.overrideload.entity.CisException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;

/**
 * JDBC bulk writer for override-load (50k+ rows).
 * Coexists with JPA — same DataSource / transaction manager.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class CisExceptionJdbcRepository {

    private static final String COUNT_SQL = "SELECT COUNT(*) FROM landing.cis_exception";

    private static final String TRUNCATE_SQL =
            "TRUNCATE TABLE landing.cis_exception RESTART IDENTITY";

    private static final String TRUNCATE_FALLBACK_SQL =
            "TRUNCATE TABLE landing.cis_exception";

    private static final String DELETE_SQL = "DELETE FROM landing.cis_exception";

    private static final String INSERT_SQL = """
            INSERT INTO landing.cis_exception (
                batch_id,
                coid,
                customer_number,
                linked_product_code,
                linked_account_number,
                linked_sub_product_code,
                linked_cuac_code,
                exc_acac,
                std_acac,
                create_date,
                change_date,
                deleted_flag
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * Counts existing rows, then truncates staging. Falls back to DELETE when needed (H2).
     *
     * @return previous row count (maps to deletedEntitlements in processStats)
     */
    public int clearLandingTable() {
        Integer previous = jdbcTemplate.queryForObject(COUNT_SQL, Integer.class);
        int previousCount = previous == null ? 0 : previous;

        try {
            jdbcTemplate.execute(TRUNCATE_SQL);
        } catch (Exception truncateEx) {
            log.warn("TRUNCATE RESTART IDENTITY failed, trying plain TRUNCATE: {}", truncateEx.getMessage());
            try {
                jdbcTemplate.execute(TRUNCATE_FALLBACK_SQL);
            } catch (Exception fallbackEx) {
                log.warn("TRUNCATE unsupported, falling back to DELETE: {}", fallbackEx.getMessage());
                jdbcTemplate.update(DELETE_SQL);
            }
        }

        log.info("Cleared landing.cis_exception (previous rows={})", previousCount);
        return previousCount;
    }

    public int batchInsert(List<CisException> records, int batchSize) {
        if (records == null || records.isEmpty()) {
            return 0;
        }

        int size = Math.max(1, batchSize);
        jdbcTemplate.batchUpdate(INSERT_SQL, records, size, this::bindRow);
        log.info("JDBC batch-inserted {} rows (batchSize={})", records.size(), size);
        return records.size();
    }

    private void bindRow(PreparedStatement ps, CisException row) throws java.sql.SQLException {
        if (row.getBatchId() == null) {
            ps.setNull(1, Types.OTHER);
        } else {
            ps.setObject(1, row.getBatchId());
        }
        ps.setObject(2, row.getCoid(), Types.SMALLINT);
        ps.setObject(3, row.getCustomerNumber(), Types.BIGINT);
        setString(ps, 4, row.getLinkedProductCode());
        setLong(ps, 5, row.getLinkedAccountNumber());
        setString(ps, 6, row.getLinkedSubProductCode());
        setString(ps, 7, row.getLinkedCuacCode());
        setString(ps, 8, row.getExcAcac());
        setString(ps, 9, row.getStdAcac());
        setDate(ps, 10, row.getCreateDate());
        setDate(ps, 11, row.getChangeDate());
        setString(ps, 12, row.getDeletedFlag());
    }

    private static void setString(PreparedStatement ps, int idx, String value) throws java.sql.SQLException {
        if (value == null) {
            ps.setNull(idx, Types.VARCHAR);
        } else {
            ps.setString(idx, value);
        }
    }

    private static void setLong(PreparedStatement ps, int idx, Long value) throws java.sql.SQLException {
        if (value == null) {
            ps.setNull(idx, Types.BIGINT);
        } else {
            ps.setLong(idx, value);
        }
    }

    private static void setDate(PreparedStatement ps, int idx, java.time.LocalDate value)
            throws java.sql.SQLException {
        if (value == null) {
            ps.setNull(idx, Types.DATE);
        } else {
            ps.setDate(idx, Date.valueOf(value));
        }
    }
}
