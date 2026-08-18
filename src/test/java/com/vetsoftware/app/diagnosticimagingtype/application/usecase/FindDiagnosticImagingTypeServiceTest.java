package com.vetsoftware.app.diagnosticimagingtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingTypeRepository;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeNotFoundException;
import com.vetsoftware.app.diagnosticimagingtype.testsupport.DiagnosticImagingTypeMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindDiagnosticImagingTypeService")
class FindDiagnosticImagingTypeServiceTest {

    @Mock
    private DiagnosticImagingTypeRepository repository;

    @InjectMocks
    private FindDiagnosticImagingTypeService service;

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("devuelve el DTO del tipo encontrado en la empresa")
        void devuelve_el_dto_del_tipo_encontrado() {
            when(repository.findByIdAndCompanyId(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.propiaDeEmpresa()));

            DiagnosticImagingTypeDto dto = service.findById(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(DiagnosticImagingTypeMother.TYPE_ID);
        }

        @Test
        @DisplayName("la fila general sigue siendo accesible desde cualquier empresa")
        void la_fila_general_sigue_siendo_accesible() {
            // Acotar por empresa los caminos de ESCRITURA no toco la lectura: el finder
            // de disponibles sigue devolviendo las filas generales.
            when(repository.findByIdAndCompanyId(502L, DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.general()));

            DiagnosticImagingTypeDto dto = service.findById(502L,
                    DiagnosticImagingTypeMother.COMPANY_ID);

            assertThat(dto.general()).isTrue();
            assertThat(dto.company()).isNull();
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("tipo inexistente o de otra empresa: lanza not found")
        void tipo_inexistente_o_de_otra_empresa_lanza_not_found() {
            when(repository.findByIdAndCompanyId(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID))
                    .isInstanceOf(DiagnosticImagingTypeNotFoundException.class)
                    .hasMessageContaining("DiagnosticImagingType not found: "
                            + DiagnosticImagingTypeMother.TYPE_ID);
        }
    }
}
