package com.vetsoftware.app.withholdingcertificate.infrastructure.web.request;

import com.vetsoftware.app.withholdingcertificate.domain.WithholdingType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <strong>Sin {@code companyId}, y no por el motivo de siempre.</strong> En un
 * recurso scoped al usuario la empresa se omite porque el cliente podria
 * suplantar a otra clinica; aqui la ruta es de plataforma y ese riesgo no
 * aplica, porque el puerto esta cerrado a {@code hasRole('SYSTEM')} a secas. La
 * razon es otra: la regla dura {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} mira
 * <em>todo</em> {@code @RequestBody} sin mirar la ruta ni el rol. Tesoreria
 * sigue eligiendo de que clinica es el certificado: el {@code companyId} viaja
 * como {@code @RequestParam}, que es la forma que la regla si permite.
 *
 * @param fiscalPeriodKey
 *            {@code YYYY-A} si el impuesto es renta, {@code YYYY-B01} a
 *            {@code YYYY-B06} si es IVA o ICA, y en los dos casos el ano tiene
 *            que ser el mismo {@code fiscalYear}. La regla completa la valida
 *            el dominio, que es donde vive; aqui solo se acota la longitud
 * @param ratePercent
 *            tarifa en PORCENTAJE. Seis decimales porque las de ICA se expresan
 *            por mil: 6,9 por mil es 0,69
 * @param legalDeadlineOn
 *            ultimo dia habil de marzo. Se recibe y se guarda: calcularla
 *            necesita el calendario de festivos, que es de otra capa
 */
public record RegisterWithholdingCertificateRequest(
        @NotBlank(message = "Debes indicar el NIT de quien expide el certificado.") @Size(max = 50, message = "El NIT no puede superar los 50 caracteres.") String issuedByTaxId,
        @NotBlank(message = "Debes indicar el numero del certificado.") @Size(max = 50, message = "El numero del certificado no puede superar los 50 caracteres.") String certificateNumber,
        @NotNull(message = "Debes indicar que impuesto se retuvo.") WithholdingType withholdingType,
        @NotNull(message = "El ano gravable es obligatorio.") @Min(value = 2020, message = "El ano gravable no puede ser anterior a 2020.") @Max(value = 2100, message = "El ano gravable no puede ser posterior a 2100.") Integer fiscalYear,
        @NotBlank(message = "Debes indicar el periodo fiscal del certificado.") @Size(max = 10, message = "El periodo fiscal no puede superar los 10 caracteres.") String fiscalPeriodKey,
        @NotNull(message = "La tarifa de retencion es obligatoria.") @Positive(message = "La tarifa de retencion debe ser mayor que cero.") @DecimalMax(value = "100", message = "La tarifa de retencion no puede superar el 100 por ciento.") @Digits(integer = 3, fraction = 6, message = "La tarifa admite hasta seis decimales.") BigDecimal ratePercent,
        @NotNull(message = "El valor certificado es obligatorio.") @Positive(message = "El valor certificado debe ser mayor que cero.") BigDecimal certifiedAmount,
        @NotNull(message = "Debes indicar la fecha de expedicion.") LocalDate issuedOn,
        @NotNull(message = "Debes indicar la fecha limite legal.") LocalDate legalDeadlineOn) {
}
