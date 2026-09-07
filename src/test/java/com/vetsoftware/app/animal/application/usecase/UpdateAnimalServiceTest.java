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
import com.vetsoftware.app.animal.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.animal.application.port.out.SpecieQueryPort;
import com.vetsoftware.app.animal.domain.Animal;
import com.vetsoftware.app.animal.domain.AnimalColorRef;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import com.vetsoftware.app.animal.domain.BreedRef;
import com.vetsoftware.app.animal.domain.Gender;
import com.vetsoftware.app.animal.domain.OwnerRef;
import com.vetsoftware.app.animal.domain.ReproductiveState;
import com.vetsoftware.app.animal.domain.SpecieRef;
import com.vetsoftware.app.animal.domain.WeightType;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
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

    private static final SpecieRef GATO = new SpecieRef(10L, "Gato");
    private static final BreedRef SIAMES = new BreedRef(11L, "Siames");
    private static final OwnerRef OTRO_DUENO = new OwnerRef(12L, "Carlos Pena", "CC-2040");
    private static final AnimalColorRef BLANCO = new AnimalColorRef(13L, "Blanco");

    @Mock
    private AnimalRepository repository;
    @Mock
    private SpecieQueryPort specieQueryPort;
    @Mock
    private BreedQueryPort breedQueryPort;
    @Mock
    private OwnerQueryPort ownerQueryPort;
    @Mock
    private AnimalColorQueryPort animalColorQueryPort;

    @InjectMocks
    private UpdateAnimalService service;

    @Captor
    private ArgumentCaptor<Animal> animalCaptor;

    private void elAnimalExiste() {
        when(repository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                .thenReturn(Optional.of(AnimalMother.perroSano()));
    }

    private void referenciasOriginalesExisten() {
        when(specieQueryPort.findById(AnimalMother.PERRO.id()))
                .thenReturn(Optional.of(AnimalMother.PERRO));
        when(breedQueryPort.findById(AnimalMother.LABRADOR.id()))
                .thenReturn(Optional.of(AnimalMother.LABRADOR));
        when(ownerQueryPort.findByIdAndCompanyId(AnimalMother.DUENO.id(), AnimalMother.COMPANY_ID))
                .thenReturn(Optional.of(AnimalMother.DUENO));
        when(animalColorQueryPort.findById(AnimalMother.NEGRO.id()))
                .thenReturn(Optional.of(AnimalMother.NEGRO));
    }

    private UpdateAnimalCommand comandoConNuevasReferencias() {
        return new UpdateAnimalCommand(AnimalMother.ANIMAL_ID, "Michi", "B-002", GATO.id(),
                SIAMES.id(), OTRO_DUENO.id(), Gender.FEMALE, WeightType.POUNDS,
                com.vetsoftware.app.animal.domain.AnimalType.NONE, ReproductiveState.NO_STERILIZED,
                BLANCO.id(), AnimalMother.NACIMIENTO, 12, false, null, AnimalMother.COMPANY_ID);
    }

    @Nested
    @DisplayName("actualizacion valida")
    class ActualizacionValida {

        @Test
        @DisplayName("reemplaza los datos con las referencias resueltas por los puertos")
        void reemplaza_los_datos_con_las_referencias_resueltas() {
            elAnimalExiste();
            when(specieQueryPort.findById(GATO.id())).thenReturn(Optional.of(GATO));
            when(breedQueryPort.findById(SIAMES.id())).thenReturn(Optional.of(SIAMES));
            when(ownerQueryPort.findByIdAndCompanyId(OTRO_DUENO.id(), AnimalMother.COMPANY_ID))
                    .thenReturn(Optional.of(OTRO_DUENO));
            when(animalColorQueryPort.findById(BLANCO.id())).thenReturn(Optional.of(BLANCO));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AnimalDto dto = service.execute(comandoConNuevasReferencias());

            verify(repository).save(animalCaptor.capture());
            Animal guardado = animalCaptor.getValue();
            assertThat(guardado.getName()).isEqualTo("Michi");
            assertThat(guardado.getCode()).isEqualTo("B-002");
            assertThat(guardado.getSpecie()).isEqualTo(GATO);
            assertThat(guardado.getBreed()).isEqualTo(SIAMES);
            assertThat(guardado.getOwner()).isEqualTo(OTRO_DUENO);
            assertThat(guardado.getColor()).isEqualTo(BLANCO);
            assertThat(guardado.getGender()).isEqualTo(Gender.FEMALE);
            assertThat(guardado.getSize()).isEqualTo(12);
            // El companyId del comando autoriza y carga por tenant; la actualizacion
            // nunca reasigna la empresa del animal.
            assertThat(guardado.getCompany()).isEqualTo(AnimalMother.CLINICA);
            assertThat(dto.name()).isEqualTo("Michi");
        }
    }

    @Nested
    @DisplayName("animal inexistente")
    class AnimalInexistente {

        @Test
        @DisplayName("animal de otra empresa o inexistente: no consulta los demas puertos")
        void animal_inexistente_no_consulta_los_demas_puertos() {
            when(repository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalMother.comandoActualizar()))
                    .isInstanceOf(AnimalNotFoundException.class)
                    .hasMessageContaining("Animal not found: " + AnimalMother.ANIMAL_ID);

            verifyNoInteractions(specieQueryPort, breedQueryPort, ownerQueryPort,
                    animalColorQueryPort);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("especie inexistente: no consulta los puertos siguientes ni guarda")
        void especie_inexistente() {
            elAnimalExiste();
            when(specieQueryPort.findById(AnimalMother.PERRO.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Specie not found: " + AnimalMother.PERRO.id());

            verifyNoInteractions(breedQueryPort, ownerQueryPort, animalColorQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("dueno de otra empresa: se busca siempre acotado por companyId")
        void dueno_de_otra_empresa() {
            elAnimalExiste();
            when(specieQueryPort.findById(AnimalMother.PERRO.id()))
                    .thenReturn(Optional.of(AnimalMother.PERRO));
            when(breedQueryPort.findById(AnimalMother.LABRADOR.id()))
                    .thenReturn(Optional.of(AnimalMother.LABRADOR));
            when(ownerQueryPort.findByIdAndCompanyId(AnimalMother.DUENO.id(),
                    AnimalMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Owner not found: " + AnimalMother.DUENO.id());

            verifyNoInteractions(animalColorQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("color inexistente")
        void color_inexistente() {
            elAnimalExiste();
            referenciasOriginalesExisten();
            when(animalColorQueryPort.findById(AnimalMother.NEGRO.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("AnimalColor not found: " + AnimalMother.NEGRO.id());

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("un nombre vacio no llega a guardarse")
        void un_nombre_vacio_no_llega_a_guardarse() {
            elAnimalExiste();
            referenciasOriginalesExisten();
            UpdateAnimalCommand comando = new UpdateAnimalCommand(AnimalMother.ANIMAL_ID, "  ",
                    "A-001", AnimalMother.PERRO.id(), AnimalMother.LABRADOR.id(),
                    AnimalMother.DUENO.id(), Gender.MALE, WeightType.KILOGRAMS,
                    com.vetsoftware.app.animal.domain.AnimalType.NONE, ReproductiveState.STERILIZED,
                    AnimalMother.NEGRO.id(), AnimalMother.NACIMIENTO, 30, false, null,
                    AnimalMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

            verify(repository, never()).save(any());
        }
    }
}
