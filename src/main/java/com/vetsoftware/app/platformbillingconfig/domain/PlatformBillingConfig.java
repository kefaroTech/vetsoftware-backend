package com.vetsoftware.app.platformbillingconfig.domain;

import java.time.LocalDateTime;

/**
 * Las políticas de facturación de la plataforma, en un solo sitio. Cambiar
 * cuántos días de gracia se conceden o qué día del mes se emiten los cobros es
 * <b>editar un formulario, no desplegar una versión</b>: si alguna de estas
 * cifras vuelve a aparecer literal en un {@code if}, esta clase no ha servido
 * de nada.
 *
 * <h2>Es una sola fila, y lo garantiza el esquema</h2> La tabla lleva una
 * columna {@code singleton TINYINT NOT NULL} con {@code UNIQUE (singleton)} y
 * {@code CHECK (singleton = 1)}: el CHECK impide cualquier otro valor y el
 * UNIQUE impide un segundo 1. No puede haber dos configuraciones compitiendo
 * por mandar.
 *
 * <h2>Alta: no hay. El ciclo es leer y actualizar</h2> Esta clase no expone
 * ningún {@code create(...)} y la feature no tiene caso de uso de alta. La fila
 * la siembra el changeset que crea la tabla
 * ({@code suscripciones-datos-semilla.md} §5.5), y desde ahí solo se lee y se
 * actualiza. Se descartó el <i>upsert</i> idempotente —la otra opción legítima—
 * porque contradice la garantía que hace valiosa a la tabla: un upsert que no
 * encuentra la fila la inventaría con valores que nadie decidió, y el fallo de
 * despliegue que debía gritar quedaría enterrado bajo una política silenciosa.
 * Por eso la ausencia de la fila es
 * {@link PlatformBillingConfigNotConfiguredException} y no un alta implícita.
 *
 * <h2>Sin {@code enabled}: choque C5, decisión deliberada</h2> Es la única fila
 * de la tabla. Con {@code @SQLRestriction("enabled = true")}, desactivarla
 * dejaría la plataforma sin políticas y <b>sin ninguna forma de volver atrás
 * desde la interfaz</b>, porque el propio formulario de edición dejaría de
 * encontrarla. No hay borrado lógico ni físico: no hay {@code enable()},
 * {@code disable()} ni {@code delete}.
 *
 * <h2>Lo que deliberadamente NO se modela</h2> Ninguna de estas políticas puede
 * llegar a cortar el acceso. El máximo estado de restricción del producto es
 * <b>solo lectura</b>: una cuenta vencida consulta e imprime, no crea ni
 * modifica. No existe —ni debe añadirse— un campo que permita configurar un
 * corte total de acceso.
 */
public class PlatformBillingConfig {
    private final Long id;
    private PriceListRef defaultPriceList;
    private int defaultGraceDays;
    private int defaultTrialDays;
    private int invoiceDayOfMonth;
    private int defaultPaymentTermDays;
    private String externalBillingProvider;
    private final LocalDateTime createdDate;
    private final Long version;

    /**
     * Reconstituye el agregado. Es el único constructor y lo usa la capa de
     * persistencia: no hay factory de alta porque la fila la siembra el esquema
     * (ver el javadoc de la clase).
     */
    public PlatformBillingConfig(Long id, PriceListRef defaultPriceList, Integer defaultGraceDays,
            Integer defaultTrialDays, Integer invoiceDayOfMonth, Integer defaultPaymentTermDays,
            String externalBillingProvider, LocalDateTime createdDate, Long version) {
        validate(defaultGraceDays, defaultTrialDays, invoiceDayOfMonth, defaultPaymentTermDays,
                externalBillingProvider, createdDate);
        this.id = id;
        this.defaultPriceList = defaultPriceList;
        this.defaultGraceDays = defaultGraceDays;
        this.defaultTrialDays = defaultTrialDays;
        this.invoiceDayOfMonth = invoiceDayOfMonth;
        this.defaultPaymentTermDays = defaultPaymentTermDays;
        this.externalBillingProvider = externalBillingProvider;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Reemplaza las políticas en bloque. Es la única mutación de la feature: no hay
     * alta ni baja, solo esto.
     *
     * @param defaultPriceList
     *            tarifa por defecto, o {@code null} para dejar la plataforma sin
     *            tarifa por defecto (la columna es nulable)
     */
    public void update(PriceListRef defaultPriceList, Integer defaultGraceDays,
            Integer defaultTrialDays, Integer invoiceDayOfMonth, Integer defaultPaymentTermDays,
            String externalBillingProvider) {
        validate(defaultGraceDays, defaultTrialDays, invoiceDayOfMonth, defaultPaymentTermDays,
                externalBillingProvider, this.createdDate);
        this.defaultPriceList = defaultPriceList;
        this.defaultGraceDays = defaultGraceDays;
        this.defaultTrialDays = defaultTrialDays;
        this.invoiceDayOfMonth = invoiceDayOfMonth;
        this.defaultPaymentTermDays = defaultPaymentTermDays;
        this.externalBillingProvider = externalBillingProvider;
    }

    /**
     * Espeja las constraints CHECK de la tabla. Cada regla de aquí tiene su gemela
     * en el esquema: si una de las dos cambia, cambian las dos.
     */
    private static void validate(Integer defaultGraceDays, Integer defaultTrialDays,
            Integer invoiceDayOfMonth, Integer defaultPaymentTermDays,
            String externalBillingProvider, LocalDateTime createdDate) {
        if (defaultGraceDays == null)
            throw new IllegalArgumentException("defaultGraceDays is required");
        if (defaultGraceDays < 0)
            throw new IllegalArgumentException("defaultGraceDays cannot be negative");
        if (defaultTrialDays == null)
            throw new IllegalArgumentException("defaultTrialDays is required");
        if (defaultTrialDays < 0)
            throw new IllegalArgumentException("defaultTrialDays cannot be negative");
        if (invoiceDayOfMonth == null)
            throw new IllegalArgumentException("invoiceDayOfMonth is required");
        // 29, 30 y 31 no existen en todos los meses: aceptarlos significa que en
        // febrero la emisión no corre, o corre un día que nadie decidió.
        if (invoiceDayOfMonth < 1 || invoiceDayOfMonth > 28)
            throw new IllegalArgumentException("invoiceDayOfMonth must be between 1 and 28");
        if (defaultPaymentTermDays == null)
            throw new IllegalArgumentException("defaultPaymentTermDays is required");
        // Cero es válido y significa pago inmediato; negativo haría vencer la
        // factura antes de emitirla.
        if (defaultPaymentTermDays < 0)
            throw new IllegalArgumentException("defaultPaymentTermDays cannot be negative");
        if (externalBillingProvider != null && externalBillingProvider.isBlank())
            throw new IllegalArgumentException(
                    "externalBillingProvider must be null or a non-blank value");
        if (externalBillingProvider != null && externalBillingProvider.length() > 40)
            throw new IllegalArgumentException("externalBillingProvider must be 40 chars or less");
        if (createdDate == null)
            throw new IllegalArgumentException("createdDate is required");
    }

    public Long getId() {
        return id;
    }

    public PriceListRef getDefaultPriceList() {
        return defaultPriceList;
    }

    /**
     * Días de cortesía tras el vencimiento antes de pasar la cuenta a solo lectura.
     */
    public int getDefaultGraceDays() {
        return defaultGraceDays;
    }

    public int getDefaultTrialDays() {
        return defaultTrialDays;
    }

    /** Día del mes en que se emiten los cobros de suscripción. Entre 1 y 28. */
    public int getInvoiceDayOfMonth() {
        return invoiceDayOfMonth;
    }

    /**
     * Días desde la emisión hasta el vencimiento de la factura. Cero = pago
     * inmediato.
     */
    public int getDefaultPaymentTermDays() {
        return defaultPaymentTermDays;
    }

    /**
     * Sistema con el que se emiten las facturas de suscripción fuera de este
     * software. Documenta dónde vive la otra mitad del circuito; {@code null} si
     * todavía no se ha decidido.
     */
    public String getExternalBillingProvider() {
        return externalBillingProvider;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
