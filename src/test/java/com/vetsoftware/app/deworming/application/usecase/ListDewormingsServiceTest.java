package com.vetsoftware.app.deworming.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import com.vetsoftware.app.deworming.application.port.out.DewormingRepository;
import com.vetsoftware.app.deworming.testsupport.DewormingMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListDewormingsService")
class ListDewormingsServiceTest {

    @Mock
    private DewormingRepository repository;
    @InjectMocks
    private ListDewormingsService service;

    @Test
    @DisplayName("traduce cada desparasitacion de dominio a DTO")
    void traduce_cada_desparasitacion_a_dto() {
        when(repository.findAll()).thenReturn(List.of(DewormingMother.desparasitacionValida()));

        List<DewormingDto> resultado = service.listAll();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().id()).isEqualTo(DewormingMother.DEWORMING_ID);
    }

    @Test
    @DisplayName("sin desparasitaciones devuelve lista vacia")
    void sin_desparasitaciones_devuelve_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
