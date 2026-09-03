package com.vetsoftware.app.securityincident.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.securityincident.application.dto.AffectedCompanyDto;
import com.vetsoftware.app.securityincident.application.port.out.CompanyValidationPort;
import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentCompanyRepository;
import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentRepository;
import com.vetsoftware.app.securityincident.domain.AffectedCompanyAlreadyRegisteredException;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentCompany;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentNotFoundException;
import com.vetsoftware.app.securityincident.testsupport.SecurityIncidentMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterAffectedCompanyService (securityincident)")
class RegisterAffectedCompanyServiceTest {

    @Mock
    private SecurityIncidentCompanyRepository affectedRepository;
    @Mock
    private SecurityIncidentRepository incidentRepository;
    @Mock
    private CompanyValidationPort companyValidationPort;

    @InjectMocks
    private RegisterAffectedCompanyService service;

    @Captor
    private ArgumentCaptor<SecurityIncidentCompany> afectadaCaptor;

    @Nested
    @DisplayName("registro")
    class Registro {

        @Test
        @DisplayName("persiste la clinica alcanzada con el ambito y el contador del comando")
        void persiste_la_clinica_alcanzada() {
            when(incidentRepository.findById(SecurityIncidentMother.INCIDENT_ID))
                    .thenReturn(Optional.of(SecurityIncidentMother.registrado()));
            when(companyValidationPort.existsById(SecurityIncidentMother.COMPANY_ID))
                    .thenReturn(true);
            when(affectedRepository.existsByIncidentIdAndCompanyIdAndScope(
                    SecurityIncidentMother.INCIDENT_ID, SecurityIncidentMother.COMPANY_ID,
                    SecurityIncidentMother.AFFECTED_SCOPE)).thenReturn(false);
            when(affectedRepository.save(any())).thenReturn(SecurityIncidentMother.afectada());

            AffectedCompanyDto dto = service
                    .execute(SecurityIncidentMother.comandoRegistrarAfectada());

            verify(affectedRepository).save(afectadaCaptor.capture());
            SecurityIncidentCompany guardada = afectadaCaptor.getValue();
            assertThat(guardada.getId()).isNull();
            assertThat(guardada.getSecurityIncidentId())
                    .isEqualTo(SecurityIncidentMother.INCIDENT_ID);
            assertThat(guardada.getCompanyId()).isEqualTo(SecurityIncidentMother.COMPANY_ID);
            assertThat(guardada.getAffectedScope())
                    .isEqualTo(SecurityIncidentMother.AFFECTED_SCOPE);
            assertThat(guardada.getAffectedSubjectCount())
                    .isEqualTo(SecurityIncidentMother.COMPANY_AFFECTED_SUBJECT_COUNT);
            assertThat(dto.companyId()).isEqualTo(SecurityIncidentMother.COMPANY_ID);
        }

        @Test
        @DisplayName("una terna ya registrada revienta y no vuelve a guardar")
        void una_terna_ya_registrada_revienta() {
            when(incidentRepository.findById(SecurityIncidentMother.INCIDENT_ID))
                    .thenReturn(Optional.of(SecurityIncidentMother.registrado()));
            when(companyValidationPort.existsById(SecurityIncidentMother.COMPANY_ID))
                    .thenReturn(true);
            when(affectedRepository.existsByIncidentIdAndCompanyIdAndScope(
                    SecurityIncidentMother.INCIDENT_ID, SecurityIncidentMother.COMPANY_ID,
                    SecurityIncidentMother.AFFECTED_SCOPE)).thenReturn(true);

            assertThatThrownBy(
                    () -> service.execute(SecurityIncidentMother.comandoRegistrarAfectada()))
                    .isInstanceOf(AffectedCompanyAlreadyRegisteredException.class)
                    .hasMessageContaining("Company " + SecurityIncidentMother.COMPANY_ID);

            verify(affectedRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class Inexistentes {

        @Test
        @DisplayName("incidente inexistente: no consulta ni la empresa ni el duplicado")
        void incidente_inexistente() {
            when(incidentRepository.findById(SecurityIncidentMother.INCIDENT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(SecurityIncidentMother.comandoRegistrarAfectada()))
                    .isInstanceOf(SecurityIncidentNotFoundException.class).hasMessageContaining(
                            "Security incident not found: " + SecurityIncidentMother.INCIDENT_ID);

            verifyNoInteractions(companyValidationPort, affectedRepository);
        }

        @Test
        @DisplayName("empresa inexistente: no consulta el duplicado ni persiste")
        void empresa_inexistente() {
            when(incidentRepository.findById(SecurityIncidentMother.INCIDENT_ID))
                    .thenReturn(Optional.of(SecurityIncidentMother.registrado()));
            when(companyValidationPort.existsById(SecurityIncidentMother.COMPANY_ID))
                    .thenReturn(false);

            assertThatThrownBy(
                    () -> service.execute(SecurityIncidentMother.comandoRegistrarAfectada()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Company not found: " + SecurityIncidentMother.COMPANY_ID);

            verifyNoInteractions(affectedRepository);
        }
    }
}
