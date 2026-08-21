package com.vetsoftware.app.promotion.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.promotion.application.command.CreatePromotionCommand;
import com.vetsoftware.app.promotion.application.command.UpdatePromotionCommand;
import com.vetsoftware.app.promotion.application.dto.CompanySummaryDto;
import com.vetsoftware.app.promotion.application.dto.PromotionDto;
import com.vetsoftware.app.promotion.application.port.in.CreatePromotionUseCase;
import com.vetsoftware.app.promotion.application.port.in.DeletePromotionUseCase;
import com.vetsoftware.app.promotion.application.port.in.FindPromotionUseCase;
import com.vetsoftware.app.promotion.application.port.in.ListPromotionsUseCase;
import com.vetsoftware.app.promotion.application.port.in.UpdatePromotionUseCase;
import com.vetsoftware.app.promotion.domain.ApplicationType;
import com.vetsoftware.app.promotion.domain.PromotionStatus;
import com.vetsoftware.app.promotion.domain.PromotionType;
import com.vetsoftware.app.promotion.domain.ValueType;
import com.vetsoftware.app.promotion.infrastructure.web.request.CreatePromotionRequest;
import com.vetsoftware.app.promotion.infrastructure.web.request.UpdatePromotionRequest;
import com.vetsoftware.app.promotion.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.promotion.infrastructure.web.response.PromotionResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/promotions")
public class PromotionController {
    private final CreatePromotionUseCase createUseCase;
    private final UpdatePromotionUseCase updateUseCase;
    private final FindPromotionUseCase findUseCase;
    private final ListPromotionsUseCase listUseCase;
    private final DeletePromotionUseCase deleteUseCase;
    private final Authz authz;

    public PromotionController(CreatePromotionUseCase createUseCase,
            UpdatePromotionUseCase updateUseCase, FindPromotionUseCase findUseCase,
            ListPromotionsUseCase listUseCase, DeletePromotionUseCase deleteUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromotionResponse create(@Valid @RequestBody CreatePromotionRequest request) {
        return toResponse(createUseCase.execute(new CreatePromotionCommand(request.name(),
                parse(PromotionType.class, request.promotionType(), "promotionType"),
                parse(ApplicationType.class, request.applicationType(), "applicationType"),
                request.applicationItem(), parse(ValueType.class, request.valueType(), "valueType"),
                request.value(), request.startDate(), request.endDate(),
                parse(PromotionStatus.class, request.promotionStatus(), "promotionStatus"),
                authz.currentCompanyId())));
    }

    @GetMapping
    public List<PromotionResponse> listAll() {
        return listUseCase.listByCompany(authz.currentCompanyId()).stream().map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public PromotionResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public PromotionResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdatePromotionRequest request) {
        return toResponse(updateUseCase.execute(new UpdatePromotionCommand(id, request.name(),
                parse(PromotionType.class, request.promotionType(), "promotionType"),
                parse(ApplicationType.class, request.applicationType(), "applicationType"),
                request.applicationItem(), parse(ValueType.class, request.valueType(), "valueType"),
                request.value(), request.startDate(), request.endDate(),
                parse(PromotionStatus.class, request.promotionStatus(), "promotionStatus"),
                authz.currentCompanyId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyIdOrNull());
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid " + field + ": " + value);
        }
    }

    private PromotionResponse toResponse(PromotionDto dto) {
        CompanySummaryDto c = dto.company();
        return new PromotionResponse(dto.id(), dto.name(),
                dto.promotionType() == null ? null : dto.promotionType().name(),
                dto.applicationType() == null ? null : dto.applicationType().name(),
                dto.applicationItem(), dto.valueType() == null ? null : dto.valueType().name(),
                dto.value(), dto.startDate(), dto.endDate(),
                dto.promotionStatus() == null ? null : dto.promotionStatus().name(),
                c == null ? null : new CompanySummary(c.id(), c.name(), c.identifier()),
                dto.createdDate(), dto.enabled());
    }
}
