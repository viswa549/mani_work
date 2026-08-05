package com.ecvs.overrideload.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity for landing.cis_exception — kept for other service requirements.
 * Bulk LOAD path uses {@link com.ecvs.overrideload.repository.CisExceptionJdbcRepository}.
 */
@Entity
@Table(name = "cis_exception", schema = "landing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CisException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "coid", nullable = false)
    private Short coid;

    @Column(name = "customer_number", nullable = false)
    private Long customerNumber;

    @Column(name = "linked_product_code", length = 3)
    private String linkedProductCode;

    @Column(name = "linked_account_number")
    private Long linkedAccountNumber;

    @Column(name = "linked_sub_product_code", length = 2)
    private String linkedSubProductCode;

    @Column(name = "linked_cuac_code", length = 3)
    private String linkedCuacCode;

    @Column(name = "exc_acac", length = 3)
    private String excAcac;

    @Column(name = "std_acac", length = 3)
    private String stdAcac;

    @Column(name = "create_date")
    private LocalDate createDate;

    @Column(name = "change_date")
    private LocalDate changeDate;

    @Column(name = "deleted_flag", length = 1)
    private String deletedFlag;
}
