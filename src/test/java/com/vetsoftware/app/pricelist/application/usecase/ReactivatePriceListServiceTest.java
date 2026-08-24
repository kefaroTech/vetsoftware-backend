package com.vetsoftware.app.pricelist.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.PriceListNotFoundException;
import com.vetsoftware.app.pricelist.testsupport.PriceListMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivatePriceListService")
class ReactivatePriceListServiceTest {

    @Mock
    private PriceListRepository repository;

    @InjectMocks
    private ReactivatePriceListService service;

    @Test
    @DisplayName("reactiva y devuelve la lista ya rehabilitada")
    void reactiva_y_devuelve_la_lista() {
        when(repository.reactivate(1L)).thenReturn(1);
        when(repository.findById(1L)).thenReturn(Optional.of(PriceListMother.borrador()));

        PriceListDto dto = service.execute(1L);

        assertThat(dto.enabled()).isTrue();
        verify(repository).reactivate(1L);
    }

    @Test
    @DisplayName("cero filas actualizadas significa que la lista no existe")
    void cero_filas_es_lista_inexistente() {
        when(repository.reactivate(9L)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(9L)).isInstanceOf(PriceListNotFoundException.class)
                .hasMessageContaining("Price list not found: 9");
    }
}
