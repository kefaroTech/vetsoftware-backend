package com.vetsoftware.app.pricelist.testsupport;

import com.vetsoftware.app.pricelist.application.dto.PublicPlanComponentRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPriceListDto;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fixtures del <em>read model publico</em> de {@code pricelist}: las filas
 * planas que devuelve {@code PublicPlanQueryPort} antes de que
 * {@code GetPublicPlansService} las agrupe.
 *
 * <p>
 * Vive aparte de {@link PriceListMother} porque son dos mundos distintos: aquel
 * construye el agregado {@code PriceList} para el lado de administracion, este
 * las proyecciones que el mundo puede ver. Mezclarlos invitaria a que un test
 * del catalogo publico se apoyara en un objeto que lleva {@code status},
 * {@code publishedBy} y demas, que es justo lo que aqui no existe.
 */
public final class PublicPlanMother {

    /** El dia contra el que se decide la vigencia en los tests de este slice. */
    public static final LocalDate HOY = LocalDate.of(2026, 8, 28);

    public static final Long TARIFA_VIGENTE_ID = 500L;
    public static final Long TARIFA_CADUCADA_ID = 400L;
    public static final Long TARIFA_FUTURA_ID = 600L;

    public static final String PLAN = "ESENCIAL";

    private PublicPlanMother() {
    }

    public static PublicPriceListDto tarifa(Long id, LocalDate desde, LocalDate hasta) {
        return new PublicPriceListDto(id, "COP", desde, hasta);
    }

    /** Ventana abierta que cubre {@link #HOY}: la lista viva del catalogo. */
    public static PublicPriceListDto tarifaVigente() {
        return tarifa(TARIFA_VIGENTE_ID, LocalDate.of(2026, 8, 1), null);
    }

    /** Publicada, pero su ventana acabo el ano pasado (D-73). */
    public static PublicPriceListDto tarifaCaducada() {
        return tarifa(TARIFA_CADUCADA_ID, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
    }

    /** Publicada y firmada, pero todavia no rige. */
    public static PublicPriceListDto tarifaFutura() {
        return tarifa(TARIFA_FUTURA_ID, LocalDate.of(2027, 1, 1), null);
    }

    /** El paquete con precio en los dos ciclos, gravado al 19 %. */
    public static PublicPlanRowDto plan() {
        return plan(PLAN);
    }

    public static PublicPlanRowDto plan(String code) {
        return new PublicPlanRowDto(code, "Plan " + code, "Para una clinica que empieza",
                new BigDecimal("89000.00"), new BigDecimal("890000.00"),
                new BigDecimal("150000.00"), new BigDecimal("19.00"), TaxTreatment.TAXED);
    }

    /** Modulo que el paquete enciende, con los dias de prueba que concede. */
    public static PublicPlanComponentRowDto moduloConPrueba(String planCode) {
        return new PublicPlanComponentRowDto(planCode, "AGENDA", "Agenda", null, 1, 30, null, null);
    }

    /** Modulo {@code NEVER_FREE}: {@code trialDays} nulo es «no hay prueba». */
    public static PublicPlanComponentRowDto moduloSinPrueba(String planCode) {
        return new PublicPlanComponentRowDto(planCode, "CAJA", "Caja", null, 1, null, null, null);
    }

    /**
     * Contador: lo que lo distingue de un modulo es {@code capacityUnit}, no un
     * {@code ItemType} que este slice no importa.
     *
     * <p>
     * Tarifado en los <b>dos</b> ciclos, y el anual <em>no</em> es el mensual por
     * doce (180.000) ni por diez (150.000): 145.000 no es ningun multiplo de
     * 15.000. Una fixture donde el anual fuera un multiplo redondo del mensual
     * dejaria pasar un servicio que extrapolara en vez de leer la columna.
     */
    public static PublicPlanComponentRowDto contador(String planCode) {
        return new PublicPlanComponentRowDto(planCode, "EXTRA_USER", "Usuario adicional", "USER", 3,
                null, new BigDecimal("15000.00"), new BigDecimal("145000.00"));
    }

    /**
     * Contador que solo se vende suelto al mes: el importe anual es nulo y eso
     * significa «no se ofrece esa unidad adicional en ciclo anual», que es lo mismo
     * que la contratacion decidira con su {@code JOIN} por ciclo.
     */
    public static PublicPlanComponentRowDto contadorSoloMensual(String planCode) {
        return new PublicPlanComponentRowDto(planCode, "EXTRA_BRANCH", "Sede adicional", "BRANCH",
                1, null, new BigDecimal("45000.00"), null);
    }
}
