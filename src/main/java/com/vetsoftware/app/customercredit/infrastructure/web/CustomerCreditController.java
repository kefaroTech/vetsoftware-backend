package com.vetsoftware.app.customercredit.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.customercredit.application.port.in.FindCustomerCreditBalanceUseCase;
import com.vetsoftware.app.customercredit.application.port.in.FindCustomerCreditEntryUseCase;
import com.vetsoftware.app.customercredit.application.port.in.ListCustomerCreditEntriesUseCase;
import com.vetsoftware.app.customercredit.infrastructure.web.response.CustomerCreditBalanceResponse;
import com.vetsoftware.app.customercredit.infrastructure.web.response.CustomerCreditEntryResponse;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La cara de tenant del saldo a favor: <strong>solo lecturas</strong>.
 *
 * <p>
 * El bloque <em>Cobro y saldos</em> del modelo reparte estas tablas como
 * «escribe plataforma, leen ambos». El cliente ve lo suyo —su libro y su saldo—
 * y no puede abonarse, gastarse ni caducarse nada a si mismo: esas tres
 * escrituras viven en {@link SystemCustomerCreditController} y sus casos de uso
 * estan cerrados a {@code hasRole('SYSTEM')} a secas.
 *
 * <p>
 * La empresa sale siempre de {@code authz.currentCompanyId()} y nunca de la URL
 * ni del cuerpo: es lo que impide leer el saldo de otra clinica.
 */
@RestController
@RequestMapping("/customer-credit")
public class CustomerCreditController {

    private final FindCustomerCreditEntryUseCase findEntryUseCase;
    private final ListCustomerCreditEntriesUseCase listEntriesUseCase;
    private final FindCustomerCreditBalanceUseCase findBalanceUseCase;
    private final Authz authz;

    public CustomerCreditController(FindCustomerCreditEntryUseCase findEntryUseCase,
            ListCustomerCreditEntriesUseCase listEntriesUseCase,
            FindCustomerCreditBalanceUseCase findBalanceUseCase, Authz authz) {
        this.findEntryUseCase = findEntryUseCase;
        this.listEntriesUseCase = listEntriesUseCase;
        this.findBalanceUseCase = findBalanceUseCase;
        this.authz = authz;
    }

    /** El saldo vivo de la empresa: lo que tiene a favor y cuando se le vence. */
    @GetMapping("/balance")
    public CustomerCreditBalanceResponse findBalance() {
        return CustomerCreditBalanceResponse
                .from(findBalanceUseCase.findByCompanyId(authz.currentCompanyId()));
    }

    /** El libro de la empresa, que es lo que sostiene el saldo de arriba. */
    @GetMapping("/entries")
    public PageResponse<CustomerCreditEntryResponse> listEntries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listEntriesUseCase.listByCompany(authz.currentCompanyId(), page, pageSize),
                CustomerCreditEntryResponse::from);
    }

    @GetMapping("/entries/{id}")
    public CustomerCreditEntryResponse findEntry(@PathVariable Long id) {
        return CustomerCreditEntryResponse
                .from(findEntryUseCase.findById(id, authz.currentCompanyId()));
    }
}
