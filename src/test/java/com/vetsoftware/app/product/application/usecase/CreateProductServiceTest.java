package com.vetsoftware.app.product.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.product.application.command.CreateProductCommand;
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
@DisplayName("CreateProductService")
class CreateProductServiceTest {

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
    private CreateProductService service;

    @Captor
    private ArgumentCaptor<Product> productoCaptor;

    /** Deja empresa, categoria, impuesto, proveedor y unidad resolviendo bien. */
    private void todasLasReferenciasExisten() {
        when(companyQueryPort.findById(ProductMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductMother.CLINICA));
        when(productCategoryQueryPort.findById(ProductMother.CATEGORIA.id(),
                ProductMother.COMPANY_ID)).thenReturn(Optional.of(ProductMother.CATEGORIA));
        when(taxQueryPort.findById(ProductMother.IVA_19.id(), ProductMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductMother.IVA_19));
        when(supplierQueryPort.findById(ProductMother.PROVEEDOR.id(), ProductMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductMother.PROVEEDOR));
        when(unitMeasureQueryPort.exists("94")).thenReturn(true);
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("persiste el producto con las referencias resueltas por los puertos")
        void persiste_con_las_referencias_resueltas() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(ProductMother.gravado());

            service.execute(ProductMother.comandoCrear());

            verify(repository).save(productoCaptor.capture());
            Product guardado = productoCaptor.getValue();
            // Las refs tienen que venir de los puertos, no de los ids del comando.
            assertThat(guardado.getCompany()).isEqualTo(ProductMother.CLINICA);
            assertThat(guardado.getProductCategory()).isEqualTo(ProductMother.CATEGORIA);
            assertThat(guardado.getTax()).isEqualTo(ProductMother.IVA_19);
            assertThat(guardado.getSupplier()).isEqualTo(ProductMother.PROVEEDOR);
        }

        @Test
        @DisplayName("el producto nace sin id, habilitado y con los datos del comando")
        void el_producto_nace_sin_id_y_habilitado() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(ProductMother.gravado());

            service.execute(ProductMother.comandoCrear());

            verify(repository).save(productoCaptor.capture());
            Product guardado = productoCaptor.getValue();
            assertThat(guardado.getId()).isNull();
            assertThat(guardado.isEnabled()).isTrue();
            assertThat(guardado.getName()).isEqualTo("Concentrado adulto");
            assertThat(guardado.getCode()).isEqualTo("P-001");
            assertThat(guardado.getSalePrice()).isEqualByComparingTo(ProductMother.PRECIO);
            assertThat(guardado.getBaseUnitMeasureCode()).isEqualTo("94");
            assertThat(guardado.getTaxTreatment()).isEqualTo(TaxTreatment.GRAVADO);
        }

        @Test
        @DisplayName("siembra la presentacion por defecto con el id ya generado")
        void siembra_la_presentacion_por_defecto() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(ProductMother.gravado());

            service.execute(ProductMother.comandoCrear());

            // El id solo existe despues de save: sembrar con el del comando dejaria
            // la presentacion colgando de un producto inexistente.
            verify(defaultPresentationPort).ensureDefault(ProductMother.PRODUCT_ID,
                    ProductMother.COMPANY_ID, "94", ProductMother.PRECIO);
        }

        @Test
        @DisplayName("devuelve el DTO del producto ya persistido, con su id")
        void devuelve_el_dto_persistido() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(ProductMother.gravado());

            ProductDto dto = service.execute(ProductMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(ProductMother.PRODUCT_ID);
            assertThat(dto.name()).isEqualTo("Concentrado adulto");
            assertThat(dto.company().id()).isEqualTo(ProductMother.COMPANY_ID);
        }

        @Test
        @DisplayName("sin impuesto ni proveedor no consulta esos puertos")
        void sin_impuesto_ni_proveedor_no_consulta_esos_puertos() {
            when(companyQueryPort.findById(ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.CLINICA));
            when(productCategoryQueryPort.findById(ProductMother.CATEGORIA.id(),
                    ProductMother.COMPANY_ID)).thenReturn(Optional.of(ProductMother.CATEGORIA));
            when(unitMeasureQueryPort.exists("94")).thenReturn(true);
            when(repository.save(any())).thenReturn(ProductMother.excluido());

            service.execute(ProductMother.comandoCrearSinImpuestoNiProveedor());

            verifyNoInteractions(taxQueryPort, supplierQueryPort);
            verify(repository).save(productoCaptor.capture());
            assertThat(productoCaptor.getValue().getTax()).isNull();
            assertThat(productoCaptor.getValue().getSupplier()).isNull();
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("empresa inexistente: no consulta nada mas ni persiste")
        void empresa_inexistente() {
            when(companyQueryPort.findById(ProductMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProductMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + ProductMother.COMPANY_ID);

            verifyNoInteractions(repository, productCategoryQueryPort, taxQueryPort,
                    supplierQueryPort, unitMeasureQueryPort, defaultPresentationPort);
        }

        @Test
        @DisplayName("categoria inexistente: se busca acotada por empresa y no persiste")
        void categoria_inexistente() {
            when(companyQueryPort.findById(ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.CLINICA));
            when(productCategoryQueryPort.findById(ProductMother.CATEGORIA.id(),
                    ProductMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProductMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "ProductCategory not found: " + ProductMother.CATEGORIA.id());

            verify(repository, never()).save(any());
            verifyNoInteractions(taxQueryPort, supplierQueryPort, defaultPresentationPort);
        }

        @Test
        @DisplayName("impuesto de otra empresa: no persiste")
        void impuesto_inexistente() {
            when(companyQueryPort.findById(ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.CLINICA));
            when(productCategoryQueryPort.findById(ProductMother.CATEGORIA.id(),
                    ProductMother.COMPANY_ID)).thenReturn(Optional.of(ProductMother.CATEGORIA));
            when(taxQueryPort.findById(ProductMother.IVA_19.id(), ProductMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProductMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tax not found: " + ProductMother.IVA_19.id());

            verify(repository, never()).save(any());
            verifyNoInteractions(supplierQueryPort, defaultPresentationPort);
        }

        @Test
        @DisplayName("proveedor de otra empresa: no persiste")
        void proveedor_inexistente() {
            when(companyQueryPort.findById(ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.CLINICA));
            when(productCategoryQueryPort.findById(ProductMother.CATEGORIA.id(),
                    ProductMother.COMPANY_ID)).thenReturn(Optional.of(ProductMother.CATEGORIA));
            when(taxQueryPort.findById(ProductMother.IVA_19.id(), ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.IVA_19));
            when(supplierQueryPort.findById(ProductMother.PROVEEDOR.id(), ProductMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProductMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Supplier not found: " + ProductMother.PROVEEDOR.id());

            verify(repository, never()).save(any());
            verifyNoInteractions(defaultPresentationPort);
        }

        @Test
        @DisplayName("unidad de medida desconocida: no persiste")
        void unidad_de_medida_desconocida() {
            when(companyQueryPort.findById(ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.CLINICA));
            when(productCategoryQueryPort.findById(ProductMother.CATEGORIA.id(),
                    ProductMother.COMPANY_ID)).thenReturn(Optional.of(ProductMother.CATEGORIA));
            when(taxQueryPort.findById(ProductMother.IVA_19.id(), ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.IVA_19));
            when(supplierQueryPort.findById(ProductMother.PROVEEDOR.id(), ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.PROVEEDOR));

            CreateProductCommand comando = new CreateProductCommand("Concentrado adulto", "P-001",
                    ProductMother.PRECIO, "ZZZ", null, ProductMother.PROVEEDOR.id(), null,
                    TaxTreatment.GRAVADO, ProductMother.CATEGORIA.id(), ProductMother.IVA_19.id(),
                    ProductMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unit measure not found: ZZZ");

            verify(repository, never()).save(any());
            verifyNoInteractions(defaultPresentationPort);
        }
    }

    @Nested
    @DisplayName("unicidad dentro de la empresa")
    class Unicidad {

        @Test
        @DisplayName("codigo repetido: aborta antes de resolver la categoria")
        void codigo_repetido() {
            when(companyQueryPort.findById(ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.CLINICA));
            when(repository.existsByCompanyIdAndCode(ProductMother.COMPANY_ID, "P-001"))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(ProductMother.comandoCrear()))
                    .isInstanceOf(ProductCodeAlreadyExistsException.class)
                    .hasMessageContaining("'P-001'");

            verify(repository, never()).save(any());
            verifyNoInteractions(productCategoryQueryPort, taxQueryPort, supplierQueryPort,
                    unitMeasureQueryPort, defaultPresentationPort);
        }

        @Test
        @DisplayName("nombre repetido: aborta antes de resolver la categoria")
        void nombre_repetido() {
            when(companyQueryPort.findById(ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.CLINICA));
            when(repository.existsByCompanyIdAndName(ProductMother.COMPANY_ID,
                    "Concentrado adulto")).thenReturn(true);

            assertThatThrownBy(() -> service.execute(ProductMother.comandoCrear()))
                    .isInstanceOf(ProductNameAlreadyExistsException.class)
                    .hasMessageContaining("'Concentrado adulto'");

            verify(repository, never()).save(any());
            verifyNoInteractions(productCategoryQueryPort, taxQueryPort, supplierQueryPort,
                    unitMeasureQueryPort, defaultPresentationPort);
        }

        @Test
        @DisplayName("la unicidad se consulta siempre acotada a la empresa del comando")
        void la_unicidad_se_consulta_acotada_a_la_empresa() {
            when(companyQueryPort.findById(ProductMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductMother.CLINICA));
            when(repository.existsByCompanyIdAndCode(ProductMother.COMPANY_ID, "P-001"))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(ProductMother.comandoCrear()))
                    .isInstanceOf(ProductCodeAlreadyExistsException.class);

            // Sin el companyId, el SKU de otro tenant bloquearia el alta de este.
            verify(repository).existsByCompanyIdAndCode(ProductMother.COMPANY_ID, "P-001");
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("un precio negativo no llega a persistirse")
        void precio_negativo_no_llega_a_persistirse() {
            todasLasReferenciasExisten();

            CreateProductCommand comando = new CreateProductCommand("Concentrado adulto", "P-001",
                    new BigDecimal("-1"), "94", null, ProductMother.PROVEEDOR.id(), null,
                    TaxTreatment.GRAVADO, ProductMother.CATEGORIA.id(), ProductMother.IVA_19.id(),
                    ProductMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("salePrice cannot be negative");

            verify(repository, never()).save(any());
            verifyNoInteractions(defaultPresentationPort);
        }
    }
}
