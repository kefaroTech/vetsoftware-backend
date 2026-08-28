package com.vetsoftware.app.paymentrefund.infrastructure.web;

import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.paymentrefund.application.command.RegisterPaymentRefundCommand;
import com.vetsoftware.app.paymentrefund.application.port.in.ListAllPaymentRefundsUseCase;
import com.vetsoftware.app.paymentrefund.application.port.in.RegisterPaymentRefundUseCase;
import com.vetsoftware.app.paymentrefund.infrastructure.web.request.RegisterPaymentRefundRequest;
import com.vetsoftware.app.paymentrefund.infrastructure.web.response.SystemPaymentRefundResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tesoreria de la plataforma: el unico sitio desde el que se registra una
 * devolucion, y la consulta cross-tenant de lo devuelto.
 *
 * <p>
 * Todo lo que sirve es {@code SystemPaymentRefundResponse}, que si publica el
 * id del operador que autorizo la salida de caja. El cliente lee
 * {@code PaymentRefundResponse}, que no lo lleva.
 *
 * <p>
 * Aqui el {@code companyId} <strong>viaja como {@code @RequestParam}</strong>,
 * no en el cuerpo y no desde el principal: un principal SYSTEM no tiene empresa
 * propia, es tesoreria eligiendo a que clinica le devuelve. En el cuerpo no
 * puede ir —lo prohibe la regla dura {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}, que
 * mira todo {@code @RequestBody} sin mirar la ruta ni el rol—. La proteccion no
 * es que el servidor inyecte la empresa -no puede- sino que el caso de uso esta
 * cerrado a {@code hasRole('SYSTEM')} a secas.
 */
@RestController
@RequestMapping("/system/payment-refunds")
public class SystemPaymentRefundController {

    private final RegisterPaymentRefundUseCase registerUseCase;
    private final ListAllPaymentRefundsUseCase listUseCase;

    public SystemPaymentRefundController(RegisterPaymentRefundUseCase registerUseCase,
            ListAllPaymentRefundsUseCase listUseCase) {
        this.registerUseCase = registerUseCase;
        this.listUseCase = listUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SystemPaymentRefundResponse register(@RequestParam Long companyId,
            @Valid @RequestBody RegisterPaymentRefundRequest request) {
        return SystemPaymentRefundResponse
                .from(registerUseCase.execute(new RegisterPaymentRefundCommand(companyId,
                        request.paymentId(), request.sourceDocumentId(), request.amount(),
                        request.method(), request.destinationReference(), request.refundedAt(),
                        request.valueDate(), request.reasonCode(), request.reason(),
                        request.authorizedBySystemUserId(), request.clientRequestId())));
    }

    @GetMapping
    public PageResponse<SystemPaymentRefundResponse> listAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(companyId, page, pageSize),
                SystemPaymentRefundResponse::from);
    }
}
