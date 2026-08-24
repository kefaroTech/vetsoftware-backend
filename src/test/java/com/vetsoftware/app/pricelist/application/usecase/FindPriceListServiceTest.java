package com.vetsoftware.app.pricelist.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
@DisplayName("FindPriceListService")
class FindPriceListServiceTest {

    @Mock
    private PriceListRepository repository;

    @InjectMocks
    private FindPriceListService service;

    @Test
    @DisplayName("proyecta la lista encontrada al DTO")
    void proyecta_la_lista() {
        when(repository.findById(1L)).thenReturn(Optional.of(PriceListMother.publicada()));

        PriceListDto dto = service.findById(1L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.code()).isEqualTo("LISTA-2026-01");
        assertThat(dto.publishedBySystemUserId()).isEqualTo(PriceListMother.FIRMANTE);
    }

    @Test
    @DisplayName("una lista inexistente es un fallo explícito, no un Optional vacío que se cuela")
    void lista_inexistente() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(9L))
                .isInstanceOf(PriceListNotFoundException.class)
                .hasMessageContaining("Price list not found: 9");
    }
}
