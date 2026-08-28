package com.vetsoftware.app.companyusageevent.infrastructure.persistence;

import com.vetsoftware.app.companyusageevent.domain.CompanyUsageEvent;
import com.vetsoftware.app.companyusageevent.domain.UsageBranch;
import com.vetsoftware.app.companyusageevent.domain.UsagePeriodKey;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Aqui vive el reparto a las cuatro columnas de rama, y es el unico
 * sitio del codigo donde ese reparto puede estar mal.</strong> El dominio
 * guarda {@link UsageBranch} y una sola referencia; la tabla tiene cuatro
 * columnas nulables y un {@code CHECK} de cuatro ramas. La traduccion entre las
 * dos formas es este fichero: {@link #toJpa} abre y {@link #toDomain} cierra.
 * Concentrarla en un metodo con un {@code switch} exhaustivo es lo que hace que
 * anadir un eje contable rompa la compilacion en el sitio correcto en vez de
 * escribir una fila que {@code chk_cue_branch} rechazara en produccion.
 *
 * <p>
 * <strong>Copia la version en los dos sentidos</strong>, y de eso depende que
 * colgar el cargo sea una edicion y no un insert: si {@code toJpa} dejara la
 * version nula sobre una entidad que ya tiene id, Hibernate la tomaria por
 * transitoria y escribiria una fila nueva —un hecho duplicado, que es justo lo
 * que {@code uq_cue_fact} existe para impedir—.
 *
 * <p>
 * <strong>No toca {@code usage_ref_key}</strong>: la calcula MySQL y no esta
 * mapeada. Escribirla desde aqui haria que el motor rechazara el
 * {@code INSERT}.
 */
@Component
public class CompanyUsageEventJpaMapper {

    public CompanyUsageEventJpaEntity toJpa(CompanyUsageEvent event) {
        CompanyUsageEventJpaEntity entity = new CompanyUsageEventJpaEntity();
        entity.setId(event.getId());
        entity.setCompanyId(event.getCompanyId());
        entity.setLimitDimensionId(event.getLimitDimensionId());
        entity.setLimitDimensionCode(event.getBranch().code());
        applyBranch(entity, event.getBranch(), event.getUsageReferenceId());
        entity.setOccurredAt(event.getOccurredAt());
        entity.setPeriodKey(event.getPeriodKey().value());
        entity.setBillable(event.isBillable());
        entity.setChargeId(event.getChargeId());
        entity.setCreatedDate(event.getCreatedDate());
        entity.setVersion(event.getVersion());
        return entity;
    }

    public CompanyUsageEvent toDomain(CompanyUsageEventJpaEntity entity) {
        UsageBranch branch = UsageBranch.ofDimensionCode(entity.getLimitDimensionCode());
        return new CompanyUsageEvent(entity.getId(), entity.getCompanyId(),
                entity.getLimitDimensionId(), branch, referenceOf(entity, branch),
                entity.getOccurredAt(), UsagePeriodKey.of(entity.getPeriodKey()),
                entity.isBillable(), entity.getChargeId(), entity.getCreatedDate(),
                entity.getVersion());
    }

    /**
     * Escribe la referencia en la columna de su rama y <b>deja las otras tres en
     * nulo explicitamente</b>.
     *
     * <p>
     * Los tres {@code null} no son ruido: sobre una entidad reciclada por el
     * contexto de persistencia, no escribirlos dejaria viva la rama anterior y la
     * fila tendria dos columnas pobladas, que es exactamente lo que
     * {@code chk_cue_branch} rechaza. Es mas barato escribir tres nulos que
     * depender de que la entidad venga limpia.
     */
    private static void applyBranch(CompanyUsageEventJpaEntity entity, UsageBranch branch,
            Long usageReferenceId) {
        entity.setUsageOwnerId(branch == UsageBranch.OWNER ? usageReferenceId : null);
        entity.setUsageAnimalId(branch == UsageBranch.ANIMAL ? usageReferenceId : null);
        entity.setUsageAppointmentId(branch == UsageBranch.APPOINTMENT ? usageReferenceId : null);
        entity.setUsageElectronicDocumentId(
                branch == UsageBranch.INVOICE ? usageReferenceId : null);
    }

    /**
     * La referencia de la rama que dice el eje.
     *
     * <p>
     * <strong>Falla en voz alta si esta vacia.</strong> Una fila con el eje de
     * mascotas y {@code usage_animal_id} nulo no puede existir —{@code
     * chk_cue_branch} lo impide—, asi que si aparece es que alguien escribio en la
     * tabla por fuera del motor o que el {@code CHECK} se cayo en una migracion.
     * Devolver {@code null} y dejar que reviente mas adelante convertiria un dato
     * corrupto en un {@code NullPointerException} sin dueno; asi el mensaje nombra
     * la fila y la columna que falta.
     */
    private static Long referenceOf(CompanyUsageEventJpaEntity entity, UsageBranch branch) {
        Long reference = switch (branch) {
            case OWNER -> entity.getUsageOwnerId();
            case ANIMAL -> entity.getUsageAnimalId();
            case APPOINTMENT -> entity.getUsageAppointmentId();
            case INVOICE -> entity.getUsageElectronicDocumentId();
        };
        if (reference == null) {
            throw new IllegalStateException("company_usage_events row " + entity.getId()
                    + " has axis '" + branch.code() + "' but no reference in the matching column:"
                    + " chk_cue_branch makes that row unwritable, so either it was inserted"
                    + " bypassing the engine or the constraint was dropped in a migration");
        }
        return reference;
    }
}
