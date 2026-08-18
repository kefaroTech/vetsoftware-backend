package com.vetsoftware.app.consultationtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import com.vetsoftware.app.consultationtype.application.port.out.ConsultationTypeRepository;
import com.vetsoftware.app.consultationtype.testsupport.ConsultationTypeMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListConsultationTypesService")
class ListConsultationTypesServiceTest {

    @Mock
    private ConsultationTypeRepository repository;

    @InjectMocks
    private ListConsultationTypesService service;

    @Test
    @DisplayName("lista todos los tipos mapeados a dto")
    void lista_todos_los_tipos_mapeados_a_dto() {
        when(repository.findAll()).thenReturn(List.of(ConsultationTypeMother.consultaGeneral(1L),
                ConsultationTypeMother.consultaGeneral(2L)));

        List<ConsultationTypeDto> dtos = service.listAll();

        assertThat(dtos).hasSize(2).extracting(ConsultationTypeDto::id).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("un repositorio vacio devuelve una lista vacia")
    void un_repositorio_vacio_devuelve_una_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
