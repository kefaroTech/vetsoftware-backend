package com.vetsoftware.app.pricelist.application.usecase;

import static com.vetsoftware.app.pricelist.testsupport.PriceListMother.FIRMANTE;
import static com.vetsoftware.app.pricelist.testsupport.PriceListMother.PUBLICADA_EL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.pricelist.application.command.PublishPriceListCommand;
import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import com.vetsoftware.app.pricelist.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.pricelist.application.port.out.CatalogPriceRepository;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.InvalidPriceListTransitionException;
import com.vetsoftware.app.pricelist.domain.PriceListNotFoundException;
import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import com.vetsoftware.app.pricelist.testsupport.PriceListMother;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublishPriceListService — congelar la tarifa")
class PublishPriceListServiceTest {

    @Mock
    private PriceListRepository repository;

    // Publicar comprueba ademas que los tramos de cada (articulo, ciclo) cubran
    // todas las cantidades (#378). Sin tramos que examinar, la comprobacion pasa.
    @Mock
    private CatalogPriceRepository catalogPriceRepository;

    // R-PRICE-05: y que ningun articulo ACTIVO se quede sin precio. Sin articulos
    // activos que contrastar no hay nada que exigir, que es el caso de casi todos
    // los escenarios de aqui.
    @Mock
    private CatalogItemQueryPort catalogItemQueryPort;

    private final Clock clock = Clock.fixed(PUBLICADA_EL.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private PublishPriceListService service() {
        return new PublishPriceListService(repository, catalogPriceRepository, catalogItemQueryPort,
                clock);
    }

    @Test
    @DisplayName("sella el estado, el momento y la firma del usuario de sistema")
    void sella_estado_momento_y_firma() {
        when(repository.findById(1L)).thenReturn(Optional.of(PriceListMother.borrador()));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PriceListDto dto = service().execute(new PublishPriceListCommand(1L, FIRMANTE));

        assertThat(dto.status()).isEqualTo(PriceListStatus.PUBLISHED);
        assertThat(dto.publishedAt()).isEqualTo(PUBLICADA_EL);
        assertThat(dto.publishedBySystemUserId()).isEqualTo(FIRMANTE);
    }

    @Test
    @DisplayName("publicar una lista ya publicada no es una transición legal")
    void republicar_es_ilegal() {
        when(repository.findById(1L)).thenReturn(Optional.of(PriceListMother.publicada()));

        assertThatThrownBy(() -> service().execute(new PublishPriceListCommand(1L, FIRMANTE)))
                .isInstanceOf(InvalidPriceListTransitionException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("publicar sin firmante se rechaza antes de escribir")
    void sin_firmante_se_rechaza() {
        when(repository.findById(1L)).thenReturn(Optional.of(PriceListMother.borrador()));

        assertThatThrownBy(() -> service().execute(new PublishPriceListCommand(1L, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("publishedBySystemUserId is required");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("una lista inexistente no se publica")
    void lista_inexistente() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().execute(new PublishPriceListCommand(9L, FIRMANTE)))
                .isInstanceOf(PriceListNotFoundException.class);

        verify(repository, never()).save(any());
    }
}
