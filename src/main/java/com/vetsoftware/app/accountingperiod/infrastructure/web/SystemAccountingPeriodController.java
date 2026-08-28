package com.vetsoftware.app.accountingperiod.infrastructure.web;

import com.vetsoftware.app.accountingperiod.application.command.LockAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.command.OpenAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.command.ReopenAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.command.SoftCloseAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.port.in.FindAccountingPeriodUseCase;
import com.vetsoftware.app.accountingperiod.application.port.in.ListAccountingPeriodsUseCase;
import com.vetsoftware.app.accountingperiod.application.port.in.LockAccountingPeriodUseCase;
import com.vetsoftware.app.accountingperiod.application.port.in.OpenAccountingPeriodUseCase;
import com.vetsoftware.app.accountingperiod.application.port.in.ReopenAccountingPeriodUseCase;
import com.vetsoftware.app.accountingperiod.application.port.in.ResolvePostingPeriodUseCase;
import com.vetsoftware.app.accountingperiod.application.port.in.SoftCloseAccountingPeriodUseCase;
import com.vetsoftware.app.accountingperiod.infrastructure.web.request.OpenAccountingPeriodRequest;
import com.vetsoftware.app.accountingperiod.infrastructure.web.request.ReopenAccountingPeriodRequest;
import com.vetsoftware.app.accountingperiod.infrastructure.web.response.AccountingPeriodResponse;
import com.vetsoftware.app.auth.infrastructure.security.Authz;
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
 * El calendario contable, entero y solo desde la consola de plataforma.
 *
 * <h2>No hay controller de tenant, y eso es la decision, no un olvido</h2>
 *
 * <p>
 * Este es el <strong>unico</strong> controller de la feature. No existe un
 * {@code AccountingPeriodController} bajo la ruta del tenant, y no falta por
 * escribir: <strong>el calendario contable es de la plataforma</strong>. Si
 * cada clinica abriera y cerrara sus propios meses, la misma conciliacion
 * podria quedar imputada a marzo para una y a abril para otra y el consolidado
 * dejaria de cuadrar sin que nada fallara. La tabla no tiene
 * {@code company_id}, asi que tampoco hay un {@code companyId} con el que
 * acotar nada.
 *
 * <h2>El gate es mas ancho de lo que la regla de negocio pide</h2>
 *
 * <p>
 * La regla dice «solo el contador externo cierra y reabre». <strong>Ese rol no
 * existe en este backend</strong>: hay un unico rol, {@code ROLE_SYSTEM}. Los
 * siete puertos de entrada van con {@code hasRole('SYSTEM')} a secas —lo que
 * significa que cualquier cuenta de plataforma puede cerrar, declarar y
 * reabrir—, y no por {@code hasAuthority}, que ademas romperia
 * {@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM}. El estrechamiento, cuando el rol
 * llegue, va en el {@code @PreAuthorize} de
 * {@code SoftCloseAccountingPeriodUseCase} y
 * {@code ReopenAccountingPeriodUseCase} —no aqui, porque el puerto es lo unico
 * que protege tambien al caller que no entra por HTTP—. Mientras tanto lo que
 * queda es el rastro: las dos firmas y el motivo obligatorio de la reapertura.
 *
 * <h2>Sin borrado y sin actualizacion generica</h2>
 *
 * <p>
 * No hay {@code DELETE} ni {@code PUT}: un mes no se borra ni se edita, se
 * mueve de estado por las tres transiciones que la ficha admite. La clave del
 * mes es la identidad del periodo y no cambia nunca — renombrar {@code 2026-03}
 * a {@code 2026-04} dejaria a las conciliaciones que apuntan a esa clave
 * hablando de otro mes.
 */
@RestController
@RequestMapping("/system/accounting-periods")
public class SystemAccountingPeriodController {

    private final OpenAccountingPeriodUseCase openUseCase;
    private final SoftCloseAccountingPeriodUseCase softCloseUseCase;
    private final LockAccountingPeriodUseCase lockUseCase;
    private final ReopenAccountingPeriodUseCase reopenUseCase;
    private final FindAccountingPeriodUseCase findUseCase;
    private final ListAccountingPeriodsUseCase listUseCase;
    private final ResolvePostingPeriodUseCase resolvePostingPeriodUseCase;
    private final Authz authz;

    public SystemAccountingPeriodController(OpenAccountingPeriodUseCase openUseCase,
            SoftCloseAccountingPeriodUseCase softCloseUseCase,
            LockAccountingPeriodUseCase lockUseCase, ReopenAccountingPeriodUseCase reopenUseCase,
            FindAccountingPeriodUseCase findUseCase, ListAccountingPeriodsUseCase listUseCase,
            ResolvePostingPeriodUseCase resolvePostingPeriodUseCase, Authz authz) {
        this.openUseCase = openUseCase;
        this.softCloseUseCase = softCloseUseCase;
        this.lockUseCase = lockUseCase;
        this.reopenUseCase = reopenUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.resolvePostingPeriodUseCase = resolvePostingPeriodUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountingPeriodResponse open(@Valid @RequestBody OpenAccountingPeriodRequest request) {
        return AccountingPeriodResponse
                .from(openUseCase.execute(new OpenAccountingPeriodCommand(request.periodKey())));
    }

    /**
     * <strong>{@code PATCH} y sin cuerpo</strong>: la operacion no recibe ningun
     * dato del cliente. Quien cierra sale del principal autenticado y cuando se
     * cierra lo pone el reloj del negocio; un cuerpo vacio obligatorio seria un
     * campo que el front tendria que inventarse.
     */
    @PatchMapping("/{id}/soft-close")
    public AccountingPeriodResponse softClose(@PathVariable Long id) {
        return AccountingPeriodResponse.from(softCloseUseCase
                .execute(new SoftCloseAccountingPeriodCommand(id, authz.currentSystemUserId())));
    }

    /**
     * Declara el mes. <strong>Es la unica operacion irreversible de la API</strong>
     * y aun asi es un {@code PATCH} corriente: lo que la hace definitiva es el
     * dominio, no el verbo.
     */
    @PatchMapping("/{id}/lock")
    public AccountingPeriodResponse lock(@PathVariable Long id) {
        return AccountingPeriodResponse.from(lockUseCase
                .execute(new LockAccountingPeriodCommand(id, authz.currentSystemUserId())));
    }

    /**
     * <strong>El unico {@code PATCH} de la feature con cuerpo</strong>, y lo tiene
     * porque el motivo es obligatorio: reabrir sin decir por que es justo lo que la
     * ficha existe para impedir.
     */
    @PatchMapping("/{id}/reopen")
    public AccountingPeriodResponse reopen(@PathVariable Long id,
            @Valid @RequestBody ReopenAccountingPeriodRequest request) {
        return AccountingPeriodResponse
                .from(reopenUseCase.execute(new ReopenAccountingPeriodCommand(id,
                        authz.currentSystemUserId(), request.reason())));
    }

    /**
     * En que mes se registra un hecho ocurrido en {@code occurredOn}: ese mismo si
     * esta abierto, y si no el primer mes abierto posterior. Nunca hacia atras.
     *
     * <p>
     * Va <em>antes</em> del mapeo por {@code {id}} porque {@code PathPatternParser}
     * da preferencia al literal frente a la variable, pero dejarlo escrito arriba
     * evita que alguien tenga que comprobarlo: si {@code /posting-period}
     * resolviera contra {@code /{id}}, Spring intentaria convertir el texto a
     * {@code Long} y esta ruta contestaria un 400 de conversion.
     */
    @GetMapping("/posting-period")
    public AccountingPeriodResponse resolvePostingPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate occurredOn) {
        return AccountingPeriodResponse.from(resolvePostingPeriodUseCase.resolve(occurredOn));
    }

    @GetMapping("/{id}")
    public AccountingPeriodResponse findById(@PathVariable Long id) {
        return AccountingPeriodResponse.from(findUseCase.findById(id));
    }

    @GetMapping
    public PageResponse<AccountingPeriodResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(page, pageSize),
                AccountingPeriodResponse::from);
    }
}
