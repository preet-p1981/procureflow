package com.procureflow.vendor.web.request;

import com.procureflow.vendor.application.RegisterVendorHandler;
import com.procureflow.vendor.application.VendorLifecycleHandler;
import com.procureflow.vendor.application.VendorQueryService;
import com.procureflow.vendor.application.VendorView;
import com.procureflow.vendor.domain.VendorStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vendors")
public class VendorController {

    private final RegisterVendorHandler register;
    private final VendorLifecycleHandler lifecycle;
    private final VendorQueryService query;

    public VendorController(RegisterVendorHandler register,VendorLifecycleHandler lifecycle,VendorQueryService query){
        this.register=register;
        this.lifecycle=lifecycle;
        this.query=query;

    }
    //create endpoint
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VendorView create(@Valid @RequestBody RegisterVendorRequest req){
        return register.handle(req.legalName(), req.taxId(),req.contactEmail());
    }
    //to get by id
    @GetMapping("/{id}")
    public VendorView get(@PathVariable UUID id){
        return query.get(id);
    }
    //whole list
    @GetMapping
    public List<VendorView> list(@RequestParam(defaultValue = "PENDING")VendorStatus status){
        return query.listByStatus(status);
    }
    //for approve
    @PostMapping("/{id}/approve")
    public VendorView approve(@PathVariable UUID id){
        return lifecycle.approve(id);
    }
    //for reject
    @PostMapping("/{id}/reject")
    public VendorView reject(@PathVariable UUID id,@Valid@RequestBody ReasonRequest req){
        return lifecycle.reject(id,req.reason());

    }
    @PostMapping("/{id}/suspend")
    public VendorView suspend(@PathVariable UUID id,@Valid@RequestBody ReasonRequest req){
        return lifecycle.suspend(id,req.reason());
    }
    @PostMapping("/{id}/reinstate")
    public VendorView reinstate(@PathVariable UUID id){
        return lifecycle.reinstate(id);
    }

}
