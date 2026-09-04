package com.vetsoftware.app.accountingaccount.infrastructure.web;

import com.vetsoftware.app.accountingaccount.application.command.CloseAccountingAccountCommand;
import com.vetsoftware.app.accountingaccount.application.command.CreateAccountingAccountCommand;
import com.vetsoftware.app.accountingaccount.application.command.UpdateAccountingAccountCommand;
import com.vetsoftware.app.accountingaccount.application.port.in.CloseAccountingAccountUseCase;
import com.vetsoftware.app.accountingaccount.application.port.in.CreateAccountingAccountUseCase;
import com.vetsoftware.app.accountingaccount.application.port.in.FindAccountingAccountUseCase;
import com.vetsoftware.app.accountingaccount.application.port.in.ListAccountingAccountsUseCase;
import com.vetsoftware.app.accountingaccount.application.port.in.UpdateAccountingAccountUseCase;
import com.vetsoftware.app.accountingaccount.infrastructure.web.request.CloseAccountingAccountRequest;
import com.vetsoftware.app.accountingaccount.infrastructure.web.request.CreateAccountingAccountRequest;
import com.vetsoftware.app.accountingaccount.infrastructure.web.request.UpdateAccountingAccountRequest;
import com.vetsoftware.app.accountingaccount.infrastructure.web.response.AccountingAccountResponse;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * El plan de cuentas, entero, y <strong>solo desde la consola de
 * plataforma</strong>.
 *
 * <p>
 * <strong>No hay controller de tenant, y esa ausencia es la decision.</strong>
 * En {@code withholding_rate_rules} el catalogo lo escribe plataforma y lo leen
 * los dos, porque la tarifa que le van a retener al cliente es informacion
 * suya. Aqui no: {@code accounting_accounts} son los libros de Lumbre, y una
 * clinica no tiene nada que consultar en ellos. Por eso los cinco puertos van
 * cerrados a {@code hasRole('SYSTEM')} a secas y no hay una sola
 * {@code hasAuthority} — que es ademas lo que exige
 * {@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM}.
 *
 * <p>
 * <strong>No hay endpoint de borrado.</strong> Las tres claves foraneas de
 * {@code account_mappings} son {@code RESTRICT} y los asientos ya hechos
 * necesitan que la cuenta siga existiendo: lo que se hace es <em>cerrarla</em>.
 */
@RestController
@RequestMapping("/system/accounting-accounts")
public class SystemAccountingAccountController {

    private final CreateAccountingAccountUseCase createUseCase;
    private final UpdateAccountingAccountUseCase updateUseCase;
    private final CloseAccountingAccountUseCase closeUseCase;
    private final FindAccountingAccountUseCase findUseCase;
    private final ListAccountingAccountsUseCase listUseCase;

    public SystemAccountingAccountController(CreateAccountingAccountUseCase createUseCase,
            UpdateAccountingAccountUseCase updateUseCase,
            CloseAccountingAccountUseCase closeUseCase, FindAccountingAccountUseCase findUseCase,
            ListAccountingAccountsUseCase listUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.closeUseCase = closeUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountingAccountResponse create(
            @Valid @RequestBody CreateAccountingAccountRequest request) {
        return AccountingAccountResponse
                .from(createUseCase.execute(new CreateAccountingAccountCommand(request.code(),
                        request.name(), request.accountClass(), request.parentCode(),
                        request.accountLevel(), request.postable(), request.requiresThirdParty(),
                        request.validFrom(), request.validTo())));
    }

    @PutMapping("/{id}")
    public AccountingAccountResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateAccountingAccountRequest request) {
        return AccountingAccountResponse
                .from(updateUseCase.execute(new UpdateAccountingAccountCommand(id, request.name(),
                        request.requiresThirdParty())));
    }

    /**
     * {@code PATCH} y no {@code DELETE}: cerrar una vigencia es escribir una fecha
     * en una fila que se queda, no retirarla.
     */
    @PatchMapping("/{id}/close")
    public AccountingAccountResponse close(@PathVariable Long id,
            @Valid @RequestBody CloseAccountingAccountRequest request) {
        return AccountingAccountResponse.from(
                closeUseCase.execute(new CloseAccountingAccountCommand(id, request.validTo())));
    }

    @GetMapping("/{id}")
    public AccountingAccountResponse findById(@PathVariable Long id) {
        return AccountingAccountResponse.from(findUseCase.findById(id));
    }

    /**
     * Por codigo, que es como la nombran los mapeos. La ruta lleva el segmento
     * {@code /by-code/} para que no compita con {@code /{id}}: un codigo de cuenta
     * es numerico y sin el segmento las dos rutas serian ambiguas.
     */
    @GetMapping("/by-code/{code}")
    public AccountingAccountResponse findByCode(@PathVariable String code) {
        return AccountingAccountResponse.from(findUseCase.findByCode(code));
    }

    @GetMapping
    public PageResponse<AccountingAccountResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(page, pageSize),
                AccountingAccountResponse::from);
    }
}
