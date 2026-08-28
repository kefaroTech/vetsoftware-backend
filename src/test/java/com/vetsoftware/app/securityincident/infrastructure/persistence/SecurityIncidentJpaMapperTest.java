package com.vetsoftware.app.securityincident.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.securityincident.domain.SecurityIncident;
import com.vetsoftware.app.securityincident.testsupport.SecurityIncidentMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SecurityIncidentJpaMapper")
class SecurityIncidentJpaMapperTest {

    private final SecurityIncidentJpaMapper mapper = new SecurityIncidentJpaMapper();

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            SecurityIncident incidente = SecurityIncidentMother.cerrado();

            SecurityIncidentJpaEntity entity = mapper.toJpa(incidente);

            assertThat(entity.getId()).isEqualTo(SecurityIncidentMother.INCIDENT_ID);
            assertThat(entity.getDetectedAt()).isEqualTo(SecurityIncidentMother.DETECTED_AT);
            assertThat(entity.getOccurredAt()).isEqualTo(SecurityIncidentMother.OCCURRED_AT);
            assertThat(entity.getEscalatedAt()).isEqualTo(SecurityIncidentMother.ESCALATED_AT);
            assertThat(entity.getKind()).isEqualTo(SecurityIncidentMother.KIND);
            assertThat(entity.getSeverity()).isEqualTo(SecurityIncidentMother.SEVERITY);
            assertThat(entity.getSummary()).isEqualTo(SecurityIncidentMother.SUMMARY);
            assertThat(entity.getAffectedSubjectCount())
                    .isEqualTo(SecurityIncidentMother.AFFECTED_SUBJECT_COUNT);
            assertThat(entity.getDeadlineAt()).isEqualTo(SecurityIncidentMother.DEADLINE_AT);
            assertThat(entity.getReportedToAuthorityAt())
                    .isEqualTo(SecurityIncidentMother.REPORTED_AT);
            assertThat(entity.getReportReference())
                    .isEqualTo(SecurityIncidentMother.REPORT_REFERENCE);
            assertThat(entity.getNotifiedSubjectsAt())
                    .isEqualTo(SecurityIncidentMother.NOTIFIED_SUBJECTS_AT);
            assertThat(entity.getContainment()).isEqualTo(SecurityIncidentMother.CONTAINMENT);
            assertThat(entity.getRootCause()).isEqualTo(SecurityIncidentMother.ROOT_CAUSE);
            assertThat(entity.getClosedAt()).isEqualTo(SecurityIncidentMother.CLOSED_AT);
            assertThat(entity.getCreatedDate()).isEqualTo(SecurityIncidentMother.CREATED_DATE);
        }

        @Test
        @DisplayName("la version viaja hacia la entidad: sin ella el merge se vuelve un insert")
        void la_version_viaja_hacia_la_entidad() {
            SecurityIncident incidente = SecurityIncidentMother.reportado();

            SecurityIncidentJpaEntity entity = mapper.toJpa(incidente);

            // Una entidad CON id y version nula la trataria Hibernate como transitoria:
            // el merge insertaria una fila nueva en vez de editar la que ya existe.
            assertThat(entity.getId()).isNotNull();
            assertThat(entity.getVersion()).isEqualTo(SecurityIncidentMother.VERSION);
        }
    }

    @Nested
    @DisplayName("toDomain — entidad a dominio")
    class ToDomain {

        @Test
        @DisplayName("reconstruye el agregado con cada campo en su sitio")
        void reconstruye_el_agregado_con_cada_campo_en_su_sitio() {
            SecurityIncidentJpaEntity entity = mapper.toJpa(SecurityIncidentMother.cerrado());

            SecurityIncident incidente = mapper.toDomain(entity);

            assertThat(incidente.getId()).isEqualTo(SecurityIncidentMother.INCIDENT_ID);
            assertThat(incidente.getReportReference())
                    .isEqualTo(SecurityIncidentMother.REPORT_REFERENCE);
            assertThat(incidente.getContainment()).isEqualTo(SecurityIncidentMother.CONTAINMENT);
            assertThat(incidente.getRootCause()).isEqualTo(SecurityIncidentMother.ROOT_CAUSE);
            assertThat(incidente.getClosedAt()).isEqualTo(SecurityIncidentMother.CLOSED_AT);
        }

        @Test
        @DisplayName("la version vuelve tambien desde la entidad")
        void la_version_vuelve_desde_la_entidad() {
            SecurityIncidentJpaEntity entity = mapper.toJpa(SecurityIncidentMother.reportado());

            SecurityIncident incidente = mapper.toDomain(entity);

            assertThat(incidente.getVersion()).isEqualTo(SecurityIncidentMother.VERSION);
        }

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            SecurityIncident original = SecurityIncidentMother.cerrado();

            SecurityIncident vuelta = mapper.toDomain(mapper.toJpa(original));

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }
}
