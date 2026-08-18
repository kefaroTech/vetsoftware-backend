package com.vetsoftware.app.spa.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.spa.application.dto.SpaDto;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import com.vetsoftware.app.spa.testsupport.SpaMother;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListSpasService")
class ListSpasServiceTest {

    @Mock
    private SpaRepository repository;

    private ListSpasService service;

    @BeforeEach
    void crearServicio() {
        service = new ListSpasService(repository);
    }

    @Test
    @DisplayName("lista todos los spas mapeados a dto")
    void lista_todos_los_spas_mapeados_a_dto() {
        when(repository.findAll()).thenReturn(List.of(SpaMother.spaValido()));

        List<SpaDto> resultado = service.listAll();

        assertThat(resultado).extracting(SpaDto::id).containsExactly(SpaMother.spaValido().getId());
    }

    @Test
    @DisplayName("una empresa sin spas recibe una lista vacia")
    void sin_spas_recibe_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
