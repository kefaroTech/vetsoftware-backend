package com.vetsoftware.app.companyusageevent.infrastructure.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Anotar un hecho de consumo.
 *
 * <p>
 * <strong>Sin {@code companyId} en el cuerpo.</strong> La empresa a la que se
 * le anota el consumo la elige la consola de plataforma y viaja como
 * {@code @RequestParam} —{@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} prohibe el
 * cuerpo y solo el cuerpo—: un {@code companyId} escrito en el JSON convierte
 * cualquier comprobacion de tenant en una comparacion del numero consigo mismo.
 *
 * @param occurredAt
 *            <strong>el instante del registro consumido, NO la hora
 *            actual.</strong> Si el proceso de medicion lo rellena con su
 *            propio reloj, el reintento de un lote deja de chocar contra
 *            {@code uq_cue_fact}, el hecho se duplica y el excedente se cobra
 *            dos veces, sin un solo error que lo delate. La hora en que se
 *            anota la pone el servidor en otra columna
 * @param limitDimensionCode
 *            solo los cuatro ejes contables ({@code OWNER}, {@code ANIMAL},
 *            {@code APPOINTMENT}, {@code INVOICE}) acumulan hechos. Los de
 *            existencias ({@code USER}, {@code BRANCH}, {@code TERMINAL},
 *            {@code STORAGE_GB}) se cuentan contra su propia tabla y el motor
 *            los rechaza aqui
 * @param usageReferenceId
 *            el identificador del registro consumido en la tabla del eje: el
 *            propietario, la mascota, la cita o el documento electronico
 * @param periodKey
 *            {@code AAAA-MM}, {@code AAAA-Qn}, {@code AAAA-Sn} o
 *            {@code ALLTIME}
 * @param billable
 *            si el hecho cuenta para el cobro. Un hecho no facturable no se
 *            puede colgar despues de un cargo
 */
public record RecordCompanyUsageEventRequest(
        @NotBlank(message = "Debes indicar el codigo del eje.") @Size(max = 50, message = "El codigo del eje no puede superar los 50 caracteres.") @Schema(description = "Solo los ejes contables: OWNER, ANIMAL, APPOINTMENT o INVOICE.") String limitDimensionCode,
        @NotNull(message = "Debes indicar el registro consumido.") @Positive(message = "El identificador del registro consumido debe ser positivo.") Long usageReferenceId,
        @NotNull(message = "Debes indicar cuando ocurrio el hecho.") @Schema(description = "El instante del registro consumido, no la hora del proceso que lo mide.") LocalDateTime occurredAt,
        @NotBlank(message = "Debes indicar el periodo.") @Pattern(regexp = "^([0-9]{4}-(0[1-9]|1[0-2])|[0-9]{4}-Q[1-4]|[0-9]{4}-S[12]|ALLTIME)$", message = "El periodo debe ser AAAA-MM, AAAA-Qn, AAAA-Sn o ALLTIME.") String periodKey,
        @NotNull(message = "Debes indicar si el hecho es facturable.") Boolean billable) {
}
