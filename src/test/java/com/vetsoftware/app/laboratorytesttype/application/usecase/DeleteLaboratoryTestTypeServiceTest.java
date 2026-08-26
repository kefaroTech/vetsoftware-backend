package com.vetsoftware.app.laboratorytesttype.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestChildrenQueryPort;
import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestTypeRepository;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeHasActiveChildrenException;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeNotFoundException;
import com.vetsoftware.app.laboratorytesttype.testsupport.LaboratoryTestTypeMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteLaboratoryTestTypeService")
class DeleteLaboratoryTestTypeServiceTest {

    @Mock
    private LaboratoryTestTypeRepository repository;
    @Mock
    private LaboratoryTestChildrenQueryPort laboratoryTestChildrenQueryPort;

    @InjectMocks
    private DeleteLaboratoryTestTypeService service;

    @Nested
    @DisplayName("borrado permitido")
    class BorradoPermitido {

        @Test
        @DisplayName("sin exams de laboratorio activos borra el tipo propio de la empresa")
        void sin_examenes_activos_borra_el_tipo() {
            when(repository.findOwnedByIdAndCompanyId(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.propioDeEmpresa()));
            when(laboratoryTestChildrenQueryPort
                    .existsActiveByLaboratoryTestTypeId(LaboratoryTestTypeMother.TYPE_ID))
                    .thenReturn(false);

            service.execute(LaboratoryTestTypeMother.TYPE_ID, LaboratoryTestTypeMother.COMPANY_ID);

            verify(repository).delete(LaboratoryTestTypeMother.TYPE_ID);
        }

        @Test
        @DisplayName("sin empresa (SYSTEM) la lectura previa se acota al catalogo de plataforma y alcanza a las generales")
        void sin_empresa_la_lectura_se_acota_al_catalogo_de_plataforma() {
            when(repository.findById(LaboratoryTestTypeMother.TYPE_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.general()));
            when(laboratoryTestChildrenQueryPort
                    .existsActiveByLaboratoryTestTypeId(LaboratoryTestTypeMother.TYPE_ID))
                    .thenReturn(false);

            service.execute(LaboratoryTestTypeMother.TYPE_ID, null);

            verify(repository).delete(LaboratoryTestTypeMother.TYPE_ID);
        }
    }

    @Nested
    @DisplayName("tipo inexistente")
    class TipoInexistente {

        @Test
        @DisplayName("no consulta hijos ni borra si el tipo no existe")
        void no_consulta_hijos_ni_borra_si_no_existe() {
            when(repository.findOwnedByIdAndCompanyId(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID))
                    .isInstanceOf(LaboratoryTestTypeNotFoundException.class).hasMessageContaining(
                            "LaboratoryTestType not found: " + LaboratoryTestTypeMother.TYPE_ID);

            verifyNoInteractions(laboratoryTestChildrenQueryPort);
            verify(repository, never()).delete(LaboratoryTestTypeMother.TYPE_ID);
        }
    }

    @Nested
    @DisplayName("bloqueo por hijos activos")
    class BloqueoPorHijosActivos {

        @Test
        @DisplayName("con exams de laboratorio activos no borra y propaga el tipo de hijo")
        void con_examenes_activos_no_borra() {
            when(repository.findOwnedByIdAndCompanyId(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.propioDeEmpresa()));
            when(laboratoryTestChildrenQueryPort
                    .existsActiveByLaboratoryTestTypeId(LaboratoryTestTypeMother.TYPE_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID))
                    .isInstanceOf(LaboratoryTestTypeHasActiveChildrenException.class)
                    .hasMessageContaining(
                            "Cannot delete laboratorytesttype " + LaboratoryTestTypeMother.TYPE_ID)
                    .hasMessageContaining("has active laboratoryTest children");

            verify(repository, never()).delete(LaboratoryTestTypeMother.TYPE_ID);
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("el tipo de OTRA empresa es 404 y no se borra")
        void tipo_de_otra_empresa_es_not_found_y_no_borra() {
            when(repository.findOwnedByIdAndCompanyId(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID))
                    .isInstanceOf(LaboratoryTestTypeNotFoundException.class);

            verifyNoInteractions(laboratoryTestChildrenQueryPort);
            verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
            verify(repository, never()).findById(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("la fila general compartida tampoco se borra desde una empresa")
        void la_fila_general_no_se_borra_desde_una_empresa() {
            // El finder de ESCRITURA excluye las generales: borrarla la ocultaria a todos
            // los tenants. Sigue siendo legible por el finder de disponibles.
            when(repository.findOwnedByIdAndCompanyId(71L, LaboratoryTestTypeMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(71L, LaboratoryTestTypeMother.COMPANY_ID))
                    .isInstanceOf(LaboratoryTestTypeNotFoundException.class);

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
            when(repository.findById(LaboratoryTestTypeMother.TYPE_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.propioDeEmpresa()));

            assertThatThrownBy(() -> service.execute(LaboratoryTestTypeMother.TYPE_ID, null))
                    .isInstanceOf(LaboratoryTestTypeNotFoundException.class).hasMessageContaining(
                            "LaboratoryTestType not found: " + LaboratoryTestTypeMother.TYPE_ID);

            // La barrera actua ANTES de preguntar por hijos activos: si el orden se
            // invirtiera, un tipo privado SIN hijos llegaria igualmente al delete.
            verifyNoInteractions(laboratoryTestChildrenQueryPort);
            verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
        }
    }
}
