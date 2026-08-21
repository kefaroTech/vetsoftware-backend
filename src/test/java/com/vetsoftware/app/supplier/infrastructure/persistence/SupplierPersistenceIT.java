package com.vetsoftware.app.supplier.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.supplier.application.command.SearchSuppliersCommand;
import com.vetsoftware.app.supplier.domain.CompanyRef;
import com.vetsoftware.app.supplier.domain.Supplier;
import com.vetsoftware.app.supplier.testsupport.SupplierMother;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
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
 * Rodaja de persistencia de proveedores contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b> Las tres conductas que sostienen esta
 * feature las decide el motor, no el codigo Java, asi que un stub en memoria
 * las daria por buenas definiendo el contrato que dice verificar —donde vivio
 * BE-01—:
 *
 * <ul>
 * <li><b>La unicidad del nombre por empresa depende de la collation.</b> La
 * columna es {@code utf8mb4_..._ci}, asi que para MySQL «Distribuidora Sur» y
 * «distribuidora sur» son el mismo nombre. Un {@code Map} en memoria compara
 * con {@code equals} y deja pasar el duplicado que la BD rechazaria con el
 * indice {@code uq_suppliers_company_active_name} (migracion 197).</li>
 * <li><b>El soft delete lo hacen dos anotaciones de Hibernate.</b> El
 * {@code @SQLDelete} convierte el borrado en {@code UPDATE enabled = false} y
 * el {@code @SQLRestriction} esconde la fila de TODAS las consultas de
 * entidad... menos de las nativas, que es la unica via por la que el listado de
 * pausados y el {@code reactivate} pueden verla. Ese contraste solo existe
 * pasando por Hibernate y por el motor.</li>
 * <li><b>La columna generada {@code active_name}</b> es la que libera el nombre
 * al deshabilitar. No hay ningun campo Java que la represente.</li>
 * </ul>
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSupplierRepository — nombre unico, tenencia y soft delete contra MySQL real")
class SupplierPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY_ID = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY_ID = SchemaSeed.OTRA_COMPANY_ID;

    private static final CompanyRef EMPRESA = SupplierMother.empresa(COMPANY_ID,
            "Veterinaria de prueba", "900123456");
    private static final CompanyRef OTRA_EMPRESA = SupplierMother.empresa(OTRA_COMPANY_ID,
            "Veterinaria ajena", "900654321");

    private static final String SUR = "Distribuidora Sur";
    private static final String NORTE = "Insumos Norte";

    @Autowired
    private JpaSupplierRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private Supplier guardar(String nombre) {
        return repository.save(SupplierMother.completo(nombre, EMPRESA));
    }

    private Supplier guardarEn(CompanyRef empresa, String nombre) {
        return repository.save(SupplierMother.completo(nombre, empresa));
    }

    private Supplier guardarCreadoEn(String nombre, LocalDateTime creado) {
        return repository.save(SupplierMother.completo(nombre, EMPRESA, creado));
    }

    /** Soft delete + flush: el UPDATE tiene que llegar a la BD antes de releer. */
    private void deshabilitar(Long id) {
        repository.delete(id);
        entityManager.flush();
        entityManager.clear();
    }

    private static SearchSuppliersCommand busqueda(Long companyId, String nombre, String nit,
            int page, int pageSize) {
        return new SearchSuppliersCommand(companyId, nombre, nit, page, pageSize);
    }

    @Nested
    @DisplayName("unicidad del nombre por empresa")
    class UnicidadDelNombre {

        @Test
        @DisplayName("la collation del motor hace que el nombre choque aunque cambie la caja")
        void el_nombre_choca_aunque_cambie_la_caja() {
            guardar(SUR);

            // La columna es _ci: para MySQL «distribuidora sur» YA existe. Con un doble que
            // compare con equals el duplicado pasaria el check del service y lo rechazaria
            // despues el indice unico, convirtiendo un 409 de negocio en un 500 de BD.
            assertThat(repository.existsByCompanyIdAndName(COMPANY_ID, "distribuidora sur"))
                    .isTrue();
        }

        @Test
        @DisplayName("el mismo nombre queda libre en otra empresa")
        void el_mismo_nombre_queda_libre_en_otra_empresa() {
            guardar(SUR);

            assertThat(repository.existsByCompanyIdAndName(OTRA_COMPANY_ID, SUR)).isFalse();
        }

        @Test
        @DisplayName("deshabilitar un proveedor libera su nombre")
        void deshabilitar_un_proveedor_libera_su_nombre() {
            Supplier pausado = guardar(SUR);

            deshabilitar(pausado.getId());

            // Lo consigue la columna generada `active_name`, que pasa a NULL: el indice
            // unico admite varios NULL y la unicidad solo rige entre activos.
            assertThat(repository.existsByCompanyIdAndName(COMPANY_ID, SUR)).isFalse();
        }

        @Test
        @DisplayName("al excluir su propio id, un proveedor no choca consigo mismo")
        void un_proveedor_no_choca_consigo_mismo() {
            Supplier sur = guardar(SUR);

            assertThat(repository.existsByCompanyIdAndNameExcludingId(COMPANY_ID, SUR, sur.getId()))
                    .isFalse();
        }

        @Test
        @DisplayName("al excluir otro id, el nombre repetido si choca — y tambien con otra caja")
        void el_nombre_repetido_choca_al_excluir_otro_id() {
            guardar(SUR);
            Supplier norte = guardar(NORTE);

            assertThat(repository.existsByCompanyIdAndNameExcludingId(COMPANY_ID,
                    "DISTRIBUIDORA SUR", norte.getId())).isTrue();
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenencia {

        @Test
        @DisplayName("findByIdAndCompanyId no devuelve el proveedor de otra empresa")
        void no_devuelve_el_proveedor_de_otra_empresa() {
            Supplier ajeno = guardarEn(OTRA_EMPRESA, SUR);

            assertThat(repository.findByIdAndCompanyId(ajeno.getId(), COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("findById sin empresa si alcanza el ajeno: por eso las lecturas usan el otro")
        void find_by_id_sin_empresa_alcanza_el_ajeno() {
            Supplier ajeno = guardarEn(OTRA_EMPRESA, SUR);

            // Deja constancia de la diferencia: `findById` no filtra por tenant, asi que
            // ningun caso de uso de empleado puede usarlo. Si algun dia lo hiciera, este
            // test es el que explica por que se ve la ficha de otra veterinaria.
            assertThat(repository.findById(ajeno.getId())).map(Supplier::getId)
                    .contains(ajeno.getId());
        }

        @Test
        @DisplayName("el listado por empresa no mezcla los proveedores de las dos")
        void el_listado_por_empresa_no_mezcla_las_dos() {
            Supplier propio = guardar(SUR);
            guardarEn(OTRA_EMPRESA, NORTE);

            assertThat(repository.findAllByCompanyId(COMPANY_ID)).extracting(Supplier::getId)
                    .containsExactly(propio.getId());
        }

        @Test
        @DisplayName("la busqueda paginada tampoco devuelve los de otra empresa")
        void la_busqueda_no_devuelve_los_de_otra_empresa() {
            Supplier propio = guardar(SUR);
            guardarEn(OTRA_EMPRESA, SUR);

            PageResult<Supplier> pagina = repository
                    .search(busqueda(COMPANY_ID, null, null, 0, 20));

            assertThat(pagina.content()).extracting(Supplier::getId)
                    .containsExactly(propio.getId());
            assertThat(pagina.totalElements()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva cada campo y la empresa hidratada")
        void guardar_y_releer_conserva_cada_campo() {
            Supplier guardado = guardar(SUR);

            Supplier leido = repository.findByIdAndCompanyId(guardado.getId(), COMPANY_ID)
                    .orElseThrow();

            assertThat(leido.getName()).isEqualTo(SUR);
            assertThat(leido.getTaxId()).isEqualTo("901555444-1");
            assertThat(leido.getContactName()).isEqualTo("Marta Gil");
            assertThat(leido.getPhone()).isEqualTo("3001234567");
            assertThat(leido.getEmail()).isEqualTo("compras@sur.test");
            assertThat(leido.getAddress()).isEqualTo("Calle 10 # 5-20");
            assertThat(leido.getPaymentTermsDays()).isEqualTo(30);
            assertThat(leido.getNotes()).isEqualTo("Entrega los martes");
            assertThat(leido.getCreatedDate()).isEqualTo(SupplierMother.CREADO);
            assertThat(leido.getUpdatedDate()).isNull();
            assertThat(leido.getUpdatedBy()).isNull();
            assertThat(leido.getVersion()).isZero();
            assertThat(leido.isEnabled()).isTrue();
            // El CompanyRef no se guarda: se reconstruye desde el @ManyToOne al leer, y sus
            // invariantes rechazan un nombre o un identificador vacios.
            assertThat(leido.getCompany())
                    .isEqualTo(new CompanyRef(COMPANY_ID, "Veterinaria de prueba", "900123456"));
        }

        @Test
        @DisplayName("los campos opcionales vuelven nulos, no en blanco")
        void los_campos_opcionales_vuelven_nulos() {
            Supplier guardado = repository.save(SupplierMother.minimo(NORTE, EMPRESA));

            Supplier leido = repository.findByIdAndCompanyId(guardado.getId(), COMPANY_ID)
                    .orElseThrow();

            assertThat(leido.getTaxId()).isNull();
            assertThat(leido.getContactName()).isNull();
            assertThat(leido.getPhone()).isNull();
            assertThat(leido.getEmail()).isNull();
            assertThat(leido.getAddress()).isNull();
            assertThat(leido.getPaymentTermsDays()).isNull();
            assertThat(leido.getNotes()).isNull();
        }
    }

    @Nested
    @DisplayName("soft delete y reactivacion")
    class SoftDelete {

        @Test
        @DisplayName("el listado activo deja de ver al proveedor deshabilitado")
        void el_listado_activo_no_ve_al_deshabilitado() {
            Supplier activo = guardar(NORTE);
            Supplier pausado = guardar(SUR);

            deshabilitar(pausado.getId());

            assertThat(repository.findAllByCompanyId(COMPANY_ID)).extracting(Supplier::getId)
                    .containsExactly(activo.getId());
        }

        @Test
        @DisplayName("el listado de pausados si lo ve: la query nativa esquiva el @SQLRestriction")
        void el_listado_de_pausados_si_lo_ve() {
            guardar(NORTE);
            Supplier pausado = guardar(SUR);

            deshabilitar(pausado.getId());

            assertThat(repository.findAllDisabledByCompanyId(COMPANY_ID))
                    .extracting(Supplier::getId).containsExactly(pausado.getId());
        }

        @Test
        @DisplayName("reactivar devuelve el proveedor al listado activo")
        void reactivar_devuelve_el_proveedor_al_listado_activo() {
            Supplier pausado = guardar(SUR);
            deshabilitar(pausado.getId());

            int filas = repository.reactivate(pausado.getId(), COMPANY_ID);

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findAllByCompanyId(COMPANY_ID)).extracting(Supplier::getId)
                    .containsExactly(pausado.getId());
        }

        @Test
        @DisplayName("reactivar desde otra empresa no toca ninguna fila")
        void reactivar_desde_otra_empresa_no_toca_ninguna_fila() {
            Supplier pausado = guardar(SUR);
            deshabilitar(pausado.getId());

            int filas = repository.reactivate(pausado.getId(), OTRA_COMPANY_ID);

            // El UPDATE nativo lleva el company_id en el WHERE: sin el, una empresa
            // resucitaria el proveedor de otra y ese proveedor volveria a su catalogo.
            assertThat(filas).isZero();
            assertThat(repository.findAllDisabledByCompanyId(COMPANY_ID))
                    .extracting(Supplier::getId).containsExactly(pausado.getId());
        }
    }

    @Nested
    @DisplayName("busqueda paginada")
    class Busqueda {

        @Test
        @DisplayName("el filtro por nombre encuentra el fragmento sin importar la caja")
        void el_filtro_por_nombre_ignora_la_caja() {
            Supplier sur = guardar(SUR);
            guardar(NORTE);

            PageResult<Supplier> pagina = repository
                    .search(busqueda(COMPANY_ID, "SUR", null, 0, 20));

            // El LIKE hereda la collation _ci de la columna. Con un doble que use
            // String.contains, buscar en mayusculas no devolveria nada.
            assertThat(pagina.content()).extracting(Supplier::getId).containsExactly(sur.getId());
        }

        @Test
        @DisplayName("el filtro por NIT acota con coincidencia parcial")
        void el_filtro_por_nit_acota_con_coincidencia_parcial() {
            Supplier conNit = repository.save(SupplierMother.conNit(SUR, "901555444", EMPRESA));
            repository.save(SupplierMother.conNit(NORTE, "800111222", EMPRESA));

            PageResult<Supplier> pagina = repository
                    .search(busqueda(COMPANY_ID, null, "5554", 0, 20));

            assertThat(pagina.content()).extracting(Supplier::getId)
                    .containsExactly(conNit.getId());
        }

        @Test
        @DisplayName("los pausados no aparecen en la busqueda")
        void los_pausados_no_aparecen_en_la_busqueda() {
            Supplier activo = guardar(NORTE);
            Supplier pausado = guardar(SUR);
            deshabilitar(pausado.getId());

            PageResult<Supplier> pagina = repository
                    .search(busqueda(COMPANY_ID, null, null, 0, 20));

            assertThat(pagina.content()).extracting(Supplier::getId)
                    .containsExactly(activo.getId());
            assertThat(pagina.totalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("dos paginas consecutivas cubren las filas sin repetir ni omitir")
        void dos_paginas_consecutivas_cubren_las_filas() {
            Supplier antiguo = guardarCreadoEn("Proveedor A", LocalDateTime.of(2026, 1, 10, 8, 0));
            Supplier medio = guardarCreadoEn("Proveedor B", LocalDateTime.of(2026, 1, 11, 8, 0));
            Supplier reciente = guardarCreadoEn("Proveedor C", LocalDateTime.of(2026, 1, 12, 8, 0));

            PageResult<Supplier> primera = repository
                    .search(busqueda(COMPANY_ID, null, null, 0, 2));
            PageResult<Supplier> segunda = repository
                    .search(busqueda(COMPANY_ID, null, null, 1, 2));

            // El orden es createdDate DESC: la primera pagina trae los dos mas recientes y
            // la segunda el que queda. Los totales salen de la consulta de count, no del
            // contenido ya paginado.
            assertThat(primera.content()).extracting(Supplier::getId)
                    .containsExactly(reciente.getId(), medio.getId());
            assertThat(segunda.content()).extracting(Supplier::getId)
                    .containsExactly(antiguo.getId());
            assertThat(primera.totalElements()).isEqualTo(3L);
            assertThat(primera.totalPages()).isEqualTo(2);
            assertThat(primera.pageSize()).isEqualTo(2);
            assertThat(segunda.page()).isEqualTo(1);
        }

        @Test
        @DisplayName("un pageSize desmedido se topa en el maximo del kernel de paginacion")
        void un_page_size_desmedido_se_topa_en_el_maximo() {
            guardar(SUR);

            PageResult<Supplier> pagina = repository
                    .search(busqueda(COMPANY_ID, null, null, 0, 100_000));

            // Si alguien cambiase Pages.request por PageRequest.of, `?pageSize=100000`
            // devolveria la tabla entera de proveedores de la empresa.
            assertThat(pagina.pageSize()).isEqualTo(200);
        }
    }
}
