package com.vetsoftware.app.surgerytype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
@DisplayName("FindSurgeryTypeService")
class FindSurgeryTypeServiceTest {

    @Mock
    private SurgeryTypeRepository repository;

    @InjectMocks
    private FindSurgeryTypeService service;

    @Nested
    @DisplayName("Consulta")
    class Consulta {

        @Test
        @DisplayName("devuelve el DTO del tipo cuando esta disponible para la empresa")
        void devuelve_el_dto_del_tipo() {
            when(repository.findByIdAndCompanyId(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(SurgeryTypeMother.propioDeEmpresa()));

            SurgeryTypeDto dto = service.findById(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(SurgeryTypeMother.SURGERY_TYPE_ID);
        }

        @Test
        @DisplayName("la fila general sigue siendo accesible desde cualquier empresa")
        void la_fila_general_sigue_siendo_accesible() {
            // Acotar por empresa los caminos de ESCRITURA no toco la lectura: el finder
            // de disponibles sigue devolviendo las filas generales, que es lo que debe
            // seguir funcionando.
            when(repository.findByIdAndCompanyId(SurgeryTypeMother.GENERAL_SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(SurgeryTypeMother.general()));

            SurgeryTypeDto dto = service.findById(SurgeryTypeMother.GENERAL_SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(SurgeryTypeMother.GENERAL_SURGERY_TYPE_ID);
            assertThat(dto.general()).isTrue();
            assertThat(dto.company()).isNull();
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("lanza SurgeryTypeNotFoundException si no esta disponible para la empresa")
        void lanza_excepcion_si_no_esta_disponible() {
            when(repository.findByIdAndCompanyId(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(SurgeryTypeMother.SURGERY_TYPE_ID,
                    SurgeryTypeMother.COMPANY_ID)).isInstanceOf(SurgeryTypeNotFoundException.class)
                    .hasMessageContaining(
                            "SurgeryType not found: " + SurgeryTypeMother.SURGERY_TYPE_ID);
        }
    }
}
