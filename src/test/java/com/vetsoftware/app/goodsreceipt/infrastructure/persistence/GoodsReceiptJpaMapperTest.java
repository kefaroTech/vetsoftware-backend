package com.vetsoftware.app.goodsreceipt.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceipt;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptLine;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptStatus;
import com.vetsoftware.app.goodsreceipt.testsupport.GoodsReceiptMother;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaEntity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link CompanyJpaEntity}, {@link BranchJpaEntity}, {@link SupplierJpaEntity}
 * y {@link ProductJpaEntity} pertenecen a otras features y su constructor es
 * {@code protected}: desde aqui se mockean como filas, no como entidades de
 * dominio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GoodsReceiptJpaMapper — ida y vuelta dominio <-> entidad")
class GoodsReceiptJpaMapperTest {

    private final GoodsReceiptJpaMapper mapper = new GoodsReceiptJpaMapper();

    @Mock
    private CompanyJpaEntity companyEntity;
    @Mock
    private BranchJpaEntity branchEntity;
    @Mock
    private SupplierJpaEntity supplierEntity;
    @Mock
    private ProductJpaEntity vacunaEntity;

    private Map<Long, ProductJpaEntity> productsById() {
        return Map.of(GoodsReceiptMother.VACUNA.id(), vacunaEntity);
    }

    @Nested
    @DisplayName("toJpa — write path")
    class ADominioPersistente {

        @Test
        @DisplayName("copia cada campo de la cabecera y engancha las tres asociaciones recibidas")
        void copia_cada_campo_y_engancha_las_asociaciones() {
            GoodsReceipt receipt = GoodsReceiptMother.enBorrador();

            GoodsReceiptJpaEntity entity = mapper.toJpa(receipt, companyEntity, branchEntity,
                    supplierEntity, productsById());

            assertThat(entity.getId()).isEqualTo(receipt.getId());
            assertThat(entity.getCompany()).isSameAs(companyEntity);
            assertThat(entity.getBranch()).isSameAs(branchEntity);
            assertThat(entity.getSupplier()).isSameAs(supplierEntity);
            assertThat(entity.getPurchaseOrderId()).isEqualTo(receipt.getPurchaseOrderId());
            assertThat(entity.getReceiptDate()).isEqualTo(receipt.getReceiptDate());
            assertThat(entity.getSupplierInvoiceNumber())
                    .isEqualTo(receipt.getSupplierInvoiceNumber());
            assertThat(entity.getNotes()).isEqualTo(receipt.getNotes());
            assertThat(entity.getStatus()).isEqualTo(receipt.getStatus());
            assertThat(entity.getCreatedDate()).isEqualTo(receipt.getCreatedDate());
            assertThat(entity.getCreatedBy()).isEqualTo(receipt.getCreatedBy());
            assertThat(entity.getVersion()).isEqualTo(receipt.getVersion());
            assertThat(entity.isEnabled()).isEqualTo(receipt.isEnabled());
        }

        @Test
        @DisplayName("copia cada linea y mantiene el back-reference hacia la cabecera")
        void copia_cada_linea_y_mantiene_el_back_reference() {
            GoodsReceipt receipt = GoodsReceiptMother.enBorrador();
            GoodsReceiptLine lineaOriginal = receipt.getLines().get(0);

            GoodsReceiptJpaEntity entity = mapper.toJpa(receipt, companyEntity, branchEntity,
                    supplierEntity, productsById());

            assertThat(entity.getLines()).hasSize(1);
            GoodsReceiptLineJpaEntity lineEntity = entity.getLines().get(0);
            assertThat(lineEntity.getGoodsReceipt()).isSameAs(entity);
            assertThat(lineEntity.getProduct()).isSameAs(vacunaEntity);
            assertThat(lineEntity.getPurchaseOrderLineId())
                    .isEqualTo(lineaOriginal.getPurchaseOrderLineId());
            assertThat(lineEntity.getLotNumber()).isEqualTo(lineaOriginal.getLotNumber());
            assertThat(lineEntity.getExpireDate()).isEqualTo(lineaOriginal.getExpireDate());
            assertThat(lineEntity.getQuantityReceived())
                    .isEqualTo(lineaOriginal.getQuantityReceived());
            assertThat(lineEntity.getUnitCost()).isEqualByComparingTo(lineaOriginal.getUnitCost());
        }

        @Test
        @DisplayName("una recepcion nueva viaja sin id para que lo genere la base")
        void una_recepcion_nueva_viaja_sin_id() {
            GoodsReceipt nueva = GoodsReceipt.create(GoodsReceiptMother.CLINICA,
                    GoodsReceiptMother.SEDE, GoodsReceiptMother.PROVEEDOR, null,
                    GoodsReceiptMother.FECHA_RECEPCION, "FV-9999", "Nueva",
                    List.of(GoodsReceiptMother.linea()), GoodsReceiptMother.ACTOR_ID);

            GoodsReceiptJpaEntity entity = mapper.toJpa(nueva, companyEntity, branchEntity,
                    supplierEntity, productsById());

            assertThat(entity.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain(entity) — read path, resuelve las tres refs desde las asociaciones")
    class ADominioDesdeLaEntidad {

        @Test
        @DisplayName("mapea la cabecera y sus lineas con las refs resueltas por las asociaciones")
        void mapea_la_cabecera_y_sus_lineas() {
            when(companyEntity.getId()).thenReturn(GoodsReceiptMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(GoodsReceiptMother.CLINICA.name());
            when(companyEntity.getIdentifier()).thenReturn(GoodsReceiptMother.CLINICA.identifier());
            when(branchEntity.getId()).thenReturn(GoodsReceiptMother.SEDE.id());
            when(branchEntity.getName()).thenReturn(GoodsReceiptMother.SEDE.name());
            when(supplierEntity.getId()).thenReturn(GoodsReceiptMother.PROVEEDOR.id());
            when(supplierEntity.getName()).thenReturn(GoodsReceiptMother.PROVEEDOR.name());
            when(vacunaEntity.getId()).thenReturn(GoodsReceiptMother.VACUNA.id());
            when(vacunaEntity.getName()).thenReturn(GoodsReceiptMother.VACUNA.name());
            when(vacunaEntity.getCode()).thenReturn(GoodsReceiptMother.VACUNA.code());
            GoodsReceipt original = GoodsReceiptMother.enBorrador();
            GoodsReceiptJpaEntity entity = mapper.toJpa(original, companyEntity, branchEntity,
                    supplierEntity, productsById());
            entity.setId(GoodsReceiptMother.RECEIPT_ID);

            GoodsReceipt domain = mapper.toDomain(entity);

            assertThat(domain.getId()).isEqualTo(GoodsReceiptMother.RECEIPT_ID);
            assertThat(domain.getCompany()).isEqualTo(GoodsReceiptMother.CLINICA);
            assertThat(domain.getBranch()).isEqualTo(GoodsReceiptMother.SEDE);
            assertThat(domain.getSupplier()).isEqualTo(GoodsReceiptMother.PROVEEDOR);
            assertThat(domain.getStatus()).isEqualTo(GoodsReceiptStatus.DRAFT);
            assertThat(domain.getLines()).hasSize(1);
            assertThat(domain.getLines().get(0).getProduct()).isEqualTo(GoodsReceiptMother.VACUNA);
        }
    }

    @Nested
    @DisplayName("toDomainReusingRefs(saved, original) — write-return path, reusa los refs precargados")
    class ADominioReusandoLosRefs {

        @Test
        @DisplayName("toma el id de la cabecera y de cada linea del guardado, y reusa los refs del original")
        void toma_los_ids_del_guardado_y_reusa_los_refs_del_original() {
            GoodsReceipt original = GoodsReceiptMother.enBorrador();
            GoodsReceiptJpaEntity saved = mapper.toJpa(original, companyEntity, branchEntity,
                    supplierEntity, productsById());
            saved.setId(GoodsReceiptMother.RECEIPT_ID);
            saved.getLines().get(0).setId(999L);

            GoodsReceipt reconstruido = mapper.toDomainReusingRefs(saved, original);

            assertThat(reconstruido.getId()).isEqualTo(GoodsReceiptMother.RECEIPT_ID);
            assertThat(reconstruido.getCompany()).isSameAs(original.getCompany());
            assertThat(reconstruido.getBranch()).isSameAs(original.getBranch());
            assertThat(reconstruido.getSupplier()).isSameAs(original.getSupplier());
            assertThat(reconstruido.getLines()).hasSize(1);
            assertThat(reconstruido.getLines().get(0).getId()).isEqualTo(999L);
            assertThat(reconstruido.getLines().get(0).getProduct())
                    .isSameAs(original.getLines().get(0).getProduct());
        }

        @Test
        @DisplayName("con varias lineas, cada una toma el id de su posicion correspondiente en el guardado")
        void con_varias_lineas_cada_una_toma_su_id_por_posicion() {
            GoodsReceipt original = GoodsReceiptMother.conDosLineas(GoodsReceiptStatus.DRAFT);
            Map<Long, ProductJpaEntity> products = Map.of(GoodsReceiptMother.VACUNA.id(),
                    vacunaEntity, GoodsReceiptMother.JERINGA.id(),
                    org.mockito.Mockito.mock(ProductJpaEntity.class));
            GoodsReceiptJpaEntity saved = mapper.toJpa(original, companyEntity, branchEntity,
                    supplierEntity, products);
            saved.setId(GoodsReceiptMother.RECEIPT_ID);
            saved.getLines().get(0).setId(701L);
            saved.getLines().get(1).setId(702L);

            GoodsReceipt reconstruido = mapper.toDomainReusingRefs(saved, original);

            assertThat(reconstruido.getLines()).extracting(GoodsReceiptLine::getId)
                    .containsExactly(701L, 702L);
        }
    }
}
