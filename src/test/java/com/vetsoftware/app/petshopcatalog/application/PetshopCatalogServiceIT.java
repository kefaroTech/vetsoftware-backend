package com.vetsoftware.app.petshopcatalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.catalog.domain.SellableItemType;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.BundleDto;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.BundleItemWrite;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.BundleWrite;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.PresentationDto;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.PresentationWrite;
import com.vetsoftware.app.petshopcatalog.domain.PetshopCatalogConflictException;
import com.vetsoftware.app.petshopcatalog.domain.PetshopCatalogNotFoundException;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja del catalogo de petshop contra MySQL real.
 *
 * <p>
 * El servicio es orquestacion de siete repositorios JPA de otras features, y
 * casi todas sus reglas se apoyan en la base: unicidad de nombre por producto,
 * el indice unico de presentacion predeterminada (que obliga a un flush entre
 * desmarcar la anterior y marcar la nueva) y el borrado de codigos de barras
 * antes de borrar su dueño. Con dobles, todo eso da verde sin probar nada.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("PetshopCatalogService — presentaciones, combos y codigos de barras")
class PetshopCatalogServiceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long PRODUCT = SchemaSeed.PRODUCT_ID;
    private static final Long OTRO_PRODUCT = SchemaSeed.OTRO_PRODUCT_ID;
    private static final Long ACTOR = SchemaSeed.EMPLOYEE_ID;

    @Autowired
    private PetshopCatalogService service;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrar() {
        SchemaSeed.seed(entityManager);
        unidad("UN", "Unidad", "un");
        unidad("CAJ", "Caja", "cj");
        entityManager.flush();
    }

    private void unidad(String codigo, String nombre, String simbolo) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO unit_measure_catalog (code, name, symbol, created_date, enabled)
                VALUES (?, ?, ?, '2026-01-15 08:00:00', true)
                """).setParameter(1, codigo).setParameter(2, nombre).setParameter(3, simbolo)
                .executeUpdate();
    }

    private PresentationWrite presentacion(String nombre, int factor, String precio,
            boolean predeterminada, List<String> codigos, Long version) {
        return new PresentationWrite(PRODUCT, nombre, factor == 1 ? "UN" : "CAJ", factor,
                new BigDecimal(precio), predeterminada, codigos, version);
    }

    private PresentationDto crearUnidadBase() {
        return service.createPresentation(
                presentacion("Unidad", 1, "3500", true, List.of("7701234567890"), null), COMPANY,
                ACTOR);
    }

    @Nested
    @DisplayName("crear presentacion")
    class Crear {

        @Test
        @DisplayName("guarda la presentacion con su unidad, factor, precio y codigos")
        void guarda_la_presentacion_completa() {
            PresentationDto creada = crearUnidadBase();

            assertThat(creada.id()).isNotNull();
            assertThat(creada.productName()).isEqualTo("Amoxicilina 500mg");
            assertThat(creada.unitMeasureCode()).isEqualTo("UN");
            assertThat(creada.conversionFactor()).isEqualTo(1);
            assertThat(creada.salePrice()).isEqualByComparingTo("3500");
            assertThat(creada.defaultPresentation()).isTrue();
            assertThat(creada.barcodes()).containsExactly("7701234567890");
            assertThat(creada.version()).isNotNull();
        }

        @Test
        @DisplayName("dos presentaciones con el mismo nombre en el producto chocan")
        void dos_presentaciones_con_el_mismo_nombre_chocan() {
            crearUnidadBase();

            assertThatThrownBy(() -> service.createPresentation(
                    presentacion("Unidad", 12, "36000", false, List.of(), null), COMPANY, ACTOR))
                    .isInstanceOf(PetshopCatalogConflictException.class)
                    .hasMessageContaining("Ya existe una presentación con ese nombre");
        }

        @Test
        @DisplayName("la predeterminada tiene que tener factor uno")
        void la_predeterminada_tiene_que_tener_factor_uno() {
            // La predeterminada define la unidad base del producto: con factor 12 el
            // precio base del producto seria el de la caja y todo el POS vendria mal.
            assertThatThrownBy(() -> service.createPresentation(
                    presentacion("Caja x12", 12, "36000", true, List.of(), null), COMPANY, ACTOR))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("conversionFactor 1");
        }

        @Test
        @DisplayName("marcar una nueva como predeterminada desmarca la anterior")
        void marcar_una_nueva_predeterminada_desmarca_la_anterior() {
            PresentationDto primera = crearUnidadBase();

            service.createPresentation(new PresentationWrite(PRODUCT, "Unidad nueva", "UN", 1,
                    new BigDecimal("4000"), true, List.of(), null), COMPANY, ACTOR);

            // Hay un indice unico de "una sola predeterminada por producto": si el
            // servicio no hiciera flush entre desmarcar y marcar, la base rechazaria la
            // insercion. Ese flush no se puede comprobar con un doble.
            List<PresentationDto> todas = service.listPresentations(PRODUCT, COMPANY);
            assertThat(todas).filteredOn(PresentationDto::defaultPresentation)
                    .extracting(PresentationDto::name).containsExactly("Unidad nueva");
            assertThat(todas).extracting(PresentationDto::id).contains(primera.id());
        }

        @Test
        @DisplayName("la predeterminada sincroniza la unidad y el precio base del producto")
        void la_predeterminada_sincroniza_el_producto() {
            crearUnidadBase();
            entityManager.flush();

            Object[] fila = (Object[]) entityManager.createNativeQuery("""
                    SELECT base_unit_measure_code, sale_price FROM products WHERE id = ?
                    """).setParameter(1, PRODUCT).getSingleResult();

            // El producto se queda con la unidad y el precio de su presentacion base:
            // es lo que lee el POS cuando se vende el producto sin elegir presentacion.
            assertThat(fila[0]).isEqualTo("UN");
            assertThat(new BigDecimal(fila[1].toString())).isEqualByComparingTo("3500");
        }

        @Test
        @DisplayName("un producto de otra empresa no existe para el catalogo")
        void un_producto_de_otra_empresa_no_existe() {
            assertThatThrownBy(() -> service.createPresentation(
                    presentacion("Unidad", 1, "3500", false, List.of(), null),
                    SchemaSeed.OTRA_COMPANY_ID, ACTOR))
                    .isInstanceOf(PetshopCatalogNotFoundException.class);
        }

        @Test
        @DisplayName("sin productId no se puede crear la presentacion")
        void sin_product_id_no_se_puede_crear() {
            assertThatThrownBy(
                    () -> service.createPresentation(new PresentationWrite(null, "Unidad", "UN", 1,
                            new BigDecimal("3500"), false, List.of(), null), COMPANY, ACTOR))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("productId is required");
        }

        @Test
        @DisplayName("un codigo de barras ya asignado a otra presentacion no se puede repetir")
        void un_codigo_ya_asignado_a_otra_presentacion_no_se_repite() {
            crearUnidadBase();

            // El codigo de barras es unico en toda la empresa, no solo dentro de la
            // presentacion que se esta creando: si dos presentaciones lo compartieran, el
            // POS no sabria cual vender al escanearlo.
            assertThatThrownBy(() -> service.createPresentation(
                    presentacion("Caja x12", 12, "36000", false, List.of("7701234567890"), null),
                    COMPANY, ACTOR)).isInstanceOf(PetshopCatalogConflictException.class)
                    .hasMessageContaining("El código de barras ya está asignado");
        }
    }

    @Nested
    @DisplayName("actualizar presentacion")
    class Actualizar {

        @Test
        @DisplayName("cambia nombre, precio y codigos conservando el id")
        void cambia_los_campos_conservando_el_id() {
            PresentationDto creada = crearUnidadBase();

            PresentationDto actualizada = service.updatePresentation(creada.id(),
                    new PresentationWrite(PRODUCT, "Unidad suelta", "UN", 1, new BigDecimal("3900"),
                            true, List.of("7709999999999"), creada.version()),
                    COMPANY, ACTOR);

            assertThat(actualizada.id()).isEqualTo(creada.id());
            assertThat(actualizada.name()).isEqualTo("Unidad suelta");
            assertThat(actualizada.salePrice()).isEqualByComparingTo("3900");
            assertThat(actualizada.barcodes()).containsExactly("7709999999999");
        }

        @Test
        @DisplayName("una version vieja se rechaza como conflicto, no como error de datos")
        void una_version_vieja_se_rechaza_como_conflicto() {
            PresentationDto creada = crearUnidadBase();

            // Dos pestañas editando la misma presentacion: la segunda tiene que enterarse
            // en vez de pisar lo que guardo la primera.
            assertThatThrownBy(() -> service.updatePresentation(creada.id(),
                    presentacion("Otro nombre", 1, "3900", true, List.of(), 999L), COMPANY, ACTOR))
                    .isInstanceOf(PetshopCatalogConflictException.class)
                    .hasMessageContaining("modificado por otra operación");
        }

        @Test
        @DisplayName("sin version esperada no se deja actualizar")
        void sin_version_esperada_no_se_deja_actualizar() {
            PresentationDto creada = crearUnidadBase();

            assertThatThrownBy(() -> service.updatePresentation(creada.id(),
                    presentacion("Otro nombre", 1, "3900", true, List.of(), null), COMPANY, ACTOR))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expectedVersion is required");
        }

        @Test
        @DisplayName("una presentacion no se puede mover a otro producto")
        void una_presentacion_no_se_puede_mover_de_producto() {
            PresentationDto creada = crearUnidadBase();

            // Moverla arrastraria sus codigos de barras al producto equivocado.
            assertThatThrownBy(() -> service.updatePresentation(creada.id(),
                    new PresentationWrite(OTRO_PRODUCT, "Unidad", "UN", 1, new BigDecimal("3500"),
                            true, List.of(), creada.version()),
                    COMPANY, ACTOR)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be moved to another product");
        }

        @Test
        @DisplayName("sin productId en el comando no se intenta mover la presentacion")
        void sin_product_id_en_el_comando_no_se_mueve() {
            PresentationDto creada = crearUnidadBase();

            // El front puede omitir productId cuando no cambia: el servicio no debe
            // interpretarlo como "mover a ningun producto" y debe conservar el original.
            PresentationDto actualizada = service.updatePresentation(creada.id(),
                    new PresentationWrite(null, "Unidad renombrada", "UN", 1,
                            new BigDecimal("3600"), true, List.of(), creada.version()),
                    COMPANY, ACTOR);

            assertThat(actualizada.productName()).isEqualTo("Amoxicilina 500mg");
            assertThat(actualizada.name()).isEqualTo("Unidad renombrada");
        }

        @Test
        @DisplayName("desmarcar la predeterminada sin poner otra no se permite")
        void desmarcar_la_predeterminada_sin_poner_otra_no_se_permite() {
            PresentationDto creada = crearUnidadBase();

            // El producto quedaria sin unidad base y sin precio de referencia.
            assertThatThrownBy(() -> service.updatePresentation(creada.id(),
                    presentacion("Unidad", 1, "3500", false, List.of(), creada.version()), COMPANY,
                    ACTOR)).isInstanceOf(PetshopCatalogConflictException.class)
                    .hasMessageContaining("Asigna otra presentación predeterminada");
        }
    }

    @Nested
    @DisplayName("borrar presentacion")
    class Borrar {

        @Test
        @DisplayName("borra la presentacion y se lleva sus codigos de barras")
        void borra_la_presentacion_y_sus_codigos() {
            crearUnidadBase();
            PresentationDto caja = service.createPresentation(
                    presentacion("Caja x12", 12, "36000", false, List.of("7705555555555"), null),
                    COMPANY, ACTOR);

            service.deletePresentation(caja.id(), caja.version(), COMPANY);

            // Si los codigos no se borraran antes, la FK dejaria huerfanos y el siguiente
            // escaneo devolveria una presentacion que ya no existe.
            assertThat(service.listPresentations(PRODUCT, COMPANY))
                    .extracting(PresentationDto::name).containsExactly("Unidad");
            assertThatThrownBy(() -> service.findByBarcode("7705555555555", COMPANY))
                    .isInstanceOf(PetshopCatalogNotFoundException.class);
        }

        @Test
        @DisplayName("la predeterminada no se puede borrar")
        void la_predeterminada_no_se_puede_borrar() {
            PresentationDto base = crearUnidadBase();

            assertThatThrownBy(() -> service.deletePresentation(base.id(), base.version(), COMPANY))
                    .isInstanceOf(PetshopCatalogConflictException.class)
                    .hasMessageContaining("no se puede eliminar");
        }

        @Test
        @DisplayName("una presentacion dentro de un combo activo no se puede borrar")
        void una_presentacion_en_un_combo_activo_no_se_borra() {
            crearUnidadBase();
            PresentationDto caja = service.createPresentation(
                    presentacion("Caja x12", 12, "36000", false, List.of(), null), COMPANY, ACTOR);
            service.createBundle(
                    new BundleWrite("Combo cachorro", "COMBO-1", "UN", new BigDecimal("50000"),
                            List.of(new BundleItemWrite(caja.id(), 2, 0)), List.of(), null),
                    COMPANY, ACTOR);

            // Borrarla dejaria el combo vendiendo algo que no se puede despachar.
            assertThatThrownBy(() -> service.deletePresentation(caja.id(), caja.version(), COMPANY))
                    .isInstanceOf(PetshopCatalogConflictException.class)
                    .hasMessageContaining("pertenece a un combo activo");
        }
    }

    @Nested
    @DisplayName("combos")
    class Combos {

        private BundleDto comboCon(PresentationDto presentacion, String nombre, String codigo) {
            return service.createBundle(new BundleWrite(nombre, codigo, "UN",
                    new BigDecimal("50000"), List.of(new BundleItemWrite(presentacion.id(), 2, 0)),
                    List.of("7706666666666"), null), COMPANY, ACTOR);
        }

        @Test
        @DisplayName("guarda el combo con sus lineas resueltas y sus codigos")
        void guarda_el_combo_con_sus_lineas() {
            PresentationDto base = crearUnidadBase();

            BundleDto combo = comboCon(base, "Combo cachorro", "COMBO-1");

            assertThat(combo.id()).isNotNull();
            assertThat(combo.salePrice()).isEqualByComparingTo("50000");
            assertThat(combo.barcodes()).containsExactly("7706666666666");
            assertThat(combo.items()).singleElement().satisfies(linea -> {
                assertThat(linea.presentationName()).isEqualTo("Unidad");
                assertThat(linea.productName()).isEqualTo("Amoxicilina 500mg");
                assertThat(linea.quantity()).isEqualTo(2);
            });
        }

        @Test
        @DisplayName("una linea nula en el combo se rechaza")
        void una_linea_nula_se_rechaza() {
            assertThatThrownBy(() -> service.createBundle(new BundleWrite("Combo con nula",
                    "COMBO-NULL", "UN", new BigDecimal("1000"),
                    java.util.Arrays.asList(new BundleItemWrite(1L, 1, 0), null), List.of(), null),
                    COMPANY, ACTOR)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("presentationId is required");
        }

        @Test
        @DisplayName("un combo sin lineas no es un combo")
        void un_combo_sin_lineas_no_es_un_combo() {
            assertThatThrownBy(() -> service.createBundle(new BundleWrite("Vacio", "COMBO-0", "UN",
                    new BigDecimal("1000"), List.of(), List.of(), null), COMPANY, ACTOR))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one item");
        }

        @Test
        @DisplayName("la misma presentacion no se puede repetir en el combo")
        void la_misma_presentacion_no_se_repite() {
            PresentationDto base = crearUnidadBase();

            // Repetirla partiria la cantidad en dos lineas y el despacho sacaria mal.
            assertThatThrownBy(() -> service.createBundle(
                    new BundleWrite("Combo", "COMBO-2", "UN", new BigDecimal("1000"),
                            List.of(new BundleItemWrite(base.id(), 1, 0),
                                    new BundleItemWrite(base.id(), 3, 1)),
                            List.of(), null),
                    COMPANY, ACTOR)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be repeated in the same bundle");
        }

        @Test
        @DisplayName("dos lineas no pueden compartir el orden de presentacion")
        void dos_lineas_no_comparten_orden() {
            PresentationDto base = crearUnidadBase();
            PresentationDto caja = service.createPresentation(
                    presentacion("Caja x12", 12, "36000", false, List.of(), null), COMPANY, ACTOR);

            assertThatThrownBy(() -> service.createBundle(
                    new BundleWrite("Combo", "COMBO-3", "UN", new BigDecimal("1000"),
                            List.of(new BundleItemWrite(base.id(), 1, 0),
                                    new BundleItemWrite(caja.id(), 1, 0)),
                            List.of(), null),
                    COMPANY, ACTOR)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("displayOrder cannot be repeated");
        }

        @Test
        @DisplayName("el nombre y el codigo del combo son unicos en la empresa")
        void el_nombre_y_el_codigo_son_unicos() {
            PresentationDto base = crearUnidadBase();
            comboCon(base, "Combo cachorro", "COMBO-1");

            assertThatThrownBy(() -> service.createBundle(
                    new BundleWrite("Combo cachorro", "COMBO-9", "UN", new BigDecimal("1000"),
                            List.of(new BundleItemWrite(base.id(), 1, 0)), List.of(), null),
                    COMPANY, ACTOR)).isInstanceOf(PetshopCatalogConflictException.class)
                    .hasMessageContaining("Ya existe un combo con ese nombre");

            assertThatThrownBy(() -> service.createBundle(
                    new BundleWrite("Otro nombre", "COMBO-1", "UN", new BigDecimal("1000"),
                            List.of(new BundleItemWrite(base.id(), 1, 0)), List.of(), null),
                    COMPANY, ACTOR)).isInstanceOf(PetshopCatalogConflictException.class)
                    .hasMessageContaining("Ya existe un combo con ese código");
        }

        @Test
        @DisplayName("actualizar reemplaza las lineas, no las acumula")
        void actualizar_reemplaza_las_lineas() {
            PresentationDto base = crearUnidadBase();
            PresentationDto caja = service.createPresentation(
                    presentacion("Caja x12", 12, "36000", false, List.of(), null), COMPANY, ACTOR);
            BundleDto combo = comboCon(base, "Combo cachorro", "COMBO-1");

            BundleDto actualizado = service.updateBundle(combo.id(),
                    new BundleWrite("Combo cachorro", "COMBO-1", "UN", new BigDecimal("60000"),
                            List.of(new BundleItemWrite(caja.id(), 1, 0)), List.of(),
                            combo.version()),
                    COMPANY, ACTOR);

            // Acumularlas dejaria el combo con el contenido viejo mas el nuevo.
            assertThat(actualizado.items()).singleElement()
                    .satisfies(l -> assertThat(l.presentationName()).isEqualTo("Caja x12"));
            assertThat(actualizado.salePrice()).isEqualByComparingTo("60000");
        }

        @Test
        @DisplayName("borrar el combo se lleva sus codigos y sus lineas por cascada de la base")
        void borrar_el_combo_se_lleva_sus_codigos() {
            PresentationDto base = crearUnidadBase();
            BundleDto combo = comboCon(base, "Combo cachorro", "COMBO-1");

            // El servicio borra los codigos a mano pero NO las lineas: de esas se encarga
            // el ON DELETE CASCADE de fk_product_bundle_items_bundle_company. Ese cascade
            // vive en la base y JPA no lo conoce, asi que hay que vaciar el contexto para
            // que el borrado se parezca al de produccion, donde las lineas no estan
            // cargadas. Sin esto, Hibernate intenta grabar lineas que apuntan a un combo
            // ya borrado y falla con un error que no ocurre de verdad.
            entityManager.flush();
            entityManager.clear();

            service.deleteBundle(combo.id(), combo.version(), COMPANY);

            assertThat(service.listBundles(COMPANY)).isEmpty();
            assertThatThrownBy(() -> service.findByBarcode("7706666666666", COMPANY))
                    .isInstanceOf(PetshopCatalogNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("busqueda por codigo de barras")
    class PorCodigoDeBarras {

        @Test
        @DisplayName("un codigo de presentacion devuelve el producto, la unidad y el factor")
        void un_codigo_de_presentacion_devuelve_lo_vendible() {
            crearUnidadBase();

            var encontrado = service.findByBarcode("7701234567890", COMPANY);

            // Es lo que lee el POS al pasar la pistola: sin el factor no sabria cuantas
            // unidades base descontar del inventario.
            assertThat(encontrado.itemType()).isEqualTo(SellableItemType.PRESENTATION);
            assertThat(encontrado.name()).isEqualTo("Amoxicilina 500mg - Unidad");
            assertThat(encontrado.unitMeasureCode()).isEqualTo("UN");
            assertThat(encontrado.salePrice()).isEqualByComparingTo("3500");
            assertThat(encontrado.conversionFactor()).isEqualTo(1);
        }

        @Test
        @DisplayName("un codigo de combo devuelve el combo y sin factor")
        void un_codigo_de_combo_devuelve_el_combo() {
            PresentationDto base = crearUnidadBase();
            service.createBundle(new BundleWrite("Combo cachorro", "COMBO-1", "UN",
                    new BigDecimal("50000"), List.of(new BundleItemWrite(base.id(), 2, 0)),
                    List.of("7706666666666"), null), COMPANY, ACTOR);

            var encontrado = service.findByBarcode("7706666666666", COMPANY);

            assertThat(encontrado.itemType()).isEqualTo(SellableItemType.BUNDLE);
            assertThat(encontrado.name()).isEqualTo("Combo cachorro");
            assertThat(encontrado.conversionFactor()).as("un combo no tiene factor").isNull();
        }

        @Test
        @DisplayName("el codigo se busca normalizado, sin espacios de sobra")
        void el_codigo_se_busca_normalizado() {
            crearUnidadBase();

            assertThat(service.findByBarcode("  7701234567890  ", COMPANY).barcode())
                    .isEqualTo("7701234567890");
        }

        @Test
        @DisplayName("un codigo de otra empresa no se encuentra")
        void un_codigo_de_otra_empresa_no_se_encuentra() {
            crearUnidadBase();

            // El codigo de barras es global en el mundo real, pero el catalogo es por
            // empresa: escanear en otra veterinaria no puede devolver este producto.
            assertThatThrownBy(
                    () -> service.findByBarcode("7701234567890", SchemaSeed.OTRA_COMPANY_ID))
                    .isInstanceOf(PetshopCatalogNotFoundException.class);
        }

        @Test
        @DisplayName("un codigo que no existe se reporta como no encontrado")
        void un_codigo_que_no_existe_se_reporta() {
            assertThatThrownBy(() -> service.findByBarcode("0000000000000", COMPANY))
                    .isInstanceOf(PetshopCatalogNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("las unidades de medida vienen ordenadas por nombre")
        void las_unidades_vienen_ordenadas() {
            List<String> nombres = service.listUnitMeasures().stream()
                    .map(PetshopCatalogUseCase.UnitMeasureDto::name).toList();

            // El catalogo trae unidades sembradas por Liquibase, asi que la asercion no
            // puede ser la lista exacta: lo que se comprueba es el orden.
            assertThat(nombres).isSorted();
            assertThat(nombres).contains("Caja", "Unidad");
        }

        @Test
        @DisplayName("el listado pone la predeterminada primero")
        void el_listado_pone_la_predeterminada_primero() {
            service.createPresentation(
                    presentacion("Caja x12", 12, "36000", false, List.of(), null), COMPANY, ACTOR);
            crearUnidadBase();

            // La predeterminada es la que se ofrece por defecto al vender: tiene que
            // salir arriba aunque alfabeticamente vaya despues.
            assertThat(service.listPresentations(PRODUCT, COMPANY))
                    .extracting(PresentationDto::name).containsExactly("Unidad", "Caja x12");
        }
    }
}
