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
@DisplayName("ListVaccinationTypesService")
class ListVaccinationTypesServiceTest {

    @Mock
    private VaccinationTypeRepository repository;

    @InjectMocks
    private ListVaccinationTypesService service;

    @Test
    @DisplayName("lista todos los tipos mapeados a dto")
    void lista_todos_los_tipos_mapeados_a_dto() {
        when(repository.findAll()).thenReturn(
                List.of(VaccinationTypeMother.propia(1L), VaccinationTypeMother.propia(2L)));

        List<VaccinationTypeDto> dtos = service.listAll();

        assertThat(dtos).hasSize(2).extracting(VaccinationTypeDto::id).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("un repositorio vacio devuelve una lista vacia")
    void un_repositorio_vacio_devuelve_una_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
