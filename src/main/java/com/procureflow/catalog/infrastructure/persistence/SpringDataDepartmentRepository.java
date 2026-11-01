package com.procureflow.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataDepartmentRepository extends JpaRepository<DepartmentEntity, UUID> {
    boolean existsByCodeIgnoreCase(String code);
    List<DepartmentEntity> findAllByOrderByCode();

}
