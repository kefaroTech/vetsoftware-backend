package com.vetsoftware.app.surgery.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import com.vetsoftware.app.surgery.testsupport.SurgeryMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListSurgeriesService")
class ListSurgeriesServiceTest {

    @Mock
    private SurgeryRepository repository;

    @InjectMocks
    private ListSurgeriesService service;

    @Test
    @DisplayName("mapea todas las cirugias a DTO")
    void mapea_todas_las_cirugias_a_dto() {
        when(repository.findAll()).thenReturn(
                List.of(SurgeryMother.cirugiaValida(1L), SurgeryMother.cirugiaValida(2L)));

        List<SurgeryDto> resultado = service.listAll();

        assertThat(resultado).extracting(SurgeryDto::id).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("una lista vacia no es un error")
    void una_lista_vacia_no_es_un_error() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
