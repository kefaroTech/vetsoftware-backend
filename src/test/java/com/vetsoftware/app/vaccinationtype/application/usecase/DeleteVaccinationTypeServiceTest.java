package com.vetsoftware.app.vaccinationtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationChildrenQueryPort;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeHasActiveChildrenException;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeNotFoundException;
import com.vetsoftware.app.vaccinationtype.testsupport.VaccinationTypeMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteVaccinationTypeService")
class DeleteVaccinationTypeServiceTest {

    @Mock
    private VaccinationTypeRepository repository;
    @Mock
    private VaccinationChildrenQueryPort vaccinationChildrenQueryPort;

    @InjectMocks
    private DeleteVaccinationTypeService service;

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("borra el tipo propio de la empresa cuando no tiene vacunas hijas activas")
        void borra_el_tipo_sin_hijos_activos() {
            when(repository.findOwnedByIdAndCompanyId(VaccinationTypeMother.TYPE_ID,
                    VaccinationTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationTypeMother.propia()));
            when(vaccinationChildrenQueryPort
                    .existsActiveByVaccinationTypeId(VaccinationTypeMother.TYPE_ID))
                    .thenReturn(false);

            service.execute(VaccinationTypeMother.TYPE_ID, VaccinationTypeMother.COMPANY_ID);

            verify(repository).delete(VaccinationTypeMother.TYPE_ID);
        }

        @Test
        @DisplayName("sin empresa (SYSTEM) la lectura previa alcanza el catalogo de plataforma")
        void sin_empresa_la_lectura_alcanza_el_catalogo_de_plataforma() {
            when(repository.findById(VaccinationTypeMother.TYPE_ID))
                    .thenReturn(Optional.of(VaccinationTypeMother.general()));
            when(vaccinationChildrenQueryPort
                    .existsActiveByVaccinationTypeId(VaccinationTypeMother.TYPE_ID))
                    .thenReturn(false);

            service.execute(VaccinationTypeMother.TYPE_ID, null);

            verify(repository).delete(VaccinationTypeMother.TYPE_ID);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("un id inexistente no consulta hijos ni borra")
        void un_id_inexistente_no_consulta_hijos_ni_borra() {
            when(repository.findOwnedByIdAndCompanyId(99L, VaccinationTypeMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(99L, VaccinationTypeMother.COMPANY_ID))
                    .isInstanceOf(VaccinationTypeNotFoundException.class)
                    .hasMessageContaining("VaccinationType not found: 99");

            verifyNoInteractions(vaccinationChildrenQueryPort);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("no borra si tiene vacunas hijas activas")
        void no_borra_si_tiene_vacunas_hijas_activas() {
            when(repository.findOwnedByIdAndCompanyId(VaccinationTypeMother.TYPE_ID,
                    VaccinationTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationTypeMother.propia()));
            when(vaccinationChildrenQueryPort
                    .existsActiveByVaccinationTypeId(VaccinationTypeMother.TYPE_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(VaccinationTypeMother.TYPE_ID,
                    VaccinationTypeMother.COMPANY_ID))
                    .isInstanceOf(VaccinationTypeHasActiveChildrenException.class)
                    .hasMessageContaining("" + VaccinationTypeMother.TYPE_ID)
                    .hasMessageContaining("vaccination");

            verify(repository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("el tipo de OTRA empresa es 404 y no se borra")
        void tipo_de_otra_empresa_es_not_found_y_no_borra() {
            when(repository.findOwnedByIdAndCompanyId(VaccinationTypeMother.TYPE_ID,
                    VaccinationTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(VaccinationTypeMother.TYPE_ID,
                    VaccinationTypeMother.COMPANY_ID))
                    .isInstanceOf(VaccinationTypeNotFoundException.class);

            verifyNoInteractions(vaccinationChildrenQueryPort);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("el camino SYSTEM no alcanza la fila PRIVADA de una empresa: 404 y no borra")
        void el_camino_system_no_alcanza_la_fila_privada() {
            // Este camino era alcanzable desde ANTES de #565: el delete del controller
            // ya usaba currentCompanyIdOrNull(). Sin el .filter(VaccinationType::isGeneral)
            // un DELETE de plataforma con el id de una fila PRIVADA la daba de baja: 204,
            // sin error, y la clinica dejaba de verla por el @SQLRestriction. Mas
            // silencioso que la expropiacion del update, donde la fila al menos reaparecia
            // en el catalogo global.
            when(repository.findById(VaccinationTypeMother.TYPE_ID))
                    .thenReturn(Optional.of(VaccinationTypeMother.propia()));

            assertThatThrownBy(() -> service.execute(VaccinationTypeMother.TYPE_ID, null))
                    .isInstanceOf(VaccinationTypeNotFoundException.class);

            // La barrera actua ANTES de mirar hijos activos: si no, un tipo privado sin
            // vacunas colgando saldria por el 404 igualmente, pero uno CON hijos daria un
            // 409 que revela que la fila existe y que esta en uso.
            verifyNoInteractions(vaccinationChildrenQueryPort);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("la fila general compartida tampoco se borra desde una empresa")
        void la_fila_general_no_se_borra_desde_una_empresa() {
            // El finder de ESCRITURA excluye las generales: borrarla la ocultaria a todos
            // los tenants. Sigue siendo legible (ver FindVaccinationTypeServiceTest).
            when(repository.findOwnedByIdAndCompanyId(51L, VaccinationTypeMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(51L, VaccinationTypeMother.COMPANY_ID))
                    .isInstanceOf(VaccinationTypeNotFoundException.class);

            verify(repository, never()).delete(any());
        }
    }
}
