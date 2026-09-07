package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

/**
 * {@code gateway_webhook_events} — rastro auditable de todo webhook de Wompi
 * <strong>autenticado</strong>, se procese o no
 * ({@code docs/db/pasarela-wompi-modelo.md}, changeset 411). Un cuerpo no
 * parseable o con checksum inválido no deja fila; ver
 * {@code GatewayWebhookOutcome}.
 *
 * <p>
 * <strong>Sin asociacion a {@code CompanyJpaEntity}, ni siquiera un
 * {@code company_id} escalar.</strong> El evento tiene que poder persistirse
 * incluso cuando no se encuentra el pago correspondiente —para eso existe esta
 * tabla—, y llega antes de saber a que empresa pertenece: mismo criterio que
 * {@code gateway_settlements} (326). Colgar aqui una asociacion que alcance
 * {@code CompanyJpaEntity} activaria de golpe las cuatro reglas duras de BE-COV
 * sobre toda la feature.
 *
 * <p>
 * <strong>Con {@code @Version}</strong>: la fila se escribe dos veces —alta sin
 * procesar, luego relleno de {@code processedAt}/{@code processingOutcome}—,
 * una segunda escritura declarada y no una exencion de
 * {@code ENTIDADES_EXENTAS_DE_VERSION}.
 *
 * <p>
 * <strong>{@code gateway}, {@code eventType}, {@code eventChecksum} y
 * {@code gatewayReference} no llevan {@code columnDefinition}.</strong> Su
 * juego de caracteres {@code ascii} y su colacion {@code ascii_bin} los fija el
 * changeset 411 con un {@code MODIFY COLUMN}; declararlos otra vez aqui seria
 * duplicar la decision en dos sitios que pueden divergir, y el que manda es el
 * esquema.
 */
@Entity
@Table(name = "gateway_webhook_events")
public class WompiWebhookEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gateway", nullable = false, length = 40)
    private String gateway;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "event_checksum", nullable = false, length = 64)
    private String eventChecksum;

    @Column(name = "gateway_reference", length = 120)
    private String gatewayReference;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "raw_body", nullable = false, columnDefinition = "TEXT")
    private String rawBody;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processing_outcome", length = 30)
    private String processingOutcome;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected WompiWebhookEventJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventChecksum() {
        return eventChecksum;
    }

    public void setEventChecksum(String eventChecksum) {
        this.eventChecksum = eventChecksum;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public void setGatewayReference(String gatewayReference) {
        this.gatewayReference = gatewayReference;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getRawBody() {
        return rawBody;
    }

    public void setRawBody(String rawBody) {
        this.rawBody = rawBody;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getProcessingOutcome() {
        return processingOutcome;
    }

    public void setProcessingOutcome(String processingOutcome) {
        this.processingOutcome = processingOutcome;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
