package com.procureflow.catalog.infrastructure.persistence;

import com.procureflow.catalog.application.DepartmentView;
import com.procureflow.catalog.application.port.DepartmentStore;
import com.procureflow.catalog.domain.CatalogError;
import com.procureflow.shared.domain.CommonError;
import com.procureflow.shared.domain.DomainException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaDepartmentStore implements DepartmentStore {

    private final SpringDataDepartmentRepository jpa;

    JpaDepartmentStore(SpringDataDepartmentRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCodeIgnoreCase(code);
    }

    @Override
    public Optional<DepartmentView> findById(UUID id) {
        return jpa.findById(id).map(JpaDepartmentStore::view);
    }

    @Override
    public List<DepartmentView> findAll() {
        return jpa.findAllByOrderByCode().stream().map(JpaDepartmentStore::view).toList();
    }

    @Override
    @Transactional
    public DepartmentView insert(String code, String name) {
        try {
            return view(jpa.saveAndFlush(new DepartmentEntity(UUID.randomUUID(), code, name)));
        } catch (DataIntegrityViolationException e) {
            throw new DomainException(CatalogError.DEPARTMENT_CODE_EXISTS, "Department code already exists");
        }
    }

    @Override
    @Transactional
    public DepartmentView update(UUID id, String name, boolean active) {
        DepartmentEntity e = jpa.findById(id).orElseThrow(() ->
                new DomainException(CatalogError.DEPARTMENT_NOT_FOUND, "Department not found"));
        e.change(name, active);
        try {
            return view(jpa.saveAndFlush(e));
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new DomainException(CommonError.CONFLICT, "Department was modified by someone else, reload and retry");
        }
    }

    private static DepartmentView view(DepartmentEntity e) {
        return new DepartmentView(e.getId().toString(), e.getCode(), e.getName(), e.isActive());
    }
}