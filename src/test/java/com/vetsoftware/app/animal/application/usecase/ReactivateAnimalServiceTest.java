package com.vetsoftware.app.animal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateAnimalService")
class ReactivateAnimalServiceTest {

    @Mock
    private AnimalRepository repository;

    @InjectMocks
    private ReactivateAnimalService service;

    @Test
    @DisplayName("reactiva y devuelve el animal ya habilitado")
    void reactiva_y_devuelve_el_animal_ya_habilitado() {
        when(repository.reactivate(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                .thenReturn(Optional.of(AnimalMother.perroSano()));

        AnimalDto dto = service.execute(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(AnimalMother.ANIMAL_ID);
        assertThat(dto.enabled()).isTrue();
        verify(repository).reactivate(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID);
    }

    @Test
    @DisplayName("cero filas afectadas es no-encontrado y evita la lectura posterior")
    void cero_filas_afectadas_es_no_encontrado() {
        // El UPDATE ya lleva el companyId, asi que 0 filas cubre a la vez "no existe"
        // y "es de otra empresa". No hace falta un SELECT previo.
        when(repository.reactivate(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                .isInstanceOf(AnimalNotFoundException.class)
                .hasMessageContaining("Animal not found: " + AnimalMother.ANIMAL_ID);

        verify(repository, never()).findByIdAndCompanyId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("si el animal desaparece entre el UPDATE y el SELECT, falla como no-encontrado")
    void si_el_animal_desaparece_entre_el_update_y_el_select() {
        when(repository.reactivate(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                .isInstanceOf(AnimalNotFoundException.class);
    }
}
