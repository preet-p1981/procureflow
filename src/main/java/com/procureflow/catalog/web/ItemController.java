package com.procureflow.catalog.web;

import com.procureflow.catalog.application.ItemDetails;
import com.procureflow.catalog.application.ItemService;
import com.procureflow.catalog.application.ItemView;
import com.procureflow.catalog.web.request.CreateItemRequest;
import com.procureflow.catalog.web.request.UpdateItemRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    //create endpoint to create
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemView create(@Valid @RequestBody CreateItemRequest r) {
        return service.create(r.sku(),
                new ItemDetails(r.name(), r.categoryId(), r.unitOfMeasure(), r.standardPrice(), r.currency()));
    }

    @GetMapping
    public List<ItemView> list(@RequestParam(required = false) UUID categoryId) {
        return service.list(categoryId);
    }

    @GetMapping("/{id}")
    public ItemView get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public ItemView update(@PathVariable UUID id, @Valid @RequestBody UpdateItemRequest r) {
        return service.update(id,
                new ItemDetails(r.name(), r.categoryId(), r.unitOfMeasure(), r.standardPrice(), r.currency()),
                r.active());
    }
}