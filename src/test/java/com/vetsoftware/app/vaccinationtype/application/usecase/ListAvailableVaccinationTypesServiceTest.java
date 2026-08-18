package com.vetsoftware.app.vaccinationtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.testsupport.VaccinationTypeMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListAvailableVaccinationTypesService")
class ListAvailableVaccinationTypesServiceTest {

    @Mock
    private VaccinationTypeRepository repository;

    @InjectMocks
    private ListAvailableVaccinationTypesService service;

    @Test
    @DisplayName("lista los tipos disponibles para la empresa mapeados a dto")
    void lista_los_tipos_disponibles_mapeados_a_dto() {
        when(repository.findAllAvailableForCompany(VaccinationTypeMother.COMPANY_ID)).thenReturn(
                List.of(VaccinationTypeMother.propia(1L), VaccinationTypeMother.general()));

        List<VaccinationTypeDto> dtos = service.listAvailable(VaccinationTypeMother.COMPANY_ID);

        assertThat(dtos).hasSize(2).extracting(VaccinationTypeDto::id).containsExactly(1L,
                VaccinationTypeMother.TYPE_ID);
    }

    @Test
    @DisplayName("una empresa sin tipos disponibles devuelve una lista vacia")
    void una_empresa_sin_tipos_disponibles_devuelve_una_lista_vacia() {
        when(repository.findAllAvailableForCompany(VaccinationTypeMother.COMPANY_ID))
                .thenReturn(List.of());

        assertThat(service.listAvailable(VaccinationTypeMother.COMPANY_ID)).isEmpty();
    }
}
