package com.vetsoftware.app.quote.testsupport;

import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.domain.CatalogItemRef;
import com.vetsoftware.app.quote.domain.CatalogPriceRef;
import com.vetsoftware.app.quote.domain.CompanyRef;
import com.vetsoftware.app.quote.domain.ConfiguratorQuestionRef;
import com.vetsoftware.app.quote.domain.PriceListRef;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteAnswer;
import com.vetsoftware.app.quote.domain.QuoteItemType;
import com.vetsoftware.app.quote.domain.QuoteLine;
import com.vetsoftware.app.quote.domain.QuoteStatus;
import com.vetsoftware.app.quote.domain.QuoteSummary;
import com.vetsoftware.app.quote.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Fixtures de la feature quote. Valores validos por defecto, una variante por
 * metodo.
 */
public final class QuoteMother {

    public static final LocalDate HOY = LocalDate.of(2026, 8, 22);
    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 22, 10, 30);
    public static final LocalDate VIGENTE_HASTA = LocalDate.of(2026, 9, 30);
    public static final Long PRICE_LIST_ID = 7L;
    public static final String CLIENT_REQUEST_ID = "req-0001";
    public static final String NUMERO = "COT-2026-00184";

    private QuoteMother() {
    }

    public static CompanyRef empresa() {
        return new CompanyRef(42L, "Clinica Norte", "900123456");
    }

    public static PriceListRef tarifa() {
        return new PriceListRef(PRICE_LIST_ID, "LISTA-2026-01", "COP");
    }

    public static CatalogItemRef modulo() {
        return new CatalogItemRef(1L, "CLINICAL_HISTORY", "Historia clinica", QuoteItemType.MODULE);
    }

    public static CatalogItemRef usuarioExtra() {
        return new CatalogItemRef(2L, "EXTRA_USER", "Usuario adicional", QuoteItemType.CAPACITY);
    }

    public static ConfiguratorQuestionRef pregunta() {
        return new ConfiguratorQuestionRef(11L, "SELLS_PRODUCTS");
    }

    /** Precio gravado al 19 %, sin unidades incluidas. */
    public static CatalogPriceRef precioGravado(String unitAmount) {
        return new CatalogPriceRef(new BigDecimal(unitAmount), new BigDecimal("19.00"),
                TaxTreatment.TAXED, 0);
    }

    /** Precio gravado al 19 % que ya trae unidades incluidas (regla R15). */
    public static CatalogPriceRef precioConIncluidas(String unitAmount, int incluidas) {
        return new CatalogPriceRef(new BigDecimal(unitAmount), new BigDecimal("19.00"),
                TaxTreatment.TAXED, incluidas);
    }

    /** Precio excluido de IVA: tarifa obligatoriamente cero. */
    public static CatalogPriceRef precioExcluido(String unitAmount) {
        return new CatalogPriceRef(new BigDecimal(unitAmount), BigDecimal.ZERO,
                TaxTreatment.EXCLUDED, 0);
    }

    /** Linea de 100.000 gravada al 19 %, sin descuento: total 119.000. */
    public static QuoteLine lineaModulo() {
        return QuoteLine.freeze(1, modulo(), precioGravado("100000.00"), 1, BigDecimal.ZERO, AHORA);
    }

    public static QuoteLine lineaConDescuento(String descuentoPorcentaje) {
        return QuoteLine.freeze(1, modulo(), precioGravado("100000.00"), 1,
                new BigDecimal(descuentoPorcentaje), AHORA);
    }

    public static QuoteAnswer respuesta() {
        return QuoteAnswer.capture(pregunta(), 99L, "SI", AHORA);
    }

    /** Borrador a un cliente existente. */
    public static Quote borrador(List<QuoteLine> lineas) {
        return Quote.create(NUMERO, empresa(), null, null, null, null, PRICE_LIST_ID,
                BillingCycle.MONTHLY, VIGENTE_HASTA, 0, CLIENT_REQUEST_ID, lineas,
                List.of(respuesta()), AHORA);
    }

    public static Quote borrador() {
        return borrador(List.of(lineaModulo()));
    }

    /** Borrador a un prospecto: sin empresa, que es el caso raro del modelo. */
    public static Quote borradorDeProspecto() {
        return Quote.create(NUMERO, null, "Veterinaria del Sur", "ana@ejemplo.com", "12345678",
                "3001112233", PRICE_LIST_ID, BillingCycle.MONTHLY, VIGENTE_HASTA, 15,
                CLIENT_REQUEST_ID, List.of(lineaModulo()), List.of(), AHORA);
    }

    /** Ya enviada: el estado desde el que se acepta o se rechaza. */
    public static Quote enviada() {
        Quote quote = borrador();
        quote.send(HOY);
        return quote;
    }

    /**
     * Reconstruye una cotizacion tal como vuelve de la base, con id y version. Se
     * usa para los caminos que necesitan un agregado ya persistido.
     */
    public static Quote persistida(Long id, QuoteStatus status, LocalDate validUntil,
            List<QuoteLine> lineas) {
        return new Quote(id, NUMERO, empresa(), null, null, null, null, PRICE_LIST_ID,
                BillingCycle.MONTHLY, sumaBruta(lineas), sumaDescuento(lineas), sumaIva(lineas),
                sumaTotal(lineas), status, validUntil, 0,
                status == QuoteStatus.ACCEPTED ? AHORA : null, null, null, CLIENT_REQUEST_ID, AHORA,
                3L, true, lineas, List.of());
    }

    public static Quote persistida(Long id, QuoteStatus status) {
        return persistida(id, status, VIGENTE_HASTA, List.of(lineaModulo()));
    }

    /**
     * Persistida CON respuestas del configurador. La variante de arriba las deja
     * vacias, y el JSON de la respuesta web tiene que enseñar las dos colecciones.
     */
    public static Quote persistidaConRespuestas(Long id) {
        List<QuoteLine> lineas = List.of(lineaModulo());
        return new Quote(id, NUMERO, empresa(), null, null, null, null, PRICE_LIST_ID,
                BillingCycle.MONTHLY, sumaBruta(lineas), sumaDescuento(lineas), sumaIva(lineas),
                sumaTotal(lineas), QuoteStatus.DRAFT, VIGENTE_HASTA, 0, null, null, null,
                CLIENT_REQUEST_ID, AHORA, 3L, true, lineas, List.of(respuesta()));
    }

    /**
     * Persistida a un prospecto: {@code company} nula. Es la rama que el mapeo web
     * tiene que soportar sin reventar, y la unica que ejercita el null de
     * {@code toCompanySummary}.
     */
    public static Quote persistidaDeProspecto(Long id) {
        List<QuoteLine> lineas = List.of(lineaModulo());
        return new Quote(id, NUMERO, null, "Veterinaria del Sur", "ana@ejemplo.com", "12345678",
                "3001112233", PRICE_LIST_ID, BillingCycle.MONTHLY, sumaBruta(lineas),
                sumaDescuento(lineas), sumaIva(lineas), sumaTotal(lineas), QuoteStatus.DRAFT,
                VIGENTE_HASTA, 15, null, null, null, CLIENT_REQUEST_ID, AHORA, 3L, true, lineas,
                List.of());
    }

    /** Resumen de una oferta a prospecto: sin empresa en la proyeccion. */
    public static QuoteSummary resumenDeProspecto(Long id) {
        List<QuoteLine> lineas = List.of(lineaModulo());
        return new QuoteSummary(id, NUMERO, null, "Veterinaria del Sur", "ana@ejemplo.com",
                PRICE_LIST_ID, BillingCycle.MONTHLY, sumaBruta(lineas), sumaDescuento(lineas),
                sumaIva(lineas), sumaTotal(lineas), QuoteStatus.DRAFT, VIGENTE_HASTA, 15, null,
                AHORA, true);
    }

    /**
     * La proyeccion de cabecera que devuelven los listados. Deliberadamente NO
     * lleva lineas: es lo que permite paginar sin arrastrar las colecciones.
     */
    public static QuoteSummary resumen(Long id, QuoteStatus status) {
        List<QuoteLine> lineas = List.of(lineaModulo());
        return new QuoteSummary(id, NUMERO, empresa(), null, null, PRICE_LIST_ID,
                BillingCycle.MONTHLY, sumaBruta(lineas), sumaDescuento(lineas), sumaIva(lineas),
                sumaTotal(lineas), status, VIGENTE_HASTA, 0, null, AHORA, true);
    }

    private static BigDecimal sumaBruta(List<QuoteLine> lineas) {
        return lineas.stream().map(QuoteLine::grossAmount).reduce(cero(), BigDecimal::add);
    }

    private static BigDecimal sumaDescuento(List<QuoteLine> lineas) {
        return lineas.stream().map(QuoteLine::getDiscountAmount).reduce(cero(), BigDecimal::add);
    }

    private static BigDecimal sumaIva(List<QuoteLine> lineas) {
        return lineas.stream().map(QuoteLine::getTaxAmount).reduce(cero(), BigDecimal::add);
    }

    private static BigDecimal sumaTotal(List<QuoteLine> lineas) {
        return lineas.stream().map(QuoteLine::getLineTotal).reduce(cero(), BigDecimal::add);
    }

    private static BigDecimal cero() {
        return BigDecimal.ZERO.setScale(2);
    }
}
