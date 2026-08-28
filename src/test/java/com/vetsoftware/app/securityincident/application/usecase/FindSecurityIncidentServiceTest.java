package com.vetsoftware.app.securityincident.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.securityincident.application.dto.SecurityIncidentDto;
import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentRepository;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentNotFoundException;
import com.vetsoftware.app.securityincident.testsupport.SecurityIncidentMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindSecurityIncidentService")
class FindSecurityIncidentServiceTest {

    @Mock
    private SecurityIncidentRepository repository;

    @InjectMocks
    private FindSecurityIncidentService service;

    @Test
    @DisplayName("devuelve el DTO del incidente encontrado")
    void devuelve_el_dto_del_incidente_encontrado() {
        when(repository.findById(SecurityIncidentMother.INCIDENT_ID))
                .thenReturn(Optional.of(SecurityIncidentMother.reportado()));

        SecurityIncidentDto dto = service.findById(SecurityIncidentMother.INCIDENT_ID);

        assertThat(dto.id()).isEqualTo(SecurityIncidentMother.INCIDENT_ID);
        assertThat(dto.reportReference()).isEqualTo(SecurityIncidentMother.REPORT_REFERENCE);
    }

    @Test
    @DisplayName("un incidente inexistente revienta con 404")
    void un_incidente_inexistente_revienta() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(SecurityIncidentNotFoundException.class)
                .hasMessageContaining("Security incident not found: 999");
    }
}
