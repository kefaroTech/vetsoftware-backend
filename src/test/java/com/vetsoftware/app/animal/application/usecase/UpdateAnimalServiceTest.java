package com.vetsoftware.app.animal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.application.command.UpdateAnimalCommand;
import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.animal.application.port.out.AnimalColorQueryPort;
import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.application.port.out.BreedQueryPort;
import com.vetsoftware.app.animal.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.animal.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.animal.application.port.out.SpecieQueryPort;
import com.vetsoftware.app.animal.domain.Animal;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import com.vetsoftware.app.animal.domain.AnimalType;
import com.vetsoftware.app.animal.domain.Gender;
import com.vetsoftware.app.animal.domain.ReproductiveState;
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
@DisplayName("UpdateAnimalService")
class UpdateAnimalServiceTest {

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

    @InjectMocks
    private UpdateAnimalService service;

    @Captor
    private ArgumentCaptor<Animal> animalCaptor;

    private void animalExisteYLasNuevasReferenciasTambien(Animal existente) {
        when(repository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                .thenReturn(Optional.of(existente));
        when(specieQueryPort.findById(AnimalMother.GATO.id()))
                .thenReturn(Optional.of(AnimalMother.GATO));
        when(breedQueryPort.findById(AnimalMother.SIAMES.id()))
                .thenReturn(Optional.of(AnimalMother.SIAMES));
        when(ownerQueryPort.findByIdAndCompanyId(AnimalMother.OTRO_DUENO.id(),
                AnimalMother.COMPANY_ID)).thenReturn(Optional.of(AnimalMother.OTRO_DUENO));
        when(companyQueryPort.findById(AnimalMother.COMPANY_ID))
                .thenReturn(Optional.of(AnimalMother.CLINICA));
        when(animalColorQueryPort.findById(AnimalMother.BLANCO.id()))
                .thenReturn(Optional.of(AnimalMother.BLANCO));
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("aplica los campos nuevos sobre el animal cargado y lo persiste")
        void aplica_los_campos_nuevos_sobre_el_animal_cargado() {
            Animal existente = AnimalMother.perroSano();
            animalExisteYLasNuevasReferenciasTambien(existente);
            when(repository.save(any())).thenReturn(existente);

            service.execute(AnimalMother.comandoActualizar());

            verify(repository).save(animalCaptor.capture());
            Animal guardado = animalCaptor.getValue();
            assertThat(guardado).isSameAs(existente);
            assertThat(guardado.getName()).isEqualTo("Michi");
            assertThat(guardado.getCode()).isEqualTo("A-002");
            assertThat(guardado.getSpecie()).isEqualTo(AnimalMother.GATO);
            assertThat(guardado.getBreed()).isEqualTo(AnimalMother.SIAMES);
            assertThat(guardado.getOwner()).isEqualTo(AnimalMother.OTRO_DUENO);
            assertThat(guardado.getColor()).isEqualTo(AnimalMother.BLANCO);
            assertThat(guardado.getGender()).isEqualTo(Gender.FEMALE);
            assertThat(guardado.getWeightType()).isEqualTo(WeightType.GRAMS);
            assertThat(guardado.getAnimalType()).isEqualTo(AnimalType.SUPPORT);
            assertThat(guardado.getReproductiveState()).isEqualTo(ReproductiveState.NO_STERILIZED);
            assertThat(guardado.getSize()).isEqualTo(12);
        }

        @Test
        @DisplayName("no cambia el id ni la fecha de creacion")
        void no_cambia_el_id_ni_la_fecha_de_creacion() {
            Animal existente = AnimalMother.perroSano();
            animalExisteYLasNuevasReferenciasTambien(existente);
            when(repository.save(any())).thenReturn(existente);

            AnimalDto dto = service.execute(AnimalMother.comandoActualizar());

            assertThat(dto.id()).isEqualTo(AnimalMother.ANIMAL_ID);
            assertThat(dto.createdDate()).isEqualTo(AnimalMother.CREADO);
        }

        @Test
        @DisplayName("ignora el peso del comando: la serie temporal no se toca desde aqui")
        void ignora_el_peso_del_comando() {
            Animal existente = AnimalMother.conPesoDerivado(new BigDecimal("12.50"),
                    WeightType.KILOGRAMS, LocalDate.of(2026, 2, 1));
            animalExisteYLasNuevasReferenciasTambien(existente);
            when(repository.save(any())).thenReturn(existente);

            UpdateAnimalCommand comando = new UpdateAnimalCommand(AnimalMother.ANIMAL_ID, "Michi",
                    "A-002", AnimalMother.GATO.id(), AnimalMother.SIAMES.id(),
                    AnimalMother.OTRO_DUENO.id(), Gender.FEMALE, WeightType.GRAMS,
                    AnimalType.SUPPORT, ReproductiveState.NO_STERILIZED, AnimalMother.BLANCO.id(),
                    LocalDate.of(2021, 3, 1), new BigDecimal("999"), 12, false, null,
                    AnimalMother.COMPANY_ID);

            AnimalDto dto = service.execute(comando);

            // El 999 del comando se descarta; el peso sigue siendo el del ultimo
            // WeightRecord. Si alguien "arregla" esto, el peso pasaria a tener dos
            // fuentes de verdad.
            assertThat(dto.weight()).isEqualByComparingTo("12.50");
            assertThat(dto.weightMeasuredAt()).isEqualTo(LocalDate.of(2026, 2, 1));
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class AislamientoPorEmpresa {

        @Test
        @DisplayName("un animal de otra empresa no existe: ni se resuelven referencias ni se guarda")
        void un_animal_de_otra_empresa_no_existe() {
            when(repository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalMother.comandoActualizar()))
                    .isInstanceOf(AnimalNotFoundException.class)
                    .hasMessageContaining("Animal not found: " + AnimalMother.ANIMAL_ID);

            verifyNoInteractions(specieQueryPort, breedQueryPort, ownerQueryPort, companyQueryPort,
                    animalColorQueryPort);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("especie inexistente aborta antes de tocar el animal")
        void especie_inexistente() {
            when(repository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                    .thenReturn(Optional.of(AnimalMother.perroSano()));
            when(specieQueryPort.findById(AnimalMother.GATO.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Specie not found: " + AnimalMother.GATO.id());

            verifyNoInteractions(breedQueryPort, ownerQueryPort, companyQueryPort,
                    animalColorQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un animal cargado no se muta si una referencia posterior falla")
        void un_animal_cargado_no_se_muta_si_una_referencia_falla() {
            Animal existente = AnimalMother.perroSano();
            when(repository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                    .thenReturn(Optional.of(existente));
            when(specieQueryPort.findById(AnimalMother.GATO.id()))
                    .thenReturn(Optional.of(AnimalMother.GATO));
            when(breedQueryPort.findById(AnimalMother.SIAMES.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(existente.getName()).isEqualTo("Firulais");
            assertThat(existente.getSpecie()).isEqualTo(AnimalMother.PERRO);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("dueno de otra empresa")
        void dueno_de_otra_empresa() {
            when(repository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                    .thenReturn(Optional.of(AnimalMother.perroSano()));
            when(specieQueryPort.findById(AnimalMother.GATO.id()))
                    .thenReturn(Optional.of(AnimalMother.GATO));
            when(breedQueryPort.findById(AnimalMother.SIAMES.id()))
                    .thenReturn(Optional.of(AnimalMother.SIAMES));
            when(ownerQueryPort.findByIdAndCompanyId(AnimalMother.OTRO_DUENO.id(),
                    AnimalMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Owner not found: " + AnimalMother.OTRO_DUENO.id());

            verifyNoInteractions(companyQueryPort, animalColorQueryPort);
        }

        @Test
        @DisplayName("empresa inexistente")
        void empresa_inexistente() {
            when(repository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                    .thenReturn(Optional.of(AnimalMother.perroSano()));
            when(specieQueryPort.findById(AnimalMother.GATO.id()))
                    .thenReturn(Optional.of(AnimalMother.GATO));
            when(breedQueryPort.findById(AnimalMother.SIAMES.id()))
                    .thenReturn(Optional.of(AnimalMother.SIAMES));
            when(ownerQueryPort.findByIdAndCompanyId(AnimalMother.OTRO_DUENO.id(),
                    AnimalMother.COMPANY_ID)).thenReturn(Optional.of(AnimalMother.OTRO_DUENO));
            when(companyQueryPort.findById(AnimalMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + AnimalMother.COMPANY_ID);

            verifyNoInteractions(animalColorQueryPort);
        }

        @Test
        @DisplayName("color inexistente")
        void color_inexistente() {
            when(repository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                    .thenReturn(Optional.of(AnimalMother.perroSano()));
            when(specieQueryPort.findById(AnimalMother.GATO.id()))
                    .thenReturn(Optional.of(AnimalMother.GATO));
            when(breedQueryPort.findById(AnimalMother.SIAMES.id()))
                    .thenReturn(Optional.of(AnimalMother.SIAMES));
            when(ownerQueryPort.findByIdAndCompanyId(AnimalMother.OTRO_DUENO.id(),
                    AnimalMother.COMPANY_ID)).thenReturn(Optional.of(AnimalMother.OTRO_DUENO));
            when(companyQueryPort.findById(AnimalMother.COMPANY_ID))
                    .thenReturn(Optional.of(AnimalMother.CLINICA));
            when(animalColorQueryPort.findById(AnimalMother.BLANCO.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("AnimalColor not found: " + AnimalMother.BLANCO.id());

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("marcar fallecido sin fecha no persiste nada")
        void marcar_fallecido_sin_fecha_no_persiste_nada() {
            Animal existente = AnimalMother.perroSano();
            animalExisteYLasNuevasReferenciasTambien(existente);
            UpdateAnimalCommand comando = new UpdateAnimalCommand(AnimalMother.ANIMAL_ID, "Michi",
                    "A-002", AnimalMother.GATO.id(), AnimalMother.SIAMES.id(),
                    AnimalMother.OTRO_DUENO.id(), Gender.FEMALE, WeightType.GRAMS,
                    AnimalType.SUPPORT, ReproductiveState.NO_STERILIZED, AnimalMother.BLANCO.id(),
                    LocalDate.of(2021, 3, 1), null, 12, true, null, AnimalMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("deceasedDate is required when deceased is true");

            verify(repository, never()).save(any());
            assertThat(existente.isDeceased()).isFalse();
        }
    }
}
