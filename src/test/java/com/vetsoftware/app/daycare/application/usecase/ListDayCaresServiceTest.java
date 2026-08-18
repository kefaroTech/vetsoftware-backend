package com.vetsoftware.app.daycare.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import com.vetsoftware.app.daycare.application.port.out.DayCareRepository;
import com.vetsoftware.app.daycare.testsupport.DayCareMother;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListDayCaresService")
class ListDayCaresServiceTest {

    @Mock
    private DayCareRepository repository;

    private ListDayCaresService service;

    @BeforeEach
    void crearServicio() {
        service = new ListDayCaresService(repository);
    }

    @Test
    @DisplayName("lista todos los daycares mapeados a dto")
    void lista_todos_los_daycares_mapeados_a_dto() {
        when(repository.findAll()).thenReturn(List.of(DayCareMother.guarderiaValida()));

        List<DayCareDto> resultado = service.listAll();

        assertThat(resultado).extracting(DayCareDto::id)
                .containsExactly(DayCareMother.guarderiaValida().getId());
    }

    @Test
    @DisplayName("una empresa sin daycares recibe una lista vacia")
    void sin_daycares_recibe_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
