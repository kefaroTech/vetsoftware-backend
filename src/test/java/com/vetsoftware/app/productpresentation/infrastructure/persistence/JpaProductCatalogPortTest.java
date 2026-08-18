package com.vetsoftware.app.productpresentation.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import com.vetsoftware.app.unitmeasure.infrastructure.persistence.UnitMeasureCatalogJpaEntity;
import com.vetsoftware.app.unitmeasure.infrastructure.persistence.UnitMeasureCatalogJpaRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link JpaProductCatalogPort} no es un {@code Jpa<Algo>Repository}: es un
 * adaptador que envuelve tres repositorios Spring Data
 * ({@link ProductJpaRepository}, {@link ProductPresentationJpaRepository},
 * {@link UnitMeasureCatalogJpaRepository}) sin mediar un puerto de dominio
 * propio, igual que los {@code JpaXxxQueryPort} de otras features. No exige
 * rodaja {@code *IT}: se prueba con JUnit + Mockito sobre esos tres
 * repositorios, igual que {@code JpaCompanyQueryPortTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaProductCatalogPort — adaptador sobre ProductJpaRepository, ProductPresentationJpaRepository y UnitMeasureCatalogJpaRepository")
class JpaProductCatalogPortTest {

    private static final Long PRODUCT_ID = 500L;
    private static final Long COMPANY_ID = 900L;
    private static final String UNIT_CODE = "94";
    private static final Long ACTOR_ID = 77L;
    private static final BigDecimal PRICE = new BigDecimal("12000.00");

    @Mock
    private ProductJpaRepository products;
    @Mock
    private ProductPresentationJpaRepository presentations;
    @Mock
    private UnitMeasureCatalogJpaRepository units;

    @Mock
    private ProductJpaEntity productEntity;
    @Mock
    private CompanyJpaEntity companyEntity;
    @Mock
    private UnitMeasureCatalogJpaEntity unitEntity;
    @Mock
    private ProductPresentationJpaEntity existingPresentation;

    private JpaProductCatalogPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaProductCatalogPort(products, presentations, units);
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("un codigo nulo es falso sin tocar el repositorio")
        void un_codigo_nulo_es_falso_sin_tocar_el_repositorio() {
            assertThat(port.exists(null)).isFalse();

            verifyNoInteractions(units);
        }

        @Test
        @DisplayName("delega en el repositorio y devuelve true cuando el codigo existe")
        void delega_y_devuelve_true_cuando_el_codigo_existe() {
            when(units.existsById(UNIT_CODE)).thenReturn(true);

            assertThat(port.exists(UNIT_CODE)).isTrue();
        }

        @Test
        @DisplayName("delega en el repositorio y devuelve false cuando el codigo no existe")
        void delega_y_devuelve_false_cuando_el_codigo_no_existe() {
            when(units.existsById(UNIT_CODE)).thenReturn(false);

            assertThat(port.exists(UNIT_CODE)).isFalse();
        }
    }

    @Nested
    @DisplayName("ensureDefault")
    class EnsureDefault {

        @Test
        @DisplayName("no crea nada si ya existe una presentacion por defecto")
        void no_crea_nada_si_ya_existe_una_presentacion_por_defecto() {
            when(presentations.findByCompany_IdAndProduct_IdAndDefaultPresentationTrue(COMPANY_ID,
                    PRODUCT_ID)).thenReturn(Optional.of(existingPresentation));

            port.ensureDefault(PRODUCT_ID, COMPANY_ID, UNIT_CODE, PRICE);

            verify(presentations, never()).save(any());
            verifyNoInteractions(products, units);
        }

        @Test
        @DisplayName("lanza si el producto no existe para esa empresa y no toca units ni presentations.save")
        void lanza_si_el_producto_no_existe_para_esa_empresa() {
            when(presentations.findByCompany_IdAndProduct_IdAndDefaultPresentationTrue(COMPANY_ID,
                    PRODUCT_ID)).thenReturn(Optional.empty());
            when(products.findByIdAndCompany_Id(PRODUCT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> port.ensureDefault(PRODUCT_ID, COMPANY_ID, UNIT_CODE, PRICE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product not found: " + PRODUCT_ID);

            verifyNoInteractions(units);
            verify(presentations, never()).save(any());
        }

        @Test
        @DisplayName("crea la presentacion por defecto con conversion 1 cuando no existe ninguna")
        void crea_la_presentacion_por_defecto_con_conversion_uno() {
            when(presentations.findByCompany_IdAndProduct_IdAndDefaultPresentationTrue(COMPANY_ID,
                    PRODUCT_ID)).thenReturn(Optional.empty());
            when(products.findByIdAndCompany_Id(PRODUCT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(productEntity));
            when(productEntity.getCompany()).thenReturn(companyEntity);
            when(units.getReferenceById(UNIT_CODE)).thenReturn(unitEntity);
            when(presentations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            port.ensureDefault(PRODUCT_ID, COMPANY_ID, UNIT_CODE, PRICE);

            ArgumentCaptor<ProductPresentationJpaEntity> captor = ArgumentCaptor
                    .forClass(ProductPresentationJpaEntity.class);
            verify(presentations).save(captor.capture());
            ProductPresentationJpaEntity creada = captor.getValue();
            assertThat(creada.getCompany()).isEqualTo(companyEntity);
            assertThat(creada.getProduct()).isEqualTo(productEntity);
            assertThat(creada.getName()).isEqualTo("Unidad");
            assertThat(creada.getUnitMeasure()).isEqualTo(unitEntity);
            assertThat(creada.getConversionFactor()).isEqualTo(1);
            assertThat(creada.getSalePrice()).isEqualTo(PRICE);
            assertThat(creada.isDefaultPresentation()).isTrue();
        }
    }

    @Nested
    @DisplayName("synchronizeDefault")
    class SynchronizeDefault {

        @Test
        @DisplayName("delega en ensureDefault si no existe una presentacion por defecto")
        void delega_en_ensure_default_si_no_existe_una_presentacion_por_defecto() {
            when(presentations.findByCompany_IdAndProduct_IdAndDefaultPresentationTrue(COMPANY_ID,
                    PRODUCT_ID)).thenReturn(Optional.empty());
            when(products.findByIdAndCompany_Id(PRODUCT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(productEntity));
            when(productEntity.getCompany()).thenReturn(companyEntity);
            when(units.getReferenceById(UNIT_CODE)).thenReturn(unitEntity);
            when(presentations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            port.synchronizeDefault(PRODUCT_ID, COMPANY_ID, UNIT_CODE, PRICE, ACTOR_ID);

            verify(presentations).save(any());
        }

        @Test
        @DisplayName("actualiza la presentacion existente en el sitio, sin crear una nueva")
        void actualiza_la_presentacion_existente_sin_crear_una_nueva() {
            ProductPresentationJpaEntity existente = ProductPresentationJpaEntity.create(
                    companyEntity, productEntity, "Unidad vieja", unitEntity, 1,
                    new BigDecimal("1.00"), true, null);
            when(presentations.findByCompany_IdAndProduct_IdAndDefaultPresentationTrue(COMPANY_ID,
                    PRODUCT_ID)).thenReturn(Optional.of(existente));
            when(units.getReferenceById(UNIT_CODE)).thenReturn(unitEntity);

            port.synchronizeDefault(PRODUCT_ID, COMPANY_ID, UNIT_CODE, PRICE, ACTOR_ID);

            assertThat(existente.getName()).isEqualTo("Unidad vieja");
            assertThat(existente.getUnitMeasure()).isEqualTo(unitEntity);
            assertThat(existente.getSalePrice()).isEqualTo(PRICE);
            assertThat(existente.isDefaultPresentation()).isTrue();
            assertThat(existente.getUpdatedBy()).isEqualTo(ACTOR_ID);
            verify(presentations, never()).save(any());
            verifyNoInteractions(products);
        }
    }
}
