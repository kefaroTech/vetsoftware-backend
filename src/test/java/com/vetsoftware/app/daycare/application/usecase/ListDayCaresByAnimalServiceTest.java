package com.vetsoftware.app.daycare.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import com.vetsoftware.app.daycare.application.port.out.DayCareRepository;
import com.vetsoftware.app.daycare.testsupport.DayCareMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListDayCaresByAnimalService")
class ListDayCaresByAnimalServiceTest {

    @Mock
    private DayCareRepository repository;

    private ListDayCaresByAnimalService service;

    @BeforeEach
    void crearServicio() {
        service = new ListDayCaresByAnimalService(repository);
    }

    @Test
    @DisplayName("delega la paginacion en el repositorio y mapea el contenido a dto")
    void delega_la_paginacion_y_mapea_el_contenido() {
        PageResult<com.vetsoftware.app.daycare.domain.DayCare> pagina = new PageResult<>(
                List.of(DayCareMother.guarderiaValida()), 0, 20, 1, 1);
        when(repository.findAllByAnimalIdAndCompanyId(1L, 10L, "correa", 0, 20)).thenReturn(pagina);

        PageResult<DayCareDto> resultado = service.listByAnimal(1L, 10L, "correa", 0, 20);

        assertThat(resultado.content()).extracting(DayCareDto::id)
                .containsExactly(DayCareMother.guarderiaValida().getId());
        verify(repository).findAllByAnimalIdAndCompanyId(1L, 10L, "correa", 0, 20);
    }
}
