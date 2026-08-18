package com.vetsoftware.app.purchaseorder.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrder;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderLine;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderStatus;
import com.vetsoftware.app.purchaseorder.domain.ProductRef;
import com.vetsoftware.app.purchaseorder.testsupport.PurchaseOrderMother;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa: compila, persiste
 * y solo se ve en pantalla.
 *
 * <p>
 * Las entidades JPA de otras features (company, branch, supplier, product) se
 * mockean porque su constructor sin argumentos es {@code protected} y no son
 * instanciables desde este paquete. No tienen logica: son portadores de datos,
 * y mockearlas no oculta comportamiento. {@code PurchaseOrderJpaEntity} y
 * {@code PurchaseOrderLineJpaEntity} viven en este mismo paquete, asi que se
 * construyen de verdad.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseOrderJpaMapper")
class PurchaseOrderJpaMapperTest {

    private final PurchaseOrderJpaMapper mapper = new PurchaseOrderJpaMapper();

    @Mock
    private CompanyJpaEntity companyEntity;
    @Mock
    private BranchJpaEntity branchEntity;
    @Mock
    private SupplierJpaEntity supplierEntity;
    @Mock
    private ProductJpaEntity vacunaEntity;
    @Mock
    private ProductJpaEntity jeringaEntity;

    // No como campo de instancia: los @Mock aun no existen cuando corren los
    // inicializadores de campo (Mockito los inyecta en el beforeEach de la
    // extension).
    private Map<Long, ProductJpaEntity> productEntities() {
        return Map.of(PurchaseOrderMother.VACUNA.id(), vacunaEntity,
                PurchaseOrderMother.JERINGA.id(), jeringaEntity);
    }

    private PurchaseOrderJpaEntity entidadCompleta() {
        PurchaseOrderJpaEntity entity = new PurchaseOrderJpaEntity();
        entity.setId(1L);
        entity.setCompany(companyEntity);
        entity.setBranch(branchEntity);
        entity.setSupplier(supplierEntity);
        entity.setStatus(PurchaseOrderStatus.PLACED);
        entity.setOrderDate(PurchaseOrderMother.FECHA_ORDEN);
        entity.setExpectedDate(PurchaseOrderMother.FECHA_ESPERADA);
        entity.setNotes("Pedido mensual");
        entity.setCreatedDate(PurchaseOrderMother.CREADO);
        entity.setCreatedBy(PurchaseOrderMother.ACTOR_ID);
        entity.setVersion(0L);
        entity.setEnabled(true);

        PurchaseOrderLineJpaEntity line = new PurchaseOrderLineJpaEntity();
        line.setId(100L);
        line.setProduct(vacunaEntity);
        line.setQuantityOrdered(10);
        line.setUnitCost(new BigDecimal("15000.00"));
        line.setQuantityReceived(0);
        entity.setLines(List.of(line));
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            PurchaseOrder order = PurchaseOrderMother.emitida();

            PurchaseOrderJpaEntity entity = mapper.toJpa(order, companyEntity, branchEntity,
                    supplierEntity, productEntities()::get);

            assertThat(entity.getId()).isEqualTo(1L);
            assertThat(entity.getStatus()).isEqualTo(PurchaseOrderStatus.PLACED);
            assertThat(entity.getOrderDate()).isEqualTo(PurchaseOrderMother.FECHA_ORDEN);
            assertThat(entity.getExpectedDate()).isEqualTo(PurchaseOrderMother.FECHA_ESPERADA);
            assertThat(entity.getNotes()).isEqualTo("Pedido mensual");
            assertThat(entity.getCreatedDate()).isEqualTo(PurchaseOrderMother.CREADO);
            assertThat(entity.getCreatedBy()).isEqualTo(PurchaseOrderMother.ACTOR_ID);
            assertThat(entity.getVersion()).isEqualTo(0L);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha cada asociacion en su slot")
        void engancha_cada_asociacion_en_su_slot() {
            PurchaseOrderJpaEntity entity = mapper.toJpa(PurchaseOrderMother.emitida(),
                    companyEntity, branchEntity, supplierEntity, productEntities()::get);

            assertThat(entity.getCompany()).isSameAs(companyEntity);
            assertThat(entity.getBranch()).isSameAs(branchEntity);
            assertThat(entity.getSupplier()).isSameAs(supplierEntity);
        }

        @Test
        @DisplayName("mapea cada linea resolviendo su producto por id, en el mismo orden")
        void mapea_cada_linea_resolviendo_su_producto_por_id() {
            PurchaseOrder order = PurchaseOrderMother.emitidaConDosLineas();

            PurchaseOrderJpaEntity entity = mapper.toJpa(order, companyEntity, branchEntity,
                    supplierEntity, productEntities()::get);

            assertThat(entity.getLines()).hasSize(2);
            assertThat(entity.getLines()).extracting(PurchaseOrderLineJpaEntity::getProduct)
                    .containsExactly(vacunaEntity, jeringaEntity);
            assertThat(entity.getLines()).extracting(PurchaseOrderLineJpaEntity::getId)
                    .containsExactly(100L, 200L);
            assertThat(entity.getLines()).extracting(PurchaseOrderLineJpaEntity::getQuantityOrdered)
                    .containsExactly(10, 4);
            assertThat(entity.getLines())
                    .extracting(PurchaseOrderLineJpaEntity::getQuantityReceived)
                    .containsExactly(0, 0);
            assertThat(entity.getLines().get(0).getUnitCost()).isEqualByComparingTo("15000.00");
        }

        @Test
        @DisplayName("una linea nueva sin id todavia viaja con id null a la entidad")
        void una_linea_nueva_sin_id_viaja_con_id_null() {
            PurchaseOrderLine lineaNueva = PurchaseOrderMother.lineaNueva();
            PurchaseOrder order = new PurchaseOrder(null, PurchaseOrderMother.CLINICA,
                    PurchaseOrderMother.SEDE_NORTE, PurchaseOrderMother.PROVEEDOR,
                    PurchaseOrderStatus.DRAFT, PurchaseOrderMother.FECHA_ORDEN, null, null,
                    List.of(lineaNueva), PurchaseOrderMother.CREADO, PurchaseOrderMother.ACTOR_ID,
                    null, null, null, true);

            PurchaseOrderJpaEntity entity = mapper.toJpa(order, companyEntity, branchEntity,
                    supplierEntity, productEntities()::get);

            assertThat(entity.getId()).isNull();
            assertThat(entity.getVersion()).isNull();
            assertThat(entity.getLines()).hasSize(1);
            assertThat(entity.getLines().get(0).getId()).isNull();
            assertThat(entity.getLines().get(0).getProduct()).isSameAs(vacunaEntity);
        }

        @Test
        @DisplayName("sin fecha esperada ni notas las columnas opcionales quedan nulas")
        void sin_fecha_esperada_ni_notas_las_columnas_opcionales_quedan_nulas() {
            PurchaseOrder pelada = new PurchaseOrder(null, PurchaseOrderMother.CLINICA,
                    PurchaseOrderMother.SEDE_NORTE, PurchaseOrderMother.PROVEEDOR,
                    PurchaseOrderStatus.DRAFT, PurchaseOrderMother.FECHA_ORDEN, null, null,
                    List.of(PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10, 0)),
                    PurchaseOrderMother.CREADO, PurchaseOrderMother.ACTOR_ID, null, null, null,
                    true);

            PurchaseOrderJpaEntity entity = mapper.toJpa(pelada, companyEntity, branchEntity,
                    supplierEntity, productEntities()::get);

            assertThat(entity.getExpectedDate()).isNull();
            assertThat(entity.getNotes()).isNull();
            assertThat(entity.getUpdatedDate()).isNull();
            assertThat(entity.getUpdatedBy()).isNull();
        }

        @Test
        @DisplayName("una orden pausada (enabled=false) tambien copia sus campos de auditoria")
        void una_orden_pausada_copia_sus_campos_de_auditoria() {
            PurchaseOrderJpaEntity entity = mapper.toJpa(PurchaseOrderMother.pausada(),
                    companyEntity, branchEntity, supplierEntity, productEntities()::get);

            assertThat(entity.isEnabled()).isFalse();
            assertThat(entity.getUpdatedDate()).isEqualTo(PurchaseOrderMother.ACTUALIZADO);
            assertThat(entity.getUpdatedBy()).isEqualTo(PurchaseOrderMother.ACTOR_ID);
            assertThat(entity.getVersion()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("toDomain con refs precargados — camino de escritura")
    class ToDomainConRefs {

        @Test
        @DisplayName("reconstruye el agregado sin tocar las asociaciones JPA")
        void reconstruye_el_agregado_sin_tocar_las_asociaciones() {
            // Este overload existe para no inicializar los proxies de getReferenceById: si
            // leyera le.getProduct().getName(), Hibernate lanzaria un SELECT extra por
            // save.
            // Por eso aqui solo se stubea getId() de los productos, nunca su nombre/codigo.
            when(vacunaEntity.getId()).thenReturn(PurchaseOrderMother.VACUNA.id());
            when(jeringaEntity.getId()).thenReturn(PurchaseOrderMother.JERINGA.id());
            PurchaseOrderJpaEntity entity = entidadCompleta();
            PurchaseOrderLineJpaEntity segundaLinea = new PurchaseOrderLineJpaEntity();
            segundaLinea.setId(200L);
            segundaLinea.setProduct(jeringaEntity);
            segundaLinea.setQuantityOrdered(4);
            segundaLinea.setUnitCost(new BigDecimal("15000.00"));
            segundaLinea.setQuantityReceived(1);
            entity.setLines(List.of(entity.getLines().get(0), segundaLinea));
            Map<Long, ProductRef> productRefs = Map.of(PurchaseOrderMother.VACUNA.id(),
                    PurchaseOrderMother.VACUNA, PurchaseOrderMother.JERINGA.id(),
                    PurchaseOrderMother.JERINGA);

            PurchaseOrder domain = mapper.toDomain(entity, PurchaseOrderMother.CLINICA,
                    PurchaseOrderMother.SEDE_NORTE, PurchaseOrderMother.PROVEEDOR, productRefs);

            assertThat(domain.getId()).isEqualTo(1L);
            assertThat(domain.getCompany()).isEqualTo(PurchaseOrderMother.CLINICA);
            assertThat(domain.getBranch()).isEqualTo(PurchaseOrderMother.SEDE_NORTE);
            assertThat(domain.getSupplier()).isEqualTo(PurchaseOrderMother.PROVEEDOR);
            assertThat(domain.getStatus()).isEqualTo(PurchaseOrderStatus.PLACED);
            assertThat(domain.getLines()).extracting(PurchaseOrderLine::getProduct)
                    .containsExactly(PurchaseOrderMother.VACUNA, PurchaseOrderMother.JERINGA);
            assertThat(domain.getLines()).extracting(PurchaseOrderLine::getQuantityReceived)
                    .containsExactly(0, 1);
        }

        @Test
        @DisplayName("la ida y vuelta dominio → entidad → dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            PurchaseOrder original = PurchaseOrderMother.emitidaConDosLineas();
            when(vacunaEntity.getId()).thenReturn(PurchaseOrderMother.VACUNA.id());
            when(jeringaEntity.getId()).thenReturn(PurchaseOrderMother.JERINGA.id());
            Map<Long, ProductRef> productRefs = Map.of(PurchaseOrderMother.VACUNA.id(),
                    PurchaseOrderMother.VACUNA, PurchaseOrderMother.JERINGA.id(),
                    PurchaseOrderMother.JERINGA);

            PurchaseOrderJpaEntity entity = mapper.toJpa(original, companyEntity, branchEntity,
                    supplierEntity, productEntities()::get);
            PurchaseOrder vuelta = mapper.toDomain(entity, PurchaseOrderMother.CLINICA,
                    PurchaseOrderMother.SEDE_NORTE, PurchaseOrderMother.PROVEEDOR, productRefs);

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde las asociaciones — camino de lectura")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("construye cada companion VO desde su propia asociacion")
        void construye_cada_companion_vo_desde_su_asociacion() {
            when(companyEntity.getId()).thenReturn(PurchaseOrderMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(PurchaseOrderMother.CLINICA.name());
            when(companyEntity.getIdentifier())
                    .thenReturn(PurchaseOrderMother.CLINICA.identifier());
            when(branchEntity.getId()).thenReturn(PurchaseOrderMother.SEDE_NORTE.id());
            when(branchEntity.getName()).thenReturn(PurchaseOrderMother.SEDE_NORTE.name());
            when(supplierEntity.getId()).thenReturn(PurchaseOrderMother.PROVEEDOR.id());
            when(supplierEntity.getName()).thenReturn(PurchaseOrderMother.PROVEEDOR.name());
            when(vacunaEntity.getId()).thenReturn(PurchaseOrderMother.VACUNA.id());
            when(vacunaEntity.getName()).thenReturn(PurchaseOrderMother.VACUNA.name());
            when(vacunaEntity.getCode()).thenReturn(PurchaseOrderMother.VACUNA.code());

            PurchaseOrder domain = mapper.toDomain(entidadCompleta());

            assertThat(domain.getCompany()).isEqualTo(PurchaseOrderMother.CLINICA);
            assertThat(domain.getBranch()).isEqualTo(PurchaseOrderMother.SEDE_NORTE);
            assertThat(domain.getSupplier()).isEqualTo(PurchaseOrderMother.PROVEEDOR);
            assertThat(domain.getLines()).hasSize(1);
            assertThat(domain.getLines().get(0).getProduct()).isEqualTo(PurchaseOrderMother.VACUNA);
            assertThat(domain.getLines().get(0).getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("dos lineas leidas conservan cada una su propio producto")
        void dos_lineas_leidas_conservan_cada_una_su_propio_producto() {
            when(companyEntity.getId()).thenReturn(PurchaseOrderMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(PurchaseOrderMother.CLINICA.name());
            when(companyEntity.getIdentifier())
                    .thenReturn(PurchaseOrderMother.CLINICA.identifier());
            when(branchEntity.getId()).thenReturn(PurchaseOrderMother.SEDE_NORTE.id());
            when(branchEntity.getName()).thenReturn(PurchaseOrderMother.SEDE_NORTE.name());
            when(supplierEntity.getId()).thenReturn(PurchaseOrderMother.PROVEEDOR.id());
            when(supplierEntity.getName()).thenReturn(PurchaseOrderMother.PROVEEDOR.name());
            when(vacunaEntity.getId()).thenReturn(PurchaseOrderMother.VACUNA.id());
            when(vacunaEntity.getName()).thenReturn(PurchaseOrderMother.VACUNA.name());
            when(vacunaEntity.getCode()).thenReturn(PurchaseOrderMother.VACUNA.code());
            when(jeringaEntity.getId()).thenReturn(PurchaseOrderMother.JERINGA.id());
            when(jeringaEntity.getName()).thenReturn(PurchaseOrderMother.JERINGA.name());
            when(jeringaEntity.getCode()).thenReturn(PurchaseOrderMother.JERINGA.code());
            PurchaseOrderJpaEntity entity = entidadCompleta();
            PurchaseOrderLineJpaEntity segundaLinea = new PurchaseOrderLineJpaEntity();
            segundaLinea.setId(200L);
            segundaLinea.setProduct(jeringaEntity);
            segundaLinea.setQuantityOrdered(4);
            segundaLinea.setUnitCost(new BigDecimal("15000.00"));
            segundaLinea.setQuantityReceived(0);
            entity.setLines(List.of(entity.getLines().get(0), segundaLinea));

            PurchaseOrder domain = mapper.toDomain(entity);

            assertThat(domain.getLines()).extracting(PurchaseOrderLine::getProduct)
                    .containsExactly(PurchaseOrderMother.VACUNA, PurchaseOrderMother.JERINGA);
        }
    }
}
