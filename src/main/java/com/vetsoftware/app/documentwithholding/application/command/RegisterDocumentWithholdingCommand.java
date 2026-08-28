package com.vetsoftware.app.documentwithholding.application.command;

import com.vetsoftware.app.documentwithholding.domain.WithholdingType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param companyId
 *            la empresa a la que se la practicaron. Viaja en el command porque
 *            este caso de uso es de plataforma y solo de plataforma; en el
 *            camino de tenant seria la fuga que {@code CLAUDE.md} prohibe, pero
 *            ese camino no existe aqui. En HTTP llega como
 *            {@code @RequestParam}, nunca en el cuerpo
 *            ({@code EMPRESA_NO_VIAJA_EN_EL_CUERPO})
 * @param ratePercent
 *            la tarifa en <strong>porcentaje</strong>, no en fraccion, y con
 *            seis decimales. Las de industria y comercio se expresan por mil:
 *            6,9 por mil se escribe {@code 0.690000}
 * @param municipalityCode
 *            codigo DIVIPOLA del municipio. Obligatorio si y solo si el tipo es
 *            {@code ICA}; el dominio lo comprueba
 * @param fiscalPeriodKey
 *            {@code YYYY-A} en renta y {@code YYYY-B01}..{@code YYYY-B06} en
 *            IVA e ICA, con el ano coincidiendo con {@code fiscalYear}
 */
public record RegisterDocumentWithholdingCommand(Long companyId, Long billingDocumentId,
        WithholdingType type, BigDecimal taxableBase, BigDecimal ratePercent, BigDecimal amount,
        String municipalityCode, int fiscalYear, String fiscalPeriodKey, LocalDate practicedOn) {
}
