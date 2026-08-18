package com.vetsoftware.app.service.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.service.domain.TaxTreatment;
import com.vetsoftware.app.service.testsupport.ServiceMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ServiceDto — from(Service)")
class ServiceDtoTest {

    @Nested
    @DisplayName("con impuesto")
    class ConImpuesto {

        @Test
        @DisplayName("mapea cada campo, incluido el impuesto")
        void mapea_cada_campo_incluido_el_impuesto() {
            ServiceDto dto = ServiceDto.from(ServiceMother.consultaGeneral());

            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.name()).isEqualTo("Consulta general");
            assertThat(dto.price()).isEqualByComparingTo("50000.00");
            assertThat(dto.taxTreatment()).isEqualTo(TaxTreatment.GRAVADO);
            assertThat(dto.notes()).isEqualTo("Consulta veterinaria estandar");
            assertThat(dto.serviceCategory())
                    .isEqualTo(ServiceCategorySummaryDto.from(ServiceMother.CONSULTAS));
            assertThat(dto.tax()).isEqualTo(TaxSummaryDto.from(ServiceMother.IVA_19));
            assertThat(dto.company()).isEqualTo(CompanySummaryDto.from(ServiceMother.CLINICA));
            assertThat(dto.createdDate()).isEqualTo(ServiceMother.CREADO);
            assertThat(dto.updatedDate()).isNull();
            assertThat(dto.updatedBy()).isNull();
            assertThat(dto.version()).isEqualTo(0L);
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("sin impuesto")
    class SinImpuesto {

        @Test
        @DisplayName("un servicio EXENTO/EXCLUIDO mapea el impuesto a null, no a un summary vacio")
        void un_servicio_sin_impuesto_mapea_a_null() {
            ServiceDto dto = ServiceDto.from(ServiceMother.exenta());

            assertThat(dto.taxTreatment()).isEqualTo(TaxTreatment.EXENTO);
            assertThat(dto.tax()).isNull();
        }
    }
}
