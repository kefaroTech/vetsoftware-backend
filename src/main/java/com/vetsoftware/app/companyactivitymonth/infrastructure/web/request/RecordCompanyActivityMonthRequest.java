package com.vetsoftware.app.companyactivitymonth.infrastructure.web.request;

import com.vetsoftware.app.companyactivitymonth.domain.CommercialState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * <strong>Sin {@code companyId}, y la ausencia es obligatoria.</strong> Esta es
 * una ruta de plataforma: un principal {@code SYSTEM} elige a que clinica se
 * refiere la fila, y esa eleccion viaja por la query string como
 * {@code @RequestParam}. {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} —regla dura—
 * prohibe el cuerpo <em>y solo el cuerpo</em>: un {@code companyId} escrito en
 * el JSON convertiria cualquier comprobacion de tenant en una comparacion del
 * numero consigo mismo, un gate que se lee perfecto y siempre da {@code true}.
 *
 * @param periodKey
 *            el mes, {@code AAAA-MM}. El {@code @Pattern} es el mismo
 *            {@code REGEXP} de {@code chk_cam_period_key}: sin el, un
 *            {@code 2026-13} bajaria hasta el motor para volver como error de
 *            integridad en vez de como un error de campo que el front sabe
 *            pintar bajo el input
 * @param activeDays
 *            dias del mes con al menos un acceso. El techo de 31 es el de
 *            {@code chk_cam_active_days}; que ademas quepan en <em>ese</em> mes
 *            concreto —28 en un febrero no bisiesto— lo comprueba el dominio,
 *            que es donde se puede mirar el calendario
 * @param mrrSnapshot
 *            el MRR ya normalizado a mensual. Cero es legitimo: un mes en
 *            prueba, gratuito o ya de baja no factura, y por eso es
 *            {@code @PositiveOrZero} y no {@code @Positive}
 */
public record RecordCompanyActivityMonthRequest(
        @NotBlank(message = "Debes indicar el mes.") @Pattern(regexp = "^[0-9]{4}-(0[1-9]|1[0-2])$", message = "El mes debe tener el formato AAAA-MM, con el mes entre 01 y 12.") @Schema(example = "2026-08") String periodKey,
        @NotNull(message = "Debes indicar el estado comercial del mes.") CommercialState commercialState,
        @PositiveOrZero(message = "Los dias activos no pueden ser negativos.") @Max(value = 31, message = "Un mes no tiene mas de 31 dias.") @jakarta.validation.constraints.NotNull(message = "Debes indicar los dias activos del mes.") Integer activeDays,
        @PositiveOrZero(message = "Los usuarios activos no pueden ser negativos.") @jakarta.validation.constraints.NotNull(message = "Debes indicar los usuarios activos del mes.") Integer activeUsers,
        @PositiveOrZero(message = "Los registros creados no pueden ser negativos.") @jakarta.validation.constraints.NotNull(message = "Debes indicar los registros creados en el mes.") Integer recordsCreated,
        @NotNull(message = "El MRR del mes es obligatorio.") @PositiveOrZero(message = "El MRR no puede ser negativo.") @Digits(integer = 17, fraction = 2, message = "El MRR admite como maximo 2 decimales.") @Schema(description = "MRR ya normalizado a mensual. Cero en un mes gratuito, en prueba o de baja.") BigDecimal mrrSnapshot) {
}
