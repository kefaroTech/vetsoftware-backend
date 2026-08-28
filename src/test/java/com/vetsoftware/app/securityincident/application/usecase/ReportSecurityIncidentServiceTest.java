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
import com.vetsoftware.app.securityincident.domain.SecurityIncidentAlreadyReportedException;
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
@DisplayName("ReportSecurityIncidentService")
class ReportSecurityIncidentServiceTest {

    @Mock
    private SecurityIncidentRepository repository;

    @InjectMocks
    private ReportSecurityIncidentService service;

    @Captor
    private ArgumentCaptor<SecurityIncident> incidenteCaptor;

    @Nested
    @DisplayName("reporte")
    class Reporte {

        @Test
        @DisplayName("anota fecha y radicado sobre el incidente encontrado")
        void anota_fecha_y_radicado() {
            when(repository.findById(SecurityIncidentMother.INCIDENT_ID))
                    .thenReturn(Optional.of(SecurityIncidentMother.registrado()));
            when(repository.save(any())).thenReturn(SecurityIncidentMother.reportado());

            SecurityIncidentDto dto = service.execute(SecurityIncidentMother.comandoReportar());

            verify(repository).save(incidenteCaptor.capture());
            SecurityIncident guardado = incidenteCaptor.getValue();
            assertThat(guardado.getReportedToAuthorityAt())
                    .isEqualTo(SecurityIncidentMother.REPORTED_AT);
            assertThat(guardado.getReportReference())
                    .isEqualTo(SecurityIncidentMother.REPORT_REFERENCE);
            assertThat(guardado.isReported()).isTrue();
            assertThat(dto.reportReference()).isEqualTo(SecurityIncidentMother.REPORT_REFERENCE);
        }

        @Test
        @DisplayName("reportar dos veces no reescribe la fecha ya presentada")
        void reportar_dos_veces_no_reescribe_la_fecha() {
            when(repository.findById(SecurityIncidentMother.INCIDENT_ID))
                    .thenReturn(Optional.of(SecurityIncidentMother.reportado()));

            assertThatThrownBy(() -> service.execute(SecurityIncidentMother.comandoReportar()))
                    .isInstanceOf(SecurityIncidentAlreadyReportedException.class)
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

            assertThatThrownBy(() -> service.execute(SecurityIncidentMother.comandoReportar()))
                    .isInstanceOf(SecurityIncidentNotFoundException.class).hasMessageContaining(
                            "Security incident not found: " + SecurityIncidentMother.INCIDENT_ID);

            verify(repository, never()).save(any());
        }
    }
}
