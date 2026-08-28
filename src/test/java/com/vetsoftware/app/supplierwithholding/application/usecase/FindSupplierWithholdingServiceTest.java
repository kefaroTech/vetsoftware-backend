package com.vetsoftware.app.supplierwithholding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.supplierwithholding.application.dto.SupplierWithholdingDto;
import com.vetsoftware.app.supplierwithholding.application.port.out.SupplierWithholdingRepository;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholding;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholdingNotFoundException;
import com.vetsoftware.app.supplierwithholding.testsupport.SupplierWithholdingMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindSupplierWithholdingService")
class FindSupplierWithholdingServiceTest {

    private static final Long ID = 220L;

    @Mock
    private SupplierWithholdingRepository repository;

    @InjectMocks
    private FindSupplierWithholdingService service;

    @Nested
    @DisplayName("busqueda por id")
    class Busqueda {

        @Test
        @DisplayName("una retencion existente se traduce a dto")
        void una_retencion_existente_se_traduce_a_dto() {
            SupplierWithholding encontrada = SupplierWithholdingMother.conId(ID,
                    SupplierWithholdingMother.ica());
            when(repository.findById(ID)).thenReturn(Optional.of(encontrada));

            SupplierWithholdingDto dto = service.findById(ID);

            assertThat(dto.id()).isEqualTo(ID);
            assertThat(dto.withholdingType()).isEqualTo(encontrada.getWithholdingType());
            assertThat(dto.municipalityCode()).isEqualTo(encontrada.getMunicipalityCode());
        }

        @Test
        @DisplayName("retencion inexistente lanza la excepcion de dominio")
        void retencion_inexistente_lanza_la_excepcion_de_dominio() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(ID))
                    .isInstanceOf(SupplierWithholdingNotFoundException.class)
                    .hasMessageContaining("Supplier withholding not found: " + ID);
        }
    }
}
