package com.vetsoftware.app.taxreturn.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import com.vetsoftware.app.taxreturn.application.port.out.TaxReturnRepository;
import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import com.vetsoftware.app.taxreturn.domain.TaxReturnNotFoundException;
import com.vetsoftware.app.taxreturn.testsupport.TaxReturnMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindTaxReturnService")
class FindTaxReturnServiceTest {

    private static final Long ID = 84L;

    @Mock
    private TaxReturnRepository repository;

    @InjectMocks
    private FindTaxReturnService service;

    @Nested
    @DisplayName("busqueda por id")
    class Busqueda {

        @Test
        @DisplayName("una declaracion existente se traduce a dto")
        void una_declaracion_existente_se_traduce_a_dto() {
            TaxReturn encontrada = TaxReturnMother.conId(ID, TaxReturnMother.borradorDeIca());
            when(repository.findById(ID)).thenReturn(Optional.of(encontrada));

            TaxReturnDto dto = service.findById(ID);

            assertThat(dto.id()).isEqualTo(ID);
            assertThat(dto.taxKind()).isEqualTo(encontrada.getTaxKind());
            assertThat(dto.municipalityCode()).isEqualTo(encontrada.getMunicipalityCode());
        }

        @Test
        @DisplayName("declaracion inexistente lanza la excepcion de dominio")
        void declaracion_inexistente_lanza_la_excepcion_de_dominio() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(ID))
                    .isInstanceOf(TaxReturnNotFoundException.class)
                    .hasMessageContaining("Tax return not found: " + ID);
        }
    }
}
