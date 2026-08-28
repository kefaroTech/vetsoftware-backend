package com.vetsoftware.app.securityincident.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.securityincident.testsupport.SecurityIncidentMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SecurityIncidentCompany — invariantes de la puente de afectados")
class SecurityIncidentCompanyTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            SecurityIncidentCompany afectada = SecurityIncidentMother.afectada();

            assertThat(afectada.getId()).isEqualTo(SecurityIncidentMother.AFFECTED_ID);
            assertThat(afectada.getSecurityIncidentId())
                    .isEqualTo(SecurityIncidentMother.INCIDENT_ID);
            assertThat(afectada.getCompanyId()).isEqualTo(SecurityIncidentMother.COMPANY_ID);
            assertThat(afectada.getAffectedScope())
                    .isEqualTo(SecurityIncidentMother.AFFECTED_SCOPE);
            assertThat(afectada.getAffectedSubjectCount())
                    .isEqualTo(SecurityIncidentMother.COMPANY_AFFECTED_SUBJECT_COUNT);
        }

        @Test
        @DisplayName("cero titulares afectados en esa clinica es legitimo")
        void cero_titulares_afectados_es_legitimo() {
            SecurityIncidentCompany afectada = new SecurityIncidentCompany(
                    SecurityIncidentMother.AFFECTED_ID, SecurityIncidentMother.INCIDENT_ID,
                    SecurityIncidentMother.COMPANY_ID, SecurityIncidentMother.AFFECTED_SCOPE, 0);

            assertThat(afectada.getAffectedSubjectCount()).isZero();
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("securityIncidentId nulo revienta")
        void security_incident_id_nulo_revienta() {
            assertThatThrownBy(() -> new SecurityIncidentCompany(SecurityIncidentMother.AFFECTED_ID,
                    null, SecurityIncidentMother.COMPANY_ID, SecurityIncidentMother.AFFECTED_SCOPE,
                    SecurityIncidentMother.COMPANY_AFFECTED_SUBJECT_COUNT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("securityIncidentId is required");
        }

        @Test
        @DisplayName("companyId nulo revienta")
        void company_id_nulo_revienta() {
            assertThatThrownBy(() -> new SecurityIncidentCompany(SecurityIncidentMother.AFFECTED_ID,
                    SecurityIncidentMother.INCIDENT_ID, null, SecurityIncidentMother.AFFECTED_SCOPE,
                    SecurityIncidentMother.COMPANY_AFFECTED_SUBJECT_COUNT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");
        }

        @Test
        @DisplayName("affectedScope nulo revienta")
        void affected_scope_nulo_revienta() {
            assertThatThrownBy(() -> new SecurityIncidentCompany(SecurityIncidentMother.AFFECTED_ID,
                    SecurityIncidentMother.INCIDENT_ID, SecurityIncidentMother.COMPANY_ID, null,
                    SecurityIncidentMother.COMPANY_AFFECTED_SUBJECT_COUNT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("affectedScope is required");
        }

        @Test
        @DisplayName("affectedSubjectCount negativo revienta")
        void affected_subject_count_negativo_revienta() {
            assertThatThrownBy(() -> new SecurityIncidentCompany(SecurityIncidentMother.AFFECTED_ID,
                    SecurityIncidentMother.INCIDENT_ID, SecurityIncidentMother.COMPANY_ID,
                    SecurityIncidentMother.AFFECTED_SCOPE, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("affectedSubjectCount must not be negative");
        }
    }

    @Nested
    @DisplayName("alta")
    class Alta {

        @Test
        @DisplayName("register nace sin id: lo genera la base")
        void register_nace_sin_id() {
            SecurityIncidentCompany afectada = SecurityIncidentCompany.register(
                    SecurityIncidentMother.INCIDENT_ID, SecurityIncidentMother.COMPANY_ID,
                    SecurityIncidentMother.AFFECTED_SCOPE,
                    SecurityIncidentMother.COMPANY_AFFECTED_SUBJECT_COUNT);

            assertThat(afectada.getId()).isNull();
            assertThat(afectada.getSecurityIncidentId())
                    .isEqualTo(SecurityIncidentMother.INCIDENT_ID);
            assertThat(afectada.getCompanyId()).isEqualTo(SecurityIncidentMother.COMPANY_ID);
        }
    }
}
