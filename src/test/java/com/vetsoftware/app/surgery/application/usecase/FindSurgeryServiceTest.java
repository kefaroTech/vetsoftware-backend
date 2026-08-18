package com.vetsoftware.app.surgery.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import com.vetsoftware.app.surgery.domain.SurgeryNotFoundException;
import com.vetsoftware.app.surgery.testsupport.SurgeryMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindSurgeryService")
class FindSurgeryServiceTest {

    @Mock
    private SurgeryRepository repository;

    @InjectMocks
    private FindSurgeryService service;

    @Test
    @DisplayName("devuelve el DTO de la cirugia encontrada, acotada por empresa")
    void devuelve_el_dto_de_la_cirugia_encontrada() {
        when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID, SurgeryMother.COMPANY_ID))
                .thenReturn(Optional.of(SurgeryMother.cirugiaValida()));

        SurgeryDto dto = service.findById(SurgeryMother.SURGERY_ID, SurgeryMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(SurgeryMother.SURGERY_ID);
        assertThat(dto.description()).isEqualTo("Ovariohisterectomia electiva");
    }

    @Test
    @DisplayName("una cirugia inexistente en la empresa lanza SurgeryNotFoundException")
    void una_cirugia_inexistente_lanza_not_found() {
        when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID, SurgeryMother.COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.findById(SurgeryMother.SURGERY_ID, SurgeryMother.COMPANY_ID))
                .isInstanceOf(SurgeryNotFoundException.class)
                .hasMessageContaining("Surgery not found: " + SurgeryMother.SURGERY_ID);
    }
}
