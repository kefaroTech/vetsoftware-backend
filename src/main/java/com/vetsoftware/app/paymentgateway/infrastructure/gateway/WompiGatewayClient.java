package com.vetsoftware.app.paymentgateway.infrastructure.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayPort;
import com.vetsoftware.app.paymentgateway.domain.ChargeRequest;
import com.vetsoftware.app.paymentgateway.domain.CreatePaymentSourceRequest;
import com.vetsoftware.app.paymentgateway.domain.GatewayPaymentSource;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransaction;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransactionStatus;
import com.vetsoftware.app.paymentgateway.domain.MerchantAcceptance;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNotConfiguredException;
import com.vetsoftware.app.paymentgateway.domain.WompiGatewayException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptador HTTP de la pasarela Wompi.
 *
 * <p>
 * <strong>{@code enabled = false} falla cerrado en cada método</strong>, antes
 * de cualquier llamada: sin esa guarda, una configuración incompleta intentaría
 * hablar con la URL de sandbox por defecto y con una llave vacía, lo que Wompi
 * respondería con un 401 indistinguible de una credencial mal puesta.
 */
@Component
public class WompiGatewayClient implements PaymentGatewayPort {

    private final RestClient restClient;
    private final WompiProperties properties;

    public WompiGatewayClient(@Qualifier("wompiRestClient") RestClient restClient,
            WompiProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public MerchantAcceptance fetchAcceptance() {
        requireEnabled();
        try {
            MerchantEnvelope envelope = restClient.get()
                    .uri(properties.baseUrl() + "/merchants/{publicKey}", properties.publicKey())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.publicKey())
                    .retrieve().body(MerchantEnvelope.class);
            MerchantData data = envelope.data();
            return new MerchantAcceptance(data.presignedAcceptance().acceptanceToken(),
                    data.presignedAcceptance().permalink(),
                    data.presignedPersonalDataAuth().acceptanceToken(),
                    data.presignedPersonalDataAuth().permalink());
        } catch (RestClientException e) {
            throw new WompiGatewayException(
                    "No se pudo obtener la aceptación del comercio de Wompi", e);
        }
    }

    @Override
    public GatewayPaymentSource createPaymentSource(CreatePaymentSourceRequest request) {
        requireEnabled();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "CARD");
        body.put("token", request.cardToken());
        body.put("customer_email", request.customerEmail());
        body.put("acceptance_token", request.acceptanceToken());
        body.put("accept_personal_auth", request.acceptPersonalAuth());
        try {
            PaymentSourceEnvelope envelope = restClient.post()
                    .uri(properties.baseUrl() + "/payment_sources")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.privateKey())
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve()
                    .body(PaymentSourceEnvelope.class);
            return new GatewayPaymentSource(envelope.data().id(), envelope.data().status());
        } catch (RestClientException e) {
            throw new WompiGatewayException("No se pudo crear la fuente de pago en Wompi", e);
        }
    }

    @Override
    public GatewayTransaction charge(ChargeRequest request) {
        requireEnabled();
        String signature = WompiSignatures.integritySignature(request.reference(),
                request.amountInCents(), request.currency(), properties.integritySecret());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount_in_cents", request.amountInCents());
        body.put("currency", request.currency());
        body.put("signature", signature);
        body.put("customer_email", request.customerEmail());
        body.put("reference", request.reference());
        body.put("payment_source_id", request.paymentSourceId());
        body.put("recurrent", true);
        body.put("payment_method", Map.of("installments", 1));
        try {
            TransactionEnvelope envelope = restClient.post()
                    .uri(properties.baseUrl() + "/transactions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.privateKey())
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve()
                    .body(TransactionEnvelope.class);
            return toGatewayTransaction(envelope.data());
        } catch (RestClientException e) {
            throw new WompiGatewayException("No se pudo iniciar la transacción en Wompi", e);
        }
    }

    @Override
    public String publicKey() {
        return properties.publicKey();
    }

    @Override
    public String apiBaseUrl() {
        return properties.baseUrl();
    }

    @Override
    public int statusPollAttempts() {
        return properties.statusPollAttempts();
    }

    @Override
    public Duration statusPollInterval() {
        return properties.statusPollInterval();
    }

    @Override
    public GatewayTransaction findTransaction(String id) {
        requireEnabled();
        try {
            TransactionEnvelope envelope = restClient.get()
                    .uri(properties.baseUrl() + "/transactions/{id}", id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.privateKey())
                    .retrieve().body(TransactionEnvelope.class);
            return toGatewayTransaction(envelope.data());
        } catch (RestClientException e) {
            throw new WompiGatewayException(
                    "No se pudo consultar la transacción " + id + " en Wompi", e);
        }
    }

    private void requireEnabled() {
        if (!properties.enabled()) {
            throw new PaymentGatewayNotConfiguredException(
                    "Wompi no está habilitado (vetsoftware.payments.wompi.enabled=false)");
        }
    }

    private static GatewayTransaction toGatewayTransaction(TransactionData data) {
        return new GatewayTransaction(data.id(), GatewayTransactionStatus.valueOf(data.status()),
                data.statusMessage(), data.reference(), data.amountInCents(),
                data.paymentMethodType());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MerchantEnvelope(MerchantData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MerchantData(
            @JsonProperty("presigned_acceptance") AcceptanceData presignedAcceptance,
            @JsonProperty("presigned_personal_data_auth") AcceptanceData presignedPersonalDataAuth) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AcceptanceData(@JsonProperty("acceptance_token") String acceptanceToken,
            String permalink) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PaymentSourceEnvelope(PaymentSourceData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PaymentSourceData(Long id, String status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TransactionEnvelope(TransactionData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TransactionData(String id, String status,
            @JsonProperty("status_message") String statusMessage, String reference,
            @JsonProperty("amount_in_cents") long amountInCents,
            @JsonProperty("payment_method_type") String paymentMethodType) {
    }
}
