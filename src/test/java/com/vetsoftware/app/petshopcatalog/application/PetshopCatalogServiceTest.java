package com.vetsoftware.app.petshopcatalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalog.domain.SellableItemType;
import com.vetsoftware.app.catalogbarcode.infrastructure.persistence.CatalogBarcodeJpaEntity;
import com.vetsoftware.app.catalogbarcode.infrastructure.persistence.CatalogBarcodeJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.BarcodeLookupDto;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.BundleDto;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.BundleItemWrite;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.BundleWrite;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.PresentationDto;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.PresentationWrite;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.UnitMeasureDto;
import com.vetsoftware.app.petshopcatalog.domain.PetshopCatalogConflictException;
import com.vetsoftware.app.petshopcatalog.domain.PetshopCatalogNotFoundException;
import com.vetsoftware.app.petshopcatalog.testsupport.PetshopCatalogMother;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import com.vetsoftware.app.productbundle.infrastructure.persistence.ProductBundleItemJpaEntity;
import com.vetsoftware.app.productbundle.infrastructure.persistence.ProductBundleItemJpaRepository;
import com.vetsoftware.app.productbundle.infrastructure.persistence.ProductBundleJpaEntity;
import com.vetsoftware.app.productbundle.infrastructure.persistence.ProductBundleJpaRepository;
import com.vetsoftware.app.productpresentation.infrastructure.persistence.ProductPresentationJpaEntity;
import com.vetsoftware.app.productpresentation.infrastructure.persistence.ProductPresentationJpaRepository;
import com.vetsoftware.app.unitmeasure.infrastructure.persistence.UnitMeasureCatalogJpaEntity;
import com.vetsoftware.app.unitmeasure.infrastructure.persistence.UnitMeasureCatalogJpaRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;

/**
 * Unitarios de orquestacion con dobles. Complementa (no reemplaza) a
 * {@link PetshopCatalogServiceIT}: aqui se afirma el "que" via ArgumentCaptor
 * sobre lo que el servicio pide guardar y las ramas de validacion; lo que
 * necesita la base real (indices unicos, flush entre desmarcar/marcar
 * predeterminada) sigue viviendo solo en la rodaja.
 */
@ExtendWith(MockitoExtension.class)
class PetshopCatalogServiceTest {
    private static final Long COMPANY_ID = PetshopCatalogMother.COMPANY_ID;
    private static final Long OTHER_COMPANY_ID = 99L;
    private static final Long PRODUCT_ID = PetshopCatalogMother.PRODUCT_ID;
    private static final Long ACTOR_ID = PetshopCatalogMother.ACTOR_ID;

    @Mock
    private CompanyJpaRepository companies;
    @Mock
    private ProductJpaRepository products;
    @Mock
    private UnitMeasureCatalogJpaRepository units;
    @Mock
    private ProductPresentationJpaRepository presentations;
    @Mock
    private ProductBundleJpaRepository bundles;
    @Mock
    private ProductBundleItemJpaRepository bundleItems;
    @Mock
    private CatalogBarcodeJpaRepository catalogBarcodes;

    @InjectMocks
    private PetshopCatalogService service;

    private static PresentationWrite presentationCommand(String name, int factor, String price,
            boolean isDefault) {
        return presentationCommand(name, factor, price, isDefault, List.of());
    }

    private static PresentationWrite presentationCommand(String name, int factor, String price,
            boolean isDefault, List<String> barcodes) {
        return new PresentationWrite(PRODUCT_ID, name, "UN", factor, new BigDecimal(price),
                isDefault, barcodes, null);
    }

    private static BundleWrite bundleCommand(String name, String code, List<BundleItemWrite> items,
            List<String> barcodes) {
        return new BundleWrite(name, code, "UN", new BigDecimal("50000"), items, barcodes, null);
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("las unidades de medida se mapean en el orden que devuelve el repositorio")
        void las_unidades_se_mapean_en_el_orden_del_repositorio() {
            UnitMeasureCatalogJpaEntity caja = PetshopCatalogMother.unit("CAJ", "Caja", "cj");
            UnitMeasureCatalogJpaEntity unidad = PetshopCatalogMother.unit("UN", "Unidad", "un");
            when(units.findAllByOrderByNameAsc()).thenReturn(List.of(caja, unidad));

            List<UnitMeasureDto> result = service.listUnitMeasures();

            assertThat(result).extracting(UnitMeasureDto::code).containsExactly("CAJ", "UN");
            assertThat(result.get(0).name()).isEqualTo("Caja");
            assertThat(result.get(0).symbol()).isEqualTo("cj");
        }

        @Test
        @DisplayName("las presentaciones del producto se devuelven con sus codigos de barras")
        void las_presentaciones_se_devuelven_con_sus_codigos() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            ProductJpaEntity product = PetshopCatalogMother.product(PRODUCT_ID, company);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductPresentationJpaEntity presentation = PetshopCatalogMother.presentation(1L,
                    company, product, "Unidad", unit, 1, "3500", true, 5L);
            when(products.findByIdAndCompany_Id(PRODUCT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(product));
            when(presentations
                    .findAllByCompany_IdAndProduct_IdOrderByDefaultPresentationDescNameAsc(
                            COMPANY_ID, PRODUCT_ID))
                    .thenReturn(List.of(presentation));
            when(catalogBarcodes.findAllByCompany_IdAndPresentation_IdOrderByBarcode(COMPANY_ID,
                    1L))
                    .thenReturn(List.of(PetshopCatalogMother.barcodeForPresentation(100L, company,
                            "7701234567890", presentation)));

            List<PresentationDto> result = service.listPresentations(PRODUCT_ID, COMPANY_ID);

            assertThat(result).singleElement().satisfies(dto -> {
                assertThat(dto.productName()).isEqualTo("Amoxicilina 500mg");
                assertThat(dto.barcodes()).containsExactly("7701234567890");
                assertThat(dto.defaultPresentation()).isTrue();
            });
        }

        @Test
        @DisplayName("listar presentaciones de un producto de otra empresa no lo encuentra")
        void listar_presentaciones_de_un_producto_de_otra_empresa_no_lo_encuentra() {
            when(products.findByIdAndCompany_Id(PRODUCT_ID, OTHER_COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.listPresentations(PRODUCT_ID, OTHER_COMPANY_ID))
                    .isInstanceOf(PetshopCatalogNotFoundException.class)
                    .hasMessageContaining("Product not found: " + PRODUCT_ID);

            verifyNoInteractions(presentations, catalogBarcodes);
        }

        @Test
        @DisplayName("listar combos exige que la empresa exista")
        void listar_combos_exige_que_la_empresa_exista() {
            when(companies.findById(OTHER_COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.listBundles(OTHER_COMPANY_ID))
                    .isInstanceOf(PetshopCatalogNotFoundException.class)
                    .hasMessageContaining("Company not found: " + OTHER_COMPANY_ID);

            verifyNoInteractions(bundles);
        }

        @Test
        @DisplayName("los combos se devuelven con sus lineas y sus codigos")
        void los_combos_se_devuelven_con_sus_lineas_y_codigos() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            ProductJpaEntity product = PetshopCatalogMother.product(PRODUCT_ID, company);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductPresentationJpaEntity presentation = PetshopCatalogMother.presentation(1L,
                    company, product, "Unidad", unit, 1, "3500", true, 5L);
            ProductBundleJpaEntity bundle = PetshopCatalogMother.bundle(2L, company,
                    "Combo cachorro", "COMBO-1", unit, "50000", 3L);
            ProductBundleItemJpaEntity item = PetshopCatalogMother.bundleItem(50L, company, null,
                    presentation, 2, 0);
            when(companies.findById(COMPANY_ID)).thenReturn(Optional.of(company));
            when(bundles.findAllByCompany_IdOrderByNameAsc(COMPANY_ID)).thenReturn(List.of(bundle));
            when(bundleItems.findAllByCompany_IdAndBundle_IdOrderByDisplayOrderAsc(COMPANY_ID, 2L))
                    .thenReturn(List.of(item));
            when(catalogBarcodes.findAllByCompany_IdAndBundle_IdOrderByBarcode(COMPANY_ID, 2L))
                    .thenReturn(List.of(PetshopCatalogMother.barcodeForBundle(101L, company,
                            "7706666666666", bundle)));

            List<BundleDto> result = service.listBundles(COMPANY_ID);

            assertThat(result).singleElement().satisfies(dto -> {
                assertThat(dto.name()).isEqualTo("Combo cachorro");
                assertThat(dto.barcodes()).containsExactly("7706666666666");
                assertThat(dto.items()).singleElement().satisfies(line -> {
                    assertThat(line.presentationName()).isEqualTo("Unidad");
                    assertThat(line.productName()).isEqualTo("Amoxicilina 500mg");
                    assertThat(line.quantity()).isEqualTo(2);
                });
            });
        }
    }

    @Nested
    @DisplayName("crear presentacion")
    class CrearPresentacion {

        @Test
        @DisplayName("guarda la presentacion no predeterminada con su codigo de barras")
        void guarda_la_presentacion_no_predeterminada_con_su_codigo() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            ProductJpaEntity product = PetshopCatalogMother.product(PRODUCT_ID, company);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            CatalogBarcodeJpaEntity savedBarcode = PetshopCatalogMother.barcodeForPresentation(101L,
                    company, "7705555555555", null);
            when(companies.findById(COMPANY_ID)).thenReturn(Optional.of(company));
            when(products.findByIdAndCompany_Id(PRODUCT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(product));
            when(presentations.existsByCompany_IdAndProduct_IdAndName(COMPANY_ID, PRODUCT_ID,
                    "Caja x12")).thenReturn(false);
            when(units.findById("UN")).thenReturn(Optional.of(unit));
            when(presentations.save(any())).thenAnswer(invocation -> {
                ProductPresentationJpaEntity saved = invocation.getArgument(0);
                saved.setId(1L);
                saved.setVersion(1L);
                return saved;
            });
            when(catalogBarcodes.findAllByCompany_IdAndPresentation_IdOrderByBarcode(COMPANY_ID,
                    1L)).thenReturn(List.of(), List.of(savedBarcode));
            when(catalogBarcodes.existsByCompany_IdAndBarcode(COMPANY_ID, "7705555555555"))
                    .thenReturn(false);

            PresentationDto result = service.createPresentation(
                    presentationCommand("Caja x12", 12, "36000", false, List.of("7705555555555")),
                    COMPANY_ID, ACTOR_ID);

            ArgumentCaptor<ProductPresentationJpaEntity> captor = ArgumentCaptor
                    .forClass(ProductPresentationJpaEntity.class);
            verify(presentations).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Caja x12");
            assertThat(captor.getValue().getConversionFactor()).isEqualTo(12);
            assertThat(captor.getValue().isDefaultPresentation()).isFalse();

            assertThat(result.defaultPresentation()).isFalse();
            assertThat(result.barcodes()).containsExactly("7705555555555");
            verify(products, never()).save(any());
        }

        @Test
        @DisplayName("marcarla predeterminada desmarca la anterior y sincroniza el producto")
        void marcarla_predeterminada_desmarca_la_anterior_y_sincroniza_el_producto() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            ProductJpaEntity product = PetshopCatalogMother.product(PRODUCT_ID, company);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductPresentationJpaEntity previousDefault = PetshopCatalogMother.presentation(9L,
                    company, product, "Unidad vieja", unit, 1, "3000", true, 2L);
            when(companies.findById(COMPANY_ID)).thenReturn(Optional.of(company));
            when(products.findByIdAndCompany_Id(PRODUCT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(product));
            when(presentations.existsByCompany_IdAndProduct_IdAndName(COMPANY_ID, PRODUCT_ID,
                    "Unidad nueva")).thenReturn(false);
            when(presentations.findByCompany_IdAndProduct_IdAndDefaultPresentationTrue(COMPANY_ID,
                    PRODUCT_ID)).thenReturn(Optional.of(previousDefault));
            when(units.findById("UN")).thenReturn(Optional.of(unit));
            when(presentations.save(any())).thenAnswer(invocation -> {
                ProductPresentationJpaEntity saved = invocation.getArgument(0);
                saved.setId(1L);
                saved.setVersion(1L);
                return saved;
            });
            when(catalogBarcodes.findAllByCompany_IdAndPresentation_IdOrderByBarcode(COMPANY_ID,
                    1L)).thenReturn(List.of());

            PresentationDto result = service.createPresentation(
                    presentationCommand("Unidad nueva", 1, "3500", true), COMPANY_ID, ACTOR_ID);

            assertThat(previousDefault.isDefaultPresentation()).isFalse();
            verify(presentations).flush();
            ArgumentCaptor<ProductJpaEntity> productCaptor = ArgumentCaptor
                    .forClass(ProductJpaEntity.class);
            verify(products).save(productCaptor.capture());
            assertThat(productCaptor.getValue().getBaseUnitMeasureCode()).isEqualTo("UN");
            assertThat(productCaptor.getValue().getSalePrice()).isEqualByComparingTo("3500");
            assertThat(result.defaultPresentation()).isTrue();
        }

        @Test
        @DisplayName("un nombre repetido en el producto es un conflicto")
        void un_nombre_repetido_en_el_producto_es_un_conflicto() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            ProductJpaEntity product = PetshopCatalogMother.product(PRODUCT_ID, company);
            when(companies.findById(COMPANY_ID)).thenReturn(Optional.of(company));
            when(products.findByIdAndCompany_Id(PRODUCT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(product));
            when(presentations.existsByCompany_IdAndProduct_IdAndName(COMPANY_ID, PRODUCT_ID,
                    "Unidad")).thenReturn(true);

            assertThatThrownBy(() -> service.createPresentation(
                    presentationCommand("Unidad", 1, "3500", false), COMPANY_ID, ACTOR_ID))
                    .isInstanceOf(PetshopCatalogConflictException.class)
                    .hasMessageContaining("Ya existe una presentación con ese nombre");

            verify(presentations, never()).save(any());
            verifyNoInteractions(catalogBarcodes);
        }

        @Test
        @DisplayName("la predeterminada exige factor de conversion uno")
        void la_predeterminada_exige_factor_de_conversion_uno() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            ProductJpaEntity product = PetshopCatalogMother.product(PRODUCT_ID, company);
            when(companies.findById(COMPANY_ID)).thenReturn(Optional.of(company));
            when(products.findByIdAndCompany_Id(PRODUCT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(product));

            assertThatThrownBy(() -> service.createPresentation(
                    presentationCommand("Caja x12", 12, "36000", true), COMPANY_ID, ACTOR_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("conversionFactor 1");

            verify(presentations, never()).save(any());
            verifyNoInteractions(catalogBarcodes);
        }

        @Test
        @DisplayName("la empresa debe existir para crear una presentacion")
        void la_empresa_debe_existir_para_crear_una_presentacion() {
            when(companies.findById(OTHER_COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createPresentation(
                    presentationCommand("Unidad", 1, "3500", false), OTHER_COMPANY_ID, ACTOR_ID))
                    .isInstanceOf(PetshopCatalogNotFoundException.class)
                    .hasMessageContaining("Company not found: " + OTHER_COMPANY_ID);

            verifyNoInteractions(products, presentations, catalogBarcodes);
        }

        @Test
        @DisplayName("un producto de otra empresa no existe para el catalogo")
        void un_producto_de_otra_empresa_no_existe_para_el_catalogo() {
            CompanyJpaEntity otherCompany = PetshopCatalogMother.company(OTHER_COMPANY_ID);
            when(companies.findById(OTHER_COMPANY_ID)).thenReturn(Optional.of(otherCompany));
            when(products.findByIdAndCompany_Id(PRODUCT_ID, OTHER_COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createPresentation(
                    presentationCommand("Unidad", 1, "3500", false), OTHER_COMPANY_ID, ACTOR_ID))
                    .isInstanceOf(PetshopCatalogNotFoundException.class)
                    .hasMessageContaining("Product not found: " + PRODUCT_ID);

            verifyNoInteractions(presentations, catalogBarcodes);
        }

        @Test
        @DisplayName("un codigo de barras ya asignado a otra presentacion es un conflicto")
        void un_codigo_ya_asignado_a_otra_presentacion_es_un_conflicto() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            ProductJpaEntity product = PetshopCatalogMother.product(PRODUCT_ID, company);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            when(companies.findById(COMPANY_ID)).thenReturn(Optional.of(company));
            when(products.findByIdAndCompany_Id(PRODUCT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(product));
            when(presentations.existsByCompany_IdAndProduct_IdAndName(COMPANY_ID, PRODUCT_ID,
                    "Caja x12")).thenReturn(false);
            when(units.findById("UN")).thenReturn(Optional.of(unit));
            when(presentations.save(any())).thenAnswer(invocation -> {
                ProductPresentationJpaEntity saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(catalogBarcodes.findAllByCompany_IdAndPresentation_IdOrderByBarcode(COMPANY_ID,
                    1L)).thenReturn(List.of());
            when(catalogBarcodes.existsByCompany_IdAndBarcode(COMPANY_ID, "7701234567890"))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.createPresentation(
                    presentationCommand("Caja x12", 12, "36000", false, List.of("7701234567890")),
                    COMPANY_ID, ACTOR_ID)).isInstanceOf(PetshopCatalogConflictException.class)
                    .hasMessageContaining("El código de barras ya está asignado");

            verify(catalogBarcodes, never()).save(any());
        }
    }

    @Nested
    @DisplayName("actualizar presentacion")
    class ActualizarPresentacion {

        @Test
        @DisplayName("cambia nombre, precio y codigos conservando el id")
        void cambia_nombre_precio_y_codigos_conservando_el_id() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            ProductJpaEntity product = PetshopCatalogMother.product(PRODUCT_ID, company);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductPresentationJpaEntity existing = PetshopCatalogMother.presentation(1L, company,
                    product, "Unidad", unit, 1, "3500", true, 5L);
            CatalogBarcodeJpaEntity savedBarcode = PetshopCatalogMother.barcodeForPresentation(102L,
                    company, "7709999999999", null);
            when(presentations.findByIdAndCompany_Id(1L, COMPANY_ID))
                    .thenReturn(Optional.of(existing));
            when(presentations.existsByCompany_IdAndProduct_IdAndNameAndIdNot(COMPANY_ID,
                    PRODUCT_ID, "Unidad suelta", 1L)).thenReturn(false);
            when(presentations.findByCompany_IdAndProduct_IdAndDefaultPresentationTrue(COMPANY_ID,
                    PRODUCT_ID)).thenReturn(Optional.of(existing));
            when(units.findById("UN")).thenReturn(Optional.of(unit));
            when(catalogBarcodes.findAllByCompany_IdAndPresentation_IdOrderByBarcode(COMPANY_ID,
                    1L)).thenReturn(List.of(), List.of(savedBarcode));
            when(catalogBarcodes.existsByCompany_IdAndBarcode(COMPANY_ID, "7709999999999"))
                    .thenReturn(false);

            PresentationDto result = service.updatePresentation(1L,
                    new PresentationWrite(PRODUCT_ID, "Unidad suelta", "UN", 1,
                            new BigDecimal("3900"), true, List.of("7709999999999"), 5L),
                    COMPANY_ID, ACTOR_ID);

            assertThat(existing.getName()).isEqualTo("Unidad suelta");
            assertThat(existing.getSalePrice()).isEqualByComparingTo("3900");
            assertThat(result.name()).isEqualTo("Unidad suelta");
            assertThat(result.barcodes()).containsExactly("7709999999999");
            verify(presentations, never()).flush();
        }

        @Test
        @DisplayName("una version vieja se rechaza como conflicto, no como error de datos")
        void una_version_vieja_se_rechaza_como_conflicto() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            ProductJpaEntity product = PetshopCatalogMother.product(PRODUCT_ID, company);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductPresentationJpaEntity existing = PetshopCatalogMother.presentation(1L, company,
                    product, "Unidad", unit, 1, "3500", true, 5L);
            when(presentations.findByIdAndCompany_Id(1L, COMPANY_ID))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.updatePresentation(1L,
                    new PresentationWrite(PRODUCT_ID, "Otro nombre", "UN", 1,
                            new BigDecimal("3900"), true, List.of(), 999L),
                    COMPANY_ID, ACTOR_ID)).isInstanceOf(PetshopCatalogConflictException.class)
                    .hasMessageContaining("modificado por otra operación");

            verify(presentations, never()).save(any());
            verifyNoInteractions(units, catalogBarcodes);
        }

        @Test
        @DisplayName("sin version esperada no se deja actualizar")
        void sin_version_esperada_no_se_deja_actualizar() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            ProductJpaEntity product = PetshopCatalogMother.product(PRODUCT_ID, company);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductPresentationJpaEntity existing = PetshopCatalogMother.presentation(1L, company,
                    product, "Unidad", unit, 1, "3500", true, 5L);
            when(presentations.findByIdAndCompany_Id(1L, COMPANY_ID))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.updatePresentation(1L,
                    new PresentationWrite(PRODUCT_ID, "Otro nombre", "UN", 1,
                            new BigDecimal("3900"), true, List.of(), null),
                    COMPANY_ID, ACTOR_ID)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expectedVersion is required");
        }

        @Test
        @DisplayName("una presentacion no se puede mover a otro producto")
        void una_presentacion_no_se_puede_mover_a_otro_producto() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            ProductJpaEntity product = PetshopCatalogMother.product(PRODUCT_ID, company);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductPresentationJpaEntity existing = PetshopCatalogMother.presentation(1L, company,
                    product, "Unidad", unit, 1, "3500", true, 5L);
            when(presentations.findByIdAndCompany_Id(1L, COMPANY_ID))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.updatePresentation(1L,
                    new PresentationWrite(99L, "Unidad", "UN", 1, new BigDecimal("3500"), true,
                            List.of(), 5L),
                    COMPANY_ID, ACTOR_ID)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be moved to another product");
        }

        @Test
        @DisplayName("desmarcar la predeterminada sin poner otra no se permite")
        void desmarcar_la_predeterminada_sin_poner_otra_no_se_permite() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            ProductJpaEntity product = PetshopCatalogMother.product(PRODUCT_ID, company);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductPresentationJpaEntity existing = PetshopCatalogMother.presentation(1L, company,
                    product, "Unidad", unit, 1, "3500", true, 5L);
            when(presentations.findByIdAndCompany_Id(1L, COMPANY_ID))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.updatePresentation(1L,
                    new PresentationWrite(PRODUCT_ID, "Unidad", "UN", 1, new BigDecimal("3500"),
                            false, List.of(), 5L),
                    COMPANY_ID, ACTOR_ID)).isInstanceOf(PetshopCatalogConflictException.class)
                    .hasMessageContaining("Asigna otra presentación predeterminada");
        }

        @Test
        @DisplayName("una presentacion de otra empresa no se encuentra")
        void una_presentacion_de_otra_empresa_no_se_encuentra() {
            when(presentations.findByIdAndCompany_Id(1L, OTHER_COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePresentation(1L,
                    new PresentationWrite(PRODUCT_ID, "Unidad", "UN", 1, new BigDecimal("3500"),
                            true, List.of(), 5L),
                    OTHER_COMPANY_ID, ACTOR_ID)).isInstanceOf(PetshopCatalogNotFoundException.class)
                    .hasMessageContaining("Presentation not found: 1");

            verifyNoInteractions(units, catalogBarcodes, products);
        }
    }

    @Nested
    @DisplayName("borrar presentacion")
    class BorrarPresentacion {

        @Test
        @DisplayName("borra la presentacion y se lleva sus codigos de barras")
        void borra_la_presentacion_y_se_lleva_sus_codigos() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            ProductJpaEntity product = PetshopCatalogMother.product(PRODUCT_ID, company);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductPresentationJpaEntity existing = PetshopCatalogMother.presentation(1L, company,
                    product, "Caja x12", unit, 12, "36000", false, 5L);
            CatalogBarcodeJpaEntity barcode = PetshopCatalogMother.barcodeForPresentation(101L,
                    company, "7705555555555", existing);
            when(presentations.findByIdAndCompany_Id(1L, COMPANY_ID))
                    .thenReturn(Optional.of(existing));
            when(bundleItems.existsByCompany_IdAndPresentation_IdAndBundle_EnabledTrue(COMPANY_ID,
                    1L)).thenReturn(false);
            when(catalogBarcodes.findAllByCompany_IdAndPresentation_IdOrderByBarcode(COMPANY_ID,
                    1L)).thenReturn(List.of(barcode));

            service.deletePresentation(1L, 5L, COMPANY_ID);

            verify(catalogBarcodes).deleteAll(List.of(barcode));
            verify(catalogBarcodes).flush();
            verify(presentations).delete(existing);
        }

        @Test
        @DisplayName("la predeterminada no se puede borrar")
        void la_predeterminada_no_se_puede_borrar() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            ProductJpaEntity product = PetshopCatalogMother.product(PRODUCT_ID, company);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductPresentationJpaEntity existing = PetshopCatalogMother.presentation(1L, company,
                    product, "Unidad", unit, 1, "3500", true, 5L);
            when(presentations.findByIdAndCompany_Id(1L, COMPANY_ID))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.deletePresentation(1L, 5L, COMPANY_ID))
                    .isInstanceOf(PetshopCatalogConflictException.class)
                    .hasMessageContaining("no se puede eliminar");

            verify(presentations, never()).delete(any());
            verifyNoInteractions(catalogBarcodes, bundleItems);
        }

        @Test
        @DisplayName("una presentacion dentro de un combo activo no se puede borrar")
        void una_presentacion_en_un_combo_activo_no_se_borra() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            ProductJpaEntity product = PetshopCatalogMother.product(PRODUCT_ID, company);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductPresentationJpaEntity existing = PetshopCatalogMother.presentation(1L, company,
                    product, "Caja x12", unit, 12, "36000", false, 5L);
            when(presentations.findByIdAndCompany_Id(1L, COMPANY_ID))
                    .thenReturn(Optional.of(existing));
            when(bundleItems.existsByCompany_IdAndPresentation_IdAndBundle_EnabledTrue(COMPANY_ID,
                    1L)).thenReturn(true);

            assertThatThrownBy(() -> service.deletePresentation(1L, 5L, COMPANY_ID))
                    .isInstanceOf(PetshopCatalogConflictException.class)
                    .hasMessageContaining("pertenece a un combo activo");

            verify(presentations, never()).delete(any());
            verifyNoInteractions(catalogBarcodes);
        }

        @Test
        @DisplayName("una presentacion de otra empresa no se encuentra al borrar")
        void una_presentacion_de_otra_empresa_no_se_encuentra_al_borrar() {
            when(presentations.findByIdAndCompany_Id(1L, OTHER_COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deletePresentation(1L, 5L, OTHER_COMPANY_ID))
                    .isInstanceOf(PetshopCatalogNotFoundException.class);

            verifyNoInteractions(bundleItems, catalogBarcodes);
        }
    }

    @Nested
    @DisplayName("crear combo")
    class CrearBundle {

        @Test
        @DisplayName("guarda el combo con sus lineas resueltas y sus codigos de barras")
        void guarda_el_combo_con_sus_lineas_y_sus_codigos() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            ProductJpaEntity product = PetshopCatalogMother.product(PRODUCT_ID, company);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductPresentationJpaEntity presentation = PetshopCatalogMother.presentation(1L,
                    company, product, "Unidad", unit, 1, "3500", true, 5L);
            CatalogBarcodeJpaEntity savedBarcode = PetshopCatalogMother.barcodeForBundle(101L,
                    company, "7706666666666", null);
            when(companies.findById(COMPANY_ID)).thenReturn(Optional.of(company));
            when(bundles.existsByCompany_IdAndName(COMPANY_ID, "Combo cachorro")).thenReturn(false);
            when(bundles.existsByCompany_IdAndCode(COMPANY_ID, "COMBO-1")).thenReturn(false);
            when(units.findById("UN")).thenReturn(Optional.of(unit));
            when(bundles.save(any())).thenAnswer(invocation -> {
                ProductBundleJpaEntity saved = invocation.getArgument(0);
                saved.setId(2L);
                saved.setVersion(1L);
                return saved;
            });
            when(presentations.findByIdAndCompany_Id(1L, COMPANY_ID))
                    .thenReturn(Optional.of(presentation));
            when(bundleItems.findAllByCompany_IdAndBundle_IdOrderByDisplayOrderAsc(COMPANY_ID, 2L))
                    .thenReturn(List.of(PetshopCatalogMother.bundleItem(50L, company, null,
                            presentation, 2, 0)));
            when(catalogBarcodes.findAllByCompany_IdAndBundle_IdOrderByBarcode(COMPANY_ID, 2L))
                    .thenReturn(List.of(), List.of(savedBarcode));
            when(catalogBarcodes.existsByCompany_IdAndBarcode(COMPANY_ID, "7706666666666"))
                    .thenReturn(false);

            BundleDto result = service.createBundle(
                    bundleCommand("Combo cachorro", "COMBO-1",
                            List.of(new BundleItemWrite(1L, 2, 0)), List.of("7706666666666")),
                    COMPANY_ID, ACTOR_ID);

            ArgumentCaptor<ProductBundleJpaEntity> captor = ArgumentCaptor
                    .forClass(ProductBundleJpaEntity.class);
            verify(bundles).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Combo cachorro");
            assertThat(captor.getValue().getCode()).isEqualTo("COMBO-1");

            assertThat(result.barcodes()).containsExactly("7706666666666");
            assertThat(result.items()).singleElement().satisfies(line -> {
                assertThat(line.presentationName()).isEqualTo("Unidad");
                assertThat(line.productName()).isEqualTo("Amoxicilina 500mg");
                assertThat(line.quantity()).isEqualTo(2);
            });
        }

        @Test
        @DisplayName("la empresa debe existir para crear un combo")
        void la_empresa_debe_existir_para_crear_un_combo() {
            when(companies.findById(OTHER_COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.createBundle(
                            bundleCommand("Combo", "COMBO-1",
                                    List.of(new BundleItemWrite(1L, 1, 0)), List.of()),
                            OTHER_COMPANY_ID, ACTOR_ID))
                    .isInstanceOf(PetshopCatalogNotFoundException.class)
                    .hasMessageContaining("Company not found: " + OTHER_COMPANY_ID);

            verifyNoInteractions(bundles, presentations, bundleItems, catalogBarcodes, units);
        }

        @Test
        @DisplayName("el nombre del combo es unico en la empresa")
        void el_nombre_del_combo_es_unico_en_la_empresa() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            when(companies.findById(COMPANY_ID)).thenReturn(Optional.of(company));
            when(bundles.existsByCompany_IdAndName(COMPANY_ID, "Combo cachorro")).thenReturn(true);
            when(bundles.existsByCompany_IdAndCode(COMPANY_ID, "COMBO-1")).thenReturn(false);

            assertThatThrownBy(() -> service.createBundle(
                    bundleCommand("Combo cachorro", "COMBO-1",
                            List.of(new BundleItemWrite(1L, 1, 0)), List.of()),
                    COMPANY_ID, ACTOR_ID)).isInstanceOf(PetshopCatalogConflictException.class)
                    .hasMessageContaining("Ya existe un combo con ese nombre");

            verify(bundles, never()).save(any());
        }

        @Test
        @DisplayName("el codigo del combo es unico en la empresa")
        void el_codigo_del_combo_es_unico_en_la_empresa() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            when(companies.findById(COMPANY_ID)).thenReturn(Optional.of(company));
            when(bundles.existsByCompany_IdAndName(COMPANY_ID, "Combo cachorro")).thenReturn(false);
            when(bundles.existsByCompany_IdAndCode(COMPANY_ID, "COMBO-1")).thenReturn(true);

            assertThatThrownBy(() -> service.createBundle(
                    bundleCommand("Combo cachorro", "COMBO-1",
                            List.of(new BundleItemWrite(1L, 1, 0)), List.of()),
                    COMPANY_ID, ACTOR_ID)).isInstanceOf(PetshopCatalogConflictException.class)
                    .hasMessageContaining("Ya existe un combo con ese código");

            verify(bundles, never()).save(any());
        }

        @Test
        @DisplayName("un combo sin lineas no es un combo")
        void un_combo_sin_lineas_no_es_un_combo() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            when(companies.findById(COMPANY_ID)).thenReturn(Optional.of(company));
            when(bundles.existsByCompany_IdAndName(COMPANY_ID, "Vacio")).thenReturn(false);
            when(bundles.existsByCompany_IdAndCode(COMPANY_ID, "COMBO-0")).thenReturn(false);

            assertThatThrownBy(() -> service.createBundle(
                    bundleCommand("Vacio", "COMBO-0", List.of(), List.of()), COMPANY_ID, ACTOR_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one item");

            verify(bundles, never()).save(any());
        }

        @Test
        @DisplayName("una linea nula en el combo se rechaza")
        void una_linea_nula_en_el_combo_se_rechaza() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            when(companies.findById(COMPANY_ID)).thenReturn(Optional.of(company));
            when(bundles.existsByCompany_IdAndName(COMPANY_ID, "Combo con nula")).thenReturn(false);
            when(bundles.existsByCompany_IdAndCode(COMPANY_ID, "COMBO-NULL")).thenReturn(false);

            assertThatThrownBy(() -> service.createBundle(
                    bundleCommand("Combo con nula", "COMBO-NULL",
                            Arrays.asList(new BundleItemWrite(1L, 1, 0), null), List.of()),
                    COMPANY_ID, ACTOR_ID)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("presentationId is required");

            verify(bundles, never()).save(any());
        }

        @Test
        @DisplayName("la misma presentacion no se puede repetir en el combo")
        void la_misma_presentacion_no_se_puede_repetir_en_el_combo() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            when(companies.findById(COMPANY_ID)).thenReturn(Optional.of(company));
            when(bundles.existsByCompany_IdAndName(COMPANY_ID, "Combo")).thenReturn(false);
            when(bundles.existsByCompany_IdAndCode(COMPANY_ID, "COMBO-2")).thenReturn(false);

            assertThatThrownBy(
                    () -> service.createBundle(
                            bundleCommand("Combo", "COMBO-2",
                                    List.of(new BundleItemWrite(1L, 1, 0),
                                            new BundleItemWrite(1L, 3, 1)),
                                    List.of()),
                            COMPANY_ID, ACTOR_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be repeated in the same bundle");
        }

        @Test
        @DisplayName("dos lineas no pueden compartir el orden de presentacion")
        void dos_lineas_no_pueden_compartir_el_orden_de_presentacion() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            when(companies.findById(COMPANY_ID)).thenReturn(Optional.of(company));
            when(bundles.existsByCompany_IdAndName(COMPANY_ID, "Combo")).thenReturn(false);
            when(bundles.existsByCompany_IdAndCode(COMPANY_ID, "COMBO-3")).thenReturn(false);

            assertThatThrownBy(
                    () -> service.createBundle(
                            bundleCommand("Combo", "COMBO-3",
                                    List.of(new BundleItemWrite(1L, 1, 0),
                                            new BundleItemWrite(2L, 1, 0)),
                                    List.of()),
                            COMPANY_ID, ACTOR_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("displayOrder cannot be repeated");
        }

        @Test
        @DisplayName("una presentacion de otra empresa en el combo no se encuentra")
        void una_presentacion_de_otra_empresa_en_el_combo_no_se_encuentra() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            when(companies.findById(COMPANY_ID)).thenReturn(Optional.of(company));
            when(bundles.existsByCompany_IdAndName(COMPANY_ID, "Combo cachorro")).thenReturn(false);
            when(bundles.existsByCompany_IdAndCode(COMPANY_ID, "COMBO-1")).thenReturn(false);
            when(units.findById("UN")).thenReturn(Optional.of(unit));
            when(bundles.save(any())).thenAnswer(invocation -> {
                ProductBundleJpaEntity saved = invocation.getArgument(0);
                saved.setId(2L);
                return saved;
            });
            when(presentations.findByIdAndCompany_Id(1L, COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createBundle(
                    bundleCommand("Combo cachorro", "COMBO-1",
                            List.of(new BundleItemWrite(1L, 2, 0)), List.of()),
                    COMPANY_ID, ACTOR_ID)).isInstanceOf(PetshopCatalogNotFoundException.class)
                    .hasMessageContaining("Presentation not found: 1");

            verifyNoInteractions(catalogBarcodes);
        }
    }

    @Nested
    @DisplayName("actualizar combo")
    class ActualizarBundle {

        @Test
        @DisplayName("actualizar reemplaza las lineas y los campos del combo")
        void actualizar_reemplaza_las_lineas_y_los_campos_del_combo() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            ProductJpaEntity product = PetshopCatalogMother.product(PRODUCT_ID, company);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductPresentationJpaEntity presentation = PetshopCatalogMother.presentation(1L,
                    company, product, "Unidad", unit, 1, "3500", true, 5L);
            ProductBundleJpaEntity existing = PetshopCatalogMother.bundle(2L, company,
                    "Combo cachorro", "COMBO-1", unit, "50000", 3L);
            when(bundles.findByIdAndCompany_Id(2L, COMPANY_ID)).thenReturn(Optional.of(existing));
            when(bundles.existsByCompany_IdAndNameAndIdNot(COMPANY_ID, "Combo cachorro", 2L))
                    .thenReturn(false);
            when(bundles.existsByCompany_IdAndCodeAndIdNot(COMPANY_ID, "COMBO-1", 2L))
                    .thenReturn(false);
            when(units.findById("UN")).thenReturn(Optional.of(unit));
            when(presentations.findByIdAndCompany_Id(1L, COMPANY_ID))
                    .thenReturn(Optional.of(presentation));
            when(bundleItems.findAllByCompany_IdAndBundle_IdOrderByDisplayOrderAsc(COMPANY_ID, 2L))
                    .thenReturn(List.of(PetshopCatalogMother.bundleItem(51L, company, null,
                            presentation, 1, 0)));
            when(catalogBarcodes.findAllByCompany_IdAndBundle_IdOrderByBarcode(COMPANY_ID, 2L))
                    .thenReturn(List.of());

            BundleDto result = service.updateBundle(2L,
                    new BundleWrite("Combo cachorro", "COMBO-1", "UN", new BigDecimal("60000"),
                            List.of(new BundleItemWrite(1L, 1, 0)), List.of(), 3L),
                    COMPANY_ID, ACTOR_ID);

            assertThat(existing.getSalePrice()).isEqualByComparingTo("60000");
            assertThat(result.salePrice()).isEqualByComparingTo("60000");
            assertThat(result.items()).singleElement()
                    .satisfies(line -> assertThat(line.quantity()).isEqualTo(1));
        }

        @Test
        @DisplayName("una version vieja es un conflicto al actualizar un combo")
        void una_version_vieja_es_un_conflicto_al_actualizar_un_combo() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductBundleJpaEntity existing = PetshopCatalogMother.bundle(2L, company,
                    "Combo cachorro", "COMBO-1", unit, "50000", 3L);
            when(bundles.findByIdAndCompany_Id(2L, COMPANY_ID)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.updateBundle(2L,
                    new BundleWrite("Combo cachorro", "COMBO-1", "UN", new BigDecimal("60000"),
                            List.of(new BundleItemWrite(1L, 1, 0)), List.of(), 999L),
                    COMPANY_ID, ACTOR_ID)).isInstanceOf(PetshopCatalogConflictException.class)
                    .hasMessageContaining("modificado por otra operación");

            verify(bundles, never()).save(any());
            verifyNoInteractions(presentations, catalogBarcodes, bundleItems, units);
        }

        @Test
        @DisplayName("el nombre duplicado al actualizar excluye al propio combo")
        void el_nombre_duplicado_al_actualizar_excluye_al_propio_combo() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductBundleJpaEntity existing = PetshopCatalogMother.bundle(2L, company,
                    "Combo cachorro", "COMBO-1", unit, "50000", 3L);
            when(bundles.findByIdAndCompany_Id(2L, COMPANY_ID)).thenReturn(Optional.of(existing));
            when(bundles.existsByCompany_IdAndNameAndIdNot(COMPANY_ID, "Combo cachorro", 2L))
                    .thenReturn(true);
            when(bundles.existsByCompany_IdAndCodeAndIdNot(COMPANY_ID, "COMBO-1", 2L))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.updateBundle(2L,
                    new BundleWrite("Combo cachorro", "COMBO-1", "UN", new BigDecimal("60000"),
                            List.of(new BundleItemWrite(1L, 1, 0)), List.of(), 3L),
                    COMPANY_ID, ACTOR_ID)).isInstanceOf(PetshopCatalogConflictException.class)
                    .hasMessageContaining("Ya existe un combo con ese nombre");

            verify(bundles, never()).save(any());
        }

        @Test
        @DisplayName("un combo de otra empresa no se encuentra al actualizar")
        void un_combo_de_otra_empresa_no_se_encuentra_al_actualizar() {
            when(bundles.findByIdAndCompany_Id(2L, OTHER_COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateBundle(2L,
                    new BundleWrite("Combo", "COMBO-1", "UN", new BigDecimal("60000"),
                            List.of(new BundleItemWrite(1L, 1, 0)), List.of(), 3L),
                    OTHER_COMPANY_ID, ACTOR_ID))
                    .isInstanceOf(PetshopCatalogNotFoundException.class);

            verifyNoInteractions(presentations, catalogBarcodes, bundleItems, units);
        }
    }

    @Nested
    @DisplayName("borrar combo")
    class BorrarBundle {

        @Test
        @DisplayName("borra el combo y se lleva sus codigos de barras")
        void borra_el_combo_y_se_lleva_sus_codigos() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductBundleJpaEntity existing = PetshopCatalogMother.bundle(2L, company,
                    "Combo cachorro", "COMBO-1", unit, "50000", 3L);
            CatalogBarcodeJpaEntity barcode = PetshopCatalogMother.barcodeForBundle(101L, company,
                    "7706666666666", existing);
            when(bundles.findByIdAndCompany_Id(2L, COMPANY_ID)).thenReturn(Optional.of(existing));
            when(catalogBarcodes.findAllByCompany_IdAndBundle_IdOrderByBarcode(COMPANY_ID, 2L))
                    .thenReturn(List.of(barcode));

            service.deleteBundle(2L, 3L, COMPANY_ID);

            verify(catalogBarcodes).deleteAll(List.of(barcode));
            verify(catalogBarcodes).flush();
            verify(bundles).delete(existing);
        }

        @Test
        @DisplayName("un combo de otra empresa no se encuentra al borrar")
        void un_combo_de_otra_empresa_no_se_encuentra_al_borrar() {
            when(bundles.findByIdAndCompany_Id(2L, OTHER_COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteBundle(2L, 3L, OTHER_COMPANY_ID))
                    .isInstanceOf(PetshopCatalogNotFoundException.class);

            verifyNoInteractions(catalogBarcodes);
        }

        @Test
        @DisplayName("una version vieja impide borrar el combo")
        void una_version_vieja_impide_borrar_el_combo() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductBundleJpaEntity existing = PetshopCatalogMother.bundle(2L, company,
                    "Combo cachorro", "COMBO-1", unit, "50000", 3L);
            when(bundles.findByIdAndCompany_Id(2L, COMPANY_ID)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.deleteBundle(2L, 999L, COMPANY_ID))
                    .isInstanceOf(PetshopCatalogConflictException.class);

            verify(bundles, never()).delete(any());
            verifyNoInteractions(catalogBarcodes);
        }
    }

    @Nested
    @DisplayName("busqueda por codigo de barras")
    class BuscarPorCodigoDeBarras {

        @Test
        @DisplayName("un codigo de presentacion devuelve el producto, la unidad y el factor")
        void un_codigo_de_presentacion_devuelve_lo_vendible() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            ProductJpaEntity product = PetshopCatalogMother.product(PRODUCT_ID, company);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductPresentationJpaEntity presentation = PetshopCatalogMother.presentation(1L,
                    company, product, "Unidad", unit, 1, "3500", true, 5L);
            CatalogBarcodeJpaEntity found = PetshopCatalogMother.barcodeForPresentation(101L,
                    company, "7701234567890", presentation);
            when(catalogBarcodes.findByCompany_IdAndBarcode(COMPANY_ID, "7701234567890"))
                    .thenReturn(Optional.of(found));

            BarcodeLookupDto result = service.findByBarcode("7701234567890", COMPANY_ID);

            assertThat(result.itemType()).isEqualTo(SellableItemType.PRESENTATION);
            assertThat(result.name()).isEqualTo("Amoxicilina 500mg - Unidad");
            assertThat(result.unitMeasureCode()).isEqualTo("UN");
            assertThat(result.salePrice()).isEqualByComparingTo("3500");
            assertThat(result.conversionFactor()).isEqualTo(1);
        }

        @Test
        @DisplayName("un codigo de combo devuelve el combo y sin factor")
        void un_codigo_de_combo_devuelve_el_combo_sin_factor() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductBundleJpaEntity bundle = PetshopCatalogMother.bundle(2L, company,
                    "Combo cachorro", "COMBO-1", unit, "50000", 3L);
            CatalogBarcodeJpaEntity found = PetshopCatalogMother.barcodeForBundle(102L, company,
                    "7706666666666", bundle);
            when(catalogBarcodes.findByCompany_IdAndBarcode(COMPANY_ID, "7706666666666"))
                    .thenReturn(Optional.of(found));

            BarcodeLookupDto result = service.findByBarcode("7706666666666", COMPANY_ID);

            assertThat(result.itemType()).isEqualTo(SellableItemType.BUNDLE);
            assertThat(result.name()).isEqualTo("Combo cachorro");
            assertThat(result.conversionFactor()).isNull();
        }

        @Test
        @DisplayName("el codigo se busca normalizado, sin espacios de sobra")
        void el_codigo_se_busca_normalizado() {
            CompanyJpaEntity company = PetshopCatalogMother.company(COMPANY_ID);
            ProductJpaEntity product = PetshopCatalogMother.product(PRODUCT_ID, company);
            UnitMeasureCatalogJpaEntity unit = PetshopCatalogMother.unit("UN", "Unidad", "un");
            ProductPresentationJpaEntity presentation = PetshopCatalogMother.presentation(1L,
                    company, product, "Unidad", unit, 1, "3500", true, 5L);
            CatalogBarcodeJpaEntity found = PetshopCatalogMother.barcodeForPresentation(101L,
                    company, "7701234567890", presentation);
            when(catalogBarcodes.findByCompany_IdAndBarcode(COMPANY_ID, "7701234567890"))
                    .thenReturn(Optional.of(found));

            BarcodeLookupDto result = service.findByBarcode("  7701234567890  ", COMPANY_ID);

            assertThat(result.barcode()).isEqualTo("7701234567890");
        }

        @Test
        @DisplayName("un codigo que no existe se reporta como no encontrado")
        void un_codigo_que_no_existe_se_reporta() {
            when(catalogBarcodes.findByCompany_IdAndBarcode(COMPANY_ID, "0000000000000"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByBarcode("0000000000000", COMPANY_ID))
                    .isInstanceOf(PetshopCatalogNotFoundException.class);
        }

        @Test
        @DisplayName("un codigo de otra empresa no se encuentra")
        void un_codigo_de_otra_empresa_no_se_encuentra() {
            when(catalogBarcodes.findByCompany_IdAndBarcode(OTHER_COMPANY_ID, "7701234567890"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByBarcode("7701234567890", OTHER_COMPANY_ID))
                    .isInstanceOf(PetshopCatalogNotFoundException.class);
        }
    }
}
