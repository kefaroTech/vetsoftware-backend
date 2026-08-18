package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.electronicdocument.application.port.out.CatalogLineQueryPort.CatalogItem;
import com.vetsoftware.app.electronicdocument.domain.TaxCategory;
import com.vetsoftware.app.electronicdocument.domain.TaxScheme;
import com.vetsoftware.app.electronicdocument.testsupport.ReflectionEntities;
import com.vetsoftware.app.product.domain.TaxTreatment;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import com.vetsoftware.app.productcategory.infrastructure.persistence.ProductCategoryJpaEntity;
import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaEntity;
import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaRepository;
import com.vetsoftware.app.servicecategory.infrastructure.persistence.ServiceCategoryJpaEntity;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCatalogLineQueryPort — clasificacion tributaria autoritativa del catalogo")
class JpaCatalogLineQueryPortTest {

    @Mock
    private ProductJpaRepository productRepository;
    @Mock
    private ServiceJpaRepository serviceRepository;

    private JpaCatalogLineQueryPort port;

    @BeforeEach
    void montar() {
        port = new JpaCatalogLineQueryPort(productRepository, serviceRepository);
    }

    private static <T> T conId(Class<T> type, Long id) throws Exception {
        T entity = ReflectionEntities.newInstance(type);
        Field idField = type.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
        return entity;
    }

    private static CompanyJpaEntity empresa(Long id) throws Exception {
        return conId(CompanyJpaEntity.class, id);
    }

    private static TaxJpaEntity impuesto(TaxScheme scheme, String percentage) throws Exception {
        TaxJpaEntity tax = ReflectionEntities.newInstance(TaxJpaEntity.class);
        tax.setTaxScheme(com.vetsoftware.app.tax.domain.TaxScheme.valueOf(scheme.name()));
        tax.setPercentage(new BigDecimal(percentage));
        return tax;
    }

    @Nested
    @DisplayName("findProduct")
    class FindProduct {

        @Test
        @DisplayName("un producto gravado con IVA se clasifica GRAVADO con la tasa del impuesto")
        void producto_gravado_con_iva() throws Exception {
            ProductJpaEntity product = ReflectionEntities.newInstance(ProductJpaEntity.class);
            product.setName("Alimento perro");
            product.setTaxTreatment(TaxTreatment.GRAVADO);
            product.setTax(impuesto(TaxScheme.IVA, "19"));
            product.setSalePrice(new BigDecimal("10000"));
            product.setProductCategory(conId(ProductCategoryJpaEntity.class, 3L));
            product.setCompany(empresa(9L));
            when(productRepository.findById(5L)).thenReturn(Optional.of(product));

            Optional<CatalogItem> item = port.findProduct(5L, 9L);

            assertThat(item).contains(new CatalogItem("Alimento perro", TaxCategory.GRAVADO,
                    TaxScheme.IVA, new BigDecimal("19"), new BigDecimal("10000"), 3L));
        }

        @Test
        @DisplayName("un producto EXENTO siempre lleva esquema IVA a tarifa cero")
        void producto_exento_lleva_iva_tarifa_cero() throws Exception {
            ProductJpaEntity product = ReflectionEntities.newInstance(ProductJpaEntity.class);
            product.setName("Medicamento exento");
            product.setTaxTreatment(TaxTreatment.EXENTO);
            product.setSalePrice(new BigDecimal("5000"));
            product.setProductCategory(conId(ProductCategoryJpaEntity.class, 4L));
            product.setCompany(empresa(9L));
            when(productRepository.findById(6L)).thenReturn(Optional.of(product));

            Optional<CatalogItem> item = port.findProduct(6L, 9L);

            assertThat(item).contains(new CatalogItem("Medicamento exento", TaxCategory.EXENTO,
                    TaxScheme.IVA, BigDecimal.ZERO, new BigDecimal("5000"), 4L));
        }

        @Test
        @DisplayName("un producto EXCLUIDO no lleva esquema ni tasa")
        void producto_excluido_sin_esquema() throws Exception {
            ProductJpaEntity product = ReflectionEntities.newInstance(ProductJpaEntity.class);
            product.setName("Servicio excluido");
            product.setTaxTreatment(TaxTreatment.EXCLUIDO);
            product.setSalePrice(new BigDecimal("2000"));
            product.setProductCategory(conId(ProductCategoryJpaEntity.class, 2L));
            product.setCompany(empresa(9L));
            when(productRepository.findById(7L)).thenReturn(Optional.of(product));

            Optional<CatalogItem> item = port.findProduct(7L, 9L);

            assertThat(item).contains(new CatalogItem("Servicio excluido", TaxCategory.EXCLUIDO,
                    null, null, new BigDecimal("2000"), 2L));
        }

        @Test
        @DisplayName("un producto de otra empresa no es visible")
        void producto_de_otra_empresa_no_es_visible() throws Exception {
            ProductJpaEntity product = ReflectionEntities.newInstance(ProductJpaEntity.class);
            product.setCompany(empresa(77L));
            when(productRepository.findById(5L)).thenReturn(Optional.of(product));

            assertThat(port.findProduct(5L, 9L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findService")
    class FindService {

        @Test
        @DisplayName("un servicio gravado con INC se clasifica INC con la tasa del impuesto")
        void servicio_gravado_con_inc() throws Exception {
            ServiceJpaEntity service = ReflectionEntities.newInstance(ServiceJpaEntity.class);
            service.setName("Peluqueria");
            service.setTaxTreatment(com.vetsoftware.app.service.domain.TaxTreatment.INC);
            service.setTax(impuesto(TaxScheme.INC, "8"));
            service.setPrice(new BigDecimal("30000"));
            service.setServiceCategory(conId(ServiceCategoryJpaEntity.class, 1L));
            service.setCompany(empresa(9L));
            when(serviceRepository.findById(8L)).thenReturn(Optional.of(service));

            Optional<CatalogItem> item = port.findService(8L, 9L);

            assertThat(item).contains(new CatalogItem("Peluqueria", TaxCategory.INC, TaxScheme.INC,
                    new BigDecimal("8"), new BigDecimal("30000"), 1L));
        }

        @Test
        @DisplayName("un servicio inexistente devuelve vacio")
        void servicio_inexistente_devuelve_vacio() {
            when(serviceRepository.findById(9L)).thenReturn(Optional.empty());

            assertThat(port.findService(9L, 9L)).isEmpty();
        }
    }
}
