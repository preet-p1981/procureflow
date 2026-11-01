package com.procureflow.catalog.web;

import com.procureflow.catalog.application.BudgetService;
import com.procureflow.catalog.application.BudgetView;
import com.procureflow.catalog.web.request.CreateBudgetRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {
    private final BudgetService service;

    public BudgetController(BudgetService service){
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetView create(@Valid@RequestBody CreateBudgetRequest req){
        return service.create(req.departmentId(), req.fiscalYear(), req.currency(), req.allocated());

    }
    @GetMapping("/{id}")
    public BudgetView get(@PathVariable UUID id){
        return service.get(id);
    }
    @GetMapping
    public BudgetView find(@RequestParam UUID departmentId,@RequestParam int fiscalYear){
        return service.find(departmentId,fiscalYear);
    }

}
