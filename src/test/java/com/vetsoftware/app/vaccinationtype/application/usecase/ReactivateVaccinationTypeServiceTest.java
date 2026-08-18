package com.vetsoftware.app.vaccinationtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
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
@DisplayName("ReactivateVaccinationTypeService")
class ReactivateVaccinationTypeServiceTest {

    @Mock
    private VaccinationTypeRepository repository;

    @InjectMocks
    private ReactivateVaccinationTypeService service;

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve el tipo releido")
        void reactiva_y_devuelve_el_tipo_releido() {
            when(repository.reactivate(VaccinationTypeMother.TYPE_ID,
                    VaccinationTypeMother.COMPANY_ID)).thenReturn(1);
            when(repository.findOwnedByIdAndCompanyId(VaccinationTypeMother.TYPE_ID,
                    VaccinationTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationTypeMother.propia()));

            VaccinationTypeDto dto = service.execute(VaccinationTypeMother.TYPE_ID,
                    VaccinationTypeMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(VaccinationTypeMother.TYPE_ID);
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("ninguna fila afectada no vuelve a leer y lanza no encontrado")
        void ninguna_fila_afectada_no_vuelve_a_leer() {
            when(repository.reactivate(VaccinationTypeMother.TYPE_ID,
                    VaccinationTypeMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(VaccinationTypeMother.TYPE_ID,
                    VaccinationTypeMother.COMPANY_ID))
                    .isInstanceOf(VaccinationTypeNotFoundException.class).hasMessageContaining(
                            "VaccinationType not found: " + VaccinationTypeMother.TYPE_ID);

            verify(repository, never()).findOwnedByIdAndCompanyId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("una fila reactivada pero ilocalizable tambien lanza no encontrado")
        void una_fila_reactivada_pero_ilocalizable_tambien_lanza_no_encontrado() {
            when(repository.reactivate(VaccinationTypeMother.TYPE_ID,
                    VaccinationTypeMother.COMPANY_ID)).thenReturn(1);
            when(repository.findOwnedByIdAndCompanyId(VaccinationTypeMother.TYPE_ID,
                    VaccinationTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(VaccinationTypeMother.TYPE_ID,
                    VaccinationTypeMother.COMPANY_ID))
                    .isInstanceOf(VaccinationTypeNotFoundException.class).hasMessageContaining(
                            "VaccinationType not found: " + VaccinationTypeMother.TYPE_ID);
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
            when(repository.reactivate(VaccinationTypeMother.TYPE_ID,
                    VaccinationTypeMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(VaccinationTypeMother.TYPE_ID,
                    VaccinationTypeMother.COMPANY_ID))
                    .isInstanceOf(VaccinationTypeNotFoundException.class);

            verify(repository, never()).findOwnedByIdAndCompanyId(anyLong(), anyLong());
            verify(repository, never()).findById(anyLong());
            verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("la fila general compartida tampoco se reactiva desde una empresa")
        void la_fila_general_no_se_reactiva_desde_una_empresa() {
            // 51L es una fila general (company_id NULL): reactivarla la devolveria a todos
            // los tenants, asi que el UPDATE acotado la deja fuera.
            when(repository.reactivate(51L, VaccinationTypeMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(51L, VaccinationTypeMother.COMPANY_ID))
                    .isInstanceOf(VaccinationTypeNotFoundException.class);

            verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        }
    }
}
