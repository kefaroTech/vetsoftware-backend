package com.vetsoftware.app.companyactivitymonth.infrastructure.web.request;

import com.vetsoftware.app.companyactivitymonth.domain.CommercialState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * El recalculo del mes: los cinco numeros nuevos y nada mas.
 *
 * <p>
 * <strong>Sin {@code companyId} y sin {@code periodKey}, a proposito.</strong>
 * El par empresa-mes identifica la fila y no se puede mover: cambiarlo no seria
 * recalcular sino llevarse la actividad de una clinica a la de otra, o la de
 * agosto a la de julio. La fila se señala por su {@code id} en la URL.
 *
 * <p>
 * El cuerpo <b>reemplaza los cinco valores</b>, no los suma: quien recalcula ya
 * tiene los totales del mes hasta hoy. Un cuerpo parcial dejaria contadores del
 * calculo anterior mezclados con los del nuevo, y nadie sabria de que dia es
 * cada numero.
 */
public record UpdateCompanyActivityMonthRequest(
        @NotNull(message = "Debes indicar el estado comercial del mes.") CommercialState commercialState,
        @PositiveOrZero(message = "Los dias activos no pueden ser negativos.") @Max(value = 31, message = "Un mes no tiene mas de 31 dias.") @jakarta.validation.constraints.NotNull(message = "Debes indicar los dias activos del mes.") Integer activeDays,
        @PositiveOrZero(message = "Los usuarios activos no pueden ser negativos.") @jakarta.validation.constraints.NotNull(message = "Debes indicar los usuarios activos del mes.") Integer activeUsers,
        @PositiveOrZero(message = "Los registros creados no pueden ser negativos.") @jakarta.validation.constraints.NotNull(message = "Debes indicar los registros creados en el mes.") Integer recordsCreated,
        @NotNull(message = "El MRR del mes es obligatorio.") @PositiveOrZero(message = "El MRR no puede ser negativo.") @Digits(integer = 17, fraction = 2, message = "El MRR admite como maximo 2 decimales.") @Schema(description = "MRR ya normalizado a mensual. Reemplaza al anterior, no se suma.") BigDecimal mrrSnapshot) {
}
