package com.vetsoftware.app.accountmapping.infrastructure.web;

import com.vetsoftware.app.accountmapping.application.command.CloseAccountMappingCommand;
import com.vetsoftware.app.accountmapping.application.command.CreateAccountMappingCommand;
import com.vetsoftware.app.accountmapping.application.port.in.CloseAccountMappingUseCase;
import com.vetsoftware.app.accountmapping.application.port.in.CreateAccountMappingUseCase;
import com.vetsoftware.app.accountmapping.application.port.in.FindAccountMappingUseCase;
import com.vetsoftware.app.accountmapping.application.port.in.ListAccountMappingsUseCase;
import com.vetsoftware.app.accountmapping.application.port.in.ResolveAccountMappingUseCase;
import com.vetsoftware.app.accountmapping.domain.MappingKind;
import com.vetsoftware.app.accountmapping.infrastructure.web.request.CloseAccountMappingRequest;
import com.vetsoftware.app.accountmapping.infrastructure.web.request.CreateAccountMappingRequest;
import com.vetsoftware.app.accountmapping.infrastructure.web.response.AccountMappingResponse;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * El puente concepto → cuenta, y <strong>solo desde la consola de
 * plataforma</strong>.
 *
 * <p>
 * <strong>No hay controller de tenant.</strong> Contra que cuenta se asienta un
 * concepto es una decision sobre los libros de Lumbre; una clinica no tiene
 * nada que consultar aqui. Los cinco puertos van cerrados a
 * {@code hasRole('SYSTEM')} a secas.
 *
 * <p>
 * <strong>No hay endpoint de edicion, y esa ausencia es la decision.</strong>
 * Un mapeo no se edita: se cierra y se abre otro. Cambiarle la cuenta en sitio
 * reescribiria en silencio contra que cuenta se asentaron todas las facturas
 * anteriores, y ademas cerrar es lo unico que libera el hueco de
 * {@code uq_account_mappings_current} para publicar el relevo.
 */
@RestController
@RequestMapping("/system/account-mappings")
public class SystemAccountMappingController {

    private final CreateAccountMappingUseCase createUseCase;
    private final CloseAccountMappingUseCase closeUseCase;
    private final FindAccountMappingUseCase findUseCase;
    private final ListAccountMappingsUseCase listUseCase;
    private final ResolveAccountMappingUseCase resolveUseCase;

    public SystemAccountMappingController(CreateAccountMappingUseCase createUseCase,
            CloseAccountMappingUseCase closeUseCase, FindAccountMappingUseCase findUseCase,
            ListAccountMappingsUseCase listUseCase, ResolveAccountMappingUseCase resolveUseCase) {
        this.createUseCase = createUseCase;
        this.closeUseCase = closeUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.resolveUseCase = resolveUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountMappingResponse create(@Valid @RequestBody CreateAccountMappingRequest request) {
        return AccountMappingResponse.from(createUseCase.execute(
                new CreateAccountMappingCommand(request.mappingKind(), request.mappingKey(),
                        request.catalogItemId(), request.chargeType(), request.taxTreatment(),
                        request.debitAccountCode(), request.creditAccountCode(),
                        request.deferredAccountCode(), request.validFrom(), request.validTo())));
    }

    /**
     * {@code PATCH} y no {@code DELETE}: cerrar una vigencia es escribir una fecha
     * en una fila que se queda, no retirarla.
     */
    @PatchMapping("/{id}/close")
    public AccountMappingResponse close(@PathVariable Long id,
            @Valid @RequestBody CloseAccountMappingRequest request) {
        return AccountMappingResponse
                .from(closeUseCase.execute(new CloseAccountMappingCommand(id, request.validTo())));
    }

    @GetMapping("/{id}")
    public AccountMappingResponse findById(@PathVariable Long id) {
        return AccountMappingResponse.from(findUseCase.findById(id));
    }

    @GetMapping
    public PageResponse<AccountMappingResponse> listAll(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(page, pageSize), AccountMappingResponse::from);
    }

    /**
     * <strong>El endpoint por el que existe la feature</strong>: que cuentas mueve
     * este supuesto en esta fecha.
     *
     * <p>
     * Los tres afinados son opcionales porque nueve de las doce clases no los
     * llevan; el adaptador los traduce a los centinelas de las columnas generadas
     * antes de consultar. {@code on} tampoco es obligatorio, pero <b>quien genera
     * un asiento tiene que enviarlo</b>: la fecha que manda es la del hecho
     * economico.
     *
     * <p>
     * <strong>Cuando {@code on} no viene, el {@code null} viaja tal cual al caso de
     * uso y es EL quien decide que dia es hoy, con su {@code Clock}
     * inyectado.</strong> Un {@code LocalDate.now()} aqui es una fecha que ningun
     * test puede fijar y {@code RELOJ_INYECTADO_EN_VEZ_DE_NOW} rompe el build por
     * ello.
     */
    @GetMapping("/effective")
    public AccountMappingResponse resolve(@RequestParam MappingKind mappingKind,
            @RequestParam String mappingKey, @RequestParam(required = false) Long catalogItemId,
            @RequestParam(required = false) String chargeType,
            @RequestParam(required = false) String taxTreatment,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate on) {
        return AccountMappingResponse.from(resolveUseCase.resolve(mappingKind, mappingKey,
                catalogItemId, chargeType, taxTreatment, on));
    }
}
