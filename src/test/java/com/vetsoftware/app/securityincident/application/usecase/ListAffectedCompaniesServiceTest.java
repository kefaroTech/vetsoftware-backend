package com.vetsoftware.app.securityincident.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.securityincident.application.dto.AffectedCompanyDto;
import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentCompanyRepository;
import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentRepository;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentCompany;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentNotFoundException;
import com.vetsoftware.app.securityincident.testsupport.SecurityIncidentMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListAffectedCompaniesService")
class ListAffectedCompaniesServiceTest {

    @Mock
    private SecurityIncidentCompanyRepository affectedRepository;
    @Mock
    private SecurityIncidentRepository incidentRepository;

    @InjectMocks
    private ListAffectedCompaniesService service;

    @Nested
    @DisplayName("incidente inexistente")
    class Inexistente {

        @Test
        @DisplayName("404 en vez de una pagina vacia, y no consulta el reparto")
        void revienta_sin_consultar_el_reparto() {
            when(incidentRepository.findById(SecurityIncidentMother.INCIDENT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.listByIncident(SecurityIncidentMother.INCIDENT_ID, 0, 20))
                    .isInstanceOf(SecurityIncidentNotFoundException.class).hasMessageContaining(
                            "Security incident not found: " + SecurityIncidentMother.INCIDENT_ID);

            verifyNoInteractions(affectedRepository);
        }
    }

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("mapea el reparto conservando la paginacion del repositorio")
        void mapea_el_reparto_conservando_la_paginacion() {
            when(incidentRepository.findById(SecurityIncidentMother.INCIDENT_ID))
                    .thenReturn(Optional.of(SecurityIncidentMother.registrado()));
            SecurityIncidentCompany afectada = SecurityIncidentMother.afectada();
            when(affectedRepository.findByIncidentId(SecurityIncidentMother.INCIDENT_ID, 0, 20))
                    .thenReturn(new PageResult<>(List.of(afectada), 0, 20, 1L, 1));

            PageResult<AffectedCompanyDto> resultado = service
                    .listByIncident(SecurityIncidentMother.INCIDENT_ID, 0, 20);

            assertThat(resultado.content()).hasSize(1);
            assertThat(resultado.content().get(0).companyId())
                    .isEqualTo(SecurityIncidentMother.COMPANY_ID);
            assertThat(resultado.content().get(0).affectedScope())
                    .isEqualTo(SecurityIncidentMother.AFFECTED_SCOPE);
            assertThat(resultado.totalElements()).isEqualTo(1L);
        }
    }
}
