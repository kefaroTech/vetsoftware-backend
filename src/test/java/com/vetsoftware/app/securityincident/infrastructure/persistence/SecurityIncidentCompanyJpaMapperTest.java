package com.vetsoftware.app.securityincident.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.securityincident.domain.SecurityIncidentCompany;
import com.vetsoftware.app.securityincident.testsupport.SecurityIncidentMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SecurityIncidentCompanyJpaMapper")
class SecurityIncidentCompanyJpaMapperTest {

    private final SecurityIncidentCompanyJpaMapper mapper = new SecurityIncidentCompanyJpaMapper();

    /**
     * Se construye con el constructor protegido (visible en este mismo paquete) en
     * vez de mockearla: no tiene logica y un mock aqui no vale mas que el objeto
     * real.
     */
    private SecurityIncidentJpaEntity incidentEntity() {
        SecurityIncidentJpaEntity entity = new SecurityIncidentJpaEntity();
        entity.setId(SecurityIncidentMother.INCIDENT_ID);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar y engancha el incidente recibido")
        void copia_cada_campo_y_engancha_el_incidente() {
            SecurityIncidentCompany afectada = SecurityIncidentMother.afectada();
            SecurityIncidentJpaEntity incident = incidentEntity();

            SecurityIncidentCompanyJpaEntity entity = mapper.toJpa(afectada, incident);

            assertThat(entity.getId()).isEqualTo(SecurityIncidentMother.AFFECTED_ID);
            assertThat(entity.getIncident()).isSameAs(incident);
            assertThat(entity.getCompanyId()).isEqualTo(SecurityIncidentMother.COMPANY_ID);
            assertThat(entity.getAffectedScope()).isEqualTo(SecurityIncidentMother.AFFECTED_SCOPE);
            assertThat(entity.getAffectedSubjectCount())
                    .isEqualTo(SecurityIncidentMother.COMPANY_AFFECTED_SUBJECT_COUNT);
        }
    }

    @Nested
    @DisplayName("toDomain(entity) — camino de lectura")
    class ToDomainLectura {

        @Test
        @DisplayName("lee el id del incidente desde la asociacion ya hidratada")
        void lee_el_id_del_incidente_desde_la_asociacion() {
            SecurityIncidentJpaEntity incident = incidentEntity();
            SecurityIncidentCompanyJpaEntity entity = mapper
                    .toJpa(SecurityIncidentMother.afectada(), incident);

            SecurityIncidentCompany afectada = mapper.toDomain(entity);

            assertThat(afectada.getId()).isEqualTo(SecurityIncidentMother.AFFECTED_ID);
            assertThat(afectada.getSecurityIncidentId())
                    .isEqualTo(SecurityIncidentMother.INCIDENT_ID);
            assertThat(afectada.getCompanyId()).isEqualTo(SecurityIncidentMother.COMPANY_ID);
            assertThat(afectada.getAffectedScope())
                    .isEqualTo(SecurityIncidentMother.AFFECTED_SCOPE);
            assertThat(afectada.getAffectedSubjectCount())
                    .isEqualTo(SecurityIncidentMother.COMPANY_AFFECTED_SUBJECT_COUNT);
        }
    }

    @Nested
    @DisplayName("toDomain(entity, securityIncidentId) — camino de escritura")
    class ToDomainEscritura {

        @Test
        @DisplayName("reusa el id recibido en vez de leerlo de la asociacion enganchada")
        void reusa_el_id_recibido() {
            SecurityIncidentCompanyJpaEntity entity = mapper
                    .toJpa(SecurityIncidentMother.afectada(), incidentEntity());
            // Un id DISTINTO del que lleva la entidad enganchada: si el mapper leyera
            // entity.getIncident().getId() en vez del parametro, este test lo pillaria.
            Long otroIncidentId = SecurityIncidentMother.INCIDENT_ID + 1;

            SecurityIncidentCompany afectada = mapper.toDomain(entity, otroIncidentId);

            assertThat(afectada.getSecurityIncidentId()).isEqualTo(otroIncidentId);
            assertThat(afectada.getCompanyId()).isEqualTo(SecurityIncidentMother.COMPANY_ID);
        }
    }
}
