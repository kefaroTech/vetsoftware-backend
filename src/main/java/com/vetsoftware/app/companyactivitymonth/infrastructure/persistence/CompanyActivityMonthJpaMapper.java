package com.vetsoftware.app.companyactivitymonth.infrastructure.persistence;

import com.vetsoftware.app.companyactivitymonth.domain.ActivityPeriodKey;
import com.vetsoftware.app.companyactivitymonth.domain.CompanyActivityMonth;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Copia la version en los dos sentidos, y de eso depende que el
 * recalculo del mes sea una edicion y no un insert.</strong> Si {@code toJpa}
 * dejara la version en {@code null} sobre una entidad que ya tiene id,
 * Hibernate la tomaria por transitoria y el {@code merge} escribiria una fila
 * nueva —dos filas para el mismo mes, que es justo lo que {@code uq_cam_month}
 * existe para impedir, y el error llegaria disfrazado de violacion de unicidad
 * en una operacion que no insertaba nada—.
 *
 * <p>
 * {@code periodKey} cruza como {@code String} hacia la entidad y vuelve
 * envuelto en {@link ActivityPeriodKey}: el value object valida al construirse,
 * asi que una fila con un periodo mal formado —imposible mientras
 * {@code chk_cam_period_key} siga en su sitio— reventaria aqui, al leerla, y no
 * tres capas mas arriba.
 */
@Component
public class CompanyActivityMonthJpaMapper {

    public CompanyActivityMonthJpaEntity toJpa(CompanyActivityMonth month) {
        CompanyActivityMonthJpaEntity entity = new CompanyActivityMonthJpaEntity();
        entity.setId(month.getId());
        entity.setCompanyId(month.getCompanyId());
        entity.setPeriodKey(month.getPeriodKey().value());
        entity.setCommercialState(month.getCommercialState());
        entity.setActiveDays(month.getActiveDays());
        entity.setActiveUsers(month.getActiveUsers());
        entity.setRecordsCreated(month.getRecordsCreated());
        entity.setMrrSnapshot(month.getMrrSnapshot());
        entity.setCreatedDate(month.getCreatedDate());
        entity.setVersion(month.getVersion());
        return entity;
    }

    public CompanyActivityMonth toDomain(CompanyActivityMonthJpaEntity entity) {
        return new CompanyActivityMonth(entity.getId(), entity.getCompanyId(),
                new ActivityPeriodKey(entity.getPeriodKey()), entity.getCommercialState(),
                entity.getActiveDays(), entity.getActiveUsers(), entity.getRecordsCreated(),
                entity.getMrrSnapshot(), entity.getCreatedDate(), entity.getVersion());
    }
}
