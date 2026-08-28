package com.vetsoftware.app.customercredit.infrastructure.web;

import com.vetsoftware.app.customercredit.application.command.ConsumeCustomerCreditCommand;
import com.vetsoftware.app.customercredit.application.command.ExpireCustomerCreditCommand;
import com.vetsoftware.app.customercredit.application.command.GrantCustomerCreditCommand;
import com.vetsoftware.app.customercredit.application.port.in.ConsumeCustomerCreditUseCase;
import com.vetsoftware.app.customercredit.application.port.in.ExpireCustomerCreditUseCase;
import com.vetsoftware.app.customercredit.application.port.in.GrantCustomerCreditUseCase;
import com.vetsoftware.app.customercredit.application.port.in.ListAllCustomerCreditBalancesUseCase;
import com.vetsoftware.app.customercredit.application.port.in.ListAllCustomerCreditEntriesUseCase;
import com.vetsoftware.app.customercredit.application.port.in.ListExpiringCustomerCreditUseCase;
import com.vetsoftware.app.customercredit.infrastructure.web.request.ConsumeCustomerCreditRequest;
import com.vetsoftware.app.customercredit.infrastructure.web.request.GrantCustomerCreditRequest;
import com.vetsoftware.app.customercredit.infrastructure.web.response.CustomerCreditBalanceResponse;
import com.vetsoftware.app.customercredit.infrastructure.web.response.CustomerCreditEntryResponse;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * La cara de plataforma del saldo a favor: las tres escrituras y los dos
 * barridos cross-tenant.
 *
 * <p>
 * Aqui la empresa <strong>si</strong> viaja en el cuerpo o en la query, porque
 * el actor SYSTEM no tiene empresa propia y tiene que decir sobre cual opera.
 * Es la excepcion documentada para los endpoints globales, no un descuido: los
 * casos de uso que hay detras estan cerrados a {@code hasRole('SYSTEM')} a
 * secas y ninguno admite el camino de tenant.
 */
@RestController
@RequestMapping("/system/customer-credit")
public class SystemCustomerCreditController {

    private final GrantCustomerCreditUseCase grantUseCase;
    private final ConsumeCustomerCreditUseCase consumeUseCase;
    private final ExpireCustomerCreditUseCase expireUseCase;
    private final ListAllCustomerCreditEntriesUseCase listAllEntriesUseCase;
    private final ListAllCustomerCreditBalancesUseCase listAllBalancesUseCase;
    private final ListExpiringCustomerCreditUseCase listExpiringUseCase;

    public SystemCustomerCreditController(GrantCustomerCreditUseCase grantUseCase,
            ConsumeCustomerCreditUseCase consumeUseCase, ExpireCustomerCreditUseCase expireUseCase,
            ListAllCustomerCreditEntriesUseCase listAllEntriesUseCase,
            ListAllCustomerCreditBalancesUseCase listAllBalancesUseCase,
            ListExpiringCustomerCreditUseCase listExpiringUseCase) {
        this.grantUseCase = grantUseCase;
        this.consumeUseCase = consumeUseCase;
        this.expireUseCase = expireUseCase;
        this.listAllEntriesUseCase = listAllEntriesUseCase;
        this.listAllBalancesUseCase = listAllBalancesUseCase;
        this.listExpiringUseCase = listExpiringUseCase;
    }

    /** Abona saldo abriendo un lote. */
    @PostMapping("/grants")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerCreditEntryResponse grant(@RequestParam Long companyId,
            @Valid @RequestBody GrantCustomerCreditRequest request) {
        return CustomerCreditEntryResponse
                .from(grantUseCase.execute(new GrantCustomerCreditCommand(companyId,
                        request.amount(), request.originKind(), request.originPaymentId(),
                        request.originDocumentId(), request.originSubscriptionId(),
                        request.expiresOn(), request.clientRequestId())));
    }

    /**
     * Aplica saldo a un documento. Devuelve <strong>la lista</strong> de asientos
     * escritos, uno por lote tocado: un consumo casi nunca es una sola fila y
     * devolver solo la primera esconderia la mitad de lo que acaba de pasar.
     */
    @PostMapping("/consumptions")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CustomerCreditEntryResponse> consume(@RequestParam Long companyId,
            @Valid @RequestBody ConsumeCustomerCreditRequest request) {
        return consumeUseCase
                .execute(new ConsumeCustomerCreditCommand(companyId, request.amount(),
                        request.originDocumentId(), request.clientRequestId()))
                .stream().map(CustomerCreditEntryResponse::from).toList();
    }

    /**
     * Caduca el remanente vencido de una empresa.
     *
     * <p>
     * <strong>Sin cuerpo</strong>: lo unico que necesitaba era la empresa, y esa no
     * puede viajar en el {@code @RequestBody}
     * ({@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}). La fecha de corte no se acepta a
     * proposito: la pone el reloj inyectado del servicio, porque dejarsela elegir a
     * quien llama permitiria caducar saldo todavia vivo.
     */
    @PostMapping("/expirations")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CustomerCreditEntryResponse> expire(@RequestParam Long companyId) {
        return expireUseCase.execute(new ExpireCustomerCreditCommand(companyId)).stream()
                .map(CustomerCreditEntryResponse::from).toList();
    }

    @GetMapping("/entries")
    public PageResponse<CustomerCreditEntryResponse> listAllEntries(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listAllEntriesUseCase.listAll(companyId, page, pageSize),
                CustomerCreditEntryResponse::from);
    }

    @GetMapping("/balances")
    public PageResponse<CustomerCreditBalanceResponse> listAllBalances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listAllBalancesUseCase.listAll(page, pageSize),
                CustomerCreditBalanceResponse::from);
    }

    /**
     * Barrido de saldos que caducan, de todas las clinicas. Es uno de los nueve de
     * plataforma y por eso vive aqui y no en el controller de tenant.
     */
    @GetMapping("/expiring")
    public PageResponse<CustomerCreditEntryResponse> listExpiring(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate before,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listExpiringUseCase.listExpiring(before, page, pageSize),
                CustomerCreditEntryResponse::from);
    }
}
