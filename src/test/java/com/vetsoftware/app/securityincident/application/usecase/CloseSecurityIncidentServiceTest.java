package com.vetsoftware.app.securityincident.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.securityincident.application.dto.SecurityIncidentDto;
import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentRepository;
import com.vetsoftware.app.securityincident.domain.SecurityIncident;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentAlreadyClosedException;
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
@DisplayName("CloseSecurityIncidentService")
class CloseSecurityIncidentServiceTest {

    @Mock
    private SecurityIncidentRepository repository;

    @InjectMocks
    private CloseSecurityIncidentService service;

    @Captor
    private ArgumentCaptor<SecurityIncident> incidenteCaptor;

    @Nested
    @DisplayName("cierre")
    class Cierre {

        @Test
        @DisplayName("escribe contencion, causa raiz y hora de cierre sobre el incidente encontrado")
        void escribe_contencion_causa_raiz_y_hora_de_cierre() {
            when(repository.findById(SecurityIncidentMother.INCIDENT_ID))
                    .thenReturn(Optional.of(SecurityIncidentMother.reportado()));
            when(repository.save(any())).thenReturn(SecurityIncidentMother.cerrado());

            SecurityIncidentDto dto = service.execute(SecurityIncidentMother.comandoCerrar());

            verify(repository).save(incidenteCaptor.capture());
            SecurityIncident guardado = incidenteCaptor.getValue();
            assertThat(guardado.getContainment()).isEqualTo(SecurityIncidentMother.CONTAINMENT);
            assertThat(guardado.getRootCause()).isEqualTo(SecurityIncidentMother.ROOT_CAUSE);
            assertThat(guardado.getClosedAt()).isEqualTo(SecurityIncidentMother.CLOSED_AT);
            assertThat(guardado.isClosed()).isTrue();
            assertThat(dto.id()).isEqualTo(SecurityIncidentMother.INCIDENT_ID);
            assertThat(dto.closedAt()).isEqualTo(SecurityIncidentMother.CLOSED_AT);
        }

        @Test
        @DisplayName("un incidente ya cerrado no se vuelve a cerrar")
        void un_incidente_ya_cerrado_no_se_vuelve_a_cerrar() {
            when(repository.findById(SecurityIncidentMother.INCIDENT_ID))
                    .thenReturn(Optional.of(SecurityIncidentMother.cerrado()));

            assertThatThrownBy(() -> service.execute(SecurityIncidentMother.comandoCerrar()))
                    .isInstanceOf(SecurityIncidentAlreadyClosedException.class)
                    .hasMessageContaining(
                            "Security incident " + SecurityIncidentMother.INCIDENT_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("incidente inexistente")
    class Inexistente {

        @Test
        @DisplayName("no escribe nada si el incidente no existe")
        void no_escribe_nada_si_el_incidente_no_existe() {
            when(repository.findById(SecurityIncidentMother.INCIDENT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SecurityIncidentMother.comandoCerrar()))
                    .isInstanceOf(SecurityIncidentNotFoundException.class).hasMessageContaining(
                            "Security incident not found: " + SecurityIncidentMother.INCIDENT_ID);

            verify(repository, never()).save(any());
        }
    }
}
