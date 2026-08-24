package com.vetsoftware.app.platformbillingconfig.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tabla singleton de políticas de facturación.
 *
 * <p>
 * <b>Sin {@code @SQLDelete} ni {@code @SQLRestriction}</b>: la tabla no lleva
 * {@code enabled} (choque C5). No hay borrado lógico que versionar, así que
 * tampoco aplica la cláusula {@code AND version = ?} de
 * {@code BORRADO_LOGICO_RESPETA_LA_VERSION}.
 *
 * <p>
 * <b>La FK a la tarifa se mapea como columna, no como asociación.</b> Es
 * deliberado: colgar un {@code @ManyToOne PriceListJpaEntity} metería en el
 * grafo de esta feature todo lo que esa entidad alcance, y basta con que alguna
 * asociación llegue a {@code CompanyJpaEntity} para que las cuatro reglas duras
 * de BE-COV se activen sobre el slice entero —un slice que, por definición, no
 * tiene empresa a la que acotar—. El {@code PriceListRef} lo hidrata
 * {@code JpaPriceListQueryPort} con una consulta aparte; en una tabla de una
 * sola fila el coste es irrelevante y no hay N+1 posible.
 */
@Entity
@Table(name = "platform_billing_config", uniqueConstraints = {
        @UniqueConstraint(name = "uq_platform_billing_config_singleton", columnNames = {
                "singleton"})})
public class PlatformBillingConfigJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Siempre 1. Con {@code UNIQUE (singleton)} y {@code CHECK (singleton = 1)} en
     * el esquema, esta columna es lo que garantiza que la tabla no pueda tener una
     * segunda fila.
     *
     * <p>
     * Es {@code byte} y no {@code boolean}: no representa un sí/no, representa un
     * discriminador constante. Hibernate mapea {@code byte} a
     * {@code Types.TINYINT}, que es exactamente lo que declara el esquema. La
     * columna debe declararse {@code TINYINT} pelado en Liquibase —nunca
     * {@code TINYINT(1)}, que el driver reporta como {@code BIT} y rompe
     * {@code ddl-auto: validate}—.
     *
     * <p>
     * No tiene setter: el valor lo fija el inicializador, y como
     * {@code PlatformBillingConfigJpaMapper#toJpa} construye siempre una instancia
     * nueva, todo {@code merge} escribe 1.
     */
    @Column(name = "singleton", nullable = false)
    private byte singleton = 1;

    @Column(name = "default_price_list_id")
    private Long defaultPriceListId;

    @Column(name = "default_grace_days", nullable = false)
    private int defaultGraceDays;

    @Column(name = "default_trial_days", nullable = false)
    private int defaultTrialDays;

    @Column(name = "invoice_day_of_month", nullable = false)
    private int invoiceDayOfMonth;

    @Column(name = "default_payment_term_days", nullable = false)
    private int defaultPaymentTermDays;

    @Column(name = "external_billing_provider", length = 40)
    private String externalBillingProvider;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected PlatformBillingConfigJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public byte getSingleton() {
        return singleton;
    }

    public Long getDefaultPriceListId() {
        return defaultPriceListId;
    }

    public void setDefaultPriceListId(Long defaultPriceListId) {
        this.defaultPriceListId = defaultPriceListId;
    }

    public int getDefaultGraceDays() {
        return defaultGraceDays;
    }

    public void setDefaultGraceDays(int defaultGraceDays) {
        this.defaultGraceDays = defaultGraceDays;
    }

    public int getDefaultTrialDays() {
        return defaultTrialDays;
    }

    public void setDefaultTrialDays(int defaultTrialDays) {
        this.defaultTrialDays = defaultTrialDays;
    }

    public int getInvoiceDayOfMonth() {
        return invoiceDayOfMonth;
    }

    public void setInvoiceDayOfMonth(int invoiceDayOfMonth) {
        this.invoiceDayOfMonth = invoiceDayOfMonth;
    }

    public int getDefaultPaymentTermDays() {
        return defaultPaymentTermDays;
    }

    public void setDefaultPaymentTermDays(int defaultPaymentTermDays) {
        this.defaultPaymentTermDays = defaultPaymentTermDays;
    }

    public String getExternalBillingProvider() {
        return externalBillingProvider;
    }

    public void setExternalBillingProvider(String externalBillingProvider) {
        this.externalBillingProvider = externalBillingProvider;
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
