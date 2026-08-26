package com.vetsoftware.app.surgerytype.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.surgerytype.application.port.out.SurgeryChildrenQueryPort;
import com.vetsoftware.app.surgerytype.application.port.out.SurgeryTypeRepository;
import com.vetsoftware.app.surgerytype.domain.SurgeryTypeHasActiveChildrenException;
import com.vetsoftware.app.surgerytype.domain.SurgeryTypeNotFoundException;
import com.vetsoftware.app.surgerytype.testsupport.SurgeryTypeMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteSurgeryTypeService")
class DeleteSurgeryTypeServiceTest {

    @Mock
    private SurgeryTypeRepository repository;
    @Mock
    private SurgeryChildrenQueryPort surgeryChildrenQueryPort;

    @InjectMocks
    private DeleteSurgeryTypeService service;

    private void tipoExisteEnLaEmpresa() {
        when(repository.findOwnedByIdAndCompanyId(SurgeryTypeMother.SURGERY_TYPE_ID,
                SurgeryTypeMother.COMPANY_ID))
                .thenReturn(Optional.of(SurgeryTypeMother.propioDeEmpresa()));
    }

    @Nested
    @DisplayName("Eliminacion")
    class Eliminacion {

        @Test
        @DisplayName("borra el tipo propio de la empresa sin cirugias activas")
        void borra_el_tipo_sin_cirugias_activas() {
            tipoExisteEnLaEmpresa();
            when(surgeryChildrenQueryPort
                    .existsActiveBySurgeryTypeId(SurgeryTypeMother.SURGERY_TYPE_ID))
                    .thenReturn(false);

            service.execute(SurgeryTypeMother.SURGERY_TYPE_ID, SurgeryTypeMother.COMPANY_ID);

            verify(repository).delete(SurgeryTypeMother.SURGERY_TYPE_ID);
        }

        @Test
        @DisplayName("sin empresa (SYSTEM) la lectura previa alcanza el catalogo de plataforma")
        void sin_empresa_la_lectura_alcanza_el_catalogo_de_plataforma() {
            when(repository.findById(SurgeryTypeMother.GENERAL_SURGERY_TYPE_ID))
                    .thenReturn(Optional.of(SurgeryTypeMother.general()));
            when(surgeryChildrenQueryPort
                    .existsActiveBySurgeryTypeId(SurgeryTypeMother.GENERAL_SURGERY_TYPE_ID))
                    .thenReturn(false);

            service.execute(SurgeryTypeMother.GENERAL_SURGERY_TYPE_ID, null);

            verify(repository).delete(SurgeryTypeMother.GENERAL_SURGERY_TYPE_ID);
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("no borra un tipo que no existe")
        void no_borra_un_tipo_inexistente() {
            when(repository.findOwnedByIdAndCompanyId(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).isInstanceOf(SurgeryTypeNotFoundException.class)
                    .hasMessageContaining(
                            "SurgeryType not found: " + SurgeryTypeMother.SURGERY_TYPE_ID);

            // El chequeo de hijos activos jamas se consulta si el tipo ni siquiera
            // existe: es la mitad del valor de este test.
            verifyNoInteractions(surgeryChildrenQueryPort);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("no borra un tipo con cirugias activas")
        void no_borra_un_tipo_con_cirugias_activas() {
            tipoExisteEnLaEmpresa();
            when(surgeryChildrenQueryPort
                    .existsActiveBySurgeryTypeId(SurgeryTypeMother.SURGERY_TYPE_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID))
                    .isInstanceOf(SurgeryTypeHasActiveChildrenException.class)
                    .hasMessageContaining(
                            "Cannot delete surgerytype " + SurgeryTypeMother.SURGERY_TYPE_ID)
                    .hasMessageContaining("has active surgery children");

            verify(repository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("el tipo de OTRA empresa es 404 y no se borra")
        void tipo_de_otra_empresa_es_not_found_y_no_borra() {
            when(repository.findOwnedByIdAndCompanyId(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).isInstanceOf(SurgeryTypeNotFoundException.class);

            verifyNoInteractions(surgeryChildrenQueryPort);
            verify(repository, never()).delete(any());
            verify(repository, never()).findById(any());
        }

        @Test
        @DisplayName("el camino SYSTEM no alcanza la fila PRIVADA de una empresa: 404 y no borra")
        void el_camino_system_no_alcanza_la_fila_privada() {
            // Este camino era alcanzable desde ANTES de #565: el delete del controller
            // ya usaba currentCompanyIdOrNull(). Sin el .filter(SurgeryType::isGeneral)
            // un DELETE de plataforma con el id de una fila PRIVADA la daba de baja: 204,
            // sin error, y la clinica dejaba de verla por el @SQLRestriction. Mas
            // silencioso que la expropiacion del update, donde la fila al menos reaparecia
            // en el catalogo global.
            when(repository.findById(SurgeryTypeMother.SURGERY_TYPE_ID))
                    .thenReturn(Optional.of(SurgeryTypeMother.propioDeEmpresa()));

            assertThatThrownBy(() -> service.execute(SurgeryTypeMother.SURGERY_TYPE_ID, null))
                    .isInstanceOf(SurgeryTypeNotFoundException.class);

            // La barrera actua ANTES de mirar hijos activos: si no, un tipo privado sin
            // cirugias colgando saldria por el 404 igualmente, pero uno CON hijos daria un
            // 409 que revela que la fila existe y que esta en uso.
            verifyNoInteractions(surgeryChildrenQueryPort);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("la fila general compartida tampoco se borra desde una empresa")
        void la_fila_general_no_se_borra_desde_una_empresa() {
            // El finder de ESCRITURA excluye las generales: borrarla la ocultaria a todos
            // los tenants. Sigue siendo legible por el finder de disponibles (ver
            // FindSurgeryTypeServiceTest).
            when(repository.findOwnedByIdAndCompanyId(SurgeryTypeMother.GENERAL_SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SurgeryTypeMother.GENERAL_SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).isInstanceOf(SurgeryTypeNotFoundException.class);

            verify(repository, never()).delete(any());
        }
    }
}
