package com.vetsoftware.app.subscription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscription.application.command.CreateRequestedSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.CreateSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.RequestedSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemSnapshot;
import com.vetsoftware.app.subscription.application.dto.SubscriptionQuoteSnapshot;
import com.vetsoftware.app.subscription.application.port.out.ResolvedSubscriptionCreationPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionCommercialSnapshotPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionQuoteSnapshotPort;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateRequestedSubscriptionService - snapshots resueltos por el servidor")
class CreateRequestedSubscriptionServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Long COTIZACION = 55L;
    private static final Long LISTA = 3L;
    private static final Long ARTICULO = 100L;
    private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);
    private static final LocalDate FIN_PERIODO = LocalDate.of(2026, 1, 31);

    @Mock
    private ResolvedSubscriptionCreationPort creationPort;
    @Mock
    private SubscriptionQuoteSnapshotPort quoteSnapshotPort;
    @Mock
    private SubscriptionCommercialSnapshotPort commercialSnapshotPort;

    @InjectMocks
    private CreateRequestedSubscriptionService service;

    @Nested
    @DisplayName("Cotizacion")
    class Cotizacion {

        @Test
        @DisplayName("una cotizacion que no pertenece a la empresa se trata como inexistente")
        void cotizacion_de_otra_empresa_no_se_resuelve() {
            when(quoteSnapshotPort.findByIdAndCompanyId(COTIZACION, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoConCotizacion(LISTA)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quote not found for company: 55");

            verifyNoInteractions(commercialSnapshotPort, creationPort);
        }

        @Test
        @DisplayName("una cotizacion no aceptada no puede convertirse en contrato")
        void cotizacion_no_aceptada_no_crea_contrato() {
            when(quoteSnapshotPort.findByIdAndCompanyId(COTIZACION, EMPRESA))
                    .thenReturn(Optional.of(cotizacion(false, LISTA)));

            assertThatThrownBy(() -> service.execute(comandoConCotizacion(LISTA)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Quote must be ACCEPTED: 55");

            verifyNoInteractions(commercialSnapshotPort, creationPort);
        }

        @Test
        @DisplayName("la lista solicitada debe ser la misma que congelo la cotizacion")
        void lista_distinta_a_la_cotizacion_no_crea_contrato() {
            when(quoteSnapshotPort.findByIdAndCompanyId(COTIZACION, EMPRESA))
                    .thenReturn(Optional.of(cotizacion(true, 99L)));

            assertThatThrownBy(() -> service.execute(comandoConCotizacion(LISTA)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("priceListId does not match accepted quote");

            verifyNoInteractions(commercialSnapshotPort, creationPort);
        }

        @Test
        @DisplayName("copia al contrato los snapshots de la cotizacion aceptada")
        void copia_los_snapshots_de_la_cotizacion_aceptada() {
            when(quoteSnapshotPort.findByIdAndCompanyId(COTIZACION, EMPRESA))
                    .thenReturn(Optional.of(cotizacion(true, LISTA)));

            service.execute(comandoConCotizacion(LISTA));

            ArgumentCaptor<CreateSubscriptionCommand> captor = ArgumentCaptor
                    .forClass(CreateSubscriptionCommand.class);
            verify(creationPort).create(captor.capture());
            CreateSubscriptionCommand resolved = captor.getValue();
            assertThat(resolved.quoteId()).isEqualTo(COTIZACION);
            assertThat(resolved.actor()).isEqualTo("ana@example.com");
            assertThat(resolved.items()).singleElement().satisfies(item -> {
                assertThat(item.catalogItemId()).isEqualTo(ARTICULO);
                assertThat(item.itemCode()).isEqualTo("CORE");
                assertThat(item.itemName()).isEqualTo("Nucleo comercial");
                assertThat(item.quantity()).isEqualTo(3);
                assertThat(item.includedQuantity()).isEqualTo(2);
                assertThat(item.unitAmount()).isEqualByComparingTo("179000.00");
                assertThat(item.taxRate()).isEqualByComparingTo("19.00");
            });
            verifyNoInteractions(commercialSnapshotPort);
        }
    }

    @Nested
    @DisplayName("Tarifa publicada")
    class TarifaPublicada {

        @Test
        @DisplayName("sin un articulo publicado para la lista y cantidad no firma el contrato")
        void articulo_sin_precio_publicado_no_crea_contrato() {
            when(commercialSnapshotPort.findPublishedItem(LISTA, BillingCycle.MONTHLY, ARTICULO, 3,
                    INICIO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoSinCotizacion()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Published catalog price not found for item: 100");

            verifyNoInteractions(quoteSnapshotPort, creationPort);
        }

        @Test
        @DisplayName("los importes, IVA y cantidades incluidas salen del snapshot publicado")
        void toma_los_valores_comerciales_del_servidor() {
            when(commercialSnapshotPort.findPublishedItem(LISTA, BillingCycle.MONTHLY, ARTICULO, 3,
                    INICIO)).thenReturn(Optional.of(snapshotComercial()));

            service.execute(comandoSinCotizacion());

            ArgumentCaptor<CreateSubscriptionCommand> captor = ArgumentCaptor
                    .forClass(CreateSubscriptionCommand.class);
            verify(creationPort).create(captor.capture());
            CreateSubscriptionCommand resolved = captor.getValue();
            assertThat(resolved.quoteId()).isNull();
            assertThat(resolved.actor()).isEqualTo("SYSTEM");
            assertThat(resolved.items()).singleElement().satisfies(item -> {
                assertThat(item.itemCode()).isEqualTo("CORE");
                assertThat(item.itemName()).isEqualTo("Nucleo comercial");
                assertThat(item.quantity()).isEqualTo(3);
                assertThat(item.includedQuantity()).isEqualTo(2);
                assertThat(item.unitAmount()).isEqualByComparingTo("179000.00");
                assertThat(item.taxRate()).isEqualByComparingTo("19.00");
                assertThat(item.effectiveFrom()).isEqualTo(INICIO);
            });
            verifyNoInteractions(quoteSnapshotPort);
        }
    }

    private static CreateRequestedSubscriptionCommand comandoConCotizacion(Long priceListId) {
        return new CreateRequestedSubscriptionCommand(EMPRESA, COTIZACION, priceListId,
                BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, INICIO, null, INICIO, FIN_PERIODO,
                null, null, 0, true, List.of());
    }

    private static CreateRequestedSubscriptionCommand comandoSinCotizacion() {
        return new CreateRequestedSubscriptionCommand(EMPRESA, null, LISTA, BillingCycle.MONTHLY,
                SubscriptionStatus.ACTIVE, INICIO, null, INICIO, FIN_PERIODO, null, null, 0, true,
                List.of(new RequestedSubscriptionItemCommand(ARTICULO, 3, INICIO, null)));
    }

    private static SubscriptionQuoteSnapshot cotizacion(boolean accepted, Long priceListId) {
        return new SubscriptionQuoteSnapshot(COTIZACION, EMPRESA, priceListId, BillingCycle.MONTHLY,
                accepted, "ana@example.com", List.of(snapshotComercial()));
    }

    private static SubscriptionItemSnapshot snapshotComercial() {
        return new SubscriptionItemSnapshot(ARTICULO, "CORE", "Nucleo comercial",
                SubscriptionItemType.MODULE, null, 2, TaxTreatment.TAXED, 3,
                new BigDecimal("179000.00"), new BigDecimal("19.00"));
    }
}
