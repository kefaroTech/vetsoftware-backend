package com.vetsoftware.app.goodsreceipt.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.goodsreceipt.application.command.CreateGoodsReceiptCommand;
import com.vetsoftware.app.goodsreceipt.application.command.GoodsReceiptLineCommand;
import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import com.vetsoftware.app.goodsreceipt.application.port.out.BranchQueryPort;
import com.vetsoftware.app.goodsreceipt.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.goodsreceipt.application.port.out.GoodsReceiptRepository;
import com.vetsoftware.app.goodsreceipt.application.port.out.ProductQueryPort;
import com.vetsoftware.app.goodsreceipt.application.port.out.SupplierQueryPort;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceipt;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptLine;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptStatus;
import com.vetsoftware.app.goodsreceipt.testsupport.GoodsReceiptMother;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateGoodsReceiptService")
class CreateGoodsReceiptServiceTest {

    @Mock
    private GoodsReceiptRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private BranchQueryPort branchQueryPort;
    @Mock
    private SupplierQueryPort supplierQueryPort;
    @Mock
    private ProductQueryPort productQueryPort;

    @InjectMocks
    private CreateGoodsReceiptService service;

    @Captor
    private ArgumentCaptor<GoodsReceipt> receiptCaptor;

    private void empresaSedeYProveedorExisten() {
        when(companyQueryPort.findById(GoodsReceiptMother.COMPANY_ID))
                .thenReturn(Optional.of(GoodsReceiptMother.CLINICA));
        when(branchQueryPort.findById(GoodsReceiptMother.SEDE.id(), GoodsReceiptMother.COMPANY_ID))
                .thenReturn(Optional.of(GoodsReceiptMother.SEDE));
        when(supplierQueryPort.findById(GoodsReceiptMother.PROVEEDOR.id(),
                GoodsReceiptMother.COMPANY_ID))
                .thenReturn(Optional.of(GoodsReceiptMother.PROVEEDOR));
    }

    private void devuelveLoQueSeGuarda() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("Camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("persiste la recepcion en DRAFT con las referencias resueltas por los puertos")
        void persiste_la_recepcion_en_borrador() {
            empresaSedeYProveedorExisten();
            when(productQueryPort.findById(GoodsReceiptMother.VACUNA.id(),
                    GoodsReceiptMother.COMPANY_ID))
                    .thenReturn(Optional.of(GoodsReceiptMother.VACUNA));
            devuelveLoQueSeGuarda();

            service.execute(GoodsReceiptMother.comandoCrear());

            verify(repository).save(receiptCaptor.capture());
            GoodsReceipt guardada = receiptCaptor.getValue();
            assertThat(guardada.getId()).isNull();
            assertThat(guardada.getStatus()).isEqualTo(GoodsReceiptStatus.DRAFT);
            assertThat(guardada.getCompany()).isEqualTo(GoodsReceiptMother.CLINICA);
            assertThat(guardada.getBranch()).isEqualTo(GoodsReceiptMother.SEDE);
            assertThat(guardada.getSupplier()).isEqualTo(GoodsReceiptMother.PROVEEDOR);
            assertThat(guardada.getReceiptDate()).isEqualTo(GoodsReceiptMother.FECHA_RECEPCION);
            assertThat(guardada.getSupplierInvoiceNumber()).isEqualTo("FV-1001");
            assertThat(guardada.getNotes()).isEqualTo("Entrega parcial");
            assertThat(guardada.getCreatedBy()).isEqualTo(GoodsReceiptMother.ACTOR_ID);
            assertThat(guardada.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("construye cada linea con el producto que resolvio el puerto")
        void construye_las_lineas_con_el_producto_resuelto() {
            empresaSedeYProveedorExisten();
            when(productQueryPort.findById(GoodsReceiptMother.VACUNA.id(),
                    GoodsReceiptMother.COMPANY_ID))
                    .thenReturn(Optional.of(GoodsReceiptMother.VACUNA));
            when(productQueryPort.findById(GoodsReceiptMother.JERINGA.id(),
                    GoodsReceiptMother.COMPANY_ID))
                    .thenReturn(Optional.of(GoodsReceiptMother.JERINGA));
            devuelveLoQueSeGuarda();

            service.execute(
                    GoodsReceiptMother.comandoCrear(List.of(GoodsReceiptMother.comandoLinea(),
                            GoodsReceiptMother.comandoLineaDeOrden(900L))));

            verify(repository).save(receiptCaptor.capture());
            List<GoodsReceiptLine> lineas = receiptCaptor.getValue().getLines();
            assertThat(lineas).extracting(linea -> linea.getProduct().code())
                    .containsExactly("P-021", "P-022");
            assertThat(lineas).extracting(GoodsReceiptLine::getQuantityReceived).containsExactly(10,
                    4);
            assertThat(lineas).extracting(GoodsReceiptLine::getPurchaseOrderLineId)
                    .containsExactly(null, 900L);
        }

        @Test
        @DisplayName("las lineas nacen sin id: los asigna la base al persistir")
        void las_lineas_nacen_sin_id() {
            empresaSedeYProveedorExisten();
            when(productQueryPort.findById(GoodsReceiptMother.VACUNA.id(),
                    GoodsReceiptMother.COMPANY_ID))
                    .thenReturn(Optional.of(GoodsReceiptMother.VACUNA));
            devuelveLoQueSeGuarda();

            service.execute(GoodsReceiptMother.comandoCrear());

            verify(repository).save(receiptCaptor.capture());
            assertThat(receiptCaptor.getValue().getLines()).singleElement()
                    .extracting(GoodsReceiptLine::getId).isNull();
        }

        @Test
        @DisplayName("propaga el id de la orden de compra sin leerla: se valida al confirmar")
        void propaga_la_orden_sin_leerla() {
            empresaSedeYProveedorExisten();
            when(productQueryPort.findById(GoodsReceiptMother.VACUNA.id(),
                    GoodsReceiptMother.COMPANY_ID))
                    .thenReturn(Optional.of(GoodsReceiptMother.VACUNA));
            devuelveLoQueSeGuarda();

            service.execute(new CreateGoodsReceiptCommand(GoodsReceiptMother.SEDE.id(),
                    GoodsReceiptMother.PROVEEDOR.id(), GoodsReceiptMother.PURCHASE_ORDER_ID,
                    GoodsReceiptMother.FECHA_RECEPCION, null, null,
                    List.of(GoodsReceiptMother.comandoLinea()), GoodsReceiptMother.COMPANY_ID,
                    GoodsReceiptMother.ACTOR_ID));

            verify(repository).save(receiptCaptor.capture());
            assertThat(receiptCaptor.getValue().getPurchaseOrderId())
                    .isEqualTo(GoodsReceiptMother.PURCHASE_ORDER_ID);
        }

        @Test
        @DisplayName("devuelve el DTO de lo que quedo persistido, no de lo que se le paso")
        void devuelve_el_dto_de_lo_persistido() {
            empresaSedeYProveedorExisten();
            when(productQueryPort.findById(GoodsReceiptMother.VACUNA.id(),
                    GoodsReceiptMother.COMPANY_ID))
                    .thenReturn(Optional.of(GoodsReceiptMother.VACUNA));
            when(repository.save(any())).thenReturn(GoodsReceiptMother.enBorrador());

            GoodsReceiptDto dto = service.execute(GoodsReceiptMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(GoodsReceiptMother.RECEIPT_ID);
            assertThat(dto.status()).isEqualTo(GoodsReceiptStatus.DRAFT);
            assertThat(dto.lines()).singleElement().extracting(l -> l.product().code())
                    .isEqualTo("P-021");
        }
    }

    @Nested
    @DisplayName("Referencias inexistentes o de otra empresa")
    class Aislamiento {

        @Test
        @DisplayName("no escribe nada si la empresa del comando no existe")
        void empresa_inexistente() {
            when(companyQueryPort.findById(GoodsReceiptMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(GoodsReceiptMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + GoodsReceiptMother.COMPANY_ID);

            verifyNoInteractions(repository, branchQueryPort, supplierQueryPort, productQueryPort);
        }

        @Test
        @DisplayName("no escribe nada si la sede es de otra empresa")
        void sede_de_otra_empresa() {
            when(companyQueryPort.findById(GoodsReceiptMother.COMPANY_ID))
                    .thenReturn(Optional.of(GoodsReceiptMother.CLINICA));
            when(branchQueryPort.findById(GoodsReceiptMother.SEDE.id(),
                    GoodsReceiptMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(GoodsReceiptMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Branch not found: " + GoodsReceiptMother.SEDE.id());

            verifyNoInteractions(repository, supplierQueryPort, productQueryPort);
        }

        @Test
        @DisplayName("no escribe nada si el proveedor es de otra empresa")
        void proveedor_de_otra_empresa() {
            when(companyQueryPort.findById(GoodsReceiptMother.COMPANY_ID))
                    .thenReturn(Optional.of(GoodsReceiptMother.CLINICA));
            when(branchQueryPort.findById(GoodsReceiptMother.SEDE.id(),
                    GoodsReceiptMother.COMPANY_ID))
                    .thenReturn(Optional.of(GoodsReceiptMother.SEDE));
            when(supplierQueryPort.findById(GoodsReceiptMother.PROVEEDOR.id(),
                    GoodsReceiptMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(GoodsReceiptMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Supplier not found: " + GoodsReceiptMother.PROVEEDOR.id());

            verifyNoInteractions(repository, productQueryPort);
        }

        @Test
        @DisplayName("no escribe nada si un producto de las lineas es de otra empresa")
        void producto_de_otra_empresa() {
            empresaSedeYProveedorExisten();
            when(productQueryPort.findById(GoodsReceiptMother.VACUNA.id(),
                    GoodsReceiptMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(GoodsReceiptMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product not found: " + GoodsReceiptMother.VACUNA.id());

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Validacion de las lineas")
    class Validaciones {

        @Test
        @DisplayName("no escribe nada si el comando llega sin lista de lineas")
        void sin_lista_de_lineas() {
            empresaSedeYProveedorExisten();

            List<GoodsReceiptLineCommand> sinLineas = null;

            assertThatThrownBy(() -> service.execute(GoodsReceiptMother.comandoCrear(sinLineas)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one line is required");

            verifyNoInteractions(repository, productQueryPort);
        }

        @Test
        @DisplayName("no escribe nada si la lista de lineas viene vacia")
        void lista_de_lineas_vacia() {
            empresaSedeYProveedorExisten();

            assertThatThrownBy(() -> service.execute(GoodsReceiptMother.comandoCrear(List.of())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one line is required");

            verifyNoInteractions(repository, productQueryPort);
        }

        @Test
        @DisplayName("no escribe nada si una linea trae cantidad cero")
        void cantidad_cero() {
            empresaSedeYProveedorExisten();
            when(productQueryPort.findById(GoodsReceiptMother.VACUNA.id(),
                    GoodsReceiptMother.COMPANY_ID))
                    .thenReturn(Optional.of(GoodsReceiptMother.VACUNA));

            assertThatThrownBy(() -> service.execute(GoodsReceiptMother.comandoCrear(List
                    .of(new GoodsReceiptLineCommand(GoodsReceiptMother.VACUNA.id(), null, "LOTE-A",
                            GoodsReceiptMother.VENCIMIENTO, 0, GoodsReceiptMother.COSTO)))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantityReceived must be greater than 0");

            verifyNoInteractions(repository);
        }
    }
}
