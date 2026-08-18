package com.vetsoftware.app.spa.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.spa.application.dto.SpaDto;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import com.vetsoftware.app.spa.domain.Spa;
import com.vetsoftware.app.spa.testsupport.SpaMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListSpasByAnimalService")
class ListSpasByAnimalServiceTest {

    @Mock
    private SpaRepository repository;

    private ListSpasByAnimalService service;

    @BeforeEach
    void crearServicio() {
        service = new ListSpasByAnimalService(repository);
    }

    @Test
    @DisplayName("delega la paginacion en el repositorio y mapea el contenido a dto")
    void delega_la_paginacion_y_mapea_el_contenido() {
        PageResult<Spa> pagina = new PageResult<>(List.of(SpaMother.spaValido()), 0, 20, 1, 1);
        when(repository.findAllByAnimalIdAndCompanyId(SpaMother.FIRULAIS.id(),
                SpaMother.CLINICA.id(), "baño", 0, 20)).thenReturn(pagina);

        PageResult<SpaDto> resultado = service.listByAnimal(SpaMother.FIRULAIS.id(),
                SpaMother.CLINICA.id(), "baño", 0, 20);

        assertThat(resultado.content()).extracting(SpaDto::id)
                .containsExactly(SpaMother.spaValido().getId());
        verify(repository).findAllByAnimalIdAndCompanyId(SpaMother.FIRULAIS.id(),
                SpaMother.CLINICA.id(), "baño", 0, 20);
    }
}
