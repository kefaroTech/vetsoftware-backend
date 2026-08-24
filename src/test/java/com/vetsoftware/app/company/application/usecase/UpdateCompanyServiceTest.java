package com.vetsoftware.app.company.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.out.CityQueryPort;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.domain.Company;
import com.vetsoftware.app.company.domain.CompanyNotFoundException;
import com.vetsoftware.app.company.testsupport.CompanyMother;
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
@DisplayName("UpdateCompanyService")
class UpdateCompanyServiceTest {

    @Mock
    private CompanyRepository repository;
    @Mock
    private CityQueryPort cityQueryPort;

    @InjectMocks
    private UpdateCompanyService service;

    @Captor
    private ArgumentCaptor<Company> companyCaptor;

    private void laNuevaCiudadExiste() {
        when(cityQueryPort.findById(CompanyMother.MEDELLIN.id()))
                .thenReturn(Optional.of(CompanyMother.MEDELLIN));
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("muta el agregado cargado con los valores del comando")
        void muta_el_agregado_cargado_con_los_valores_del_comando() {
            Company existente = CompanyMother.clinicaNorte();
            when(repository.findById(CompanyMother.COMPANY_ID)).thenReturn(Optional.of(existente));
            laNuevaCiudadExiste();
            when(repository.save(any())).thenReturn(existente);

            service.execute(CompanyMother.comandoActualizar());

            verify(repository).save(companyCaptor.capture());
            Company guardada = companyCaptor.getValue();
            assertThat(guardada).isSameAs(existente);
            assertThat(guardada.getName()).isEqualTo("Clinica Sur");
            assertThat(guardada.getIdentifier()).isEqualTo("NIT-901");
            assertThat(guardada.getAddress()).isEqualTo("Carrera 45 #10-20");
            assertThat(guardada.getContactNumber()).isEqualTo("3009876543");
            assertThat(guardada.getCity()).isEqualTo(CompanyMother.MEDELLIN);
        }

        @Test
        @DisplayName("conserva id, fecha de creacion y estado habilitado del agregado cargado")
        void conserva_id_fecha_y_estado() {
            Company existente = CompanyMother.clinicaNorte();
            when(repository.findById(CompanyMother.COMPANY_ID)).thenReturn(Optional.of(existente));
            laNuevaCiudadExiste();
            when(repository.save(any())).thenReturn(existente);

            service.execute(CompanyMother.comandoActualizar());

            verify(repository).save(companyCaptor.capture());
            assertThat(companyCaptor.getValue().getId()).isEqualTo(CompanyMother.COMPANY_ID);
            assertThat(companyCaptor.getValue().getCreatedDate()).isEqualTo(CompanyMother.CREADO);
            assertThat(companyCaptor.getValue().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("devuelve el DTO de la empresa devuelta por el repositorio")
        void devuelve_el_dto_de_la_empresa_guardada() {
            Company existente = CompanyMother.clinicaNorte();
            when(repository.findById(CompanyMother.COMPANY_ID)).thenReturn(Optional.of(existente));
            laNuevaCiudadExiste();
            when(repository.save(any())).thenReturn(existente);

            CompanyDto dto = service.execute(CompanyMother.comandoActualizar());

            assertThat(dto.id()).isEqualTo(CompanyMother.COMPANY_ID);
            assertThat(dto.name()).isEqualTo("Clinica Sur");
            assertThat(dto.identifier()).isEqualTo("NIT-901");
            assertThat(dto.city().id()).isEqualTo(CompanyMother.MEDELLIN.id());
        }
    }

    @Nested
    @DisplayName("caminos de error")
    class Errores {

        @Test
        @DisplayName("empresa inexistente: no resuelve la ciudad ni escribe")
        void empresa_inexistente_no_escribe() {
            when(repository.findById(CompanyMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(CompanyMother.comandoActualizar()))
                    .isInstanceOf(CompanyNotFoundException.class)
                    .hasMessageContaining("Company not found: 9");

            verify(repository, never()).save(any());
            verifyNoInteractions(cityQueryPort);
        }

        @Test
        @DisplayName("ciudad inexistente: no escribe")
        void ciudad_inexistente_no_escribe() {
            when(repository.findById(CompanyMother.COMPANY_ID))
                    .thenReturn(Optional.of(CompanyMother.clinicaNorte()));
            when(cityQueryPort.findById(CompanyMother.MEDELLIN.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(CompanyMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("City not found: 12");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("comando invalido: no escribe y el agregado cargado queda intacto")
        void comando_invalido_no_escribe_y_deja_el_agregado_intacto() {
            Company existente = CompanyMother.clinicaNorte();
            when(repository.findById(CompanyMother.COMPANY_ID)).thenReturn(Optional.of(existente));
            laNuevaCiudadExiste();
            UpdateCompanyCommand comando = new UpdateCompanyCommand(CompanyMother.COMPANY_ID, "",
                    "NIT-901", null, null, CompanyMother.MEDELLIN.id());

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

            verify(repository, never()).save(any());
            assertThat(existente.getName()).isEqualTo("Clinica Norte");
            assertThat(existente.getCity()).isEqualTo(CompanyMother.BOGOTA);
        }
    }
}
