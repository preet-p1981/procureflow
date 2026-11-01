package com.procureflow.catalog.application.port;

import com.procureflow.catalog.application.DepartmentView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentStore {
    boolean existsByCode(String code);
    Optional<DepartmentView> findById(UUID id);
    List<DepartmentView> findAll();
    DepartmentView insert(String code,String name);
    DepartmentView update(UUID id,String name,boolean active);
}
