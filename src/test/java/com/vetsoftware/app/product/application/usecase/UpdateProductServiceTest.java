package com.vetsoftware.app.product.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.product.application.command.UpdateProductCommand;
import com.vetsoftware.app.product.application.dto.ProductDto;
import com.vetsoftware.app.product.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.product.application.port.out.DefaultProductPresentationPort;
import com.vetsoftware.app.product.application.port.out.ProductCategoryQueryPort;
import com.vetsoftware.app.product.application.port.out.ProductRepository;
import com.vetsoftware.app.product.application.port.out.SupplierQueryPort;
import com.vetsoftware.app.product.application.port.out.TaxQueryPort;
import com.vetsoftware.app.product.application.port.out.UnitMeasureQueryPort;
import com.vetsoftware.app.product.domain.Product;
import com.vetsoftware.app.product.domain.ProductCodeAlreadyExistsException;
import com.vetsoftware.app.product.domain.ProductNameAlreadyExistsException;
import com.vetsoftware.app.product.domain.ProductNotFoundException;
import com.vetsoftware.app.product.domain.TaxTreatment;
import com.vetsoftware.app.product.testsupport.ProductMother;
import java.math.BigDecimal;
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
@DisplayName("UpdateProductService")
class UpdateProductServiceTest {

    @Mock
    private ProductRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private ProductCategoryQueryPort productCategoryQueryPort;
    @Mock
    private TaxQueryPort taxQueryPort;
    @Mock
    private SupplierQueryPort supplierQueryPort;
    @Mock
    private UnitMeasureQueryPort unitMeasureQueryPort;
    @Mock
    private DefaultProductPresentationPort defaultPresentationPort;

    @InjectMocks
    private UpdateProductService service;

    @Captor
    private ArgumentCaptor<Product> productoCaptor;

    private void elProductoExiste() {
        when(repository.findByIdAndCompanyId(ProductMother.PRODUCT_ID, ProductMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductMother.gravado()));
    }

    /** Empresa + las tres referencias nuevas del comando de actualizacion. */
    private void todasLasReferenciasExisten() {
        when(companyQueryPort.findById(ProductMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductMother.CLINICA));
        when(productCategoryQueryPort.findById(ProductMother.OTRA_CATEGORIA.id(),
                ProductMother.COMPANY_ID)).thenReturn(Optional.of(ProductMother.OTRA_CATEGORIA));
        when(taxQueryPort.findById(ProductMother.IVA_5.id(), ProductMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductMother.IVA_5));
        when(supplierQueryPort.findById(ProductMother.OTRO_PROVEEDOR.id(),
                ProductMother.COMPANY_ID)).thenReturn(Optional.of(ProductMother.OTRO_PROVEEDOR));
        when(unitMeasureQueryPort.exists("KGM")).thenReturn(true);
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("guarda el producto con los campos y referencias nuevas")
        void guarda_el_producto_actualizado() {
            elProductoExiste();
            todasLasReferenciasExisten();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(ProductMother.comandoActualizar());

            verify(repository).save(productoCaptor.capture());
            Product guardado = productoCaptor.getValue();
            assertThat(guardado.getId()).isEqualTo(ProductMother.PRODUCT_ID);
            assertThat(guardado.getName()).isEqualTo("Concentrado senior");
            assertThat(guardado.getCode()).isEqualTo("P-009");
            assertThat(guardado.getSalePrice()).isEqualByComparingTo("18000.00");
            assertThat(guardado.getBaseUnitMeasureCode()).isEqualTo("KGM");
            assertThat(guardado.getProductCategory()).isEqualTo(ProductMother.OTRA_CATEGORIA);
            assertThat(guardado.getTax()).isEqualTo(ProductMother.IVA_5);
            assertThat(guardado.getSupplier()).isEqualTo(ProductMother.OTRO_PROVEEDOR);
        }

        @Test
        @DisplayName("sella quien actualiza y la version esperada del comando")
        void sella_el_autor_y_la_version() {
            elProductoExiste();
            todasLasReferenciasExisten();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(ProductMother.comandoActualizar());

            verify(repository).save(productoCaptor.capture());
            assertThat(productoCaptor.getValue().getUpdatedBy()).isEqualTo(77L);
            assertThat(productoCaptor.getValue().getVersion()).isEqualTo(3L);
            assertThat(productoCaptor.getValue().getUpdatedDate()).isNotNull();
        }

        @Test
        @DisplayName("sincroniza la presentacion por defecto con el precio y la unidad nuevos")
        void sincroniza_la_presentacion_por_defecto() {
            elProductoExiste();
            todasLasReferenciasExisten();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(ProductMother.comandoActualizar());

            verify(defaultPresentationPort).synchronizeDefault(ProductMother.PRODUCT_ID,
                    ProductMother.COMPANY_ID, "KGM", new BigDecimal("18000.00"), 77L);
        }

        @Test
        @DisplayName("devuelve el DTO ya actualizado")
        void devuelve_el_dto_actualizado() {
            elProductoExiste();
            todasLasReferenciasExisten();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ProductDto dto = service.execute(ProductMother.comandoActualizar());

            assertThat(dto.name()).isEqualTo("Concentrado senior");
            assertThat(dto.tax().id()).isEqualTo(ProductMother.IVA_5.id());
            assertThat(dto.supplier().id()).isEqualTo(ProductMother.OTRO_PROVEEDOR.id());
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class Tenancy {

        @Test
        @DisplayName("producto de otra empresa: 404 de dominio y CERO escrituras")
        void producto_de_otra_empresa() {
            when(repository.findByIdAndCompanyId(ProductMother.PRODUCT_ID,
                    ProductMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProductMother.comandoActualizar()))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining("Product not found: " + ProductMother.PRODUCT_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(companyQueryPort, productCategoryQueryPort, taxQueryPort,
                    supplierQueryPort, unitMeasureQueryPort, defaultPresentationPort);
        }

        @Test
        @DisplayName("empresa inexistente: no persiste")
        void empresa_inexistente() {
            elProductoExiste();
            when(companyQueryPort.findById(ProductMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProductMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + ProductMother.COMPANY_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(defaultPresentationPort);
        }
    }

    @Nested
    @DisplayName("unicidad excluyendo el propio producto")
    class Unicidad {

        @Test
        @DisplayName("codigo tomado por otro producto de la empresa")
        void codigo_tomado_por_otro() {
            elProductoExiste();
            when(companyQueryPort.findById(ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.CLINICA));
            when(repository.existsByCompanyIdAndCodeExcludingId(ProductMother.COMPANY_ID, "P-009",
                    ProductMother.PRODUCT_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(ProductMother.comandoActualizar()))
                    .isInstanceOf(ProductCodeAlreadyExistsException.class)
                    .hasMessageContaining("'P-009'");

            verify(repository, never()).save(any());
            verifyNoInteractions(productCategoryQueryPort, taxQueryPort, supplierQueryPort,
                    unitMeasureQueryPort, defaultPresentationPort);
        }

        @Test
        @DisplayName("nombre tomado por otro producto de la empresa")
        void nombre_tomado_por_otro() {
            elProductoExiste();
            when(companyQueryPort.findById(ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.CLINICA));
            when(repository.existsByCompanyIdAndNameExcludingId(ProductMother.COMPANY_ID,
                    "Concentrado senior", ProductMother.PRODUCT_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(ProductMother.comandoActualizar()))
                    .isInstanceOf(ProductNameAlreadyExistsException.class)
                    .hasMessageContaining("'Concentrado senior'");

            verify(repository, never()).save(any());
            verifyNoInteractions(productCategoryQueryPort, taxQueryPort, supplierQueryPort,
                    unitMeasureQueryPort, defaultPresentationPort);
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("categoria inexistente")
        void categoria_inexistente() {
            elProductoExiste();
            when(companyQueryPort.findById(ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.CLINICA));
            when(productCategoryQueryPort.findById(ProductMother.OTRA_CATEGORIA.id(),
                    ProductMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProductMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "ProductCategory not found: " + ProductMother.OTRA_CATEGORIA.id());

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("impuesto inexistente")
        void impuesto_inexistente() {
            elProductoExiste();
            when(companyQueryPort.findById(ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.CLINICA));
            when(productCategoryQueryPort.findById(ProductMother.OTRA_CATEGORIA.id(),
                    ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.OTRA_CATEGORIA));
            when(taxQueryPort.findById(ProductMother.IVA_5.id(), ProductMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProductMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tax not found: " + ProductMother.IVA_5.id());

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("proveedor inexistente")
        void proveedor_inexistente() {
            elProductoExiste();
            when(companyQueryPort.findById(ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.CLINICA));
            when(productCategoryQueryPort.findById(ProductMother.OTRA_CATEGORIA.id(),
                    ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.OTRA_CATEGORIA));
            when(taxQueryPort.findById(ProductMother.IVA_5.id(), ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.IVA_5));
            when(supplierQueryPort.findById(ProductMother.OTRO_PROVEEDOR.id(),
                    ProductMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProductMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Supplier not found: " + ProductMother.OTRO_PROVEEDOR.id());

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("unidad de medida desconocida")
        void unidad_desconocida() {
            elProductoExiste();
            when(companyQueryPort.findById(ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.CLINICA));
            when(productCategoryQueryPort.findById(ProductMother.CATEGORIA.id(),
                    ProductMother.COMPANY_ID)).thenReturn(Optional.of(ProductMother.CATEGORIA));

            UpdateProductCommand comando = new UpdateProductCommand(ProductMother.PRODUCT_ID,
                    "Concentrado senior", "P-009", new BigDecimal("18000.00"), "ZZZ", null, null,
                    null, TaxTreatment.EXCLUIDO, ProductMother.CATEGORIA.id(), null,
                    ProductMother.COMPANY_ID, 77L, 3L);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unit measure not found: ZZZ");

            verify(repository, never()).save(any());
            verifyNoInteractions(taxQueryPort, supplierQueryPort, defaultPresentationPort);
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("un nombre de mas de 100 chars no llega a persistirse")
        void nombre_demasiado_largo() {
            elProductoExiste();
            when(companyQueryPort.findById(ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.CLINICA));
            when(productCategoryQueryPort.findById(ProductMother.CATEGORIA.id(),
                    ProductMother.COMPANY_ID)).thenReturn(Optional.of(ProductMother.CATEGORIA));
            when(unitMeasureQueryPort.exists("94")).thenReturn(true);

            UpdateProductCommand comando = new UpdateProductCommand(ProductMother.PRODUCT_ID,
                    "n".repeat(101), "P-009", new BigDecimal("100.00"), "94", null, null, null,
                    TaxTreatment.EXCLUIDO, ProductMother.CATEGORIA.id(), null,
                    ProductMother.COMPANY_ID, 77L, 3L);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name must be 100 chars or less");

            verify(repository, never()).save(any());
            verifyNoInteractions(defaultPresentationPort);
        }
    }
}
