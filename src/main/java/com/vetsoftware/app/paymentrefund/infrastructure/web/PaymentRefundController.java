package com.vetsoftware.app.paymentrefund.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.paymentrefund.application.port.in.FindPaymentRefundUseCase;
import com.vetsoftware.app.paymentrefund.application.port.in.ListPaymentRefundsByPaymentUseCase;
import com.vetsoftware.app.paymentrefund.application.port.in.ListPaymentRefundsUseCase;
import com.vetsoftware.app.paymentrefund.infrastructure.web.response.PaymentRefundResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La cara de tenant de las devoluciones, y es <strong>solo de lectura</strong>.
 *
 * <p>
 * No es que las escrituras aun no esten hechas: no van aqui. El bloque «Cobro y
 * saldos» del documento maestro lo escribe la plataforma y lo leen los dos, asi
 * que el registro vive en {@link SystemPaymentRefundController} y este
 * controller no tiene un solo {@code @PostMapping}. Un endpoint de escritura
 * aqui seria una clinica autorizandose a si misma una salida de caja.
 *
 * <p>
 * La empresa sale siempre de {@code authz.currentCompanyId()} y nunca de la URL
 * ni del cuerpo: es lo que impide leer las devoluciones de otra clinica
 * escribiendo su id.
 */
@RestController
@RequestMapping("/payment-refunds")
public class PaymentRefundController {

    private final FindPaymentRefundUseCase findUseCase;
    private final ListPaymentRefundsUseCase listUseCase;
    private final ListPaymentRefundsByPaymentUseCase listByPaymentUseCase;
    private final Authz authz;

    public PaymentRefundController(FindPaymentRefundUseCase findUseCase,
            ListPaymentRefundsUseCase listUseCase,
            ListPaymentRefundsByPaymentUseCase listByPaymentUseCase, Authz authz) {
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listByPaymentUseCase = listByPaymentUseCase;
        this.authz = authz;
    }

    @GetMapping("/{id}")
    public PaymentRefundResponse findById(@PathVariable Long id) {
        return PaymentRefundResponse.from(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @GetMapping
    public PageResponse<PaymentRefundResponse> listByCompany(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUseCase.listByCompany(authz.currentCompanyId(), page, pageSize),
                PaymentRefundResponse::from);
    }

    /** Cuanto se ha devuelto ya de un pago, que es lo que se mira antes de otra. */
    @GetMapping("/by-payment/{paymentId}")
    public PageResponse<PaymentRefundResponse> listByPayment(@PathVariable Long paymentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listByPaymentUseCase.listByPaymentAndCompany(paymentId,
                authz.currentCompanyId(), page, pageSize), PaymentRefundResponse::from);
    }
}
