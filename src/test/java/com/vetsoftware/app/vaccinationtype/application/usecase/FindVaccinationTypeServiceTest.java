package com.vetsoftware.app.vaccinationtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeNotFoundException;
import com.vetsoftware.app.vaccinationtype.testsupport.VaccinationTypeMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindVaccinationTypeService")
class FindVaccinationTypeServiceTest {

    @Mock
    private VaccinationTypeRepository repository;

    @InjectMocks
    private FindVaccinationTypeService service;

    @Test
    @DisplayName("encuentra y devuelve el dto del tipo disponible para la empresa")
    void encuentra_y_devuelve_el_dto() {
        when(repository.findByIdAndCompanyId(VaccinationTypeMother.TYPE_ID,
                VaccinationTypeMother.COMPANY_ID))
                .thenReturn(Optional.of(VaccinationTypeMother.propia()));

        VaccinationTypeDto dto = service.findById(VaccinationTypeMother.TYPE_ID,
                VaccinationTypeMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(VaccinationTypeMother.TYPE_ID);
    }

    @Test
    @DisplayName("la fila general sigue siendo accesible desde cualquier empresa")
    void la_fila_general_sigue_siendo_accesible() {
        // Acotar por empresa los caminos de ESCRITURA no toco la lectura: el finder de
        // disponibles sigue devolviendo las filas generales.
        when(repository.findByIdAndCompanyId(VaccinationTypeMother.TYPE_ID,
                VaccinationTypeMother.COMPANY_ID))
                .thenReturn(Optional.of(VaccinationTypeMother.general()));

        VaccinationTypeDto dto = service.findById(VaccinationTypeMother.TYPE_ID,
                VaccinationTypeMother.COMPANY_ID);

        assertThat(dto.general()).isTrue();
        assertThat(dto.company()).isNull();
    }

    @Test
    @DisplayName("un id inexistente para la empresa lanza VaccinationTypeNotFoundException")
    void un_id_inexistente_lanza_not_found() {
        when(repository.findByIdAndCompanyId(99L, VaccinationTypeMother.COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L, VaccinationTypeMother.COMPANY_ID))
                .isInstanceOf(VaccinationTypeNotFoundException.class)
                .hasMessageContaining("VaccinationType not found: 99");
    }
}
