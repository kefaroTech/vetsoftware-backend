package com.vetsoftware.app.pricelist.application.usecase;

import static com.vetsoftware.app.pricelist.testsupport.PriceListMother.DESDE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.pricelist.application.command.UpdatePriceListCommand;
import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.PriceList;
import com.vetsoftware.app.pricelist.domain.PriceListNotEditableException;
import com.vetsoftware.app.pricelist.domain.PriceListNotFoundException;
import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import com.vetsoftware.app.pricelist.testsupport.PriceListMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdatePriceListService")
class UpdatePriceListServiceTest {

    @Mock
    private PriceListRepository repository;

    @InjectMocks
    private UpdatePriceListService service;

    private static UpdatePriceListCommand comando() {
        return new UpdatePriceListCommand(1L, "Tarifa 2026 rev. B", "USD", DESDE, null);
    }

    @Nested
    @DisplayName("Edición de un borrador")
    class Borrador {

        @Test
        @DisplayName("aplica los cambios y devuelve el DTO actualizado")
        void aplica_los_cambios() {
            when(repository.findById(1L)).thenReturn(Optional.of(PriceListMother.borrador()));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            PriceListDto dto = service.execute(comando());

            ArgumentCaptor<PriceList> guardada = ArgumentCaptor.forClass(PriceList.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getName()).isEqualTo("Tarifa 2026 rev. B");
            assertThat(guardada.getValue().getCurrency()).isEqualTo("USD");
            assertThat(dto.name()).isEqualTo("Tarifa 2026 rev. B");
        }
    }

    @Nested
    @DisplayName("Inmutabilidad de una lista publicada (R9)")
    class InmutabilidadR9 {

        @ParameterizedTest
        @EnumSource(value = PriceListStatus.class, names = {"PUBLISHED", "ARCHIVED"})
        @DisplayName("rechaza la edición y no escribe nada")
        void rechaza_y_no_escribe(PriceListStatus estado) {
            when(repository.findById(1L)).thenReturn(Optional.of(PriceListMother.enEstado(estado)));

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(PriceListNotEditableException.class)
                    .hasMessageContaining("cannot be modified");

            verify(repository, never()).save(any());
        }
    }

    @Test
    @DisplayName("una lista inexistente es un 404 de dominio, no un guardado silencioso")
    void lista_inexistente() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(comando()))
                .isInstanceOf(PriceListNotFoundException.class)
                .hasMessageContaining("Price list not found: 1");

        verify(repository, never()).save(any());
    }
}
