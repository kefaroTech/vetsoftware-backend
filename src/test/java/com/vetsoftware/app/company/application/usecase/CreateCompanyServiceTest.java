package com.vetsoftware.app.company.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.out.CityQueryPort;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.domain.Company;
import com.vetsoftware.app.company.testsupport.CompanyMother;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCompanyService")
class CreateCompanyServiceTest {

    @Mock
    private CompanyRepository repository;
    @Mock
    private CityQueryPort cityQueryPort;
    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-01-15T10:30:00Z"), ZoneOffset.UTC);

    @InjectMocks
    private CreateCompanyService service;

    @Captor
    private ArgumentCaptor<Company> companyCaptor;

    private void laCiudadExiste() {
        when(cityQueryPort.findById(CompanyMother.BOGOTA.id()))
                .thenReturn(Optional.of(CompanyMother.BOGOTA));
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("persiste la empresa con las referencias resueltas por los puertos")
        void persiste_la_empresa_con_las_referencias_resueltas() {
            laCiudadExiste();
            when(repository.save(any())).thenReturn(CompanyMother.clinicaNorte());

            service.execute(CompanyMother.comandoCrear());

            verify(repository).save(companyCaptor.capture());
            Company guardada = companyCaptor.getValue();
            assertThat(guardada.getName()).isEqualTo("Clinica Norte");
            assertThat(guardada.getIdentifier()).isEqualTo("NIT-900");
            assertThat(guardada.getAddress()).isEqualTo("Calle 123 #45-67");
            assertThat(guardada.getContactNumber()).isEqualTo("3001234567");
            assertThat(guardada.getCity()).isEqualTo(CompanyMother.BOGOTA);
        }

        @Test
        @DisplayName("la empresa guardada nace sin id y habilitada")
        void la_empresa_guardada_nace_sin_id_y_habilitada() {
            laCiudadExiste();
            when(repository.save(any())).thenReturn(CompanyMother.clinicaNorte());

            service.execute(CompanyMother.comandoCrear());

            verify(repository).save(companyCaptor.capture());
            assertThat(companyCaptor.getValue().getId()).isNull();
            assertThat(companyCaptor.getValue().isEnabled()).isTrue();
            assertThat(companyCaptor.getValue().getCreatedDate()).isEqualTo(CompanyMother.CREADO);
        }

        @Test
        @DisplayName("devuelve el DTO de lo que el repositorio devolvio, no del comando")
        void devuelve_el_dto_de_lo_que_devolvio_el_repositorio() {
            laCiudadExiste();
            when(repository.save(any())).thenReturn(CompanyMother.clinicaNorte(77L));

            CompanyDto dto = service.execute(CompanyMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(77L);
            assertThat(dto.name()).isEqualTo("Clinica Norte");
            assertThat(dto.city().name()).isEqualTo("Bogota");
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("referencias que no resuelven")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("sin ciudad no escribe nada")
        void sin_ciudad_no_escribe_nada() {
            when(cityQueryPort.findById(CompanyMother.BOGOTA.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(CompanyMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("City not found: 11");

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("comando que viola las invariantes del agregado")
    class ComandoInvalido {

        @Test
        @DisplayName("un nombre en blanco aborta antes de tocar el repositorio")
        void un_nombre_en_blanco_aborta_antes_de_escribir() {
            laCiudadExiste();
            CreateCompanyCommand comando = new CreateCompanyCommand("  ", "NIT-900", null, null,
                    CompanyMother.BOGOTA.id());

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("un identificador demasiado largo aborta antes de tocar el repositorio")
        void un_identificador_demasiado_largo_aborta() {
            laCiudadExiste();
            CreateCompanyCommand comando = new CreateCompanyCommand("Clinica Norte", "i".repeat(51),
                    null, null, CompanyMother.BOGOTA.id());

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("identifier must be 50 chars or less");

            verifyNoInteractions(repository);
        }
    }
}
