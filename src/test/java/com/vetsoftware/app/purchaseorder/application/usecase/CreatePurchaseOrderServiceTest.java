package com.vetsoftware.app.purchaseorder.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.purchaseorder.application.command.CreatePurchaseOrderCommand;
import com.vetsoftware.app.purchaseorder.application.command.PurchaseOrderLineCommand;
import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import com.vetsoftware.app.purchaseorder.application.port.out.BranchQueryPort;
import com.vetsoftware.app.purchaseorder.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.purchaseorder.application.port.out.ProductQueryPort;
import com.vetsoftware.app.purchaseorder.application.port.out.PurchaseOrderRepository;
import com.vetsoftware.app.purchaseorder.application.port.out.SupplierQueryPort;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrder;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderLine;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderStatus;
import com.vetsoftware.app.purchaseorder.testsupport.PurchaseOrderMother;
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
@DisplayName("CreatePurchaseOrderService — alta de la orden de compra")
class CreatePurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private BranchQueryPort branchQueryPort;
    @Mock
    private SupplierQueryPort supplierQueryPort;
    @Mock
    private ProductQueryPort productQueryPort;
    @InjectMocks
    private CreatePurchaseOrderService service;

    private static CreatePurchaseOrderCommand comandoValido() {
        return new CreatePurchaseOrderCommand(4L, 7L, PurchaseOrderMother.FECHA_ORDEN,
                PurchaseOrderMother.FECHA_ESPERADA, "Pedido mensual",
                List.of(new PurchaseOrderLineCommand(11L, 10, new BigDecimal("15000.00"))),
                PurchaseOrderMother.COMPANY_ID, PurchaseOrderMother.ACTOR_ID);
    }

    private void resolveCompany() {
        when(companyQueryPort.findById(PurchaseOrderMother.COMPANY_ID))
                .thenReturn(Optional.of(PurchaseOrderMother.CLINICA));
    }

    private void resolveBranch() {
        when(branchQueryPort.findById(4L, PurchaseOrderMother.COMPANY_ID))
                .thenReturn(Optional.of(PurchaseOrderMother.SEDE_NORTE));
    }

    private void resolveSupplier() {
        when(supplierQueryPort.findById(7L, PurchaseOrderMother.COMPANY_ID))
                .thenReturn(Optional.of(PurchaseOrderMother.PROVEEDOR));
    }

    private void resolveProduct() {
        when(productQueryPort.findById(11L, PurchaseOrderMother.COMPANY_ID))
                .thenReturn(Optional.of(PurchaseOrderMother.VACUNA));
    }

    @Nested
    @DisplayName("Camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("persiste la orden en DRAFT con las referencias resueltas por los puertos")
        void persiste_la_orden_con_las_referencias_resueltas() {
            resolveCompany();
            resolveBranch();
            resolveSupplier();
            resolveProduct();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoValido());

            ArgumentCaptor<PurchaseOrder> guardado = ArgumentCaptor.forClass(PurchaseOrder.class);
            verify(repository).save(guardado.capture());
            PurchaseOrder order = guardado.getValue();
            assertThat(order.getId()).isNull();
            assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
            assertThat(order.getCompany()).isEqualTo(PurchaseOrderMother.CLINICA);
            assertThat(order.getBranch()).isEqualTo(PurchaseOrderMother.SEDE_NORTE);
            assertThat(order.getSupplier()).isEqualTo(PurchaseOrderMother.PROVEEDOR);
            assertThat(order.getOrderDate()).isEqualTo(PurchaseOrderMother.FECHA_ORDEN);
            assertThat(order.getExpectedDate()).isEqualTo(PurchaseOrderMother.FECHA_ESPERADA);
            assertThat(order.getNotes()).isEqualTo("Pedido mensual");
            assertThat(order.getCreatedBy()).isEqualTo(PurchaseOrderMother.ACTOR_ID);
            assertThat(order.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("arma una linea por cada linea del comando, sin nada recibido")
        void arma_una_linea_por_cada_linea_del_comando() {
            resolveCompany();
            resolveBranch();
            resolveSupplier();
            resolveProduct();
            when(productQueryPort.findById(12L, PurchaseOrderMother.COMPANY_ID))
                    .thenReturn(Optional.of(PurchaseOrderMother.JERINGA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(new CreatePurchaseOrderCommand(4L, 7L, PurchaseOrderMother.FECHA_ORDEN,
                    null, null,
                    List.of(new PurchaseOrderLineCommand(11L, 10, new BigDecimal("15000.00")),
                            new PurchaseOrderLineCommand(12L, 3, new BigDecimal("500.00"))),
                    PurchaseOrderMother.COMPANY_ID, PurchaseOrderMother.ACTOR_ID));

            ArgumentCaptor<PurchaseOrder> guardado = ArgumentCaptor.forClass(PurchaseOrder.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getLines()).extracting(PurchaseOrderLine::getProduct,
                    PurchaseOrderLine::getQuantityOrdered, PurchaseOrderLine::getQuantityReceived)
                    .containsExactly(tuple(PurchaseOrderMother.VACUNA, 10, 0),
                            tuple(PurchaseOrderMother.JERINGA, 3, 0));
        }

        @Test
        @DisplayName("devuelve el DTO de lo que quedo persistido, no del comando")
        void devuelve_el_dto_de_lo_persistido() {
            resolveCompany();
            resolveBranch();
            resolveSupplier();
            resolveProduct();
            when(repository.save(any())).thenReturn(PurchaseOrderMother.borrador());

            PurchaseOrderDto dto = service.execute(comandoValido());

            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.status()).isEqualTo(PurchaseOrderStatus.DRAFT);
            assertThat(dto.company().id()).isEqualTo(9L);
        }
    }

    @Nested
    @DisplayName("Aislamiento por empresa y referencias inexistentes")
    class Validaciones {

        @Test
        @DisplayName("empresa inexistente aborta antes de resolver nada mas y no escribe")
        void empresa_inexistente_no_escribe() {
            when(companyQueryPort.findById(PurchaseOrderMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: 9");

            verifyNoInteractions(repository, branchQueryPort, supplierQueryPort, productQueryPort);
        }

        @Test
        @DisplayName("sede de otra empresa se comporta como inexistente y no escribe")
        void sede_de_otra_empresa_no_escribe() {
            resolveCompany();
            when(branchQueryPort.findById(4L, PurchaseOrderMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Branch not found: 4");

            verifyNoInteractions(repository, supplierQueryPort, productQueryPort);
        }

        @Test
        @DisplayName("proveedor de otra empresa se comporta como inexistente y no escribe")
        void proveedor_de_otra_empresa_no_escribe() {
            resolveCompany();
            resolveBranch();
            when(supplierQueryPort.findById(7L, PurchaseOrderMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Supplier not found: 7");

            verifyNoInteractions(repository, productQueryPort);
        }

        @Test
        @DisplayName("producto de otra empresa se comporta como inexistente y no escribe")
        void producto_de_otra_empresa_no_escribe() {
            resolveCompany();
            resolveBranch();
            resolveSupplier();
            when(productQueryPort.findById(11L, PurchaseOrderMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoValido()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product not found: 11");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("un comando sin lineas lo rechaza el agregado y no escribe")
        void comando_sin_lineas_no_escribe() {
            resolveCompany();
            resolveBranch();
            resolveSupplier();

            CreatePurchaseOrderCommand sinLineas = new CreatePurchaseOrderCommand(4L, 7L,
                    LocalDate.of(2026, 3, 10), null, null, null, PurchaseOrderMother.COMPANY_ID,
                    PurchaseOrderMother.ACTOR_ID);

            assertThatThrownBy(() -> service.execute(sinLineas))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one line");

            verifyNoInteractions(repository, productQueryPort);
        }

        @Test
        @DisplayName("dos lineas del mismo producto las rechaza el agregado y no escribe")
        void producto_repetido_no_escribe() {
            resolveCompany();
            resolveBranch();
            resolveSupplier();
            resolveProduct();

            CreatePurchaseOrderCommand repetido = new CreatePurchaseOrderCommand(4L, 7L,
                    PurchaseOrderMother.FECHA_ORDEN, null, null,
                    List.of(new PurchaseOrderLineCommand(11L, 10, new BigDecimal("15000.00")),
                            new PurchaseOrderLineCommand(11L, 2, new BigDecimal("15000.00"))),
                    PurchaseOrderMother.COMPANY_ID, PurchaseOrderMother.ACTOR_ID);

            assertThatThrownBy(() -> service.execute(repetido))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("duplicate product in purchase order lines: 11");

            verifyNoInteractions(repository);
        }
    }
}
