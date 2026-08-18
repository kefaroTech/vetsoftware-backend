package com.vetsoftware.app.specie.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.specie.application.port.out.AnimalChildrenQueryPort;
import com.vetsoftware.app.specie.application.port.out.AnimalColorChildrenQueryPort;
import com.vetsoftware.app.specie.application.port.out.BreedChildrenQueryPort;
import com.vetsoftware.app.specie.application.port.out.SpecieRepository;
import com.vetsoftware.app.specie.domain.SpecieHasActiveChildrenException;
import com.vetsoftware.app.specie.domain.SpecieNotFoundException;
import com.vetsoftware.app.specie.testsupport.SpecieMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Los tres tipos de hijo se prueban uno a uno y no en bucle: lo que este test
 * tiene que detectar es precisamente un copiar-pegar que compruebe un puerto y
 * reporte el nombre de otro, y un bucle no distingue eso (ver
 * {@code DeleteAnimalServiceTest}, el mismo patron en el modulo de referencia).
 *
 * <p>
 * Sin escenario de "tenant ajeno": {@code Specie} es un catalogo global sin
 * {@code companyId} en ningun punto de la cadena — ver el comentario en
 * {@code UpdateSpecieServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteSpecieService")
class DeleteSpecieServiceTest {

    @Mock
    private SpecieRepository repository;
    @Mock
    private BreedChildrenQueryPort breed;
    @Mock
    private AnimalColorChildrenQueryPort animalColor;
    @Mock
    private AnimalChildrenQueryPort animal;

    @InjectMocks
    private DeleteSpecieService service;

    private void especieExiste() {
        when(repository.findById(SpecieMother.SPECIE_ID))
                .thenReturn(Optional.of(SpecieMother.perro()));
    }

    private void borrar() {
        service.execute(SpecieMother.SPECIE_ID);
    }

    private void assertBloqueadoPor(String tipoHijo) {
        assertThatThrownBy(DeleteSpecieServiceTest.this::borrar)
                .isInstanceOf(SpecieHasActiveChildrenException.class)
                .hasMessageContaining("Cannot delete specie " + SpecieMother.SPECIE_ID)
                .hasMessageContaining("has active " + tipoHijo + " children");
        verify(repository, never()).delete(SpecieMother.SPECIE_ID);
    }

    @Nested
    @DisplayName("borrado permitido")
    class BorradoPermitido {

        @Test
        @DisplayName("sin hijos activos borra la especie")
        void sin_hijos_activos_borra_la_especie() {
            especieExiste();
            when(breed.existsActiveBySpecieId(SpecieMother.SPECIE_ID)).thenReturn(false);
            when(animalColor.existsActiveBySpecieId(SpecieMother.SPECIE_ID)).thenReturn(false);
            when(animal.existsActiveBySpecieId(SpecieMother.SPECIE_ID)).thenReturn(false);

            borrar();

            verify(repository).delete(SpecieMother.SPECIE_ID);
        }
    }

    @Nested
    @DisplayName("especie inexistente")
    class EspecieInexistente {

        @Test
        @DisplayName("una especie inexistente no consulta ningun puerto de hijos")
        void una_especie_inexistente_no_consulta_hijos() {
            when(repository.findById(SpecieMother.SPECIE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(DeleteSpecieServiceTest.this::borrar)
                    .isInstanceOf(SpecieNotFoundException.class)
                    .hasMessageContaining("Specie not found: " + SpecieMother.SPECIE_ID);

            verifyNoInteractions(breed, animalColor, animal);
            verify(repository, never()).delete(SpecieMother.SPECIE_ID);
        }
    }

    @Nested
    @DisplayName("bloqueo por hijos activos — un tipo por test, en el orden en que se comprueban")
    class BloqueoPorHijos {

        @Test
        @DisplayName("razas activas — primer puerto que se comprueba")
        void razas_activas() {
            especieExiste();
            when(breed.existsActiveBySpecieId(SpecieMother.SPECIE_ID)).thenReturn(true);

            assertBloqueadoPor("breed");
            verifyNoInteractions(animalColor, animal);
        }

        @Test
        @DisplayName("colores de animal activos")
        void colores_de_animal_activos() {
            especieExiste();
            when(breed.existsActiveBySpecieId(SpecieMother.SPECIE_ID)).thenReturn(false);
            when(animalColor.existsActiveBySpecieId(SpecieMother.SPECIE_ID)).thenReturn(true);

            assertBloqueadoPor("animalColor");
            verifyNoInteractions(animal);
        }

        @Test
        @DisplayName("animales activos — el ultimo puerto que se comprueba")
        void animales_activos() {
            especieExiste();
            when(breed.existsActiveBySpecieId(SpecieMother.SPECIE_ID)).thenReturn(false);
            when(animalColor.existsActiveBySpecieId(SpecieMother.SPECIE_ID)).thenReturn(false);
            when(animal.existsActiveBySpecieId(SpecieMother.SPECIE_ID)).thenReturn(true);

            assertBloqueadoPor("animal");
        }
    }
}
