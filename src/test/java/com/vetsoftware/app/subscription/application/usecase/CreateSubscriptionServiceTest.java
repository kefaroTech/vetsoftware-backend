package com.vetsoftware.app.subscription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscription.application.command.CreateSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.SubscriptionItemLineCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.application.port.out.CatalogItemValidationPort;
import com.vetsoftware.app.subscription.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.subscription.application.port.out.PlatformCatalogPort;
import com.vetsoftware.app.subscription.application.port.out.CompanyValidationPort;
import com.vetsoftware.app.subscription.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionChangedPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionNumberPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemCompositionPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionStatusHistoryRepository;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.CompanyAlreadyHasActiveSubscriptionException;
import com.vetsoftware.app.subscription.domain.ItemOrigin;
import com.vetsoftware.app.subscription.domain.PriceListRef;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionChangeKind;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import com.vetsoftware.app.subscription.domain.SubscriptionItemOverlapException;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChange;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateSubscriptionService - el alta del contrato")
class CreateSubscriptionServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Long CONTRATO = 7L;
    private static final LocalDate ENERO_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate JUNIO_30 = LocalDate.of(2026, 6, 30);

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
    // D-76: el alta congela la composicion de cada linea en la misma transaccion.
    @Mock
    private SubscriptionItemCompositionPort compositionPort;
    @Mock
    private SubscriptionNumberPort subscriptionNumberPort;
    @Mock
    private SubscriptionChangedPort subscriptionChangedPort;
    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-01-01T10:15:30Z"), ZoneOffset.UTC);

    @InjectMocks
    private CreateSubscriptionService service;

    private static Subscription contratoGuardado() {
        return new Subscription(CONTRATO, "SUS-2026-00184", EMPRESA, null, 3L, BillingCycle.MONTHLY,
                SubscriptionStatus.ACTIVE, ENERO_1, null, ENERO_1, LocalDate.of(2026, 1, 31), null,
                null, 0, null, true, null, null, 0L, true);
    }

    private static SubscriptionItemLineCommand linea(Long articulo, LocalDate from, LocalDate to) {
        return new SubscriptionItemLineCommand(articulo, "CORE", "Nucleo",
                SubscriptionItemType.MODULE, null, 2, TaxTreatment.TAXED, 1,
                new BigDecimal("179000.00"), new BigDecimal("19.00"), from, to);
    }

    private static CreateSubscriptionCommand comando(List<SubscriptionItemLineCommand> lineas) {
        return new CreateSubscriptionCommand(EMPRESA, null, 3L, BillingCycle.MONTHLY,
                SubscriptionStatus.ACTIVE, ENERO_1, null, ENERO_1, LocalDate.of(2026, 1, 31), null,
                null, 0, true, "consola-plataforma", lineas);
    }

    @BeforeEach
    void elConsecutivoLoReservaElServidor() {
        // El numero del contrato ya no viaja en el command: lo reserva el puerto dentro
        // de la misma transaccion, para que un fallo deshaga la reserva.
        lenient().when(subscriptionNumberPort.nextSubscriptionNumber(anyInt()))
                .thenReturn("SUS-2026-00184");
        // La cabecera ya no comprueba existencia: exige tarifa PUBLICADA y VIGENTE el
        // dia del alta (D-73). El reloj de este test marca 2026-01-01, dentro de la
        // ventana que devuelve esta tarifa.
        lenient().when(priceListQueryPort.findPublishedById(3L)).thenReturn(Optional
                .of(new PriceListRef(3L, "LISTA-2026", ENERO_1, LocalDate.of(2026, 12, 31))));
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("crea el contrato y sus lineas iniciales en la misma llamada")
        void creaContratoYLineas() {
            when(repository.save(any())).thenReturn(contratoGuardado());
            when(itemRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            service.execute(comando(List.of(linea(100L, ENERO_1, null))));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SubscriptionItem>> captor = ArgumentCaptor.forClass(List.class);
            verify(itemRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            SubscriptionItem linea = captor.getValue().get(0);
            assertThat(linea.getOrigin()).isEqualTo(ItemOrigin.INITIAL);
            assertThat(linea.getSubscriptionId()).isEqualTo(CONTRATO);
            assertThat(linea.getIncludedQuantity()).isEqualTo(2);
            assertThat(linea.getUnitAmount()).isEqualByComparingTo("179000.00");
        }

        @Test
        @DisplayName("la linea sin fecha propia arranca el dia que arranca el contrato")
        void lineaSinFechaHeredaLaDelContrato() {
            when(repository.save(any())).thenReturn(contratoGuardado());
            when(itemRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            service.execute(comando(List.of(linea(100L, null, null))));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SubscriptionItem>> captor = ArgumentCaptor.forClass(List.class);
            verify(itemRepository).saveAll(captor.capture());
            assertThat(captor.getValue().get(0).getPeriod().from()).isEqualTo(ENERO_1);
        }

        @Test
        @DisplayName("la primera fila de la bitacora no viene de ningun estado")
        void primeraFilaSinEstadoAnterior() {
            when(repository.save(any())).thenReturn(contratoGuardado());
            when(itemRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            service.execute(comando(List.of()));

            ArgumentCaptor<SubscriptionStatusChange> captor = ArgumentCaptor
                    .forClass(SubscriptionStatusChange.class);
            verify(historyRepository).append(captor.capture());
            assertThat(captor.getValue().getFromStatus()).isNull();
            assertThat(captor.getValue().getToStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(captor.getValue().getActor()).isEqualTo("consola-plataforma");
        }

        @Test
        @DisplayName("anuncia el alta para que se calculen los permisos de la empresa")
        void anunciaElAlta() {
            when(repository.save(any())).thenReturn(contratoGuardado());
            when(itemRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            service.execute(comando(List.of()));

            verify(subscriptionChangedPort).subscriptionChanged(new SubscriptionChangedEvent(
                    EMPRESA, CONTRATO, SubscriptionChangeKind.SUBSCRIPTION_CREATED, ENERO_1));
        }
    }

    @Nested
    @DisplayName("Una empresa, un contrato vivo")
    class UnContratoVivo {

        @Test
        @DisplayName("no comprueba antes si ya hay contrato: deja hablar al indice unico")
        void noComprebaAntes() {
            when(repository.save(any()))
                    .thenThrow(new CompanyAlreadyHasActiveSubscriptionException(EMPRESA));

            assertThatThrownBy(() -> service.execute(comando(List.of())))
                    .isInstanceOf(CompanyAlreadyHasActiveSubscriptionException.class);

            // Un SELECT previo seria una carrera: dos altas simultaneas leerian las dos
            // «no hay contrato» e insertarian las dos.
            verify(repository, never()).findCurrentByCompanyId(any());
            verify(itemRepository, never()).saveAll(any());
            verify(historyRepository, never()).append(any());
            verify(subscriptionChangedPort, never()).subscriptionChanged(any());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("dos lineas del mismo articulo que se pisan no pueden firmarse juntas")
        void dosLineasDelMismoArticuloQueSePisan() {
            when(repository.save(any())).thenReturn(contratoGuardado());

            assertThatThrownBy(() -> service.execute(comando(List.of(linea(100L, ENERO_1, JUNIO_30),
                    linea(100L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 12, 31))))))
                    .isInstanceOf(SubscriptionItemOverlapException.class);
        }

        @Test
        @DisplayName("dos lineas de articulos distintos en las mismas fechas son legitimas")
        void dosArticulosDistintosEnLasMismasFechas() {
            when(repository.save(any())).thenReturn(contratoGuardado());
            when(itemRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            service.execute(
                    comando(List.of(linea(100L, ENERO_1, null), linea(200L, ENERO_1, null))));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SubscriptionItem>> captor = ArgumentCaptor.forClass(List.class);
            verify(itemRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(2);
        }

        @Test
        @DisplayName("valida la empresa y la tarifa antes de escribir nada")
        void validaEmpresaYTarifa() {
            when(repository.save(any())).thenReturn(contratoGuardado());
            when(itemRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            service.execute(comando(List.of()));

            verify(companyValidationPort).validateExists(EMPRESA);
            verify(priceListQueryPort).findPublishedById(3L);
        }

        @Test
        @DisplayName("valida cada articulo del catalogo que se firma")
        void validaCadaArticulo() {
            when(repository.save(any())).thenReturn(contratoGuardado());
            when(itemRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            service.execute(comando(List.of(linea(100L, ENERO_1, null))));

            verify(catalogItemValidationPort).validateExists(100L);
        }
    }
}
