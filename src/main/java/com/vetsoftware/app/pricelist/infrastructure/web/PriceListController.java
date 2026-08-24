package com.vetsoftware.app.pricelist.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.pricelist.application.command.CreatePriceListCommand;
import com.vetsoftware.app.pricelist.application.command.PublishPriceListCommand;
import com.vetsoftware.app.pricelist.application.command.UpdatePriceListCommand;
import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import com.vetsoftware.app.pricelist.application.port.in.ArchivePriceListUseCase;
import com.vetsoftware.app.pricelist.application.port.in.CreatePriceListUseCase;
import com.vetsoftware.app.pricelist.application.port.in.DeletePriceListUseCase;
import com.vetsoftware.app.pricelist.application.port.in.FindPriceListUseCase;
import com.vetsoftware.app.pricelist.application.port.in.ListPriceListsUseCase;
import com.vetsoftware.app.pricelist.application.port.in.PublishPriceListUseCase;
import com.vetsoftware.app.pricelist.application.port.in.ReactivatePriceListUseCase;
import com.vetsoftware.app.pricelist.application.port.in.UpdatePriceListUseCase;
import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import com.vetsoftware.app.pricelist.infrastructure.web.request.CreatePriceListRequest;
import com.vetsoftware.app.pricelist.infrastructure.web.request.UpdatePriceListRequest;
import com.vetsoftware.app.pricelist.infrastructure.web.response.PriceListResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Consola de plataforma. Ningun endpoint recibe ni deriva {@code companyId}: la
 * tarifa es global y todos sus puertos van cerrados a
 * {@code hasRole('SYSTEM')}.
 *
 * <p>
 * El unico dato de identidad que este controller sella es la firma de la
 * publicacion, y la toma del principal con {@code authz.currentSystemUserId()}
 * -nunca del cuerpo-.
 */
@RestController
@RequestMapping("/price-lists")
public class PriceListController {

    private final CreatePriceListUseCase createUseCase;
    private final UpdatePriceListUseCase updateUseCase;
    private final FindPriceListUseCase findUseCase;
    private final ListPriceListsUseCase listUseCase;
    private final DeletePriceListUseCase deleteUseCase;
    private final PublishPriceListUseCase publishUseCase;
    private final ArchivePriceListUseCase archiveUseCase;
    private final ReactivatePriceListUseCase reactivateUseCase;
    private final Authz authz;

    public PriceListController(CreatePriceListUseCase createUseCase,
            UpdatePriceListUseCase updateUseCase, FindPriceListUseCase findUseCase,
            ListPriceListsUseCase listUseCase, DeletePriceListUseCase deleteUseCase,
            PublishPriceListUseCase publishUseCase, ArchivePriceListUseCase archiveUseCase,
            ReactivatePriceListUseCase reactivateUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.publishUseCase = publishUseCase;
        this.archiveUseCase = archiveUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PriceListResponse create(@Valid @RequestBody CreatePriceListRequest request) {
        return toResponse(createUseCase.execute(new CreatePriceListCommand(request.code(),
                request.name(), request.currency(), request.validFrom(), request.validTo())));
    }

    /**
     * {@code status} es opcional y sin el se devuelven todas, que es el
     * comportamiento anterior. Con el, el cliente puede pedir
     * {@code ?status=PUBLISHED} -el unico subconjunto que se puede ofrecer para
     * elegir una tarifa- en vez de traerse el tope de 200 filas y descartar en el
     * navegador (incidencia #450).
     */
    @GetMapping
    public PageResponse<PriceListResponse> listAll(
            @RequestParam(required = false) PriceListStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listByStatus(status, page, pageSize),
                this::toResponse);
    }

    @GetMapping("/{id}")
    public PriceListResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public PriceListResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdatePriceListRequest request) {
        return toResponse(updateUseCase.execute(new UpdatePriceListCommand(id, request.name(),
                request.currency(), request.validFrom(), request.validTo())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    /**
     * Congela la tarifa: a partir de aqui ni ella ni sus precios se pueden tocar.
     *
     * <p>
     * Sin cuerpo. La firma la pone {@code authz.currentSystemUserId()}, que lanza
     * {@code AccessDeniedException} si el actor no es una cuenta de plataforma: se
     * usa la variante que lanza y no la opcional porque publicar una tarifa siempre
     * tiene un responsable, y una firma nula seria una tarifa vigente que nadie
     * publico.
     */
    @PatchMapping("/{id}/publish")
    public PriceListResponse publish(@PathVariable Long id) {
        return toResponse(publishUseCase
                .execute(new PublishPriceListCommand(id, authz.currentSystemUserId())));
    }

    @PatchMapping("/{id}/archive")
    public PriceListResponse archive(@PathVariable Long id) {
        return toResponse(archiveUseCase.execute(id));
    }

    @PatchMapping("/{id}/enable")
    public PriceListResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private PriceListResponse toResponse(PriceListDto dto) {
        return new PriceListResponse(dto.id(), dto.code(), dto.name(), dto.currency(),
                dto.validFrom(), dto.validTo(), dto.status(), dto.publishedAt(),
                dto.publishedBySystemUserId(), dto.createdDate(), dto.enabled());
    }
}
