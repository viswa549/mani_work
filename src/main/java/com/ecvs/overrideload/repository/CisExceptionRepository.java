package com.ecvs.overrideload.repository;

import com.ecvs.overrideload.entity.CisException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CisExceptionRepository extends JpaRepository<CisException, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM landing.cis_exception", nativeQuery = true)
    int deleteAllInLanding();
}
