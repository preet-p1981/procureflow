package com.procureflow.catalog.application;

import com.procureflow.catalog.application.port.DepartmentStore;
import com.procureflow.catalog.domain.CatalogError;
import com.procureflow.shared.domain.DomainException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DepartmentService {

    private final DepartmentStore store;

    public DepartmentService(DepartmentStore store){
        this.store = store;
    }

    @Transactional
    @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
    public DepartmentView create(String code ,String name){
        String normalized = code.trim().toUpperCase();
        if(store.existsByCode(normalized)){
            throw new DomainException(CatalogError.DEPARTMENT_CODE_EXISTS,"Department code already exist");

        }
        return store.insert(normalized,name.trim());
    }

    @Transactional
    @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
    public DepartmentView update(UUID id, String name, boolean active){
        return store.update(id,name.trim(),active);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CATALOG_READ')")
    public DepartmentView get(UUID id){
        return store.findById(id).orElseThrow(()->
                new DomainException(CatalogError.DEPARTMENT_NOT_FOUND,"Department not found"));
    }
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CATALOG_READ')")
    public List<DepartmentView> list(){
        return store.findAll();
    }

}
