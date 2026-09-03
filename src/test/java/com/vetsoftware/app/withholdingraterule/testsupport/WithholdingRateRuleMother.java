package com.vetsoftware.app.withholdingraterule.testsupport;

import com.vetsoftware.app.withholdingraterule.application.command.CreateWithholdingRateRuleCommand;
import com.vetsoftware.app.withholdingraterule.application.dto.WithholdingRateRuleDto;
import com.vetsoftware.app.withholdingraterule.domain.ServiceNature;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingRateRule;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tarifas de retencion de ejemplo, con <b>numeros reales y todos distintos
 * entre si</b>.
 *
 * <p>
 * Los valores no son decorativos. {@link #ICA_BOGOTA} es {@code 0.690000}: el
 * 6,9 <em>por mil</em> escrito como porcentaje, que es la unidad de la columna.
 * Es el numero con el que se ve si alguien confundio el porcentaje con la
 * fraccion ({@code 0.0069}) o con el por mil crudo ({@code 6.900000}), y el que
 * se pierde si alguien recorta la escala a dos decimales.
 *
 * <p>
 * Las dos bases minimas tampoco coinciden ni entre si ni con la tarifa: cruzar
 * {@code minimumBaseAmount} con {@code minimumBaseUvt} compila sin una queja,
 * los dos son {@code BigDecimal}, y con valores iguales ningun test lo veria.
 */
public final class WithholdingRateRuleMother {

    /**
     * Codigo DANE real de Bogota, ya sembrado en {@code cities} por el changeset
     * 114: la FK de {@code municipality_code} apunta a {@code cities.dane_code}, y
     * {@code uq_cities_dane_code} es GLOBAL, asi que una rodaja que lo insertara de
     * nuevo chocaria. Ver {@code WithholdingRateRulePersistenceIT}.
     */
    public static final String BOGOTA = "11001";

    /**
     * El segundo municipio, tambien con su codigo DANE real. Ver {@link #BOGOTA}.
     */
    public static final String MEDELLIN = "05001";

    /**
     * El 6,9 por mil de Bogota, en porcentaje y con los seis decimales de la
     * columna. Con dos decimales se retendria casi un uno por ciento de menos en
     * cada factura, en silencio.
     */
    public static final BigDecimal ICA_BOGOTA = new BigDecimal("0.690000");

    /** Retencion en la fuente por servicios: 11 %. */
    public static final BigDecimal RENTA_SERVICIOS = new BigDecimal("11.000000");

    public static final BigDecimal BASE_EN_PESOS = new BigDecimal("213010.00");
    public static final BigDecimal BASE_EN_UVT = new BigDecimal("4.00");

    public static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    public static final LocalDate HASTA = LocalDate.of(2027, 1, 1);
    public static final LocalDateTime CREADA_EL = LocalDateTime.of(2026, 1, 3, 8, 45, 0);

    private WithholdingRateRuleMother() {
    }

    /** Nacional y abierta: sin municipio, sin fecha de fin. */
    public static WithholdingRateRule nacional() {
        return new WithholdingRateRule(8301L, WithholdingType.INCOME_TAX,
                ServiceNature.TECHNICAL_SERVICE, null, RENTA_SERVICIOS, BASE_EN_PESOS, BASE_EN_UVT,
                "Art. 392 ET", DESDE, null, CREADA_EL, true, 0L);
    }

    /** Municipal y abierta: ICA de Bogota, con su tarifa por mil. */
    public static WithholdingRateRule ica() {
        return new WithholdingRateRule(8302L, WithholdingType.ICA, ServiceNature.CONSULTING, BOGOTA,
                ICA_BOGOTA, BASE_EN_PESOS, BASE_EN_UVT, "Acuerdo 65 de 2002", DESDE, null,
                CREADA_EL, true, 0L);
    }

    /** Nacional y ya cerrada: el historico que sigue explicando facturas viejas. */
    public static WithholdingRateRule cerrada() {
        return new WithholdingRateRule(8303L, WithholdingType.VAT, ServiceNature.SOFTWARE_LICENSING,
                null, new BigDecimal("15.000000"), BASE_EN_PESOS, null, "Art. 437-1 ET", DESDE,
                HASTA, CREADA_EL, true, 3L);
    }

    /** Constructor sin id ni version, tal como sale de {@code create}. */
    public static WithholdingRateRule nueva() {
        return WithholdingRateRule.create(WithholdingType.INCOME_TAX,
                ServiceNature.TECHNICAL_SERVICE, null, RENTA_SERVICIOS, BASE_EN_PESOS, BASE_EN_UVT,
                "Art. 392 ET", DESDE, null, CREADA_EL);
    }

    public static WithholdingRateRuleDto dtoNacional() {
        return WithholdingRateRuleDto.from(nacional());
    }

    public static WithholdingRateRuleDto dtoIca() {
        return WithholdingRateRuleDto.from(ica());
    }

    /**
     * Alta nacional: sin municipio, que es lo que el dominio exige fuera de ICA.
     */
    public static CreateWithholdingRateRuleCommand comandoNacional() {
        return new CreateWithholdingRateRuleCommand(WithholdingType.INCOME_TAX,
                ServiceNature.TECHNICAL_SERVICE, null, RENTA_SERVICIOS, BASE_EN_PESOS, BASE_EN_UVT,
                "Art. 392 ET", DESDE, null);
    }

    /** Alta de ICA: con municipio, que es lo que el dominio exige en ICA. */
    public static CreateWithholdingRateRuleCommand comandoIca() {
        return new CreateWithholdingRateRuleCommand(WithholdingType.ICA, ServiceNature.CONSULTING,
                BOGOTA, ICA_BOGOTA, BASE_EN_PESOS, BASE_EN_UVT, "Acuerdo 65 de 2002", DESDE, null);
    }
}
