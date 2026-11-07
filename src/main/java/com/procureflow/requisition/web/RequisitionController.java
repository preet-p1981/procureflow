package com.procureflow.requisition.web;

import com.procureflow.requisition.application.*;
import com.procureflow.requisition.domain.RequisitionStatus;
import com.procureflow.requisition.web.request.CreateRequisitionRequest;
import com.procureflow.requisition.web.request.RequisitionLineRequest;
import com.procureflow.requisition.web.request.UpdateRequisitionRequest;
import com.procureflow.shared.application.PageResult;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/requisitions")
public class RequisitionController {

    private final RequisitionService service;
    private final RequisitionQueryService queries;

    public RequisitionController(RequisitionService service, RequisitionQueryService queries) {
        this.service = service;
        this.queries = queries;
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequisitionView create(@Valid @RequestBody CreateRequisitionRequest req) {
        return service.createDraft(new CreateRequisitionCommand(
                req.departmentId(), req.currency(), req.justification(), toInputs(req.lines())));
    }

    @GetMapping("/{id}")
    public RequisitionView get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public RequisitionView update(@PathVariable UUID id, @Valid @RequestBody UpdateRequisitionRequest req) {
        return service.updateDraft(id, req.justification(), toInputs(req.lines()));
    }

    @PostMapping("/{id}/submit")
    public RequisitionView submit(@PathVariable UUID id) {
        return service.submit(id);
    }

    @PostMapping("/{id}/reopen")
    public RequisitionView reopen(@PathVariable UUID id) {
        return service.reopen(id);
    }

    @PostMapping("/{id}/cancel")
    public RequisitionView cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }

    private static List<RequisitionLineInput> toInputs(List<RequisitionLineRequest> lines) {
        if (lines == null) {
            return List.of();
        }
        return lines.stream()
                .map(l -> new RequisitionLineInput(l.itemId(), l.description(), l.quantity(),
                        l.unitOfMeasure(), l.unitPrice()))
                .toList();
    }
    @GetMapping
    public PageResult<RequisitionSummary> listMine(
            @RequestParam(required = false) RequisitionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return queries.listMine(status, page, size);
    }

    @GetMapping("/all")
    public PageResult<RequisitionSummary> listAll(
            @RequestParam(required = false) UUID requesterId,
            @RequestParam(required = false) RequisitionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return queries.listAll(requesterId, status, page, size);
    }

}