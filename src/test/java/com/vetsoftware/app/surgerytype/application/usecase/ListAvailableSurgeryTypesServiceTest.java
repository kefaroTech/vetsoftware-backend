package com.vetsoftware.app.surgerytype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import com.vetsoftware.app.surgerytype.application.port.out.SurgeryTypeRepository;
import com.vetsoftware.app.surgerytype.testsupport.SurgeryTypeMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListAvailableSurgeryTypesService")
class ListAvailableSurgeryTypesServiceTest {

    @Mock
    private SurgeryTypeRepository repository;

    @InjectMocks
    private ListAvailableSurgeryTypesService service;

    @Nested
    @DisplayName("listAvailable")
    class ListAvailable {

        @Test
        @DisplayName("mapea los tipos disponibles para la empresa, generales y propios")
        void mapea_los_tipos_disponibles_para_la_empresa() {
            when(repository.findAllAvailableForCompany(SurgeryTypeMother.COMPANY_ID)).thenReturn(
                    List.of(SurgeryTypeMother.propioDeEmpresa(), SurgeryTypeMother.general()));

            List<SurgeryTypeDto> dtos = service.listAvailable(SurgeryTypeMother.COMPANY_ID);

            assertThat(dtos).extracting(SurgeryTypeDto::id).containsExactly(
                    SurgeryTypeMother.SURGERY_TYPE_ID, SurgeryTypeMother.GENERAL_SURGERY_TYPE_ID);
        }
    }
}
