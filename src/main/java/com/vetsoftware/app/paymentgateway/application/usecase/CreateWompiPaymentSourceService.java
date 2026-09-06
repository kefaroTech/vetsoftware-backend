package com.vetsoftware.app.paymentgateway.application.usecase;

import com.vetsoftware.app.paymentgateway.application.command.CreateWompiPaymentSourceCommand;
import com.vetsoftware.app.paymentgateway.application.dto.WompiPaymentMethodDto;
import com.vetsoftware.app.paymentgateway.application.port.in.CreateWompiPaymentSourceUseCase;
import com.vetsoftware.app.paymentgateway.application.port.out.CompanyBillingEmailQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentMethodRegistrarPort;
import com.vetsoftware.app.paymentgateway.domain.CreatePaymentSourceRequest;
import com.vetsoftware.app.paymentgateway.domain.GatewayPaymentSource;
import com.vetsoftware.app.paymentgateway.domain.MerchantAcceptance;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;

/**
 * Da de alta la tarjeta tokenizada como fuente de pago recurrente.
 *
 * <p>
 * <strong>Sin {@code @Transactional}</strong>: llama dos veces a la pasarela
 * (aceptación y fuente de pago), y ambas son I/O HTTP fuera de cualquier
 * transacción.
 *
 * <p>
 * <strong>Por qué pide la aceptación otra vez, si el front ya la
 * tiene.</strong> El {@code acceptanceToken} que manda el comando es el JWT que
 * el front capturó al cargar el checkout, y ese es el que viaja a Wompi. Pero
 * {@code mandateEvidence} necesita los <em>permalinks</em> de los documentos
 * aceptados, y esos no vuelven en el comando (no se cachean, ver
 * {@code GetWompiCheckoutConfigUseCase}); esta segunda llamada solo lee los
 * permalinks vigentes, que son URLs estables de los mismos documentos.
 */
@Observed(name = "payment.gateway.payment.source.create")
@Service
public class CreateWompiPaymentSourceService implements CreateWompiPaymentSourceUseCase {

    private static final int MAX_MANDATE_EVIDENCE_LENGTH = 255;

    private final PaymentGatewayPort paymentGatewayPort;
    private final CompanyBillingEmailQueryPort companyBillingEmailQueryPort;
    private final PaymentMethodRegistrarPort paymentMethodRegistrarPort;
    private final Clock clock;

    public CreateWompiPaymentSourceService(PaymentGatewayPort paymentGatewayPort,
            CompanyBillingEmailQueryPort companyBillingEmailQueryPort,
            PaymentMethodRegistrarPort paymentMethodRegistrarPort, Clock clock) {
        this.paymentGatewayPort = paymentGatewayPort;
        this.companyBillingEmailQueryPort = companyBillingEmailQueryPort;
        this.paymentMethodRegistrarPort = paymentMethodRegistrarPort;
        this.clock = clock;
    }

    @Override
    public WompiPaymentMethodDto execute(CreateWompiPaymentSourceCommand command) {
        String fiscalEmail = companyBillingEmailQueryPort.findFiscalEmail(command.companyId())
                .orElseThrow(() -> new IllegalStateException(
                        "La empresa " + command.companyId() + " no tiene perfil fiscal vigente"));

        MerchantAcceptance acceptance = paymentGatewayPort.fetchAcceptance();
        GatewayPaymentSource paymentSource = paymentGatewayPort
                .createPaymentSource(new CreatePaymentSourceRequest(command.cardToken(),
                        fiscalEmail, command.acceptanceToken(), true));

        LocalDateTime authorizedAt = LocalDateTime.now(clock);
        LocalDate expiresOn = YearMonth.of(command.expYear(), command.expMonth()).atEndOfMonth();
        String mandateEvidence = buildMandateEvidence(paymentSource.id(), acceptance,
                Instant.now(clock));

        return paymentMethodRegistrarPort.registerDefaultCard(command.companyId(),
                String.valueOf(paymentSource.id()), command.brand(), command.lastFour(), expiresOn,
                mandateEvidence, authorizedAt);
    }

    /**
     * Se guarda el <strong>último segmento</strong> de cada permalink y no la URL
     * completa: medido, el permalink entero deja el texto a solo 4 caracteres del
     * tope de 255 de {@code mandate_evidence}, demasiado ajustado para el margen
     * que cualquier cambio de dominio o de longitud del permalink necesita.
     */
    private static String buildMandateEvidence(Long paymentSourceId, MerchantAcceptance acceptance,
            Instant at) {
        String evidence = "wompi:ps=" + paymentSourceId + ";acc="
                + lastPathSegment(acceptance.acceptancePermalink()) + ";pda="
                + lastPathSegment(acceptance.personalDataAuthPermalink()) + ";at="
                + at.atOffset(ZoneOffset.UTC);
        return evidence.length() > MAX_MANDATE_EVIDENCE_LENGTH
                ? evidence.substring(0, MAX_MANDATE_EVIDENCE_LENGTH)
                : evidence;
    }

    private static String lastPathSegment(String permalink) {
        if (permalink == null || permalink.isBlank())
            return "";
        int slash = permalink.lastIndexOf('/');
        return slash >= 0 && slash < permalink.length() - 1
                ? permalink.substring(slash + 1)
                : permalink;
    }
}
