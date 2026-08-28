package com.vetsoftware.app.withholdingraterule.application.command;

import com.vetsoftware.app.withholdingraterule.domain.ServiceNature;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <strong>Sin {@code companyId}, y aqui ni siquiera es la regla de siempre: es
 * que no existe la columna.</strong> {@code withholding_rate_rules} es un
 * catalogo global —la tarifa depende del tipo de retencion, de la naturaleza
 * del servicio y del municipio, no del cliente— y anadirle una empresa seria
 * modelar mal, no proteger mejor. Lo que si depende del cliente, si es agente
 * de retencion, vive en {@code company_billing_profiles}.
 *
 * @param municipalityCode
 *            codigo DIVIPOLA de cinco digitos. Obligatorio si y solo si
 *            {@code withholdingType} es {@code ICA}: es el unico de los tres
 *            que fija cada municipio
 * @param ratePercent
 *            <b>porcentaje, no fraccion</b>, con hasta seis decimales. El ICA
 *            de Bogota son 6,9 por mil y se escribe {@code 0.690000}
 * @param minimumBaseAmount
 *            base minima en pesos. Envejece cada ano, por eso convive con la de
 *            UVT y por eso al menos una de las dos tiene que venir
 * @param minimumBaseUvt
 *            base minima en unidades de valor tributario. La que no envejece
 * @param validTo
 *            nulo abre la vigencia; con fecha entra ya cerrada, que es como se
 *            carga el historico
 */
public record CreateWithholdingRateRuleCommand(WithholdingType withholdingType,
        ServiceNature serviceNature, String municipalityCode, BigDecimal ratePercent,
        BigDecimal minimumBaseAmount, BigDecimal minimumBaseUvt, String legalReference,
        LocalDate validFrom, LocalDate validTo) {
}
