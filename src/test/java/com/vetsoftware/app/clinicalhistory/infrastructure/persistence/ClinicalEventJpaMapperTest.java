package com.vetsoftware.app.clinicalhistory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.clinicalhistory.domain.ClinicalEvent;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@code ClinicalEventViewJpaEntity} es una vista JPA {@code @Immutable} sin
 * setters públicos (constructor {@code protected} vacío). Como este test vive
 * en el mismo paquete, se instancia directamente y se puebla con
 * {@link ReflectionTestUtils} — no hay otra forma de construirla desde fuera de
 * Hibernate.
 */
@DisplayName("ClinicalEventJpaMapper — de la vista JPA al dominio")
class ClinicalEventJpaMapperTest {

    private final ClinicalEventJpaMapper mapper = new ClinicalEventJpaMapper();

    private static ClinicalEventViewJpaEntity entidad(Long sourceId, Long animalId, Long companyId,
            Long consultationId, LocalDate eventDate, LocalDate endDate, ClinicalEventType type,
            String summary) {
        ClinicalEventViewJpaEntity e = new ClinicalEventViewJpaEntity();
        ReflectionTestUtils.setField(e, "compositeKey", type + "-" + sourceId);
        ReflectionTestUtils.setField(e, "sourceId", sourceId);
        ReflectionTestUtils.setField(e, "animalId", animalId);
        ReflectionTestUtils.setField(e, "companyId", companyId);
        ReflectionTestUtils.setField(e, "consultationId", consultationId);
        ReflectionTestUtils.setField(e, "eventDate", eventDate);
        ReflectionTestUtils.setField(e, "endDate", endDate);
        ReflectionTestUtils.setField(e, "eventType", type);
        ReflectionTestUtils.setField(e, "summary", summary);
        return e;
    }

    @Test
    @DisplayName("copia cada campo de la vista al dominio")
    void copia_cada_campo() {
        ClinicalEventViewJpaEntity entidad = entidad(500L, 100L, 9L, 500L, LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3), ClinicalEventType.HOSPITALIZATION, "Resumen");

        ClinicalEvent evento = mapper.toDomain(entidad);

        assertThat(evento.sourceId()).isEqualTo(500L);
        assertThat(evento.animalId()).isEqualTo(100L);
        assertThat(evento.companyId()).isEqualTo(9L);
        assertThat(evento.consultationId()).isEqualTo(500L);
        assertThat(evento.eventDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(evento.endDate()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(evento.eventType()).isEqualTo(ClinicalEventType.HOSPITALIZATION);
        assertThat(evento.summary()).isEqualTo("Resumen");
    }

    @Test
    @DisplayName("conserva los opcionales en null (endDate, consultationId, summary)")
    void conserva_opcionales_en_null() {
        ClinicalEventViewJpaEntity entidad = entidad(501L, 100L, 9L, null, LocalDate.of(2026, 8, 1),
                null, ClinicalEventType.VACCINATION, null);

        ClinicalEvent evento = mapper.toDomain(entidad);

        assertThat(evento.consultationId()).isNull();
        assertThat(evento.endDate()).isNull();
        assertThat(evento.summary()).isNull();
    }
}
