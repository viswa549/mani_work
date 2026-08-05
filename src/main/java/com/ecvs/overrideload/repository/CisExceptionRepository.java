package com.ecvs.overrideload.repository;

import com.ecvs.overrideload.entity.CisException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository retained for non-bulk requirements (lookups, counts, etc.).
 * Do not use saveAll() for 50k+ CSV loads — use {@link CisExceptionJdbcRepository}.
 */
@Repository
public interface CisExceptionRepository extends JpaRepository<CisException, Long> {

    List<CisException> findByBatchId(UUID batchId);

    long countByBatchId(UUID batchId);

    @Query(value = "SELECT COUNT(*) FROM landing.cis_exception", nativeQuery = true)
    long countAllInLanding();
}
