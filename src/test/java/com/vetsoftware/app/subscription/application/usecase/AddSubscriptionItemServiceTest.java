package com.vetsoftware.app.subscription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscription.application.command.AddSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.command.SubscriptionItemLineCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemDto;
import com.vetsoftware.app.subscription.application.port.out.CatalogItemValidationPort;
import com.vetsoftware.app.subscription.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionAmendmentRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionChangedPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionNumberPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.application.port.out.SystemUserValidationPort;
import com.vetsoftware.app.subscription.domain.AmendmentType;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.EffectivePeriod;
import com.vetsoftware.app.subscription.domain.EmployeeRef;
import com.vetsoftware.app.subscription.domain.ItemOrigin;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionAmendment;
import com.vetsoftware.app.subscription.domain.SubscriptionChangeKind;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import com.vetsoftware.app.subscription.domain.SubscriptionItemOverlapException;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.SubscriptionNotFoundException;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddSubscriptionItemService - abrir una linea")
class AddSubscriptionItemServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Long CONTRATO = 7L;
    private static final Long ARTICULO = 100L;
    private static final String LLAVE = "req-abc-123";
    private static final LocalDate MAYO_1 = LocalDate.of(2026, 5, 1);
    private static final LocalDate JUNIO_30 = LocalDate.of(2026, 6, 30);
    private static final LocalDate DICIEMBRE_31 = LocalDate.of(2026, 12, 31);

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionItemRepository itemRepository;
    @Mock
    private SubscriptionAmendmentRepository amendmentRepository;
    @Mock
    private CatalogItemValidationPort catalogItemValidationPort;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @Mock
    private SystemUserValidationPort systemUserValidationPort;
    @Mock
    private SubscriptionNumberPort subscriptionNumberPort;
    @Mock
    private SubscriptionChangedPort subscriptionChangedPort;

    @InjectMocks
    private AddSubscriptionItemService service;

    private static Subscription contrato() {
        return new Subscription(CONTRATO, "SUS-2026-00184", EMPRESA, null, 3L, BillingCycle.MONTHLY,
                SubscriptionStatus.ACTIVE, LocalDate.of(2026, 1, 1), null, LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31), null, null, 0, null, true, null, null, 0L, true);
    }

    private static SubscriptionItemLineCommand linea(LocalDate from, LocalDate to) {
        return new SubscriptionItemLineCommand(ARTICULO, "VET", "Veterinaria",
                SubscriptionItemType.MODULE, null, 2, TaxTreatment.TAXED, 5,
                new BigDecimal("179000.00"), new BigDecimal("19.00"), from, to);
    }

    private static AddSubscriptionItemCommand comando(SubscriptionItemLineCommand linea) {
        return new AddSubscriptionItemCommand(CONTRATO, EMPRESA, LLAVE, MAYO_1,
                "Contrato veterinaria", 55L, null, null, linea);
    }

    private static SubscriptionAmendment otrosiGuardado() {
        return new SubscriptionAmendment(900L, EMPRESA, CONTRATO, "AMD-2026-0001",
                AmendmentType.ADD_ITEM, MAYO_1, null, 55L, null, BigDecimal.ZERO, BigDecimal.ZERO,
                null, LLAVE, null);
    }

    private static SubscriptionItem tramoExistente(LocalDate from, LocalDate to) {
        return SubscriptionItem.open(EMPRESA, CONTRATO, ARTICULO, "VET", "Veterinaria",
                SubscriptionItemType.MODULE, null, 2, TaxTreatment.TAXED, 1,
                new BigDecimal("179000.00"), BigDecimal.ZERO, new EffectivePeriod(from, to),
                ItemOrigin.ADDON, null);
    }

    @BeforeEach
    void elConsecutivoLoReservaElServidor() {
        // El numero del otrosi ya no viaja en el command: lo reserva el puerto dentro
        // de la transaccion. Leniente porque los caminos que rechazan antes —solape,
        // reintento idempotente, empleado ajeno— no llegan a pedirlo.
        lenient().when(subscriptionNumberPort.nextAmendmentNumber(anyInt()))
                .thenReturn("AMD-2026-00001");
    }

    @Nested
    @DisplayName("Solapes")
    class Solapes {

        @Test
        @DisplayName("rechaza el tramo que se pisa con uno ya existente")
        void rechazaElSolape() {
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.lockByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(contrato()));
            when(employeeQueryPort.findByIdAndCompanyId(55L, EMPRESA))
                    .thenReturn(Optional.of(new EmployeeRef(55L, "Ana")));
            when(itemRepository.findOverlapping(EMPRESA, CONTRATO, ARTICULO, MAYO_1, DICIEMBRE_31,
                    null)).thenReturn(List.of(tramoExistente(LocalDate.of(2026, 1, 1), JUNIO_30)));

            assertThatThrownBy(() -> service.execute(comando(linea(MAYO_1, DICIEMBRE_31))))
                    .isInstanceOf(SubscriptionItemOverlapException.class);

            // Y no deja rastro: ni otrosi, ni linea, ni evento.
            verify(amendmentRepository, never()).save(any());
            verify(itemRepository, never()).save(any());
            verify(subscriptionChangedPort, never()).subscriptionChanged(any());
        }

        @Test
        @DisplayName("bloquea el contrato ANTES de preguntar por el solape")
        void bloqueaAntesDeComprobar() {
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.lockByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(contrato()));
            when(employeeQueryPort.findByIdAndCompanyId(55L, EMPRESA))
                    .thenReturn(Optional.of(new EmployeeRef(55L, "Ana")));
            when(itemRepository.findOverlapping(anyLong(), anyLong(), anyLong(), any(), any(),
                    any())).thenReturn(List.of());
            when(amendmentRepository.save(any())).thenReturn(otrosiGuardado());
            when(itemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.execute(comando(linea(MAYO_1, null)));

            // Sin el bloqueo la comprobacion es una carrera: dos transacciones
            // concurrentes pasan las dos y las dos insertan.
            InOrder orden = Mockito.inOrder(subscriptionRepository, itemRepository);
            orden.verify(subscriptionRepository).lockByIdAndCompanyId(CONTRATO, EMPRESA);
            orden.verify(itemRepository).findOverlapping(anyLong(), anyLong(), anyLong(), any(),
                    any(), any());
        }

        @Test
        @DisplayName("acepta el tramo que empieza donde acaba el anterior")
        void aceptaElTramoConsecutivo() {
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.lockByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(contrato()));
            when(employeeQueryPort.findByIdAndCompanyId(55L, EMPRESA))
                    .thenReturn(Optional.of(new EmployeeRef(55L, "Ana")));
            when(itemRepository.findOverlapping(anyLong(), anyLong(), anyLong(), any(), any(),
                    any())).thenReturn(List.of());
            when(amendmentRepository.save(any())).thenReturn(otrosiGuardado());
            when(itemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            SubscriptionItemDto resultado = service.execute(comando(linea(JUNIO_30, null)));

            assertThat(resultado.effectiveFrom()).isEqualTo(JUNIO_30);
        }
    }

    @Nested
    @DisplayName("Idempotencia")
    class Idempotencia {

        @Test
        @DisplayName("el segundo clic devuelve la linea del primero y no crea otra")
        void segundoClicNoDuplica() {
            SubscriptionItem yaCreada = new SubscriptionItem(500L, EMPRESA, CONTRATO, ARTICULO,
                    "VET", "Veterinaria", SubscriptionItemType.MODULE, null, 2, TaxTreatment.TAXED,
                    5, new BigDecimal("179000.00"), BigDecimal.ZERO,
                    EffectivePeriod.openFrom(MAYO_1), ItemOrigin.ADDON, 900L, null, null, 0L, true);
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.of(otrosiGuardado()));
            when(itemRepository.findByCreatedAmendmentIdAndCompanyId(900L, EMPRESA))
                    .thenReturn(Optional.of(yaCreada));

            SubscriptionItemDto resultado = service.execute(comando(linea(MAYO_1, null)));

            assertThat(resultado.id()).isEqualTo(500L);
            // Ni siquiera llega a bloquear el contrato: se busca antes de insertar.
            verify(subscriptionRepository, never()).lockByIdAndCompanyId(anyLong(), anyLong());
            verify(amendmentRepository, never()).save(any());
            verify(itemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Congelacion y efectos")
    class CongelacionYEfectos {

        @Test
        @DisplayName("copia a la linea lo que trae el command, incluido included_quantity")
        void congelaLoQueTraeElCommand() {
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.lockByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(contrato()));
            when(employeeQueryPort.findByIdAndCompanyId(55L, EMPRESA))
                    .thenReturn(Optional.of(new EmployeeRef(55L, "Ana")));
            when(itemRepository.findOverlapping(anyLong(), anyLong(), anyLong(), any(), any(),
                    any())).thenReturn(List.of());
            when(amendmentRepository.save(any())).thenReturn(otrosiGuardado());
            when(itemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.execute(comando(linea(MAYO_1, null)));

            ArgumentCaptor<SubscriptionItem> captor = ArgumentCaptor
                    .forClass(SubscriptionItem.class);
            verify(itemRepository).save(captor.capture());
            SubscriptionItem guardada = captor.getValue();
            assertThat(guardada.getIncludedQuantity()).isEqualTo(2);
            assertThat(guardada.getUnitAmount()).isEqualByComparingTo("179000.00");
            assertThat(guardada.getTaxRate()).isEqualByComparingTo("19.00");
            assertThat(guardada.getOrigin()).isEqualTo(ItemOrigin.ADDON);
            assertThat(guardada.getCreatedAmendmentId()).isEqualTo(900L);
            assertThat(guardada.billableQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("anuncia que el contrato cambio, para que el recalculo pueda dispararse")
        void anunciaElCambio() {
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.lockByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(contrato()));
            when(employeeQueryPort.findByIdAndCompanyId(55L, EMPRESA))
                    .thenReturn(Optional.of(new EmployeeRef(55L, "Ana")));
            when(itemRepository.findOverlapping(anyLong(), anyLong(), anyLong(), any(), any(),
                    any())).thenReturn(List.of());
            when(amendmentRepository.save(any())).thenReturn(otrosiGuardado());
            when(itemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.execute(comando(linea(MAYO_1, null)));

            verify(subscriptionChangedPort).subscriptionChanged(new SubscriptionChangedEvent(
                    EMPRESA, CONTRATO, SubscriptionChangeKind.ITEM_ADDED, MAYO_1));
        }

        @Test
        @DisplayName("R14: el empleado que firma tiene que ser de la empresa del contrato")
        void empleadoDeOtraEmpresa() {
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.lockByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(contrato()));
            when(employeeQueryPort.findByIdAndCompanyId(55L, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(linea(MAYO_1, null))))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Employee");

            verify(amendmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("un contrato de otra empresa no existe para el caller")
        void contratoAjeno() {
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.lockByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(linea(MAYO_1, null))))
                    .isInstanceOf(SubscriptionNotFoundException.class);
        }

        @Test
        @DisplayName("valida que el articulo del catalogo existe")
        void validaElArticulo() {
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.lockByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(contrato()));
            when(employeeQueryPort.findByIdAndCompanyId(55L, EMPRESA))
                    .thenReturn(Optional.of(new EmployeeRef(55L, "Ana")));
            when(itemRepository.findOverlapping(anyLong(), anyLong(), anyLong(), any(), any(),
                    any())).thenReturn(List.of());
            when(amendmentRepository.save(any())).thenReturn(otrosiGuardado());
            when(itemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.execute(comando(linea(MAYO_1, null)));

            verify(catalogItemValidationPort).validateExists(eq(ARTICULO));
        }
    }
}
