package com.vetsoftware.app.diagnosticimagingtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingChildrenQueryPort;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingTypeRepository;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeHasActiveChildrenException;
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
@DisplayName("DeleteDiagnosticImagingTypeService")
class DeleteDiagnosticImagingTypeServiceTest {

    @Mock
    private DiagnosticImagingTypeRepository repository;
    @Mock
    private DiagnosticImagingChildrenQueryPort diagnosticImagingChildrenQueryPort;

    @InjectMocks
    private DeleteDiagnosticImagingTypeService service;

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("borra el tipo propio de la empresa cuando no tiene imagenes activas")
        void borra_el_tipo_sin_hijos_activos() {
            when(repository.findOwnedByIdAndCompanyId(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.propiaDeEmpresa()));
            when(diagnosticImagingChildrenQueryPort
                    .existsActiveByDiagnosticImagingTypeId(DiagnosticImagingTypeMother.TYPE_ID))
                    .thenReturn(false);

            service.execute(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID);

            verify(repository).delete(DiagnosticImagingTypeMother.TYPE_ID);
        }

        @Test
        @DisplayName("sin empresa (SYSTEM) la lectura previa se acota al catalogo de plataforma y alcanza a las generales")
        void sin_empresa_la_lectura_se_acota_al_catalogo_de_plataforma() {
            when(repository.findById(DiagnosticImagingTypeMother.TYPE_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.general()));
            when(diagnosticImagingChildrenQueryPort
                    .existsActiveByDiagnosticImagingTypeId(DiagnosticImagingTypeMother.TYPE_ID))
                    .thenReturn(false);

            service.execute(DiagnosticImagingTypeMother.TYPE_ID, null);

            verify(repository).delete(DiagnosticImagingTypeMother.TYPE_ID);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("tipo inexistente: no consulta hijos activos ni borra")
        void tipo_inexistente_no_borra() {
            when(repository.findOwnedByIdAndCompanyId(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID))
                    .isInstanceOf(DiagnosticImagingTypeNotFoundException.class)
                    .hasMessageContaining("DiagnosticImagingType not found: "
                            + DiagnosticImagingTypeMother.TYPE_ID);

            verify(diagnosticImagingChildrenQueryPort, never())
                    .existsActiveByDiagnosticImagingTypeId(org.mockito.ArgumentMatchers.any());
            verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("con imagenes activas asociadas: no borra")
        void con_hijos_activos_no_borra() {
            when(repository.findOwnedByIdAndCompanyId(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.propiaDeEmpresa()));
            when(diagnosticImagingChildrenQueryPort
                    .existsActiveByDiagnosticImagingTypeId(DiagnosticImagingTypeMother.TYPE_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID))
                    .isInstanceOf(DiagnosticImagingTypeHasActiveChildrenException.class)
                    .hasMessageContaining(String.valueOf(DiagnosticImagingTypeMother.TYPE_ID));

            verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("el tipo de OTRA empresa es 404 y no se borra")
        void tipo_de_otra_empresa_es_not_found_y_no_borra() {
            when(repository.findOwnedByIdAndCompanyId(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DiagnosticImagingTypeMother.TYPE_ID,
                    DiagnosticImagingTypeMother.COMPANY_ID))
                    .isInstanceOf(DiagnosticImagingTypeNotFoundException.class);

            verify(diagnosticImagingChildrenQueryPort, never())
                    .existsActiveByDiagnosticImagingTypeId(org.mockito.ArgumentMatchers.any());
            verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
            verify(repository, never()).findById(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("la fila general compartida tampoco se borra desde una empresa")
        void la_fila_general_no_se_borra_desde_una_empresa() {
            // El finder de ESCRITURA excluye las generales: borrarla la ocultaria a todos
            // los tenants. Sigue siendo legible (ver FindDiagnosticImagingTypeServiceTest).
            when(repository.findOwnedByIdAndCompanyId(502L, DiagnosticImagingTypeMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(502L, DiagnosticImagingTypeMother.COMPANY_ID))
                    .isInstanceOf(DiagnosticImagingTypeNotFoundException.class);

            verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("el camino SYSTEM no alcanza una fila PRIVADA: es 404 y no la borra")
        void el_camino_system_no_alcanza_una_fila_privada() {
            // La direccion contraria del caso de arriba, y la mas silenciosa de las dos:
            // sin el .filter(isGeneral) un DELETE de plataforma con el id de una fila
            // PRIVADA la cargaba y la daba de baja. 204, sin error, y la clinica dejaba
            // de verla por el @SQLRestriction. Esta rama NO la abrio #565: el delete de
            // este controller usaba currentCompanyIdOrNull() desde el principio, asi que
            // llevaba expuesta mas tiempo que la del update.
            when(repository.findById(DiagnosticImagingTypeMother.TYPE_ID))
                    .thenReturn(Optional.of(DiagnosticImagingTypeMother.propiaDeEmpresa()));

            assertThatThrownBy(() -> service.execute(DiagnosticImagingTypeMother.TYPE_ID, null))
                    .isInstanceOf(DiagnosticImagingTypeNotFoundException.class)
                    .hasMessageContaining("DiagnosticImagingType not found: "
                            + DiagnosticImagingTypeMother.TYPE_ID);

            // La barrera actua ANTES de preguntar por hijos activos: si el orden se
            // invirtiera, un tipo privado SIN hijos llegaria igualmente al delete.
            verify(diagnosticImagingChildrenQueryPort, never())
                    .existsActiveByDiagnosticImagingTypeId(org.mockito.ArgumentMatchers.any());
            verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
        }
    }
}
