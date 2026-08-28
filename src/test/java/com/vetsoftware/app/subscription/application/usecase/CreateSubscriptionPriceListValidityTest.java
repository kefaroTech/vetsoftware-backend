package com.vetsoftware.app.subscription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.shared.pricing.PriceListNotEffectiveException;
import com.vetsoftware.app.subscription.application.command.CreateSubscriptionCommand;
import com.vetsoftware.app.subscription.application.port.out.CatalogItemValidationPort;
import com.vetsoftware.app.subscription.application.port.out.CompanyValidationPort;
import com.vetsoftware.app.subscription.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.subscription.application.port.out.PlatformCatalogPort;
import com.vetsoftware.app.subscription.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionChangedPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemCompositionPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionNumberPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionStatusHistoryRepository;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.PriceListRef;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * D-73 por el lado del contrato: <em>la cabecera se firma contra la tarifa
 * vigente POR FECHA</em>, no contra cualquiera que exista.
 *
 * <p>
 * Antes aqui habia un {@code existsById} pelado, asi que se podia firmar
 * apuntando a una lista en borrador o caducada. No se colaba del todo —las
 * lineas fallaban despues contra el catalogo publicado— pero fallaba con el
 * mensaje equivocado: «Published catalog price not found for item» acusa al
 * articulo cuando la culpable es la tarifa, y quien lo leyera se pondria a
 * revisar el catalogo. Estas pruebas fijan que el fallo ahora nombra la tarifa
 * y trae su ventana dentro.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateSubscriptionService - la tarifa de la cabecera, publicada y vigente")
class CreateSubscriptionPriceListValidityTest {

    private static final Long EMPRESA = 42L;
    private static final Long TARIFA = 3L;
    private static final LocalDate ENERO_1 = LocalDate.of(2026, 1, 1);
    /** El dia que marca el reloj inyectado, que es el que lleva la zona (D-81). */
    private static final LocalDate HOY = LocalDate.of(2026, 6, 15);

    @Mock
    private SubscriptionRepository repository;
    @Mock
    private SubscriptionItemRepository itemRepository;
    @Mock
    private SubscriptionStatusHistoryRepository historyRepository;
    @Mock
    private CompanyValidationPort companyValidationPort;
    @Mock
    private PriceListQueryPort priceListQueryPort;
    @Mock
    private CatalogItemValidationPort catalogItemValidationPort;
    @Mock
    private LimitDimensionQueryPort limitDimensionQueryPort;
    @Mock
    private PlatformCatalogPort platformCatalogPort;
    @Mock
    private SubscriptionItemCompositionPort compositionPort;
    @Mock
    private SubscriptionNumberPort subscriptionNumberPort;
    @Mock
    private SubscriptionChangedPort subscriptionChangedPort;
    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-06-15T10:15:30Z"), ZoneOffset.UTC);

    @InjectMocks
    private CreateSubscriptionService service;

    private static CreateSubscriptionCommand comando() {
        return new CreateSubscriptionCommand(EMPRESA, null, TARIFA, BillingCycle.MONTHLY,
                SubscriptionStatus.ACTIVE, ENERO_1, null, ENERO_1, LocalDate.of(2026, 1, 31), null,
                null, 0, true, "consola-plataforma", List.of());
    }

    private void tarifaPublicada(LocalDate validFrom, LocalDate validTo) {
        when(priceListQueryPort.findPublishedById(TARIFA)).thenReturn(
                Optional.of(new PriceListRef(TARIFA, "LISTA-2026", validFrom, validTo)));
    }

    private void nadaSeEscribio() {
        verify(repository, never()).save(any());
        verify(itemRepository, never()).saveAll(any());
        verify(historyRepository, never()).append(any());
        verifyNoInteractions(subscriptionChangedPort, compositionPort);
    }

    @Nested
    @DisplayName("Vigencia")
    class Vigencia {

        @Test
        @DisplayName("una lista en borrador no llega siquiera a mirarse la ventana")
        void listaEnBorrador() {
            // El adaptador filtra por estado publicado, asi que una lista en borrador o
            // retirada vuelve vacia: es indistinguible de un id que no existe, y las
            // dos se arreglan igual, eligiendo otra tarifa.
            when(priceListQueryPort.findPublishedById(TARIFA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Published price list not found: 3");

            nadaSeEscribio();
        }

        @Test
        @DisplayName("una lista caducada se rechaza nombrando la TARIFA, no el articulo")
        void listaCaducada() {
            tarifaPublicada(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOfSatisfying(PriceListNotEffectiveException.class, fallo -> {
                        assertThat(fallo.getPriceListId()).isEqualTo(TARIFA);
                        assertThat(fallo.getCode()).isEqualTo("LISTA-2026");
                        // La ventana viaja dentro del fallo: sin ella el operador no sabe
                        // si publicar la del periodo nuevo o corregir las fechas.
                        assertThat(fallo.getValidFrom()).isEqualTo(LocalDate.of(2025, 1, 1));
                        assertThat(fallo.getValidTo()).isEqualTo(LocalDate.of(2025, 12, 31));
                        assertThat(fallo.getQuotedOn()).isEqualTo(HOY);
                    });

            nadaSeEscribio();
        }

        @Test
        @DisplayName("una lista futura tampoco vale: aun no ha empezado")
        void listaFutura() {
            tarifaPublicada(LocalDate.of(2026, 7, 1), null);

            assertThatThrownBy(() -> service.execute(comando())).isInstanceOfSatisfying(
                    PriceListNotEffectiveException.class,
                    fallo -> assertThat(fallo.getValidFrom()).isEqualTo(LocalDate.of(2026, 7, 1)));

            nadaSeEscribio();
        }

        @Test
        @DisplayName("una lista sin cierre es vigente: valid_to nulo no es un error")
        void listaSinCierre() {
            // La unica tarifa viva del catalogo esta sembrada asi, con valid_to nulo. Un
            // «hoy menor o igual que validTo» escrito sin pensar en el nulo la
            // descartaria y tumbaria el alta de empresas entera, que es peor que el
            // fallo que se venia a corregir.
            tarifaPublicada(ENERO_1, null);
            when(subscriptionNumberPort.nextSubscriptionNumber(anyInt()))
                    .thenReturn("SUS-2026-00184");
            when(repository.save(any())).thenAnswer(i -> {
                Subscription enviado = i.getArgument(0);
                return new Subscription(7L, enviado.getSubscriptionNumber(), enviado.getCompanyId(),
                        null, TARIFA, BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, ENERO_1,
                        null, ENERO_1, LocalDate.of(2026, 1, 31), null, null, 0, null, true, null,
                        null, 0L, true);
            });
            when(itemRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            assertThatCode(() -> service.execute(comando())).doesNotThrowAnyException();

            verify(repository).save(any());
        }
    }
}
