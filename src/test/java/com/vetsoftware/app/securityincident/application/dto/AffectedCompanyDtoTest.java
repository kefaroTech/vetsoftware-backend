package com.vetsoftware.app.securityincident.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.securityincident.testsupport.SecurityIncidentMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AffectedCompanyDto.from")
class AffectedCompanyDtoTest {

    @Test
    @DisplayName("copia id, incidente, empresa, ambito y contador sin cruzarlos")
    void copia_cada_campo_sin_cruzarlos() {
        AffectedCompanyDto dto = AffectedCompanyDto.from(SecurityIncidentMother.afectada());

        assertThat(dto.id()).isEqualTo(SecurityIncidentMother.AFFECTED_ID);
        assertThat(dto.securityIncidentId()).isEqualTo(SecurityIncidentMother.INCIDENT_ID);
        assertThat(dto.companyId()).isEqualTo(SecurityIncidentMother.COMPANY_ID);
        assertThat(dto.affectedScope()).isEqualTo(SecurityIncidentMother.AFFECTED_SCOPE);
        assertThat(dto.affectedSubjectCount())
                .isEqualTo(SecurityIncidentMother.COMPANY_AFFECTED_SUBJECT_COUNT);
    }
}
