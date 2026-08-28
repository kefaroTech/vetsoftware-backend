package com.vetsoftware.app.companyactivitymonth.application.dto;

import com.vetsoftware.app.companyactivitymonth.domain.CommercialState;
import com.vetsoftware.app.companyactivitymonth.domain.CompanyActivityMonth;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * La fila de actividad tal como sale de la capa de aplicacion.
 *
 * <p>
 * <strong>Sin {@code version}</strong>: el numero de bloqueo optimista es una
 * barandilla del que escribe, no un dato de la medicion. Publicarlo invitaria a
 * un cliente a devolverlo y a construir un protocolo de concurrencia sobre una
 * serie que solo escribe plataforma.
 *
 * <p>
 * {@code periodKey} sale como {@code String} y no como el value object: cruzar
 * la frontera de la aplicacion con un tipo que tiene invariantes obligaria a
 * todo consumidor a conocerlas para reconstruirlo.
 */
public record CompanyActivityMonthDto(Long id, Long companyId, String periodKey,
        CommercialState commercialState, int activeDays, int activeUsers, int recordsCreated,
        BigDecimal mrrSnapshot, LocalDateTime createdDate) {

    public static CompanyActivityMonthDto from(CompanyActivityMonth month) {
        return new CompanyActivityMonthDto(month.getId(), month.getCompanyId(),
                month.getPeriodKey().value(), month.getCommercialState(), month.getActiveDays(),
                month.getActiveUsers(), month.getRecordsCreated(), month.getMrrSnapshot(),
                month.getCreatedDate());
    }
}
