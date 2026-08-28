package com.vetsoftware.app.companyactivitymonth.infrastructure.web.response;

import com.vetsoftware.app.companyactivitymonth.application.dto.CompanyActivityMonthDto;
import com.vetsoftware.app.companyactivitymonth.domain.CommercialState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * La actividad de una clinica en un mes, tal como sale por HTTP.
 *
 * <p>
 * <strong>Solo la ve la consola de plataforma.</strong> Ningun puerto de
 * cliente la sirve: es la serie con la que se decide si una cuenta se esta
 * enfriando, y devolverle a la clinica su propio {@code mrrSnapshot} junto a su
 * recuento de dias activos seria enseñarle el instrumento con el que se la
 * mide.
 *
 * <p>
 * <strong>No lleva {@code version}</strong> —es una barandilla del que escribe,
 * no un dato de la medicion—. Publicarla invitaria a devolverla en el cuerpo
 * del recalculo y a construir un protocolo de concurrencia sobre una tabla que
 * solo escribe plataforma.
 *
 * <p>
 * <strong>{@code activeDays} y {@code activeUsers} son medidas independientes y
 * ninguna implica a la otra</strong>: una clinica puede entrar los treinta dias
 * con un solo usuario, o tener quince usuarios que entraron un dia.
 * Presentarlas como si una explicara a la otra —«dias activos por usuario»—
 * seria un numero inventado.
 */
public record CompanyActivityMonthResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-08", description = "El mes, en formato AAAA-MM.") String periodKey,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "La relacion comercial de ESE mes, no la de hoy.") CommercialState commercialState,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Dias del mes con al menos un acceso. Cero es un dato, no un hueco.") int activeDays,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int activeUsers,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int recordsCreated,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "MRR ya normalizado a mensual, congelado tal como estaba ese mes.") BigDecimal mrrSnapshot,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static CompanyActivityMonthResponse from(CompanyActivityMonthDto dto) {
        return new CompanyActivityMonthResponse(dto.id(), dto.companyId(), dto.periodKey(),
                dto.commercialState(), dto.activeDays(), dto.activeUsers(), dto.recordsCreated(),
                dto.mrrSnapshot(), dto.createdDate());
    }
}
