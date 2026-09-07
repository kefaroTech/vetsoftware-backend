package com.vetsoftware.app.animal.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteAnimalService")
class DeleteAnimalServiceTest {

    @Mock
    private AnimalRepository repository;

    @InjectMocks
    private DeleteAnimalService service;

    @Nested
    @DisplayName("animal existente")
    class AnimalExistente {

        @Test
        @DisplayName("borra el animal de la empresa del comando")
        void borra_el_animal_de_la_empresa_del_comando() {
            when(repository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                    .thenReturn(Optional.of(AnimalMother.perroSano()));

            service.execute(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID);

            verify(repository).delete(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("animal inexistente")
    class AnimalInexistente {

        @Test
        @DisplayName("animal inexistente o de otra empresa: no borra nada")
        void animal_inexistente_o_de_otra_empresa_no_borra_nada() {
            when(repository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                    .isInstanceOf(AnimalNotFoundException.class)
                    .hasMessageContaining("Animal not found: " + AnimalMother.ANIMAL_ID);

            verify(repository, never()).delete(any(), any());
        }
    }
}
