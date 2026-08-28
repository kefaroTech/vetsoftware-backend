package com.vetsoftware.app.accountingexport.infrastructure.web;

import com.vetsoftware.app.accountingexport.application.command.GenerateAccountingExportCommand;
import com.vetsoftware.app.accountingexport.application.command.RejectAccountingExportCommand;
import com.vetsoftware.app.accountingexport.application.port.in.FindAccountingExportUseCase;
import com.vetsoftware.app.accountingexport.application.port.in.GenerateAccountingExportUseCase;
import com.vetsoftware.app.accountingexport.application.port.in.ListAccountingExportsUseCase;
import com.vetsoftware.app.accountingexport.application.port.in.ResolveAccountingExportUseCase;
import com.vetsoftware.app.accountingexport.infrastructure.web.request.GenerateAccountingExportRequest;
import com.vetsoftware.app.accountingexport.infrastructure.web.request.RejectAccountingExportRequest;
import com.vetsoftware.app.accountingexport.infrastructure.web.response.AccountingExportResponse;
import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import jakarta.validation.Valid;
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
 * La bandeja de exportaciones al software contable, y <strong>solo desde la
 * consola de plataforma</strong>.
 *
 * <p>
 * <strong>Quien firma la exportacion sale de
 * {@code authz.currentSystemUserId()}, nunca del cuerpo.</strong> Es el mismo
 * criterio que aplica {@code SystemAccountingPeriodController} al cierre de un
 * mes: la firma es lo que sostiene la trazabilidad de quien entrego que al
 * contador, y aceptarla por HTTP dejaria firmarla a nombre de otro.
 *
 * <p>
 * <strong>No hay endpoint de borrado ni de edicion del fichero.</strong> Un
 * fichero equivocado no se corrige: se rechaza o se reemplaza —lo que libera el
 * hueco de {@code uq_accounting_exports_current}— y se genera el intento
 * siguiente. Editar los totales de una exportacion ya entregada dejaria al
 * contador con un fichero que no coincide con lo que dice la base, y el
 * {@code totalsHash} existe precisamente para que esa divergencia se pueda
 * demostrar.
 */
@RestController
@RequestMapping("/system/accounting-exports")
public class SystemAccountingExportController {

    private final GenerateAccountingExportUseCase generateUseCase;
    private final ResolveAccountingExportUseCase resolveUseCase;
    private final FindAccountingExportUseCase findUseCase;
    private final ListAccountingExportsUseCase listUseCase;
    private final Authz authz;

    public SystemAccountingExportController(GenerateAccountingExportUseCase generateUseCase,
            ResolveAccountingExportUseCase resolveUseCase, FindAccountingExportUseCase findUseCase,
            ListAccountingExportsUseCase listUseCase, Authz authz) {
        this.generateUseCase = generateUseCase;
        this.resolveUseCase = resolveUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountingExportResponse generate(
            @Valid @RequestBody GenerateAccountingExportRequest request) {
        return AccountingExportResponse.from(
                generateUseCase.execute(new GenerateAccountingExportCommand(request.periodKey(),
                        request.exportKind(), authz.currentSystemUserId(), request.totalDebit(),
                        request.totalCredit(), request.totalsHash(), request.fileRef())));
    }

    /**
     * El contador lo recibio. La fecha la pone el caso de uso con su reloj
     * inyectado.
     */
    @PatchMapping("/{id}/deliver")
    public AccountingExportResponse deliver(@PathVariable Long id) {
        return AccountingExportResponse.from(resolveUseCase.markDelivered(id));
    }

    @PatchMapping("/{id}/reject")
    public AccountingExportResponse reject(@PathVariable Long id,
            @Valid @RequestBody RejectAccountingExportRequest request) {
        return AccountingExportResponse.from(resolveUseCase
                .markRejected(new RejectAccountingExportCommand(id, request.rejectionReason())));
    }

    @PatchMapping("/{id}/supersede")
    public AccountingExportResponse supersede(@PathVariable Long id) {
        return AccountingExportResponse.from(resolveUseCase.markSuperseded(id));
    }

    @GetMapping("/{id}")
    public AccountingExportResponse findById(@PathVariable Long id) {
        return AccountingExportResponse.from(findUseCase.findById(id));
    }

    @GetMapping
    public PageResponse<AccountingExportResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(page, pageSize),
                AccountingExportResponse::from);
    }

    /** La bandeja de un mes. */
    @GetMapping("/by-period/{periodKey}")
    public PageResponse<AccountingExportResponse> listByPeriod(@PathVariable String periodKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listByPeriod(periodKey, page, pageSize),
                AccountingExportResponse::from);
    }
}
