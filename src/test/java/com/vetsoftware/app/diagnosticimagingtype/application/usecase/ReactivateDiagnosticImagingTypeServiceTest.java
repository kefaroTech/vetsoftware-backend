package com.vetsoftware.app.diagnosticimagingtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
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
@DisplayName("ReactivateDiagnosticImagingTypeService")
class ReactivateDiagnosticImagingTypeServiceTest {

    @Mock
    private DiagnosticImagingTypeRepository repository;

    @InjectMocks
    private ReactivateDiagnosticImagingTypeService service;

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("reactiva el tipo y devuelve su DTO recien leido")
        void reactiva_el_tipo_y_devuelve_su_dto() {
            when(repository.reactivate(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID)).thenReturn(1);
            when(repository.findOwnedByIdAndCompanyId(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.propiaDeEmpresa()));

            DiagnosticImagingTypeDto dto = service.execute(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(DiagnosticImagingTypeMother.TYPE_ID);
            verify(repository).reactivate(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("id inexistente (0 filas afectadas): lanza not found sin volver a leer")
        void id_inexistente_lanza_not_found() {
            when(repository.reactivate(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID))
                    .isInstanceOf(DiagnosticImagingTypeNotFoundException.class)
                    .hasMessageContaining("DiagnosticImagingType not found: "
                            + DiagnosticImagingTypeMother.TYPE_ID);
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("el tipo de OTRA empresa es 404 y no se reactiva")
        void tipo_de_otra_empresa_es_not_found_y_no_escribe() {
            // El company_id viaja dentro del UPDATE: es la unica barrera, porque aqui no
            // hay lectura previa que valide la propiedad.
            when(repository.reactivate(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID))
                    .isInstanceOf(DiagnosticImagingTypeNotFoundException.class);

            verify(repository, org.mockito.Mockito.never()).findOwnedByIdAndCompanyId(
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
            verify(repository, org.mockito.Mockito.never())
                    .findById(org.mockito.ArgumentMatchers.any());
            verify(repository, org.mockito.Mockito.never())
                    .save(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("la fila general compartida tampoco se reactiva desde una empresa")
        void la_fila_general_no_se_reactiva_desde_una_empresa() {
            // 502L es una fila general (company_id NULL): reactivarla la devolveria a
            // todos los tenants, asi que el UPDATE acotado la deja fuera.
            when(repository.reactivate(502L, DiagnosticImagingTypeMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(502L, DiagnosticImagingTypeMother.COMPANY_ID))
                    .isInstanceOf(DiagnosticImagingTypeNotFoundException.class);

            verify(repository, org.mockito.Mockito.never())
                    .save(org.mockito.ArgumentMatchers.any());
        }
    }
}
