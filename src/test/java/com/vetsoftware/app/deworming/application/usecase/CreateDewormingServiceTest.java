package com.vetsoftware.app.deworming.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.deworming.application.command.CreateDewormingCommand;
import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import com.vetsoftware.app.deworming.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.deworming.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.deworming.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.deworming.application.port.out.DewormingRepository;
import com.vetsoftware.app.deworming.domain.Deworming;
import com.vetsoftware.app.deworming.domain.DewormingType;
import com.vetsoftware.app.deworming.testsupport.DewormingMother;
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
@DisplayName("CreateDewormingService")
class CreateDewormingServiceTest {

    @Mock
    private DewormingRepository repository;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private ConsultationQueryPort consultationQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private CreateDewormingService service;

    @Captor
    private ArgumentCaptor<Deworming> capturador;

    /**
     * Deja los tres puertos resolviendo la referencia esperada, con consulta
     * incluida.
     */
    private void referenciasResueltasConConsulta() {
        when(animalQueryPort.findByIdAndCompanyId(DewormingMother.ANIMAL_ID,
                DewormingMother.COMPANY_ID)).thenReturn(Optional.of(DewormingMother.FIRULAIS));
        when(consultationQueryPort.findByIdAndCompanyId(DewormingMother.CONSULTATION_ID,
                DewormingMother.COMPANY_ID)).thenReturn(Optional.of(DewormingMother.CONSULTA));
        when(companyQueryPort.findById(DewormingMother.COMPANY_ID))
                .thenReturn(Optional.of(DewormingMother.CLINICA));
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("persiste la desparasitacion con las referencias resueltas por los puertos")
        void persiste_la_desparasitacion_con_las_referencias_resueltas() {
            referenciasResueltasConConsulta();
            when(repository.save(any())).thenReturn(DewormingMother.desparasitacionValida());

            service.execute(DewormingMother.comandoCrear());

            verify(repository).save(capturador.capture());
            Deworming guardada = capturador.getValue();
            // Lo que importa no es que se llamara a save, sino que se guardara ESTO: las
            // refs
            // tienen que venir de los puertos, no de los ids del comando.
            assertThat(guardada.getAnimal()).isEqualTo(DewormingMother.FIRULAIS);
            assertThat(guardada.getConsultation()).isEqualTo(DewormingMother.CONSULTA);
            assertThat(guardada.getCompany()).isEqualTo(DewormingMother.CLINICA);
            assertThat(guardada.getProduct()).isEqualTo("Drontal Plus");
            assertThat(guardada.getType()).isEqualTo(DewormingType.INTERNAL);
            assertThat(guardada.getId()).isNull();
        }

        @Test
        @DisplayName("con consultationId null no consulta el puerto de consultas")
        void con_consultation_id_null_no_consulta_el_puerto() {
            when(animalQueryPort.findByIdAndCompanyId(DewormingMother.ANIMAL_ID,
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.of(DewormingMother.FIRULAIS));
            when(companyQueryPort.findById(DewormingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DewormingMother.CLINICA));
            when(repository.save(any())).thenReturn(DewormingMother.sinConsulta());

            service.execute(DewormingMother.comandoCrearSinConsulta());

            verifyNoInteractions(consultationQueryPort);
            verify(repository).save(capturador.capture());
            assertThat(capturador.getValue().getConsultation()).isNull();
        }

        @Test
        @DisplayName("devuelve el DTO de la desparasitacion ya persistida, con su id")
        void devuelve_el_dto_de_la_desparasitacion_ya_persistida() {
            referenciasResueltasConConsulta();
            when(repository.save(any())).thenReturn(DewormingMother.desparasitacionValida());

            DewormingDto dto = service.execute(DewormingMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(DewormingMother.DEWORMING_ID);
            assertThat(dto.product()).isEqualTo("Drontal Plus");
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("animal inexistente: no consulta los puertos siguientes ni persiste")
        void animal_inexistente() {
            when(animalQueryPort.findByIdAndCompanyId(DewormingMother.ANIMAL_ID,
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DewormingMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + DewormingMother.ANIMAL_ID);

            verifyNoInteractions(consultationQueryPort, companyQueryPort, repository);
        }

        @Test
        @DisplayName("consulta inexistente")
        void consulta_inexistente() {
            when(animalQueryPort.findByIdAndCompanyId(DewormingMother.ANIMAL_ID,
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.of(DewormingMother.FIRULAIS));
            when(consultationQueryPort.findByIdAndCompanyId(DewormingMother.CONSULTATION_ID,
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DewormingMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Consultation not found: " + DewormingMother.CONSULTATION_ID);

            verifyNoInteractions(companyQueryPort, repository);
        }

        @Test
        @DisplayName("empresa inexistente")
        void empresa_inexistente() {
            when(animalQueryPort.findByIdAndCompanyId(DewormingMother.ANIMAL_ID,
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.of(DewormingMother.FIRULAIS));
            when(consultationQueryPort.findByIdAndCompanyId(DewormingMother.CONSULTATION_ID,
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.of(DewormingMother.CONSULTA));
            when(companyQueryPort.findById(DewormingMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DewormingMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + DewormingMother.COMPANY_ID);

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("un producto vacio no llega a persistirse")
        void un_producto_vacio_no_llega_a_persistirse() {
            referenciasResueltasConConsulta();
            CreateDewormingCommand comando = new CreateDewormingCommand(DewormingMother.FECHA, null,
                    DewormingType.INTERNAL, "  ", "1 tableta", null, null,
                    DewormingMother.ANIMAL_ID, DewormingMother.CONSULTATION_ID,
                    DewormingMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("product is required");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tenancy — referencias de otra empresa")
    class Tenancy {

        /**
         * En el alta no hay carga previa de ninguna desparasitacion que valide la
         * empresa: el filtro de las dos referencias es toda la barrera. Con un
         * {@code findById} pelado, un animalId ajeno resolvia y la desparasitacion
         * quedaba escrita en la historia clinica del otro tenant.
         */
        @Test
        @DisplayName("un animal de otra empresa no resuelve: no persiste ni sigue resolviendo")
        void un_animal_de_otra_empresa_no_resuelve() {
            when(animalQueryPort.findByIdAndCompanyId(DewormingMother.ANIMAL_ID,
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DewormingMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + DewormingMother.ANIMAL_ID);

            verifyNoInteractions(consultationQueryPort, companyQueryPort, repository);
        }

        @Test
        @DisplayName("una consulta de otra empresa no resuelve: no persiste nada")
        void una_consulta_de_otra_empresa_no_resuelve() {
            when(animalQueryPort.findByIdAndCompanyId(DewormingMother.ANIMAL_ID,
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.of(DewormingMother.FIRULAIS));
            when(consultationQueryPort.findByIdAndCompanyId(DewormingMother.CONSULTATION_ID,
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DewormingMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Consultation not found: " + DewormingMother.CONSULTATION_ID);

            verifyNoInteractions(companyQueryPort, repository);
        }
    }
}
