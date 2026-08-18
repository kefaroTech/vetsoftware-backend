package com.vetsoftware.app.laboratorytesttype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestTypeRepository;
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
@DisplayName("ReactivateLaboratoryTestTypeService")
class ReactivateLaboratoryTestTypeServiceTest {

    @Mock
    private LaboratoryTestTypeRepository repository;

    @InjectMocks
    private ReactivateLaboratoryTestTypeService service;

    @Nested
    @DisplayName("reactivacion permitida")
    class ReactivacionPermitida {

        @Test
        @DisplayName("reactiva y devuelve el tipo releido")
        void reactiva_y_devuelve_el_tipo_releido() {
            when(repository.reactivate(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID)).thenReturn(1);
            when(repository.findOwnedByIdAndCompanyId(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(LaboratoryTestTypeMother.propioDeEmpresa()));

            LaboratoryTestTypeDto dto = service.execute(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(LaboratoryTestTypeMother.TYPE_ID);
        }
    }

    @Nested
    @DisplayName("tipo inexistente")
    class TipoInexistente {

        @Test
        @DisplayName("cero filas afectadas no relee el tipo y lanza NotFound")
        void cero_filas_afectadas_lanza_not_found() {
            when(repository.reactivate(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID))
                    .isInstanceOf(LaboratoryTestTypeNotFoundException.class).hasMessageContaining(
                            "LaboratoryTestType not found: " + LaboratoryTestTypeMother.TYPE_ID);

            verify(repository, never()).findOwnedByIdAndCompanyId(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID);
        }

        @Test
        @DisplayName("una fila afectada pero sin relectura posterior tambien lanza NotFound")
        void fila_afectada_sin_relectura_lanza_not_found() {
            when(repository.reactivate(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID)).thenReturn(1);
            when(repository.findOwnedByIdAndCompanyId(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID))
                    .isInstanceOf(LaboratoryTestTypeNotFoundException.class).hasMessageContaining(
                            "LaboratoryTestType not found: " + LaboratoryTestTypeMother.TYPE_ID);
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
            when(repository.reactivate(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(LaboratoryTestTypeMother.TYPE_ID,
                    LaboratoryTestTypeMother.COMPANY_ID))
                    .isInstanceOf(LaboratoryTestTypeNotFoundException.class);

            verify(repository, never()).findOwnedByIdAndCompanyId(
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
            verify(repository, never()).findById(org.mockito.ArgumentMatchers.any());
            verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("la fila general compartida tampoco se reactiva desde una empresa")
        void la_fila_general_no_se_reactiva_desde_una_empresa() {
            // 71L es una fila general (company_id NULL): reactivarla la devolveria a todos
            // los tenants, asi que el UPDATE acotado la deja fuera.
            when(repository.reactivate(71L, LaboratoryTestTypeMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(71L, LaboratoryTestTypeMother.COMPANY_ID))
                    .isInstanceOf(LaboratoryTestTypeNotFoundException.class);

            verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        }
    }
}
