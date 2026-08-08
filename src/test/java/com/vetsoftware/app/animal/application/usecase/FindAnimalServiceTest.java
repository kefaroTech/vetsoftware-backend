package com.vetsoftware.app.animal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindAnimalService")
class FindAnimalServiceTest {

    @Mock
    private AnimalRepository repository;

    @InjectMocks
    private FindAnimalService service;

    @Test
    @DisplayName("devuelve el DTO del animal de la empresa")
    void devuelve_el_dto_del_animal_de_la_empresa() {
        when(repository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                .thenReturn(Optional.of(AnimalMother.perroSano()));

        AnimalDto dto = service.findById(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(AnimalMother.ANIMAL_ID);
        assertThat(dto.name()).isEqualTo("Firulais");
    }

    @Test
    @DisplayName("propaga el peso derivado que trae el repositorio")
    void propaga_el_peso_derivado_que_trae_el_repositorio() {
        when(repository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                .thenReturn(Optional.of(AnimalMother.conPesoDerivado(new BigDecimal("12.50"),
                        com.vetsoftware.app.animal.domain.WeightType.KILOGRAMS,
                        LocalDate.of(2026, 2, 1))));

        AnimalDto dto = service.findById(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID);

        assertThat(dto.weight()).isEqualByComparingTo("12.50");
        assertThat(dto.weightMeasuredAt()).isEqualTo(LocalDate.of(2026, 2, 1));
    }

    @Test
    @DisplayName("un animal de otra empresa se comporta como inexistente")
    void un_animal_de_otra_empresa_se_comporta_como_inexistente() {
        when(repository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID, 999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(AnimalMother.ANIMAL_ID, 999L))
                .isInstanceOf(AnimalNotFoundException.class)
                .hasMessageContaining("Animal not found: " + AnimalMother.ANIMAL_ID);

        // Nunca se usa findById(id) a secas: eso devolveria animales de otro tenant.
        verify(repository).findByIdAndCompanyId(AnimalMother.ANIMAL_ID, 999L);
    }
}
