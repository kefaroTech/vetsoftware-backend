package com.vetsoftware.app.animal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.application.command.CreateAnimalCommand;
import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.animal.application.port.out.AnimalColorQueryPort;
import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.application.port.out.BreedQueryPort;
import com.vetsoftware.app.animal.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.animal.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.animal.application.port.out.SpecieQueryPort;
import com.vetsoftware.app.animal.application.port.out.WeightRecordRepository;
import com.vetsoftware.app.animal.domain.Animal;
import com.vetsoftware.app.animal.domain.WeightRecord;
import com.vetsoftware.app.animal.domain.WeightSource;
import com.vetsoftware.app.animal.domain.WeightType;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@DisplayName("CreateAnimalService")
class CreateAnimalServiceTest {

    @Mock
    private AnimalRepository repository;
    @Mock
    private SpecieQueryPort specieQueryPort;
    @Mock
    private BreedQueryPort breedQueryPort;
    @Mock
    private OwnerQueryPort ownerQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private AnimalColorQueryPort animalColorQueryPort;
    @Mock
    private WeightRecordRepository weightRecordRepository;

    @InjectMocks
    private CreateAnimalService service;

    @Captor
    private ArgumentCaptor<Animal> animalCaptor;
    @Captor
    private ArgumentCaptor<WeightRecord> weightCaptor;

    /** Deja los cinco puertos resolviendo la referencia esperada. */
    private void todasLasReferenciasExisten() {
        when(specieQueryPort.findById(AnimalMother.PERRO.id()))
                .thenReturn(Optional.of(AnimalMother.PERRO));
        when(breedQueryPort.findById(AnimalMother.LABRADOR.id()))
                .thenReturn(Optional.of(AnimalMother.LABRADOR));
        when(ownerQueryPort.findByIdAndCompanyId(AnimalMother.DUENO.id(), AnimalMother.COMPANY_ID))
                .thenReturn(Optional.of(AnimalMother.DUENO));
        when(companyQueryPort.findById(AnimalMother.COMPANY_ID))
                .thenReturn(Optional.of(AnimalMother.CLINICA));
        when(animalColorQueryPort.findById(AnimalMother.NEGRO.id()))
                .thenReturn(Optional.of(AnimalMother.NEGRO));
    }

    @Nested
    @DisplayName("creacion sin peso inicial")
    class SinPesoInicial {

        @Test
        @DisplayName("persiste el animal con las referencias resueltas por los puertos")
        void persiste_el_animal_con_las_referencias_resueltas() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(AnimalMother.perroSano());

            service.execute(AnimalMother.comandoCrear());

            verify(repository).save(animalCaptor.capture());
            Animal guardado = animalCaptor.getValue();
            // Lo que importa no es que se llamara a save, sino que se guardara ESTO:
            // las refs tienen que venir de los puertos, no de los ids del comando.
            assertThat(guardado.getSpecie()).isEqualTo(AnimalMother.PERRO);
            assertThat(guardado.getBreed()).isEqualTo(AnimalMother.LABRADOR);
            assertThat(guardado.getOwner()).isEqualTo(AnimalMother.DUENO);
            assertThat(guardado.getCompany()).isEqualTo(AnimalMother.CLINICA);
            assertThat(guardado.getColor()).isEqualTo(AnimalMother.NEGRO);
            assertThat(guardado.getName()).isEqualTo("Firulais");
            assertThat(guardado.getCode()).isEqualTo("A-001");
            assertThat(guardado.getId()).isNull();
            assertThat(guardado.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("no crea ningun registro de peso")
        void no_crea_ningun_registro_de_peso() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(AnimalMother.perroSano());

            AnimalDto dto = service.execute(AnimalMother.comandoCrear());

            verifyNoInteractions(weightRecordRepository);
            assertThat(dto.weight()).isNull();
            assertThat(dto.weightMeasuredAt()).isNull();
        }

        @Test
        @DisplayName("devuelve el DTO del animal ya persistido, con su id")
        void devuelve_el_dto_del_animal_ya_persistido() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(AnimalMother.perroSano());

            AnimalDto dto = service.execute(AnimalMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(AnimalMother.ANIMAL_ID);
            assertThat(dto.name()).isEqualTo("Firulais");
        }
    }

    @Nested
    @DisplayName("creacion con peso inicial")
    class ConPesoInicial {

        @Test
        @DisplayName("abre la serie temporal con un registro MANUAL medido hoy")
        void abre_la_serie_temporal_con_un_registro_manual_medido_hoy() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(AnimalMother.perroSano());

            service.execute(AnimalMother.comandoCrear(new BigDecimal("12.50")));

            verify(weightRecordRepository).save(weightCaptor.capture());
            WeightRecord registro = weightCaptor.getValue();
            assertThat(registro.getValue()).isEqualByComparingTo("12.50");
            assertThat(registro.getUnit()).isEqualTo(WeightType.KILOGRAMS);
            assertThat(registro.getSource()).isEqualTo(WeightSource.MANUAL);
            assertThat(registro.getSourceId()).isNull();
            assertThat(registro.getMeasuredAt()).isEqualTo(LocalDate.now());
            assertThat(registro.getCompany()).isEqualTo(AnimalMother.CLINICA);
        }

        @Test
        @DisplayName("el registro apunta al animal YA persistido, no al que aun no tiene id")
        void el_registro_apunta_al_animal_ya_persistido() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(AnimalMother.perroSano());

            service.execute(AnimalMother.comandoCrear(new BigDecimal("12.50")));

            verify(weightRecordRepository).save(weightCaptor.capture());
            // AnimalRef exige id no nulo: si el ref se construyera con el animal de
            // entrada en vez de con el devuelto por save(), esto reventaria en runtime.
            assertThat(weightCaptor.getValue().getAnimal().id()).isEqualTo(AnimalMother.ANIMAL_ID);
            assertThat(weightCaptor.getValue().getAnimal().name()).isEqualTo("Firulais");
            assertThat(weightCaptor.getValue().getAnimal().code()).isEqualTo("A-001");
        }

        @Test
        @DisplayName("el DTO devuelto ya trae el peso, sin volver a leer de base")
        void el_dto_devuelto_ya_trae_el_peso() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(AnimalMother.perroSano());

            AnimalDto dto = service.execute(AnimalMother.comandoCrear(new BigDecimal("12.50")));

            assertThat(dto.weight()).isEqualByComparingTo("12.50");
            assertThat(dto.weightMeasuredAt()).isEqualTo(LocalDate.now());
            assertThat(dto.weightType()).isEqualTo(WeightType.KILOGRAMS);
        }

        @Test
        @DisplayName("un peso inicial invalido aborta toda la creacion")
        void un_peso_inicial_invalido_aborta_toda_la_creacion() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(AnimalMother.perroSano());

            // El animal ya se guardo cuando el peso se rechaza. Lo que impide dejar el
            // animal huerfano sin su primer registro es el @Transactional del metodo:
            // si alguien lo quita, este caso deja basura en base y el test no lo ve.
            assertThatThrownBy(
                    () -> service.execute(AnimalMother.comandoCrear(new BigDecimal("-1"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("value must be greater than zero");

            verify(weightRecordRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("especie inexistente: no consulta los puertos siguientes ni persiste")
        void especie_inexistente() {
            when(specieQueryPort.findById(AnimalMother.PERRO.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Specie not found: " + AnimalMother.PERRO.id());

            verifyNoInteractions(breedQueryPort, ownerQueryPort, companyQueryPort,
                    animalColorQueryPort, repository, weightRecordRepository);
        }

        @Test
        @DisplayName("raza inexistente")
        void raza_inexistente() {
            when(specieQueryPort.findById(AnimalMother.PERRO.id()))
                    .thenReturn(Optional.of(AnimalMother.PERRO));
            when(breedQueryPort.findById(AnimalMother.LABRADOR.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Breed not found: " + AnimalMother.LABRADOR.id());

            verifyNoInteractions(ownerQueryPort, companyQueryPort, animalColorQueryPort,
                    repository);
        }

        @Test
        @DisplayName("dueno de otra empresa: se busca siempre acotado por companyId")
        void dueno_de_otra_empresa() {
            when(specieQueryPort.findById(AnimalMother.PERRO.id()))
                    .thenReturn(Optional.of(AnimalMother.PERRO));
            when(breedQueryPort.findById(AnimalMother.LABRADOR.id()))
                    .thenReturn(Optional.of(AnimalMother.LABRADOR));
            when(ownerQueryPort.findByIdAndCompanyId(AnimalMother.DUENO.id(),
                    AnimalMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Owner not found: " + AnimalMother.DUENO.id());

            // El puerto de dueno es el unico con firma (id, companyId): es la defensa
            // que impide colgar un animal de un dueno de otro tenant.
            verify(ownerQueryPort).findByIdAndCompanyId(AnimalMother.DUENO.id(),
                    AnimalMother.COMPANY_ID);
            verifyNoInteractions(companyQueryPort, animalColorQueryPort, repository);
        }

        @Test
        @DisplayName("empresa inexistente")
        void empresa_inexistente() {
            when(specieQueryPort.findById(AnimalMother.PERRO.id()))
                    .thenReturn(Optional.of(AnimalMother.PERRO));
            when(breedQueryPort.findById(AnimalMother.LABRADOR.id()))
                    .thenReturn(Optional.of(AnimalMother.LABRADOR));
            when(ownerQueryPort.findByIdAndCompanyId(AnimalMother.DUENO.id(),
                    AnimalMother.COMPANY_ID)).thenReturn(Optional.of(AnimalMother.DUENO));
            when(companyQueryPort.findById(AnimalMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + AnimalMother.COMPANY_ID);

            verifyNoInteractions(animalColorQueryPort, repository);
        }

        @Test
        @DisplayName("color inexistente")
        void color_inexistente() {
            when(specieQueryPort.findById(AnimalMother.PERRO.id()))
                    .thenReturn(Optional.of(AnimalMother.PERRO));
            when(breedQueryPort.findById(AnimalMother.LABRADOR.id()))
                    .thenReturn(Optional.of(AnimalMother.LABRADOR));
            when(ownerQueryPort.findByIdAndCompanyId(AnimalMother.DUENO.id(),
                    AnimalMother.COMPANY_ID)).thenReturn(Optional.of(AnimalMother.DUENO));
            when(companyQueryPort.findById(AnimalMother.COMPANY_ID))
                    .thenReturn(Optional.of(AnimalMother.CLINICA));
            when(animalColorQueryPort.findById(AnimalMother.NEGRO.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("AnimalColor not found: " + AnimalMother.NEGRO.id());

            verifyNoInteractions(repository, weightRecordRepository);
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("un nombre vacio no llega a persistirse")
        void un_nombre_vacio_no_llega_a_persistirse() {
            todasLasReferenciasExisten();
            CreateAnimalCommand comando = new CreateAnimalCommand("  ", "A-001",
                    AnimalMother.PERRO.id(), AnimalMother.LABRADOR.id(), AnimalMother.DUENO.id(),
                    com.vetsoftware.app.animal.domain.Gender.MALE, WeightType.KILOGRAMS,
                    com.vetsoftware.app.animal.domain.AnimalType.NONE,
                    com.vetsoftware.app.animal.domain.ReproductiveState.STERILIZED,
                    AnimalMother.NEGRO.id(), AnimalMother.NACIMIENTO, null, 30, false, null,
                    AnimalMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

            verify(repository, never()).save(any());
        }
    }
}
