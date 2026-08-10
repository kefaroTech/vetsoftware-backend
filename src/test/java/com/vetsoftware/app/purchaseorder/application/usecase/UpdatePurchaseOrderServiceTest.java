package com.vetsoftware.app.purchaseorder.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.purchaseorder.application.command.PurchaseOrderLineCommand;
import com.vetsoftware.app.purchaseorder.application.command.UpdatePurchaseOrderCommand;
import com.vetsoftware.app.purchaseorder.application.dto.CompanySummaryDto;
import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import com.vetsoftware.app.purchaseorder.application.port.out.BranchQueryPort;
import com.vetsoftware.app.purchaseorder.application.port.out.ProductQueryPort;
import com.vetsoftware.app.purchaseorder.application.port.out.PurchaseOrderRepository;
import com.vetsoftware.app.purchaseorder.application.port.out.SupplierQueryPort;
import com.vetsoftware.app.purchaseorder.domain.InvalidPurchaseOrderStatusTransitionException;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrder;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderNotFoundException;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderStatus;
import com.vetsoftware.app.purchaseorder.testsupport.PurchaseOrderMother;
import java.math.BigDecimal;
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
@DisplayName("UpdatePurchaseOrderService — edicion de una orden en borrador")
class UpdatePurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository repository;
    @Mock
    private BranchQueryPort branchQueryPort;
    @Mock
    private SupplierQueryPort supplierQueryPort;
    @Mock
    private ProductQueryPort productQueryPort;
    @InjectMocks
    private UpdatePurchaseOrderService service;

    private static UpdatePurchaseOrderCommand comandoValido() {
        return new UpdatePurchaseOrderCommand(1L, 5L, 8L, PurchaseOrderMother.FECHA_ORDEN,
                PurchaseOrderMother.FECHA_ESPERADA, "nueva nota",
                List.of(new PurchaseOrderLineCommand(12L, 3, new BigDecimal("500.00"))),
                PurchaseOrderMother.COMPANY_ID, 55L, 4L);
    }

    private void loadOrder(PurchaseOrder order) {
        when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID))
                .thenReturn(Optional.of(order));
    }

    private void resolveBranch() {
        when(branchQueryPort.findById(5L, PurchaseOrderMother.COMPANY_ID))
                .thenReturn(Optional.of(PurchaseOrderMother.SEDE_SUR));
    }

    private void resolveSupplier() {
        when(supplierQueryPort.findById(8L, PurchaseOrderMother.COMPANY_ID))
                .thenReturn(Optional.of(PurchaseOrderMother.OTRO_PROVEEDOR));
    }

    private void resolveProduct() {
        when(productQueryPort.findById(12L, PurchaseOrderMother.COMPANY_ID))
                .thenReturn(Optional.of(PurchaseOrderMother.JERINGA));
    }

    @Nested
    @DisplayName("Camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("guarda la orden con la cabecera, las lineas y la version del comando")
        void guarda_la_orden_con_lo_del_comando() {
            loadOrder(PurchaseOrderMother.borrador());
            resolveBranch();
            resolveSupplier();
            resolveProduct();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoValido());

            ArgumentCaptor<PurchaseOrder> guardado = ArgumentCaptor.forClass(PurchaseOrder.class);
            verify(repository).save(guardado.capture());
            PurchaseOrder order = guardado.getValue();
            assertThat(order.getBranch()).isEqualTo(PurchaseOrderMother.SEDE_SUR);
            assertThat(order.getSupplier()).isEqualTo(PurchaseOrderMother.OTRO_PROVEEDOR);
            assertThat(order.getNotes()).isEqualTo("nueva nota");
            assertThat(order.getVersion()).isEqualTo(4L);
            assertThat(order.getUpdatedBy()).isEqualTo(55L);
            assertThat(order.getLines()).singleElement().satisfies(l -> {
                assertThat(l.getProduct()).isEqualTo(PurchaseOrderMother.JERINGA);
                assertThat(l.getQuantityOrdered()).isEqualTo(3);
                assertThat(l.getQuantityReceived()).isZero();
            });
        }

        @Test
        @DisplayName("no cambia la empresa de la orden aunque el comando traiga otra sede")
        void no_cambia_la_empresa_de_la_orden() {
            loadOrder(PurchaseOrderMother.borrador());
            resolveBranch();
            resolveSupplier();
            resolveProduct();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseOrderDto dto = service.execute(comandoValido());

            assertThat(dto.company())
                    .isEqualTo(CompanySummaryDto.from(PurchaseOrderMother.CLINICA));
        }
    }

    @Nested
    @DisplayName("Aislamiento por empresa y estado")
    class Validaciones {

        @Test
        @DisplayName("orden de otra empresa se comporta como inexistente y no escribe")
        void orden_de_otra_empresa_no_escribe() {
            when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(PurchaseOrderNotFoundException.class)
                    .hasMessageContaining("Purchase order not found: 1");

            verify(repository, never()).save(any());
            verifyNoInteractions(branchQueryPort, supplierQueryPort, productQueryPort);
        }

        @Test
        @DisplayName("sede inexistente aborta antes de tocar el agregado y no escribe")
        void sede_inexistente_no_escribe() {
            loadOrder(PurchaseOrderMother.borrador());
            when(branchQueryPort.findById(5L, PurchaseOrderMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Branch not found: 5");

            verify(repository, never()).save(any());
            verifyNoInteractions(supplierQueryPort, productQueryPort);
        }

        @Test
        @DisplayName("proveedor inexistente aborta y no escribe")
        void proveedor_inexistente_no_escribe() {
            loadOrder(PurchaseOrderMother.borrador());
            resolveBranch();
            when(supplierQueryPort.findById(8L, PurchaseOrderMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Supplier not found: 8");

            verify(repository, never()).save(any());
            verifyNoInteractions(productQueryPort);
        }

        @Test
        @DisplayName("producto inexistente aborta y no escribe")
        void producto_inexistente_no_escribe() {
            loadOrder(PurchaseOrderMother.borrador());
            resolveBranch();
            resolveSupplier();
            when(productQueryPort.findById(12L, PurchaseOrderMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product not found: 12");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una orden ya emitida no se puede editar y no se escribe")
        void orden_emitida_no_se_edita() {
            loadOrder(PurchaseOrderMother.emitida());
            resolveBranch();
            resolveSupplier();
            resolveProduct();

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(InvalidPurchaseOrderStatusTransitionException.class)
                    .hasMessageContaining("only be edited while in DRAFT");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un comando sin lineas lo rechaza el agregado y no escribe")
        void comando_sin_lineas_no_escribe() {
            PurchaseOrder order = PurchaseOrderMother.borrador();
            loadOrder(order);
            resolveBranch();
            resolveSupplier();

            UpdatePurchaseOrderCommand sinLineas = new UpdatePurchaseOrderCommand(1L, 5L, 8L,
                    PurchaseOrderMother.FECHA_ORDEN, null, null, null,
                    PurchaseOrderMother.COMPANY_ID, 55L, 4L);

            assertThatThrownBy(() -> service.execute(sinLineas))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one line");

            assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
            verify(repository, never()).save(any());
            verifyNoInteractions(productQueryPort);
        }
    }
}
