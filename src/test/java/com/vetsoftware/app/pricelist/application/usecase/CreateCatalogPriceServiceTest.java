package com.vetsoftware.app.pricelist.application.usecase;

import static com.vetsoftware.app.pricelist.testsupport.CatalogPriceMother.ARTICULO;
import static com.vetsoftware.app.pricelist.testsupport.CatalogPriceMother.CREADO_EL;
import static com.vetsoftware.app.pricelist.testsupport.CatalogPriceMother.LISTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.pricelist.application.command.CreateCatalogPriceCommand;
import com.vetsoftware.app.pricelist.application.dto.CatalogPriceDto;
import com.vetsoftware.app.pricelist.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.pricelist.application.port.out.CatalogItemValidationPort;
import com.vetsoftware.app.pricelist.application.port.out.CatalogPriceRepository;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.BillingCycle;
import com.vetsoftware.app.pricelist.domain.CatalogPrice;
import com.vetsoftware.app.pricelist.domain.CatalogPriceTierOverlapException;
import com.vetsoftware.app.pricelist.domain.PriceListNotEditableException;
import com.vetsoftware.app.pricelist.domain.PriceListNotFoundException;
import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import com.vetsoftware.app.pricelist.testsupport.CatalogPriceMother;
import com.vetsoftware.app.pricelist.testsupport.PriceListMother;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCatalogPriceService")
class CreateCatalogPriceServiceTest {

    @Mock
    private CatalogPriceRepository repository;
    @Mock
    private PriceListRepository priceListRepository;
    @Mock
    private CatalogItemValidationPort catalogItemValidationPort;

    // La respuesta del alta lleva el resumen del articulo (#379): puerto de
    // LECTURA,
    // aparte de la guarda de existencia, que sigue siendo el ValidationPort.
    @Mock
    private CatalogItemQueryPort catalogItemQueryPort;

    private final Clock clock = Clock.fixed(CREADO_EL.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private CreateCatalogPriceService service() {
        return new CreateCatalogPriceService(repository, priceListRepository,
                catalogItemValidationPort, catalogItemQueryPort, clock);
    }

    private static CreateCatalogPriceCommand comando(int tierMin, Integer tierMax) {
        return new CreateCatalogPriceCommand(LISTA, ARTICULO, BillingCycle.MONTHLY, tierMin,
                tierMax, 2, new BigDecimal("12000.00"), new BigDecimal("0.00"),
                new BigDecimal("19.00"), TaxTreatment.TAXED);
    }

    @Nested
    @DisplayName("Alta en un borrador")
    class Alta {

        @Test
        @DisplayName("guarda el precio con su tramo, sus importes y su fiscalidad")
        void guarda_el_precio() {
            when(priceListRepository.lockById(LISTA))
                    .thenReturn(Optional.of(PriceListMother.borrador()));
            when(catalogItemValidationPort.existsById(ARTICULO)).thenReturn(true);
            when(repository.findTierScope(LISTA, ARTICULO, BillingCycle.MONTHLY))
                    .thenReturn(List.of());
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            CatalogPriceDto dto = service().execute(comando(1, 10));

            ArgumentCaptor<CatalogPrice> guardado = ArgumentCaptor.forClass(CatalogPrice.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getTierMin()).isEqualTo(1);
            assertThat(guardado.getValue().getTierMax()).isEqualTo(10);
            assertThat(guardado.getValue().getCreatedDate()).isEqualTo(CREADO_EL);
            assertThat(dto.taxTreatment()).isEqualTo(TaxTreatment.TAXED);
            assertThat(dto.unitAmount()).isEqualTo(new BigDecimal("12000.00"));
        }

        @Test
        @DisplayName("bloquea la lista antes de leer los hermanos, para serializar el read-then-write")
        void bloquea_la_lista_antes_de_leer() {
            when(priceListRepository.lockById(LISTA))
                    .thenReturn(Optional.of(PriceListMother.borrador()));
            when(catalogItemValidationPort.existsById(ARTICULO)).thenReturn(true);
            when(repository.findTierScope(LISTA, ARTICULO, BillingCycle.MONTHLY))
                    .thenReturn(List.of());
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service().execute(comando(1, 10));

            verify(priceListRepository).lockById(LISTA);
            verify(priceListRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("Inmutabilidad heredada de la lista (R9)")
    class InmutabilidadR9 {

        @ParameterizedTest
        @EnumSource(value = PriceListStatus.class, names = {"PUBLISHED", "ARCHIVED"})
        @DisplayName("no se puede añadir un precio a una lista que ya no es borrador")
        void no_se_anaden_precios_fuera_de_draft(PriceListStatus estado) {
            when(priceListRepository.lockById(LISTA))
                    .thenReturn(Optional.of(PriceListMother.enEstado(estado)));

            assertThatThrownBy(() -> service().execute(comando(1, 10)))
                    .isInstanceOf(PriceListNotEditableException.class);

            verify(repository, never()).save(any());
            verify(catalogItemValidationPort, never()).existsById(any());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("una lista inexistente no admite precios")
        void lista_inexistente() {
            when(priceListRepository.lockById(LISTA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().execute(comando(1, 10)))
                    .isInstanceOf(PriceListNotFoundException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un artículo inexistente se rechaza antes de construir el precio")
        void articulo_inexistente() {
            when(priceListRepository.lockById(LISTA))
                    .thenReturn(Optional.of(PriceListMother.borrador()));
            when(catalogItemValidationPort.existsById(ARTICULO)).thenReturn(false);

            assertThatThrownBy(() -> service().execute(comando(1, 10)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Catalog item not found: " + ARTICULO);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un tramo que pisa a otro del mismo artículo y ciclo se rechaza")
        void tramo_solapado_se_rechaza() {
            when(priceListRepository.lockById(LISTA))
                    .thenReturn(Optional.of(PriceListMother.borrador()));
            when(catalogItemValidationPort.existsById(ARTICULO)).thenReturn(true);
            when(repository.findTierScope(LISTA, ARTICULO, BillingCycle.MONTHLY))
                    .thenReturn(List.of(CatalogPriceMother.conTramo(99L, 1, 10)));

            assertThatThrownBy(() -> service().execute(comando(5, 20)))
                    .isInstanceOf(CatalogPriceTierOverlapException.class)
                    .hasMessageContaining("overlaps catalog price 99");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un tramo consecutivo al existente sí entra")
        void tramo_consecutivo_entra() {
            when(priceListRepository.lockById(LISTA))
                    .thenReturn(Optional.of(PriceListMother.borrador()));
            when(catalogItemValidationPort.existsById(ARTICULO)).thenReturn(true);
            when(repository.findTierScope(LISTA, ARTICULO, BillingCycle.MONTHLY))
                    .thenReturn(List.of(CatalogPriceMother.conTramo(99L, 1, 10)));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            CatalogPriceDto dto = service().execute(comando(11, null));

            assertThat(dto.tierMin()).isEqualTo(11);
            assertThat(dto.tierMax()).isNull();
        }
    }
}
