package com.vetsoftware.app.goodsreceipt.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.goodsreceipt.domain.GoodsReceipt;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptLine;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptStatus;
import com.vetsoftware.app.goodsreceipt.testsupport.GoodsReceiptMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El {@code from(...)} es el punto donde un campo cruzado no rompe nada:
 * compila y solo se ve en la respuesta HTTP. Por eso se afirma campo por campo.
 */
@DisplayName("DTOs de salida de goodsreceipt — proyeccion campo por campo")
class GoodsReceiptDtoTest {

    @Nested
    @DisplayName("GoodsReceiptDto.from")
    class Cabecera {

        @Test
        @DisplayName("copia los dieciseis campos del agregado sin perder ninguno")
        void copia_todos_los_campos() {
            GoodsReceipt receipt = GoodsReceiptMother.conDosLineas(GoodsReceiptStatus.CONFIRMED);

            GoodsReceiptDto dto = GoodsReceiptDto.from(receipt);

            assertThat(dto.id()).isEqualTo(GoodsReceiptMother.RECEIPT_ID);
            assertThat(dto.company().id()).isEqualTo(GoodsReceiptMother.COMPANY_ID);
            assertThat(dto.company().name()).isEqualTo("Clinica Norte");
            assertThat(dto.company().identifier()).isEqualTo("NIT-900123");
            assertThat(dto.branch().id()).isEqualTo(GoodsReceiptMother.SEDE.id());
            assertThat(dto.branch().name()).isEqualTo("Sede Norte");
            assertThat(dto.supplier().id()).isEqualTo(GoodsReceiptMother.PROVEEDOR.id());
            assertThat(dto.supplier().name()).isEqualTo("Distribuidora Vet");
            assertThat(dto.purchaseOrderId()).isNull();
            assertThat(dto.receiptDate()).isEqualTo(GoodsReceiptMother.FECHA_RECEPCION);
            assertThat(dto.supplierInvoiceNumber()).isEqualTo("FV-1001");
            assertThat(dto.notes()).isEqualTo("Entrega parcial");
            assertThat(dto.status()).isEqualTo(GoodsReceiptStatus.CONFIRMED);
            assertThat(dto.lines()).hasSize(2);
            assertThat(dto.createdDate()).isEqualTo(GoodsReceiptMother.CREADO);
            assertThat(dto.createdBy()).isEqualTo(GoodsReceiptMother.ACTOR_ID);
            assertThat(dto.updatedDate()).isEqualTo(GoodsReceiptMother.ACTUALIZADO);
            assertThat(dto.updatedBy()).isEqualTo(GoodsReceiptMother.ACTOR_ID);
            assertThat(dto.version()).isEqualTo(3L);
            assertThat(dto.enabled()).isTrue();
        }

        @Test
        @DisplayName("propaga el id de la orden de compra cuando la recepcion viene de una")
        void propaga_la_orden_de_compra() {
            GoodsReceiptDto dto = GoodsReceiptDto
                    .from(GoodsReceiptMother.conOrdenDeCompra(GoodsReceiptStatus.DRAFT));

            assertThat(dto.purchaseOrderId()).isEqualTo(GoodsReceiptMother.PURCHASE_ORDER_ID);
        }

        @Test
        @DisplayName("respeta el orden de las lineas del agregado")
        void respeta_el_orden_de_las_lineas() {
            GoodsReceiptDto dto = GoodsReceiptDto
                    .from(GoodsReceiptMother.conOrdenDeCompra(GoodsReceiptStatus.DRAFT));

            assertThat(dto.lines()).extracting(l -> l.product().code()).containsExactly("P-022",
                    "P-021");
        }
    }

    @Nested
    @DisplayName("GoodsReceiptLineDto.from")
    class Linea {

        @Test
        @DisplayName("copia los siete campos de la linea de dominio")
        void copia_todos_los_campos() {
            GoodsReceiptLine line = GoodsReceiptMother.lineaDeOrden(900L);

            GoodsReceiptLineDto dto = GoodsReceiptLineDto.from(line);

            assertThat(dto.id()).isEqualTo(line.getId());
            assertThat(dto.product().id()).isEqualTo(GoodsReceiptMother.JERINGA.id());
            assertThat(dto.product().name()).isEqualTo("Jeringa 5 ml");
            assertThat(dto.product().code()).isEqualTo("P-022");
            assertThat(dto.purchaseOrderLineId()).isEqualTo(900L);
            assertThat(dto.lotNumber()).isEqualTo("LOTE-B");
            assertThat(dto.expireDate()).isEqualTo(GoodsReceiptMother.VENCIMIENTO);
            assertThat(dto.quantityReceived()).isEqualTo(4);
            assertThat(dto.unitCost()).isEqualByComparingTo("3.00");
        }

        @Test
        @DisplayName("deja en null los campos opcionales de una recepcion directa")
        void deja_en_null_los_opcionales() {
            GoodsReceiptLineDto dto = GoodsReceiptLineDto.from(GoodsReceiptMother.linea());

            assertThat(dto.purchaseOrderLineId()).isNull();
        }
    }

    @Nested
    @DisplayName("Resumenes de referencias")
    class Resumenes {

        @Test
        @DisplayName("CompanySummaryDto copia id, nombre e identificador")
        void company_summary() {
            CompanySummaryDto dto = CompanySummaryDto.from(GoodsReceiptMother.CLINICA);

            assertThat(dto.id()).isEqualTo(GoodsReceiptMother.COMPANY_ID);
            assertThat(dto.name()).isEqualTo("Clinica Norte");
            assertThat(dto.identifier()).isEqualTo("NIT-900123");
        }

        @Test
        @DisplayName("BranchSummaryDto copia id y nombre")
        void branch_summary() {
            BranchSummaryDto dto = BranchSummaryDto.from(GoodsReceiptMother.SEDE);

            assertThat(dto.id()).isEqualTo(GoodsReceiptMother.SEDE.id());
            assertThat(dto.name()).isEqualTo("Sede Norte");
        }

        @Test
        @DisplayName("SupplierSummaryDto copia id y nombre")
        void supplier_summary() {
            SupplierSummaryDto dto = SupplierSummaryDto.from(GoodsReceiptMother.PROVEEDOR);

            assertThat(dto.id()).isEqualTo(GoodsReceiptMother.PROVEEDOR.id());
            assertThat(dto.name()).isEqualTo("Distribuidora Vet");
        }

        @Test
        @DisplayName("ProductSummaryDto copia id, nombre y codigo")
        void product_summary() {
            ProductSummaryDto dto = ProductSummaryDto.from(GoodsReceiptMother.VACUNA);

            assertThat(dto.id()).isEqualTo(GoodsReceiptMother.VACUNA.id());
            assertThat(dto.name()).isEqualTo("Vacuna triple");
            assertThat(dto.code()).isEqualTo("P-021");
        }
    }

    @Nested
    @DisplayName("PageResult")
    class Paginacion {

        @Test
        @DisplayName("map transforma el contenido y conserva los metadatos de la pagina")
        void map_conserva_los_metadatos() {
            PageResult<GoodsReceipt> origen = new PageResult<>(
                    List.of(GoodsReceiptMother.enBorrador()), 2, 20, 41L, 3);

            PageResult<GoodsReceiptDto> resultado = origen.map(GoodsReceiptDto::from);

            assertThat(resultado.content()).singleElement().extracting(GoodsReceiptDto::id)
                    .isEqualTo(GoodsReceiptMother.RECEIPT_ID);
            assertThat(resultado.page()).isEqualTo(2);
            assertThat(resultado.pageSize()).isEqualTo(20);
            assertThat(resultado.totalElements()).isEqualTo(41L);
            assertThat(resultado.totalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("map sobre una pagina vacia devuelve contenido vacio, no null")
        void map_sobre_pagina_vacia() {
            PageResult<GoodsReceipt> origen = new PageResult<>(List.of(), 0, 20, 0L, 0);

            PageResult<GoodsReceiptDto> resultado = origen.map(GoodsReceiptDto::from);

            assertThat(resultado.content()).isEmpty();
            assertThat(resultado.totalElements()).isZero();
        }
    }
}
