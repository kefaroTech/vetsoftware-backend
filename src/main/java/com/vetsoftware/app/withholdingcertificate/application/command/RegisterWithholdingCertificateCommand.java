package com.vetsoftware.app.withholdingcertificate.application.command;

import com.vetsoftware.app.withholdingcertificate.domain.WithholdingType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param companyId
 *            la empresa a la que le practicaron la retencion. Viaja en el
 *            command porque este caso de uso es de plataforma y solo de
 *            plataforma; en el camino de tenant seria la fuga que
 *            {@code CLAUDE.md} prohibe, pero ese camino no existe aqui. En el
 *            cuerpo HTTP no viaja: llega como {@code @RequestParam}
 * @param fiscalYear
 *            ano gravable. Sin el, la retencion no se puede imputar a ninguna
 *            declaracion y se acaba pagando dos veces el mismo impuesto
 * @param ratePercent
 *            tarifa en PORCENTAJE, no en fraccion. Las de ICA se expresan por
 *            mil -6,9 por mil es 0,69- y por eso admite seis decimales
 * @param legalDeadlineOn
 *            ultimo dia habil de marzo. Llega como dato y no se calcula:
 *            derivarla necesita el calendario de festivos, que es de otra capa
 */
public record RegisterWithholdingCertificateCommand(Long companyId, String issuedByTaxId,
        String certificateNumber, WithholdingType withholdingType, Integer fiscalYear,
        String fiscalPeriodKey, BigDecimal ratePercent, BigDecimal certifiedAmount,
        LocalDate issuedOn, LocalDate legalDeadlineOn) {
}
