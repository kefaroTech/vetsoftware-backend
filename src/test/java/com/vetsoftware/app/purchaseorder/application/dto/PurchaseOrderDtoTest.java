package com.vetsoftware.app.purchaseorder.application.dto;

import com.vetsoftware.app.shared.pagination.PageResult;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.purchaseorder.domain.PurchaseOrder;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderLine;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderStatus;
import com.vetsoftware.app.purchaseorder.testsupport.PurchaseOrderMother;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DTOs de purchaseorder — proyeccion campo por campo")
class PurchaseOrderDtoTest {

    @Nested
    @DisplayName("PurchaseOrderDto.from")
    class DesdeElAgregado {

        @Test
        @DisplayName("copia cabecera, refs, auditoria y estado del agregado")
        void copia_cabecera_refs_y_auditoria() {
            PurchaseOrder order = new PurchaseOrder(3L, PurchaseOrderMother.CLINICA,
                    PurchaseOrderMother.SEDE_NORTE, PurchaseOrderMother.PROVEEDOR,
                    PurchaseOrderStatus.PARTIALLY_RECEIVED, PurchaseOrderMother.FECHA_ORDEN,
                    PurchaseOrderMother.FECHA_ESPERADA, "urgente",
                    List.of(PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10, 4)),
                    PurchaseOrderMother.CREADO, 77L, PurchaseOrderMother.ACTUALIZADO, 88L, 5L,
                    true);

            PurchaseOrderDto dto = PurchaseOrderDto.from(order);

            assertThat(dto.id()).isEqualTo(3L);
            assertThat(dto.company())
                    .isEqualTo(new CompanySummaryDto(9L, "Clinica Veterinaria", "900123456"));
            assertThat(dto.branch()).isEqualTo(new BranchSummaryDto(4L, "Sede Norte"));
            assertThat(dto.supplier())
                    .isEqualTo(new SupplierSummaryDto(7L, "Distribuidora Animal"));
            assertThat(dto.status()).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);
            assertThat(dto.orderDate()).isEqualTo(PurchaseOrderMother.FECHA_ORDEN);
            assertThat(dto.expectedDate()).isEqualTo(PurchaseOrderMother.FECHA_ESPERADA);
            assertThat(dto.notes()).isEqualTo("urgente");
            assertThat(dto.createdDate()).isEqualTo(PurchaseOrderMother.CREADO);
            assertThat(dto.createdBy()).isEqualTo(77L);
            assertThat(dto.updatedDate()).isEqualTo(PurchaseOrderMother.ACTUALIZADO);
            assertThat(dto.updatedBy()).isEqualTo(88L);
            assertThat(dto.version()).isEqualTo(5L);
            assertThat(dto.enabled()).isTrue();
        }

        @Test
        @DisplayName("proyecta una linea por cada linea del agregado, en orden")
        void proyecta_una_linea_por_cada_linea() {
            PurchaseOrder order = PurchaseOrderMother.emitidaConDosLineas();

            PurchaseOrderDto dto = PurchaseOrderDto.from(order);

            assertThat(dto.lines()).extracting(l -> l.product().id()).containsExactly(11L, 12L);
        }

        @Test
        @DisplayName("propaga el soft-delete y los campos de edicion nulos de una orden nueva")
        void propaga_soft_delete_y_edicion_nula() {
            PurchaseOrderDto dto = PurchaseOrderDto.from(PurchaseOrderMother.pausada());

            assertThat(dto.enabled()).isFalse();
            assertThat(dto.notes()).isNull();
            assertThat(dto.updatedBy()).isEqualTo(PurchaseOrderMother.ACTOR_ID);
        }
    }

    @Nested
    @DisplayName("PurchaseOrderLineDto.from")
    class DesdeLaLinea {

        @Test
        @DisplayName("expone lo pedido, lo recibido y los derivados pendiente y completitud")
        void expone_pedido_recibido_y_derivados() {
            PurchaseOrderLine line = new PurchaseOrderLine(100L, PurchaseOrderMother.VACUNA, 10,
                    new BigDecimal("15000.00"), 4);

            PurchaseOrderLineDto dto = PurchaseOrderLineDto.from(line);

            assertThat(dto.id()).isEqualTo(100L);
            assertThat(dto.product())
                    .isEqualTo(new ProductSummaryDto(11L, "Vacuna Triple", "VAC-001"));
            assertThat(dto.quantityOrdered()).isEqualTo(10);
            assertThat(dto.unitCost()).isEqualByComparingTo("15000.00");
            assertThat(dto.quantityReceived()).isEqualTo(4);
            assertThat(dto.pendingQuantity()).isEqualTo(6);
            assertThat(dto.fullyReceived()).isFalse();
        }

        @Test
        @DisplayName("marca fullyReceived cuando ya no queda pendiente")
        void marca_fully_received_sin_pendiente() {
            PurchaseOrderLineDto dto = PurchaseOrderLineDto
                    .from(PurchaseOrderMother.linea(100L, PurchaseOrderMother.JERINGA, 6, 6));

            assertThat(dto.fullyReceived()).isTrue();
            assertThat(dto.pendingQuantity()).isZero();
        }

        @Test
        @DisplayName("una linea sin persistir viaja con id nulo")
        void linea_sin_persistir_viaja_con_id_nulo() {
            PurchaseOrderLineDto dto = PurchaseOrderLineDto.from(PurchaseOrderMother.lineaNueva());

            assertThat(dto.id()).isNull();
        }
    }

    @Nested
    @DisplayName("Companion summaries")
    class Summaries {

        @Test
        @DisplayName("CompanySummaryDto copia id, nombre e identificador")
        void company_summary_copia_los_tres_campos() {
            assertThat(CompanySummaryDto.from(PurchaseOrderMother.CLINICA))
                    .isEqualTo(new CompanySummaryDto(9L, "Clinica Veterinaria", "900123456"));
        }

        @Test
        @DisplayName("BranchSummaryDto copia id y nombre")
        void branch_summary_copia_id_y_nombre() {
            assertThat(BranchSummaryDto.from(PurchaseOrderMother.SEDE_SUR))
                    .isEqualTo(new BranchSummaryDto(5L, "Sede Sur"));
        }

        @Test
        @DisplayName("SupplierSummaryDto copia id y nombre")
        void supplier_summary_copia_id_y_nombre() {
            assertThat(SupplierSummaryDto.from(PurchaseOrderMother.OTRO_PROVEEDOR))
                    .isEqualTo(new SupplierSummaryDto(8L, "Insumos Vet"));
        }

        @Test
        @DisplayName("ProductSummaryDto copia id, nombre y codigo")
        void product_summary_copia_los_tres_campos() {
            assertThat(ProductSummaryDto.from(PurchaseOrderMother.JERINGA))
                    .isEqualTo(new ProductSummaryDto(12L, "Jeringa 5 ml", "JER-005"));
        }
    }

    @Nested
    @DisplayName("PageResult.map")
    class Paginacion {

        @Test
        @DisplayName("transforma el contenido y conserva los metadatos de la pagina")
        void transforma_contenido_y_conserva_metadatos() {
            PageResult<PurchaseOrder> origen = new PageResult<>(
                    List.of(PurchaseOrderMother.borrador()), 2, 20, 41L, 3);

            PageResult<PurchaseOrderDto> mapeado = origen.map(PurchaseOrderDto::from);

            assertThat(mapeado.content()).singleElement().extracting(PurchaseOrderDto::id)
                    .isEqualTo(1L);
            assertThat(mapeado.page()).isEqualTo(2);
            assertThat(mapeado.pageSize()).isEqualTo(20);
            assertThat(mapeado.totalElements()).isEqualTo(41L);
            assertThat(mapeado.totalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("una pagina vacia se mapea a una pagina vacia")
        void pagina_vacia_se_mapea_vacia() {
            PageResult<PurchaseOrder> origen = new PageResult<>(List.of(), 0, 20, 0L, 0);

            assertThat(origen.map(PurchaseOrderDto::from).content()).isEmpty();
        }
    }
}
