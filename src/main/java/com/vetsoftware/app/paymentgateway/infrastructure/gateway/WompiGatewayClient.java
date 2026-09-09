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
import com.vetsoftware.app.paymentgateway.domain.WompiRateLimitedException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
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
        body.put("accept_personal_auth", request.personalDataAuthToken());
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
            return toGatewayTransaction(requireData(envelope));
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new WompiRateLimitedException("Wompi limitó la tasa de peticiones al cobrar",
                    retryAfterFrom(e));
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
    public Duration pendingTransactionMaxAge() {
        return properties.pendingTransactionMaxAge();
    }

    @Override
    public GatewayTransaction findTransaction(String id) {
        requireEnabled();
        try {
            TransactionEnvelope envelope = restClient.get()
                    .uri(properties.baseUrl() + "/transactions/{id}", id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.privateKey())
                    .retrieve().body(TransactionEnvelope.class);
            return toGatewayTransaction(requireData(envelope));
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new WompiRateLimitedException(
                    "Wompi limitó la tasa de peticiones al consultar la transacción " + id,
                    retryAfterFrom(e));
        } catch (RestClientException e) {
            throw new WompiGatewayException(
                    "No se pudo consultar la transacción " + id + " en Wompi", e);
        }
    }

    @Override
    public Optional<GatewayTransaction> findByReference(String reference) {
        requireEnabled();
        try {
            TransactionListEnvelope envelope = restClient.get()
                    .uri(properties.baseUrl() + "/transactions?reference={reference}", reference)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.privateKey())
                    .retrieve().body(TransactionListEnvelope.class);
            if (envelope == null || envelope.data() == null || envelope.data().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(toGatewayTransaction(envelope.data().getFirst()));
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new WompiRateLimitedException("Wompi limitó la tasa de peticiones al consultar "
                    + "transacciones por referencia " + reference, retryAfterFrom(e));
        } catch (RestClientException e) {
            throw new WompiGatewayException(
                    "No se pudo consultar transacciones por referencia " + reference + " en Wompi",
                    e);
        }
    }

    /**
     * Delta-seconds (RFC 9110 §10.2.3), el único formato que Wompi documenta para
     * esta cabecera. Un valor ausente o no numérico deja que
     * {@code WompiRateLimitedException} aplique su mínimo de una hora.
     */
    private static Duration retryAfterFrom(HttpClientErrorException e) {
        HttpHeaders headers = e.getResponseHeaders();
        String value = headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null) {
            return null;
        }
        try {
            return Duration.ofSeconds(Long.parseLong(value.trim()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void requireEnabled() {
        if (!properties.enabled()) {
            throw new PaymentGatewayNotConfiguredException(
                    "Wompi no está habilitado (vetsoftware.payments.wompi.enabled=false)");
        }
    }

    private static TransactionData requireData(TransactionEnvelope envelope) {
        if (envelope == null || envelope.data() == null) {
            throw new WompiGatewayException("Wompi respondió sin datos de transacción");
        }
        return envelope.data();
    }

    /**
     * Un {@code status} que Wompi todavía no documenta se trata como
     * {@code PENDING} en vez de reventar: la transacción ya existe en Wompi -con su
     * {@code id}- y el sondeo o la conciliación posterior resuelven el estado real.
     * Interpretarlo mal no debe impedir asignar la referencia.
     */
    private static GatewayTransactionStatus toStatus(String status) {
        try {
            return GatewayTransactionStatus.valueOf(status);
        } catch (RuntimeException e) {
            return GatewayTransactionStatus.PENDING;
        }
    }

    private static GatewayTransaction toGatewayTransaction(TransactionData data) {
        Instant createdAt = data.createdAt() == null ? null : Instant.parse(data.createdAt());
        return new GatewayTransaction(data.id(), toStatus(data.status()), data.statusMessage(),
                data.reference(), data.amountInCents(), data.paymentMethodType(), createdAt);
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
    private record TransactionListEnvelope(List<TransactionData> data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TransactionData(String id, String status,
            @JsonProperty("status_message") String statusMessage, String reference,
            @JsonProperty("amount_in_cents") long amountInCents,
            @JsonProperty("payment_method_type") String paymentMethodType,
            @JsonProperty("created_at") String createdAt) {
    }
}
