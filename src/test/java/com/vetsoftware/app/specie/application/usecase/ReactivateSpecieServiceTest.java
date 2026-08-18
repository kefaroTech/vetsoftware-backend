package com.vetsoftware.app.specie.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.specie.application.dto.SpecieDto;
import com.vetsoftware.app.specie.application.port.out.SpecieRepository;
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
 * Igual que en {@link UpdateSpecieServiceTest}: {@code Specie} es un catalogo
 * global sin {@code companyId}, asi que no hay escenario de tenant ajeno que
 * escribir aqui.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateSpecieService")
class ReactivateSpecieServiceTest {

    @Mock
    private SpecieRepository repository;
    @InjectMocks
    private ReactivateSpecieService service;

    @Nested
    @DisplayName("reactivacion permitida")
    class ReactivacionPermitida {

        @Test
        @DisplayName("reactiva y devuelve el dto ya habilitado")
        void reactiva_y_devuelve_el_dto_ya_habilitado() {
            when(repository.reactivate(SpecieMother.SPECIE_ID)).thenReturn(1);
            when(repository.findById(SpecieMother.SPECIE_ID))
                    .thenReturn(Optional.of(SpecieMother.perro()));

            SpecieDto dto = service.execute(SpecieMother.SPECIE_ID);

            assertThat(dto.enabled()).isTrue();
            assertThat(dto.id()).isEqualTo(SpecieMother.SPECIE_ID);
        }
    }

    @Nested
    @DisplayName("especie inexistente")
    class EspecieInexistente {

        @Test
        @DisplayName("cero filas afectadas lanza SpecieNotFoundException sin volver a consultar")
        void cero_filas_lanza_excepcion() {
            when(repository.reactivate(999L)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(999L))
                    .isInstanceOf(SpecieNotFoundException.class)
                    .hasMessageContaining("Specie not found: 999");

            verify(repository, never()).findById(999L);
        }
    }
}
