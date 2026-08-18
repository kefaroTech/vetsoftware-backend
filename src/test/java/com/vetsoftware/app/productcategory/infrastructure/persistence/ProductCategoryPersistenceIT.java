package com.vetsoftware.app.productcategory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.productcategory.domain.CompanyRef;
import com.vetsoftware.app.productcategory.domain.ProductCategory;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de categorias de producto contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b> Las conductas que sostienen esta
 * feature las decide el motor, no el codigo Java:
 *
 * <ul>
 * <li><b>La unicidad del nombre por empresa depende de la collation.</b> La
 * columna es {@code utf8mb4_..._ci}, asi que para MySQL «Medicamentos» y
 * «medicamentos» son el mismo nombre. Un {@code Map} en memoria compara con
 * {@code equals} y deja pasar el duplicado que la BD rechazaria.</li>
 * <li><b>El soft delete lo hacen dos anotaciones de Hibernate.</b> El
 * {@code @SQLDelete} convierte el borrado en {@code UPDATE enabled = false} y
 * el {@code @SQLRestriction} esconde la fila de todas las consultas de entidad,
 * menos de la nativa de {@code reactivate}.</li>
 * <li><b>El {@code @EntityGraph} en {@code company}</b> es lo que evita el N+1
 * al listar: solo se ve pasando por Hibernate.</li>
 * </ul>
 */
@Import({JpaProductCategoryRepository.class, ProductCategoryJpaMapper.class})
@DisplayName("JpaProductCategoryRepository — nombre unico, tenencia y soft delete contra MySQL real")
class ProductCategoryPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY_ID = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY_ID = SchemaSeed.OTRA_COMPANY_ID;

    private static final CompanyRef EMPRESA = new CompanyRef(COMPANY_ID, "Veterinaria de prueba",
            "900123456");
    private static final CompanyRef OTRA_EMPRESA = new CompanyRef(OTRA_COMPANY_ID,
            "Veterinaria ajena", "900654321");

    /** Nombre ya sembrado por {@link SchemaSeed} para {@link #COMPANY_ID}. */
    private static final String MEDICAMENTOS = "Medicamentos";
    private static final String INSUMOS = "Insumos";
    private static final String ANTIBIOTICOS = "Antibioticos";

    @Autowired
    private JpaProductCategoryRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private ProductCategory nueva(String nombre, CompanyRef empresa) {
        return new ProductCategory(null, nombre, "Categoria de prueba", empresa, CREADO, null, null,
                null, true);
    }

    private ProductCategory guardar(String nombre) {
        return repository.save(nueva(nombre, EMPRESA));
    }

    private ProductCategory guardarEn(CompanyRef empresa, String nombre) {
        return repository.save(nueva(nombre, empresa));
    }

    /** Soft delete + flush: el UPDATE tiene que llegar a la BD antes de releer. */
    private void deshabilitar(Long id) {
        repository.delete(id);
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("unicidad del nombre por empresa")
    class UnicidadDelNombre {

        @Test
        @DisplayName("la collation del motor hace que el nombre choque aunque cambie la caja")
        void el_nombre_choca_aunque_cambie_la_caja() {
            // "Medicamentos" ya viene sembrado por SchemaSeed para esta empresa.
            assertThat(repository.existsByCompanyIdAndName(COMPANY_ID, "medicamentos")).isTrue();
        }

        @Test
        @DisplayName("el mismo nombre queda libre en otra empresa")
        void el_mismo_nombre_queda_libre_en_otra_empresa() {
            assertThat(repository.existsByCompanyIdAndName(OTRA_COMPANY_ID, MEDICAMENTOS))
                    .isFalse();
        }

        @Test
        @DisplayName("deshabilitar una categoria libera su nombre")
        void deshabilitar_una_categoria_libera_su_nombre() {
            ProductCategory pausada = guardar(INSUMOS);

            deshabilitar(pausada.getId());

            assertThat(repository.existsByCompanyIdAndName(COMPANY_ID, INSUMOS)).isFalse();
        }

        @Test
        @DisplayName("al excluir su propio id, una categoria no choca consigo misma")
        void una_categoria_no_choca_consigo_misma() {
            ProductCategory insumos = guardar(INSUMOS);

            assertThat(repository.existsByCompanyIdAndNameExcludingId(COMPANY_ID, INSUMOS,
                    insumos.getId())).isFalse();
        }

        @Test
        @DisplayName("al excluir otro id, el nombre repetido si choca — y tambien con otra caja")
        void el_nombre_repetido_choca_al_excluir_otro_id() {
            guardar(INSUMOS);
            ProductCategory antibioticos = guardar(ANTIBIOTICOS);

            assertThat(repository.existsByCompanyIdAndNameExcludingId(COMPANY_ID, "INSUMOS",
                    antibioticos.getId())).isTrue();
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenencia {

        @Test
        @DisplayName("findByIdAndCompanyId no devuelve la categoria de otra empresa")
        void no_devuelve_la_categoria_de_otra_empresa() {
            ProductCategory ajena = guardarEn(OTRA_EMPRESA, INSUMOS);

            assertThat(repository.findByIdAndCompanyId(ajena.getId(), COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("findById sin empresa si alcanza la ajena: por eso las lecturas usan el otro")
        void find_by_id_sin_empresa_alcanza_la_ajena() {
            ProductCategory ajena = guardarEn(OTRA_EMPRESA, INSUMOS);

            // Deja constancia de la diferencia: findById no filtra por tenant, asi que
            // ningun caso de uso de empleado puede usarlo.
            assertThat(repository.findById(ajena.getId())).map(ProductCategory::getId)
                    .contains(ajena.getId());
        }

        @Test
        @DisplayName("el listado por empresa no mezcla las categorias de las dos")
        void el_listado_por_empresa_no_mezcla_las_dos() {
            ProductCategory propia = guardar(INSUMOS);
            ProductCategory ajena = guardarEn(OTRA_EMPRESA, ANTIBIOTICOS);

            assertThat(repository.findAllByCompanyId(COMPANY_ID)).extracting(ProductCategory::getId)
                    .contains(propia.getId()).doesNotContain(ajena.getId());
        }
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva cada campo y la empresa hidratada")
        void guardar_y_releer_conserva_cada_campo() {
            ProductCategory guardada = guardar(INSUMOS);

            ProductCategory leida = repository.findByIdAndCompanyId(guardada.getId(), COMPANY_ID)
                    .orElseThrow();

            assertThat(leida.getName()).isEqualTo(INSUMOS);
            assertThat(leida.getDescription()).isEqualTo("Categoria de prueba");
            assertThat(leida.getUpdatedDate()).isNull();
            assertThat(leida.getUpdatedBy()).isNull();
            assertThat(leida.getVersion()).isZero();
            assertThat(leida.isEnabled()).isTrue();
            // El CompanyRef no se guarda: se reconstruye desde el @ManyToOne al leer, y
            // sus invariantes rechazan un nombre o un identificador vacios.
            assertThat(leida.getCompany())
                    .isEqualTo(new CompanyRef(COMPANY_ID, "Veterinaria de prueba", "900123456"));
        }
    }

    @Nested
    @DisplayName("soft delete y reactivacion")
    class SoftDelete {

        @Test
        @DisplayName("el listado activo deja de ver la categoria deshabilitada")
        void el_listado_activo_no_ve_la_deshabilitada() {
            ProductCategory pausada = guardar(INSUMOS);

            deshabilitar(pausada.getId());

            assertThat(repository.findAllByCompanyId(COMPANY_ID)).extracting(ProductCategory::getId)
                    .doesNotContain(pausada.getId());
        }

        @Test
        @DisplayName("reactivar devuelve la categoria al listado activo")
        void reactivar_devuelve_la_categoria_al_listado_activo() {
            ProductCategory pausada = guardar(INSUMOS);
            deshabilitar(pausada.getId());

            int filas = repository.reactivate(pausada.getId(), COMPANY_ID);

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findAllByCompanyId(COMPANY_ID)).extracting(ProductCategory::getId)
                    .contains(pausada.getId());
        }

        @Test
        @DisplayName("reactivar desde otra empresa no toca ninguna fila")
        void reactivar_desde_otra_empresa_no_toca_ninguna_fila() {
            ProductCategory pausada = guardar(INSUMOS);
            deshabilitar(pausada.getId());

            int filas = repository.reactivate(pausada.getId(), OTRA_COMPANY_ID);

            // El UPDATE nativo lleva el company_id en el WHERE: sin el, una empresa
            // resucitaria la categoria de otra y esta volveria a su catalogo.
            assertThat(filas).isZero();
            assertThat(repository.findByIdAndCompanyId(pausada.getId(), COMPANY_ID)).isEmpty();
        }
    }
}
