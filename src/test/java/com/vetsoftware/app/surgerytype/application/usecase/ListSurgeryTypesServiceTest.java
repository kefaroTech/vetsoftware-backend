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
@DisplayName("ListSurgeryTypesService")
class ListSurgeryTypesServiceTest {

    @Mock
    private SurgeryTypeRepository repository;

    @InjectMocks
    private ListSurgeryTypesService service;

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        @DisplayName("mapea todos los tipos de cirugia, sin acotar por empresa — listado SYSTEM")
        void mapea_todos_los_tipos_de_cirugia() {
            when(repository.findAll()).thenReturn(
                    List.of(SurgeryTypeMother.propioDeEmpresa(), SurgeryTypeMother.general()));

            List<SurgeryTypeDto> dtos = service.listAll();

            assertThat(dtos).extracting(SurgeryTypeDto::id).containsExactly(
                    SurgeryTypeMother.SURGERY_TYPE_ID, SurgeryTypeMother.GENERAL_SURGERY_TYPE_ID);
        }
    }
}
