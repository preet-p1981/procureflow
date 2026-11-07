package com.procureflow.catalog.web;

import com.procureflow.catalog.application.CategoryService;
import com.procureflow.catalog.application.CategoryView;
import com.procureflow.catalog.web.request.CategoryRequest;
import com.procureflow.catalog.web.request.UpdateCategoryRequest;
import jakarta.validation.Valid;
import jakarta.websocket.server.PathParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service){
        this.service =service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryView create(@Valid @RequestBody CategoryRequest req){
        return service.create(req.code(), req.name());
    }
    @GetMapping
    public List<CategoryView> list(){
        return service.list();
    }

    @GetMapping("/{id}")
    public CategoryView get(@PathVariable UUID id){
        return service.get(id);
    }

    @PutMapping("/{id}")
    public CategoryView update(@PathVariable UUID id, @Valid@RequestBody UpdateCategoryRequest req){
        return service.update(id,req.name(), req.active());
    }


}
