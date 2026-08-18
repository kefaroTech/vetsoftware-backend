package com.vetsoftware.app.surgerytype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import com.vetsoftware.app.surgerytype.application.port.out.SurgeryTypeRepository;
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
@DisplayName("ReactivateSurgeryTypeService")
class ReactivateSurgeryTypeServiceTest {

    @Mock
    private SurgeryTypeRepository repository;

    @InjectMocks
    private ReactivateSurgeryTypeService service;

    @Nested
    @DisplayName("Reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve el tipo ya activo")
        void reactiva_y_devuelve_el_tipo_activo() {
            when(repository.reactivate(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).thenReturn(1);
            when(repository.findOwnedByIdAndCompanyId(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(SurgeryTypeMother.propioDeEmpresa()));

            SurgeryTypeDto dto = service.execute(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID);

            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("cero filas afectadas es no-encontrado y evita la lectura posterior")
        void cero_filas_afectadas_es_no_encontrado() {
            when(repository.reactivate(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).isInstanceOf(SurgeryTypeNotFoundException.class)
                    .hasMessageContaining(
                            "SurgeryType not found: " + SurgeryTypeMother.SURGERY_TYPE_ID);

            verify(repository, never()).findOwnedByIdAndCompanyId(any(), any());
        }

        @Test
        @DisplayName("si el tipo desaparece entre el UPDATE y el SELECT, falla como no-encontrado")
        void si_el_tipo_desaparece_entre_el_update_y_el_select() {
            when(repository.reactivate(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).thenReturn(1);
            when(repository.findOwnedByIdAndCompanyId(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).isInstanceOf(SurgeryTypeNotFoundException.class);
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
            when(repository.reactivate(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).isInstanceOf(SurgeryTypeNotFoundException.class);

            verify(repository, never()).findOwnedByIdAndCompanyId(any(), any());
            verify(repository, never()).findById(any());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("la fila general compartida tampoco se reactiva desde una empresa")
        void la_fila_general_no_se_reactiva_desde_una_empresa() {
            // Reactivarla la devolveria a TODOS los tenants, asi que el UPDATE acotado por
            // company_id la deja fuera igual que a la fila ajena.
            when(repository.reactivate(SurgeryTypeMother.GENERAL_SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(SurgeryTypeMother.GENERAL_SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).isInstanceOf(SurgeryTypeNotFoundException.class);

            verify(repository, never()).save(any());
        }
    }
}
