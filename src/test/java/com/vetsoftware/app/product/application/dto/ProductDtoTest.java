package com.vetsoftware.app.product.application.dto;

import com.vetsoftware.app.shared.pagination.PageResult;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.product.domain.Product;
import com.vetsoftware.app.product.domain.TaxTreatment;
import com.vetsoftware.app.product.testsupport.ProductMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Proyeccion de dominio a DTO: campo por campo. Un {@code from} que se olvida
 * de un campo no rompe nada hasta que el front muestra un hueco.
 */
@DisplayName("DTOs de product")
class ProductDtoTest {

    @Nested
    @DisplayName("ProductDto.from")
    class Producto {

        @Test
        @DisplayName("copia los diecisiete campos del producto gravado")
        void copia_todos_los_campos() {
            Product product = ProductMother.gravado();

            ProductDto dto = ProductDto.from(product);

            assertThat(dto.id()).isEqualTo(ProductMother.PRODUCT_ID);
            assertThat(dto.name()).isEqualTo("Concentrado adulto");
            assertThat(dto.code()).isEqualTo("P-001");
            assertThat(dto.salePrice()).isEqualByComparingTo(ProductMother.PRECIO);
            assertThat(dto.baseUnitMeasureCode()).isEqualTo("94");
            assertThat(dto.provider()).isEqualTo("Proveedor texto");
            assertThat(dto.taxTreatment()).isEqualTo(TaxTreatment.GRAVADO);
            assertThat(dto.notes()).isEqualTo("Bulto de 15 kg");
            assertThat(dto.createdDate()).isEqualTo(ProductMother.CREADO);
            assertThat(dto.updatedDate()).isNull();
            assertThat(dto.updatedBy()).isNull();
            assertThat(dto.version()).isEqualTo(0L);
            assertThat(dto.enabled()).isTrue();
        }

        @Test
        @DisplayName("aplana las referencias en sus resumenes")
        void aplana_las_referencias() {
            ProductDto dto = ProductDto.from(ProductMother.gravado());

            assertThat(dto.productCategory())
                    .isEqualTo(new ProductCategorySummaryDto(3L, "Alimentos"));
            assertThat(dto.company()).isEqualTo(
                    new CompanySummaryDto(ProductMother.COMPANY_ID, "Clinica Norte", "NIT-900"));
            assertThat(dto.supplier()).isEqualTo(new SupplierSummaryDto(6L, "Distribuidora Sur"));
            assertThat(dto.tax().id()).isEqualTo(4L);
            assertThat(dto.tax().name()).isEqualTo("IVA 19%");
            assertThat(dto.tax().percentage()).isEqualByComparingTo("19.00");
        }

        @Test
        @DisplayName("deja en null impuesto y proveedor cuando el producto no los tiene")
        void deja_en_null_las_referencias_opcionales() {
            ProductDto dto = ProductDto.from(ProductMother.excluido());

            assertThat(dto.tax()).isNull();
            assertThat(dto.supplier()).isNull();
            assertThat(dto.productCategory()).isNotNull();
            assertThat(dto.company()).isNotNull();
        }
    }

    @Nested
    @DisplayName("resumenes")
    class Resumenes {

        @Test
        @DisplayName("CompanySummaryDto.from copia id, nombre e identificador")
        void company_summary() {
            CompanySummaryDto dto = CompanySummaryDto.from(ProductMother.CLINICA);

            assertThat(dto.id()).isEqualTo(ProductMother.COMPANY_ID);
            assertThat(dto.name()).isEqualTo("Clinica Norte");
            assertThat(dto.identifier()).isEqualTo("NIT-900");
        }

        @Test
        @DisplayName("ProductCategorySummaryDto.from copia id y nombre")
        void category_summary() {
            ProductCategorySummaryDto dto = ProductCategorySummaryDto.from(ProductMother.CATEGORIA);

            assertThat(dto.id()).isEqualTo(3L);
            assertThat(dto.name()).isEqualTo("Alimentos");
        }

        @Test
        @DisplayName("SupplierSummaryDto.from copia id y nombre")
        void supplier_summary() {
            SupplierSummaryDto dto = SupplierSummaryDto.from(ProductMother.PROVEEDOR);

            assertThat(dto.id()).isEqualTo(6L);
            assertThat(dto.name()).isEqualTo("Distribuidora Sur");
        }

        @Test
        @DisplayName("TaxSummaryDto.from conserva el porcentaje sin reescalar")
        void tax_summary() {
            TaxSummaryDto dto = TaxSummaryDto.from(ProductMother.IVA_19);

            assertThat(dto.id()).isEqualTo(4L);
            assertThat(dto.name()).isEqualTo("IVA 19%");
            assertThat(dto.percentage()).isEqualByComparingTo("19.00");
        }
    }

    @Nested
    @DisplayName("PageResult.map")
    class Paginacion {

        @Test
        @DisplayName("transforma el contenido y conserva los metadatos de la pagina")
        void conserva_los_metadatos() {
            PageResult<Product> page = new PageResult<>(List.of(ProductMother.gravado()), 2, 20,
                    41L, 3);

            PageResult<ProductDto> mapped = page.map(ProductDto::from);

            assertThat(mapped.content()).hasSize(1);
            assertThat(mapped.content().get(0).id()).isEqualTo(ProductMother.PRODUCT_ID);
            assertThat(mapped.page()).isEqualTo(2);
            assertThat(mapped.pageSize()).isEqualTo(20);
            assertThat(mapped.totalElements()).isEqualTo(41L);
            assertThat(mapped.totalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("una pagina vacia se mapea a una pagina vacia")
        void pagina_vacia() {
            PageResult<Product> page = new PageResult<>(List.of(), 0, 20, 0L, 0);

            assertThat(page.map(ProductDto::from).content()).isEmpty();
        }
    }
}
