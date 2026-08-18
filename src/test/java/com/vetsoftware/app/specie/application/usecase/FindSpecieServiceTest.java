package com.vetsoftware.app.specie.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.specie.application.dto.SpecieDto;
import com.vetsoftware.app.specie.application.port.out.SpecieRepository;
import com.vetsoftware.app.specie.domain.SpecieNotFoundException;
import com.vetsoftware.app.specie.testsupport.SpecieMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindSpecieService")
class FindSpecieServiceTest {

    @Mock
    private SpecieRepository repository;
    @InjectMocks
    private FindSpecieService service;

    @Test
    @DisplayName("devuelve el dto de la especie encontrada")
    void devuelve_el_dto_de_la_especie_encontrada() {
        when(repository.findById(SpecieMother.SPECIE_ID))
                .thenReturn(Optional.of(SpecieMother.perro()));

        SpecieDto dto = service.findById(SpecieMother.SPECIE_ID);

        assertThat(dto.id()).isEqualTo(SpecieMother.SPECIE_ID);
        assertThat(dto.name()).isEqualTo("Perro");
    }

    @Test
    @DisplayName("una especie inexistente lanza SpecieNotFoundException")
    void una_especie_inexistente_lanza_excepcion() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L)).isInstanceOf(SpecieNotFoundException.class)
                .hasMessageContaining("Specie not found: 999");
    }
}
