package com.procureflow.catalog.web;

import com.procureflow.catalog.application.DepartmentService;
import com.procureflow.catalog.application.DepartmentView;
import com.procureflow.catalog.web.request.DepartmentRequest;
import com.procureflow.catalog.web.request.UpdateDepartmentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private final DepartmentService service;

    public DepartmentController(DepartmentService service){
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentView create(@Valid @RequestBody DepartmentRequest req){
        return service.create(req.code(),req.name());
    }

    @GetMapping
    public List<DepartmentView> list(){
        return service.list();
    }

    @GetMapping("/{id}")
    public DepartmentView get(@PathVariable UUID id){
        return service.get(id);
    }
    @PutMapping("/{id}")
    public DepartmentView update(@PathVariable UUID id , @Valid @RequestBody UpdateDepartmentRequest req){
        return service.update(id, req.name(), req.active());
    }
}
