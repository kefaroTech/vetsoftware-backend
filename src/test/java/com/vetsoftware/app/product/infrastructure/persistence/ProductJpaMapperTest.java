package com.vetsoftware.app.product.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.product.domain.CompanyRef;
import com.vetsoftware.app.product.domain.Product;
import com.vetsoftware.app.product.domain.ProductCategoryRef;
import com.vetsoftware.app.product.domain.SupplierRef;
import com.vetsoftware.app.product.domain.TaxRef;
import com.vetsoftware.app.product.domain.TaxTreatment;
import com.vetsoftware.app.product.testsupport.ProductMother;
import com.vetsoftware.app.productcategory.infrastructure.persistence.ProductCategoryJpaEntity;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaEntity;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Ida y vuelta dominio &harr; entidad JPA.
 *
 * <p>
 * <b>Por que hay mocks en un test de mapper.</b> Las entidades JPA de OTRAS
 * features ({@code CompanyJpaEntity}, {@code ProductCategoryJpaEntity},
 * {@code TaxJpaEntity}, {@code SupplierJpaEntity}) declaran su constructor sin
 * argumentos como {@code protected} —lo exige Hibernate— y viven en otro
 * paquete, asi que no se pueden instanciar desde aqui. Se sustituyen por dobles
 * que solo responden a los getters que el mapper lee. La entidad propia
 * {@link ProductJpaEntity} SI se construye de verdad: comparte paquete con este
 * test.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductJpaMapper")
class ProductJpaMapperTest {

    private final ProductJpaMapper mapper = new ProductJpaMapper();

    @Mock
    private CompanyJpaEntity companyEntity;
    @Mock
    private ProductCategoryJpaEntity categoryEntity;
    @Mock
    private TaxJpaEntity taxEntity;
    @Mock
    private SupplierJpaEntity supplierEntity;

    private static ProductJpaEntity entidadPoblada() {
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setId(ProductMother.PRODUCT_ID);
        entity.setName("Concentrado adulto");
        entity.setCode("P-001");
        entity.setSalePrice(new BigDecimal("12500.00"));
        entity.setBaseUnitMeasureCode("94");
        entity.setProvider("Proveedor texto");
        entity.setTaxTreatment(TaxTreatment.GRAVADO);
        entity.setNotes("Bulto de 15 kg");
        entity.setCreatedDate(ProductMother.CREADO);
        entity.setUpdatedDate(LocalDateTime.of(2026, 3, 1, 8, 0));
        entity.setUpdatedBy(77L);
        entity.setVersion(4L);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa")
    class ADominioJpa {

        @Test
        @DisplayName("copia los campos planos del producto")
        void copia_los_campos_planos() {
            ProductJpaEntity entity = mapper.toJpa(ProductMother.gravado(), categoryEntity,
                    taxEntity, companyEntity, supplierEntity);

            assertThat(entity.getId()).isEqualTo(ProductMother.PRODUCT_ID);
            assertThat(entity.getName()).isEqualTo("Concentrado adulto");
            assertThat(entity.getCode()).isEqualTo("P-001");
            assertThat(entity.getSalePrice()).isEqualByComparingTo(ProductMother.PRECIO);
            assertThat(entity.getBaseUnitMeasureCode()).isEqualTo("94");
            assertThat(entity.getProvider()).isEqualTo("Proveedor texto");
            assertThat(entity.getTaxTreatment()).isEqualTo(TaxTreatment.GRAVADO);
            assertThat(entity.getNotes()).isEqualTo("Bulto de 15 kg");
            assertThat(entity.getCreatedDate()).isEqualTo(ProductMother.CREADO);
            assertThat(entity.getVersion()).isEqualTo(0L);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha las asociaciones con las entidades que le pasan, sin recrearlas")
        void engancha_las_asociaciones_recibidas() {
            ProductJpaEntity entity = mapper.toJpa(ProductMother.gravado(), categoryEntity,
                    taxEntity, companyEntity, supplierEntity);

            // El repositorio pasa proxies de getReferenceById: el mapper debe
            // reusarlos, no construir entidades nuevas que Hibernate trataria
            // como transitorias.
            assertThat(entity.getProductCategory()).isSameAs(categoryEntity);
            assertThat(entity.getTax()).isSameAs(taxEntity);
            assertThat(entity.getCompany()).isSameAs(companyEntity);
            assertThat(entity.getSupplier()).isSameAs(supplierEntity);
        }

        @Test
        @DisplayName("acepta impuesto y proveedor nulos")
        void acepta_impuesto_y_proveedor_nulos() {
            ProductJpaEntity entity = mapper.toJpa(ProductMother.excluido(), categoryEntity, null,
                    companyEntity, null);

            assertThat(entity.getTax()).isNull();
            assertThat(entity.getSupplier()).isNull();
            assertThat(entity.getTaxTreatment()).isEqualTo(TaxTreatment.EXCLUIDO);
        }

        @Test
        @DisplayName("un producto nuevo viaja sin id, para que la BD lo genere")
        void producto_nuevo_viaja_sin_id() {
            Product nuevo = Product.create("Concentrado", "P-777", BigDecimal.TEN, "94", null, null,
                    TaxTreatment.EXCLUIDO, null, ProductMother.CATEGORIA, null,
                    ProductMother.CLINICA);

            ProductJpaEntity entity = mapper.toJpa(nuevo, categoryEntity, null, companyEntity,
                    null);

            assertThat(entity.getId()).isNull();
            assertThat(entity.getVersion()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain leyendo las asociaciones")
    class ADominio {

        @Test
        @DisplayName("reconstruye el producto con sus cuatro referencias")
        void reconstruye_el_producto_completo() {
            when(categoryEntity.getId()).thenReturn(3L);
            when(categoryEntity.getName()).thenReturn("Alimentos");
            when(taxEntity.getId()).thenReturn(4L);
            when(taxEntity.getName()).thenReturn("IVA 19%");
            when(taxEntity.getPercentage()).thenReturn(new BigDecimal("19.00"));
            when(companyEntity.getId()).thenReturn(ProductMother.COMPANY_ID);
            when(companyEntity.getName()).thenReturn("Clinica Norte");
            when(companyEntity.getIdentifier()).thenReturn("NIT-900");
            when(supplierEntity.getId()).thenReturn(6L);
            when(supplierEntity.getName()).thenReturn("Distribuidora Sur");

            ProductJpaEntity entity = entidadPoblada();
            entity.setProductCategory(categoryEntity);
            entity.setTax(taxEntity);
            entity.setCompany(companyEntity);
            entity.setSupplier(supplierEntity);

            Product product = mapper.toDomain(entity);

            assertThat(product.getProductCategory()).isEqualTo(ProductMother.CATEGORIA);
            assertThat(product.getTax()).isEqualTo(ProductMother.IVA_19);
            assertThat(product.getCompany()).isEqualTo(ProductMother.CLINICA);
            assertThat(product.getSupplier()).isEqualTo(ProductMother.PROVEEDOR);
        }

        @Test
        @DisplayName("deja en null impuesto y proveedor cuando la fila no los tiene")
        void impuesto_y_proveedor_nulos() {
            when(categoryEntity.getId()).thenReturn(3L);
            when(categoryEntity.getName()).thenReturn("Alimentos");
            when(companyEntity.getId()).thenReturn(ProductMother.COMPANY_ID);
            when(companyEntity.getName()).thenReturn("Clinica Norte");
            when(companyEntity.getIdentifier()).thenReturn("NIT-900");

            ProductJpaEntity entity = entidadPoblada();
            entity.setTaxTreatment(TaxTreatment.EXCLUIDO);
            entity.setProductCategory(categoryEntity);
            entity.setCompany(companyEntity);

            Product product = mapper.toDomain(entity);

            assertThat(product.getTax()).isNull();
            assertThat(product.getSupplier()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain con las referencias ya resueltas")
    class ADominioConRefs {

        @Test
        @DisplayName("copia los campos planos de la fila")
        void copia_los_campos_planos() {
            Product product = mapper.toDomain(entidadPoblada(), ProductMother.CATEGORIA,
                    ProductMother.IVA_19, ProductMother.CLINICA, ProductMother.PROVEEDOR);

            assertThat(product.getId()).isEqualTo(ProductMother.PRODUCT_ID);
            assertThat(product.getName()).isEqualTo("Concentrado adulto");
            assertThat(product.getCode()).isEqualTo("P-001");
            assertThat(product.getSalePrice()).isEqualByComparingTo("12500.00");
            assertThat(product.getBaseUnitMeasureCode()).isEqualTo("94");
            assertThat(product.getProvider()).isEqualTo("Proveedor texto");
            assertThat(product.getNotes()).isEqualTo("Bulto de 15 kg");
            assertThat(product.getCreatedDate()).isEqualTo(ProductMother.CREADO);
            assertThat(product.getUpdatedDate()).isEqualTo(LocalDateTime.of(2026, 3, 1, 8, 0));
            assertThat(product.getUpdatedBy()).isEqualTo(77L);
            assertThat(product.getVersion()).isEqualTo(4L);
            assertThat(product.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("ida y vuelta: dominio -> entidad -> dominio conserva el producto")
        void ida_y_vuelta_conserva_el_producto() {
            Product original = ProductMother.gravado();

            ProductJpaEntity entity = mapper.toJpa(original, categoryEntity, taxEntity,
                    companyEntity, supplierEntity);
            Product vuelta = mapper.toDomain(entity, ProductMother.CATEGORIA, ProductMother.IVA_19,
                    ProductMother.CLINICA, ProductMother.PROVEEDOR);

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getName()).isEqualTo(original.getName());
            assertThat(vuelta.getCode()).isEqualTo(original.getCode());
            assertThat(vuelta.getSalePrice()).isEqualByComparingTo(original.getSalePrice());
            assertThat(vuelta.getBaseUnitMeasureCode())
                    .isEqualTo(original.getBaseUnitMeasureCode());
            assertThat(vuelta.getTaxTreatment()).isEqualTo(original.getTaxTreatment());
            assertThat(vuelta.getNotes()).isEqualTo(original.getNotes());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
            assertThat(vuelta.getVersion()).isEqualTo(original.getVersion());
            assertThat(vuelta.isEnabled()).isEqualTo(original.isEnabled());
        }

        @Test
        @DisplayName("las refs que se le pasan mandan sobre las asociaciones de la fila")
        void las_refs_pasadas_mandan() {
            ProductCategoryRef otraCategoria = new ProductCategoryRef(999L, "Otra");
            TaxRef otroImpuesto = new TaxRef(998L, "IVA 5%", new BigDecimal("5.00"));
            CompanyRef otraEmpresa = new CompanyRef(997L, "Otra Clinica", "NIT-111");
            SupplierRef otroProveedor = new SupplierRef(996L, "Otro Proveedor");

            Product product = mapper.toDomain(entidadPoblada(), otraCategoria, otroImpuesto,
                    otraEmpresa, otroProveedor);

            assertThat(product.getProductCategory()).isEqualTo(otraCategoria);
            assertThat(product.getTax()).isEqualTo(otroImpuesto);
            assertThat(product.getCompany()).isEqualTo(otraEmpresa);
            assertThat(product.getSupplier()).isEqualTo(otroProveedor);
        }
    }
}
