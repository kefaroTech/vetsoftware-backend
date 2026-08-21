package com.vetsoftware.app.product.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.product.application.command.SearchProductsCommand;
import com.vetsoftware.app.product.domain.CompanyRef;
import com.vetsoftware.app.product.domain.Product;
import com.vetsoftware.app.product.domain.ProductCategoryRef;
import com.vetsoftware.app.product.domain.SupplierRef;
import com.vetsoftware.app.product.domain.TaxRef;
import com.vetsoftware.app.product.domain.TaxTreatment;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia del catalogo de productos contra MySQL real.
 *
 * <p>
 * {@link JpaProductRepository} resuelve cuatro asociaciones (categoria,
 * impuesto, empresa, proveedor) con {@code getReferenceById} y arma una
 * {@link org.springframework.data.jpa.domain.Specification} a mano para la
 * busqueda paginada, con fetch-join condicional segun si la query es de datos o
 * de conteo. Ese SQL real, la query nativa de deshabilitados y el UPDATE nativo
 * de reactivacion no los ejercita un mock de {@code ProductJpaRepository}.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaProductRepository — productos, existencia, busqueda y reactivacion contra MySQL real")
class ProductPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long CATEGORY_ID = SchemaSeed.CATEGORY_ID;
    private static final Long OTRA_CATEGORY_ID = 960L;
    private static final Long TAX_ID = 961L;
    private static final Long SUPPLIER_ID = 962L;

    private static final CompanyRef CLINICA = new CompanyRef(COMPANY, "Veterinaria de prueba",
            "900123456");
    private static final ProductCategoryRef MEDICAMENTOS = new ProductCategoryRef(CATEGORY_ID,
            "Medicamentos");
    private static final ProductCategoryRef ACCESORIOS = new ProductCategoryRef(OTRA_CATEGORY_ID,
            "Accesorios");
    private static final TaxRef IVA_19 = new TaxRef(TAX_ID, "IVA 19%", new BigDecimal("19.00"));
    private static final SupplierRef PROVEEDOR = new SupplierRef(SUPPLIER_ID, "Distribuidora Sur");

    @Autowired
    private JpaProductRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrar() {
        SchemaSeed.seed(entityManager);
        // SchemaSeed inserta sus dos productos raiz por SQL nativo con
        // tax_treatment=GRAVADO pero sin tax_id: pasa la FK obligatoria de otras
        // features, pero el dominio Product exige impuesto para ese tratamiento. Un
        // listado sin filtro (findAll/findAllByCompanyId) los mapea junto con los
        // productos propios de este test, y el mapper reconstruiria un Product
        // invalido. Se neutralizan aqui, dentro de la transaccion con rollback de
        // este test, sin tocar SchemaSeed (lo usan otras features en paralelo).
        insert(String.format("""
                UPDATE products SET tax_treatment = 'EXCLUIDO' WHERE company_id = %d
                """, COMPANY));
        insert(String.format("""
                INSERT IGNORE INTO product_categories (id, name, description, company_id)
                VALUES (%d, 'Accesorios', 'Categoria secundaria', %d)
                """, OTRA_CATEGORY_ID, COMPANY));
        insert(String.format("""
                INSERT IGNORE INTO taxes (id, name, percentage, tax_scheme, company_id,
                                          created_date, version, enabled)
                VALUES (%d, 'IVA 19%%', 19.00, 'IVA', %d, '2026-01-15 08:00:00', 0, true)
                """, TAX_ID, COMPANY));
        insert(String.format("""
                INSERT IGNORE INTO suppliers (id, name, company_id, created_date, version, enabled)
                VALUES (%d, 'Distribuidora Sur', %d, '2026-01-15 08:00:00', 0, true)
                """, SUPPLIER_ID, COMPANY));
        entityManager.flush();
    }

    private void insert(String sql) {
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    private Product gravado(String nombre, String codigo) {
        return Product.create(nombre, codigo, new BigDecimal("12500.00"), "94", "Proveedor texto",
                PROVEEDOR, TaxTreatment.GRAVADO, "Bulto de 15 kg", MEDICAMENTOS, IVA_19, CLINICA);
    }

    private Product excluido(String nombre, String codigo) {
        return Product.create(nombre, codigo, new BigDecimal("0.00"), "94", null, null,
                TaxTreatment.EXCLUIDO, null, MEDICAMENTOS, null, CLINICA);
    }

    @Nested
    @DisplayName("guardar")
    class Guardar {

        @Test
        @DisplayName("guarda un producto nuevo con sus cuatro referencias y asigna id y version")
        void guarda_un_producto_nuevo() {
            Product guardado = repository.save(gravado("Concentrado adulto", "P-001"));

            assertThat(guardado.getId()).isNotNull();
            assertThat(guardado.getVersion()).isEqualTo(0L);
            assertThat(guardado.getProductCategory()).isEqualTo(MEDICAMENTOS);
            assertThat(guardado.getTax()).isEqualTo(IVA_19);
            assertThat(guardado.getCompany()).isEqualTo(CLINICA);
            assertThat(guardado.getSupplier()).isEqualTo(PROVEEDOR);
        }

        @Test
        @DisplayName("acepta impuesto y proveedor nulos para un tratamiento excluido")
        void acepta_impuesto_y_proveedor_nulos() {
            Product guardado = repository.save(excluido("Vacuna social", "P-002"));

            assertThat(guardado.getTax()).isNull();
            assertThat(guardado.getSupplier()).isNull();
        }

        @Test
        @DisplayName("guardar de nuevo el mismo producto conserva el id y sube la version")
        void guardar_de_nuevo_conserva_el_id() {
            Product creado = repository.save(gravado("Concentrado adulto", "P-003"));

            creado.update("Concentrado senior", "P-003", new BigDecimal("18000.00"), "94", null,
                    null, TaxTreatment.EXCLUIDO, null, MEDICAMENTOS, null, CLINICA, 77L,
                    creado.getVersion());
            Product actualizado = repository.save(creado);

            assertThat(actualizado.getId()).isEqualTo(creado.getId());
            assertThat(actualizado.getVersion()).isEqualTo(1L);
            assertThat(actualizado.getName()).isEqualTo("Concentrado senior");
        }
    }

    @Nested
    @DisplayName("lecturas por id")
    class LecturasPorId {

        @Test
        @DisplayName("findById devuelve el producto guardado")
        void find_by_id_devuelve_el_producto() {
            Product guardado = repository.save(gravado("Concentrado adulto", "P-010"));

            assertThat(repository.findById(guardado.getId())).map(Product::getCode)
                    .contains("P-010");
        }

        @Test
        @DisplayName("findById de un id inexistente no devuelve nada")
        void find_by_id_inexistente_no_devuelve_nada() {
            assertThat(repository.findById(-1L)).isEmpty();
        }

        @Test
        @DisplayName("findByIdAndCompanyId encuentra el producto de la propia empresa")
        void find_by_id_and_company_id_de_la_propia_empresa() {
            Product guardado = repository.save(gravado("Concentrado adulto", "P-011"));

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), COMPANY))
                    .map(Product::getId).contains(guardado.getId());
        }

        @Test
        @DisplayName("findByIdAndCompanyId no encuentra un producto de otra empresa")
        void find_by_id_and_company_id_de_otra_empresa_no_lo_encuentra() {
            Product guardado = repository.save(gravado("Concentrado adulto", "P-012"));

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), OTRA_COMPANY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("listados")
    class Listados {

        @Test
        @DisplayName("findAll incluye los productos guardados")
        void find_all_incluye_los_productos_guardados() {
            Product guardado = repository.save(gravado("Concentrado ZZZ", "P-ZZZ-020"));

            assertThat(repository.findAll()).extracting(Product::getId).contains(guardado.getId());
        }

        @Test
        @DisplayName("findAllByCompanyId solo trae los productos de esa empresa")
        void find_all_by_company_id_solo_trae_los_de_la_empresa() {
            Product propio = repository.save(gravado("Concentrado ZZZ propio", "P-ZZZ-021"));

            assertThat(repository.findAllByCompanyId(COMPANY)).extracting(Product::getId)
                    .contains(propio.getId());
            assertThat(repository.findAllByCompanyId(OTRA_COMPANY)).extracting(Product::getId)
                    .doesNotContain(propio.getId());
        }

        @Test
        @DisplayName("findAllDisabledByCompanyId solo trae los productos pausados")
        void find_all_disabled_by_company_id_solo_trae_los_pausados() {
            Product activo = repository.save(gravado("Concentrado activo", "P-030"));
            Product pausado = repository.save(gravado("Concentrado pausado", "P-031"));
            repository.delete(pausado.getId());

            List<Product> pausados = repository.findAllDisabledByCompanyId(COMPANY);

            assertThat(pausados).extracting(Product::getId).contains(pausado.getId())
                    .doesNotContain(activo.getId());
        }
    }

    @Nested
    @DisplayName("existencia de codigo y nombre")
    class Existencia {

        @Test
        @DisplayName("existsByCompanyIdAndCode distingue codigos usados de libres")
        void exists_by_company_id_and_code() {
            repository.save(gravado("Concentrado adulto", "P-040"));

            assertThat(repository.existsByCompanyIdAndCode(COMPANY, "P-040")).isTrue();
            assertThat(repository.existsByCompanyIdAndCode(COMPANY, "P-999")).isFalse();
        }

        @Test
        @DisplayName("existsByCompanyIdAndCodeExcludingId no cuenta el propio producto")
        void exists_by_company_id_and_code_excluding_id() {
            Product guardado = repository.save(gravado("Concentrado adulto", "P-041"));

            assertThat(repository.existsByCompanyIdAndCodeExcludingId(COMPANY, "P-041",
                    guardado.getId())).isFalse();
            assertThat(repository.existsByCompanyIdAndCodeExcludingId(COMPANY, "P-041", -1L))
                    .isTrue();
        }

        @Test
        @DisplayName("existsByCompanyIdAndName distingue nombres usados de libres")
        void exists_by_company_id_and_name() {
            repository.save(gravado("Concentrado unico ZZZ", "P-042"));

            assertThat(repository.existsByCompanyIdAndName(COMPANY, "Concentrado unico ZZZ"))
                    .isTrue();
            assertThat(repository.existsByCompanyIdAndName(COMPANY, "No existe ZZZ")).isFalse();
        }

        @Test
        @DisplayName("existsByCompanyIdAndNameExcludingId no cuenta el propio producto")
        void exists_by_company_id_and_name_excluding_id() {
            Product guardado = repository.save(gravado("Concentrado unico ZZZ2", "P-043"));

            assertThat(repository.existsByCompanyIdAndNameExcludingId(COMPANY,
                    "Concentrado unico ZZZ2", guardado.getId())).isFalse();
            assertThat(repository.existsByCompanyIdAndNameExcludingId(COMPANY,
                    "Concentrado unico ZZZ2", -1L)).isTrue();
        }
    }

    @Nested
    @DisplayName("busqueda paginada")
    class Busqueda {

        private SearchProductsCommand comando(String name, String code, Long categoryId, Long taxId,
                int page, int pageSize) {
            return new SearchProductsCommand(COMPANY, name, code, categoryId, taxId, page,
                    pageSize);
        }

        @Test
        @DisplayName("sin filtros trae los productos de la empresa, paginados")
        void sin_filtros_pagina_los_productos_de_la_empresa() {
            repository.save(gravado("Pagina ZZZ uno", "P-PAG-050"));
            repository.save(gravado("Pagina ZZZ dos", "P-PAG-051"));

            PageResult<Product> pagina = repository
                    .search(comando("Pagina ZZZ", null, null, null, 0, 1));

            assertThat(pagina.content()).hasSize(1);
            assertThat(pagina.totalElements()).isEqualTo(2);
            assertThat(pagina.totalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("filtra por nombre")
        void filtra_por_nombre() {
            repository.save(gravado("Filtro ZZZ nombre", "P-060"));
            repository.save(gravado("Otro producto ZZZ", "P-061"));

            PageResult<Product> resultado = repository
                    .search(comando("Filtro ZZZ nombre", null, null, null, 0, 20));

            assertThat(resultado.content()).extracting(Product::getCode).containsExactly("P-060");
        }

        @Test
        @DisplayName("filtra por codigo")
        void filtra_por_codigo() {
            repository.save(gravado("Producto ZZZ A", "P-CODIGO-070"));
            repository.save(gravado("Producto ZZZ B", "P-OTRO-071"));

            PageResult<Product> resultado = repository
                    .search(comando(null, "P-CODIGO-070", null, null, 0, 20));

            assertThat(resultado.content()).extracting(Product::getCode)
                    .containsExactly("P-CODIGO-070");
        }

        @Test
        @DisplayName("filtra por categoria")
        void filtra_por_categoria() {
            Product enAccesorios = new Product(null, "Correa ZZZ", "P-080",
                    new BigDecimal("30000.00"), "94", null, null, TaxTreatment.EXCLUIDO, null,
                    ACCESORIOS, null, CLINICA, java.time.LocalDateTime.now(), null, null, null,
                    true);
            repository.save(enAccesorios);
            repository.save(gravado("Concentrado ZZZ", "P-081"));

            PageResult<Product> resultado = repository
                    .search(comando(null, null, OTRA_CATEGORY_ID, null, 0, 20));

            assertThat(resultado.content()).extracting(Product::getCode).containsExactly("P-080");
        }

        @Test
        @DisplayName("filtra por impuesto")
        void filtra_por_impuesto() {
            repository.save(gravado("Con impuesto ZZZ", "P-090"));
            repository.save(excluido("Sin impuesto ZZZ", "P-091"));

            PageResult<Product> resultado = repository
                    .search(comando(null, null, null, TAX_ID, 0, 20));

            assertThat(resultado.content()).extracting(Product::getCode).contains("P-090")
                    .doesNotContain("P-091");
        }

        @Test
        @DisplayName("no trae productos de otra empresa aunque el nombre coincida")
        void no_trae_productos_de_otra_empresa() {
            Product ajeno = new Product(null, "Filtro ZZZ ajeno", "P-100",
                    new BigDecimal("1000.00"), "94", null, null, TaxTreatment.EXCLUIDO, null,
                    MEDICAMENTOS, null,
                    new CompanyRef(OTRA_COMPANY, "Veterinaria ajena", "900654321"),
                    java.time.LocalDateTime.now(), null, null, null, true);
            repository.save(ajeno);

            PageResult<Product> resultado = repository
                    .search(comando("Filtro ZZZ ajeno", null, null, null, 0, 20));

            assertThat(resultado.content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("borrar")
    class Borrar {

        @Test
        @DisplayName("delete hace soft delete: desaparece de findById y aparece entre los pausados")
        void delete_hace_soft_delete() {
            Product guardado = repository.save(gravado("Concentrado a borrar", "P-110"));

            repository.delete(guardado.getId());

            assertThat(repository.findById(guardado.getId())).isEmpty();
            assertThat(repository.findAllDisabledByCompanyId(COMPANY)).extracting(Product::getId)
                    .contains(guardado.getId());
        }
    }

    @Nested
    @DisplayName("reactivar")
    class Reactivar {

        @Test
        @DisplayName("reactivate marca el producto habilitado y devuelve una fila afectada")
        void reactivate_marca_el_producto_habilitado() {
            Product guardado = repository.save(gravado("Concentrado a reactivar", "P-120"));
            repository.delete(guardado.getId());

            int filas = repository.reactivate(guardado.getId(), COMPANY);

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardado.getId())).isPresent();
        }

        @Test
        @DisplayName("reactivate de un producto de otra empresa no afecta ninguna fila")
        void reactivate_de_otro_producto_no_afecta_filas() {
            Product guardado = repository.save(gravado("Concentrado ajeno", "P-121"));
            repository.delete(guardado.getId());

            int filas = repository.reactivate(guardado.getId(), OTRA_COMPANY);

            assertThat(filas).isZero();
            Optional<Product> tras = repository.findById(guardado.getId());
            assertThat(tras).isEmpty();
        }
    }
}
