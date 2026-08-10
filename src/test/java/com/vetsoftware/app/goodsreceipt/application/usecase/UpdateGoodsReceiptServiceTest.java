package com.vetsoftware.app.goodsreceipt.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.goodsreceipt.application.command.GoodsReceiptLineCommand;
import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import com.vetsoftware.app.goodsreceipt.application.port.out.BranchQueryPort;
import com.vetsoftware.app.goodsreceipt.application.port.out.GoodsReceiptRepository;
import com.vetsoftware.app.goodsreceipt.application.port.out.ProductQueryPort;
import com.vetsoftware.app.goodsreceipt.application.port.out.SupplierQueryPort;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceipt;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptLine;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptNotFoundException;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptStatus;
import com.vetsoftware.app.goodsreceipt.domain.InvalidGoodsReceiptStatusTransitionException;
import com.vetsoftware.app.goodsreceipt.testsupport.GoodsReceiptMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateGoodsReceiptService")
class UpdateGoodsReceiptServiceTest {

    @Mock
    private GoodsReceiptRepository repository;
    @Mock
    private BranchQueryPort branchQueryPort;
    @Mock
    private SupplierQueryPort supplierQueryPort;
    @Mock
    private ProductQueryPort productQueryPort;

    @InjectMocks
    private UpdateGoodsReceiptService service;

    @Captor
    private ArgumentCaptor<GoodsReceipt> receiptCaptor;

    private void recepcionExiste(GoodsReceipt receipt) {
        when(repository.findByIdAndCompanyId(GoodsReceiptMother.RECEIPT_ID,
                GoodsReceiptMother.COMPANY_ID)).thenReturn(Optional.of(receipt));
    }

    private void nuevasReferenciasExisten() {
        when(branchQueryPort.findById(GoodsReceiptMother.OTRA_SEDE.id(),
                GoodsReceiptMother.COMPANY_ID))
                .thenReturn(Optional.of(GoodsReceiptMother.OTRA_SEDE));
        when(supplierQueryPort.findById(GoodsReceiptMother.OTRO_PROVEEDOR.id(),
                GoodsReceiptMother.COMPANY_ID))
                .thenReturn(Optional.of(GoodsReceiptMother.OTRO_PROVEEDOR));
    }

    private void productoExiste() {
        when(productQueryPort.findById(GoodsReceiptMother.JERINGA.id(),
                GoodsReceiptMother.COMPANY_ID)).thenReturn(Optional.of(GoodsReceiptMother.JERINGA));
    }

    @Nested
    @DisplayName("Camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("reemplaza sede, proveedor, fecha, factura y notas de la recepcion en DRAFT")
        void reemplaza_los_campos_editables() {
            recepcionExiste(GoodsReceiptMother.enBorrador());
            nuevasReferenciasExisten();
            productoExiste();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(GoodsReceiptMother.comandoActualizar());

            verify(repository).save(receiptCaptor.capture());
            GoodsReceipt guardada = receiptCaptor.getValue();
            assertThat(guardada.getBranch()).isEqualTo(GoodsReceiptMother.OTRA_SEDE);
            assertThat(guardada.getSupplier()).isEqualTo(GoodsReceiptMother.OTRO_PROVEEDOR);
            assertThat(guardada.getPurchaseOrderId())
                    .isEqualTo(GoodsReceiptMother.PURCHASE_ORDER_ID);
            assertThat(guardada.getReceiptDate()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(guardada.getSupplierInvoiceNumber()).isEqualTo("FV-2002");
            assertThat(guardada.getNotes()).isEqualTo("Corregida");
            assertThat(guardada.getUpdatedBy()).isEqualTo(GoodsReceiptMother.ACTOR_ID);
            assertThat(guardada.getVersion()).isEqualTo(3L);
        }

        @Test
        @DisplayName("sustituye las lineas por las del comando, no las acumula")
        void sustituye_las_lineas() {
            recepcionExiste(GoodsReceiptMother.enBorrador());
            nuevasReferenciasExisten();
            productoExiste();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(GoodsReceiptMother.comandoActualizar());

            verify(repository).save(receiptCaptor.capture());
            assertThat(receiptCaptor.getValue().getLines()).singleElement()
                    .extracting(linea -> linea.getProduct().code()).isEqualTo("P-022");
        }

        @Test
        @DisplayName("las lineas nuevas nacen sin id: la anterior se descarta entera")
        void las_lineas_nuevas_nacen_sin_id() {
            recepcionExiste(GoodsReceiptMother.enBorrador());
            nuevasReferenciasExisten();
            productoExiste();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(GoodsReceiptMother.comandoActualizar());

            verify(repository).save(receiptCaptor.capture());
            assertThat(receiptCaptor.getValue().getLines())
                    .allSatisfy(linea -> assertThat(linea.getId()).isNull());
        }

        @Test
        @DisplayName("no cambia la empresa ni el estado: la recepcion sigue en DRAFT")
        void no_cambia_empresa_ni_estado() {
            recepcionExiste(GoodsReceiptMother.enBorrador());
            nuevasReferenciasExisten();
            productoExiste();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            GoodsReceiptDto dto = service.execute(GoodsReceiptMother.comandoActualizar());

            assertThat(dto.company().id()).isEqualTo(GoodsReceiptMother.COMPANY_ID);
            assertThat(dto.status()).isEqualTo(GoodsReceiptStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("Aislamiento por empresa y referencias inexistentes")
    class Aislamiento {

        @Test
        @DisplayName("no escribe nada si la recepcion no es de la empresa del comando")
        void recepcion_de_otra_empresa() {
            when(repository.findByIdAndCompanyId(GoodsReceiptMother.RECEIPT_ID,
                    GoodsReceiptMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(GoodsReceiptMother.comandoActualizar()))
                    .isInstanceOf(GoodsReceiptNotFoundException.class).hasMessageContaining(
                            "Goods receipt not found: " + GoodsReceiptMother.RECEIPT_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(branchQueryPort, supplierQueryPort, productQueryPort);
        }

        @Test
        @DisplayName("no escribe nada si la nueva sede es de otra empresa")
        void sede_de_otra_empresa() {
            recepcionExiste(GoodsReceiptMother.enBorrador());
            when(branchQueryPort.findById(GoodsReceiptMother.OTRA_SEDE.id(),
                    GoodsReceiptMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(GoodsReceiptMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Branch not found: " + GoodsReceiptMother.OTRA_SEDE.id());

            verify(repository, never()).save(any());
            verifyNoInteractions(supplierQueryPort, productQueryPort);
        }

        @Test
        @DisplayName("no escribe nada si el nuevo proveedor es de otra empresa")
        void proveedor_de_otra_empresa() {
            recepcionExiste(GoodsReceiptMother.enBorrador());
            when(branchQueryPort.findById(GoodsReceiptMother.OTRA_SEDE.id(),
                    GoodsReceiptMother.COMPANY_ID))
                    .thenReturn(Optional.of(GoodsReceiptMother.OTRA_SEDE));
            when(supplierQueryPort.findById(GoodsReceiptMother.OTRO_PROVEEDOR.id(),
                    GoodsReceiptMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(GoodsReceiptMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Supplier not found: " + GoodsReceiptMother.OTRO_PROVEEDOR.id());

            verify(repository, never()).save(any());
            verifyNoInteractions(productQueryPort);
        }

        @Test
        @DisplayName("no escribe nada si un producto de las lineas es de otra empresa")
        void producto_de_otra_empresa() {
            recepcionExiste(GoodsReceiptMother.enBorrador());
            nuevasReferenciasExisten();
            when(productQueryPort.findById(GoodsReceiptMother.JERINGA.id(),
                    GoodsReceiptMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(GoodsReceiptMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product not found: " + GoodsReceiptMother.JERINGA.id());

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Guarda de estado")
    class GuardaDeEstado {

        @ParameterizedTest(name = "estado {0}")
        @EnumSource(value = GoodsReceiptStatus.class, names = "DRAFT", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("no escribe nada si la recepcion ya salio de DRAFT")
        void rechaza_editar_fuera_de_borrador(GoodsReceiptStatus status) {
            recepcionExiste(GoodsReceiptMother.conEstado(status));
            nuevasReferenciasExisten();
            productoExiste();

            assertThatThrownBy(() -> service.execute(GoodsReceiptMother.comandoActualizar()))
                    .isInstanceOf(InvalidGoodsReceiptStatusTransitionException.class)
                    .hasMessageContaining("only be edited while in DRAFT");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Validacion de las lineas")
    class Validaciones {

        @Test
        @DisplayName("no escribe nada si el comando llega sin lista de lineas")
        void sin_lista_de_lineas() {
            recepcionExiste(GoodsReceiptMother.enBorrador());
            nuevasReferenciasExisten();
            List<GoodsReceiptLineCommand> sinLineas = null;

            assertThatThrownBy(
                    () -> service.execute(GoodsReceiptMother.comandoActualizar(sinLineas)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one line is required");

            verify(repository, never()).save(any());
            verifyNoInteractions(productQueryPort);
        }

        @Test
        @DisplayName("no escribe nada si la lista de lineas viene vacia")
        void lista_de_lineas_vacia() {
            recepcionExiste(GoodsReceiptMother.enBorrador());
            nuevasReferenciasExisten();

            assertThatThrownBy(
                    () -> service.execute(GoodsReceiptMother.comandoActualizar(List.of())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one line is required");

            verify(repository, never()).save(any());
            verifyNoInteractions(productQueryPort);
        }

        @Test
        @DisplayName("no escribe nada si una linea trae costo negativo")
        void costo_negativo() {
            recepcionExiste(GoodsReceiptMother.enBorrador());
            nuevasReferenciasExisten();
            productoExiste();

            assertThatThrownBy(() -> service.execute(GoodsReceiptMother.comandoActualizar(List
                    .of(new GoodsReceiptLineCommand(GoodsReceiptMother.JERINGA.id(), null, "LOTE-B",
                            GoodsReceiptMother.VENCIMIENTO, 4, new BigDecimal("-1.00"))))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unitCost cannot be negative");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Resultado")
    class Resultado {

        @Test
        @DisplayName("devuelve el DTO de lo que quedo persistido")
        void devuelve_el_dto_persistido() {
            recepcionExiste(GoodsReceiptMother.enBorrador());
            nuevasReferenciasExisten();
            productoExiste();
            when(repository.save(any()))
                    .thenReturn(GoodsReceiptMother.conDosLineas(GoodsReceiptStatus.DRAFT));

            GoodsReceiptDto dto = service.execute(GoodsReceiptMother.comandoActualizar());

            assertThat(dto.id()).isEqualTo(GoodsReceiptMother.RECEIPT_ID);
            assertThat(dto.lines()).hasSize(2);
            assertThat(dto.version()).isEqualTo(3L);
        }

        @Test
        @DisplayName("las lineas construidas conservan lote, vencimiento, cantidad y costo")
        void las_lineas_conservan_los_datos_del_comando() {
            recepcionExiste(GoodsReceiptMother.enBorrador());
            nuevasReferenciasExisten();
            productoExiste();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(GoodsReceiptMother.comandoActualizar());

            verify(repository).save(receiptCaptor.capture());
            GoodsReceiptLine linea = receiptCaptor.getValue().getLines().get(0);
            assertThat(linea.getLotNumber()).isEqualTo("LOTE-B");
            assertThat(linea.getExpireDate()).isEqualTo(GoodsReceiptMother.VENCIMIENTO);
            assertThat(linea.getQuantityReceived()).isEqualTo(4);
            assertThat(linea.getUnitCost()).isEqualByComparingTo("3.00");
            assertThat(linea.getPurchaseOrderLineId()).isEqualTo(900L);
        }
    }
}
