package com.procureflow.catalog.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataBudgetRepository extends JpaRepository<BudgetEntity, UUID> {

    Optional<BudgetEntity> findByDepartmentIdAndFiscalYear(UUID departmentId, int fiscalYear);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE budget
               SET committed = committed + :amount, updated_at = now(), version = version + 1
             WHERE id = :id AND currency = :currency
               AND allocated - committed - consumed >= :amount
            """, nativeQuery = true)
    int atomicCommit(@Param("id") UUID id, @Param("amount") BigDecimal amount,
                     @Param("currency") String currency);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE budget
               SET committed = committed - :amount, updated_at = now(), version = version + 1
             WHERE id = :id AND currency = :currency AND committed >= :amount
            """, nativeQuery = true)
    int atomicRelease(@Param("id") UUID id, @Param("amount") BigDecimal amount,
                      @Param("currency") String currency);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE budget
               SET committed = committed - :amount, consumed = consumed + :amount,
                   updated_at = now(), version = version + 1
             WHERE id = :id AND currency = :currency AND committed >= :amount
            """, nativeQuery = true)
    int atomicConsume(@Param("id") UUID id, @Param("amount") BigDecimal amount,
                      @Param("currency") String currency);
}