package com.vetsoftware.app.service.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.service.application.command.SearchServicesCommand;
import com.vetsoftware.app.service.domain.CompanyRef;
import com.vetsoftware.app.service.domain.Service;
import com.vetsoftware.app.service.domain.ServiceCategoryRef;
import com.vetsoftware.app.service.domain.TaxRef;
import com.vetsoftware.app.service.domain.TaxTreatment;
import com.vetsoftware.app.shared.pagination.PageResult;
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
 * Rodaja de persistencia del catalogo de servicios contra MySQL real.
 *
 * <p>
 * Lo que un doble no puede comprobar: que el {@code @EntityGraph} evita el N+1
 * al traer {@code serviceCategory}/{@code tax}/{@code company} de una vez, que
 * el filtro de {@code search} navega tres FKs sin duplicar filas (de ahi el
 * {@code distinct} en el fetch-join de datos, ausente en la query de conteo),
 * que la query nativa de {@code findAllDisabledByCompanyId} sortea el
 * {@code @SQLRestriction("enabled = true")} —que SI aplica al resto de queries
 * derivadas— y que {@code reactivate} solo toca filas de la empresa correcta.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaServiceRepository — catalogo de servicios contra MySQL real")
class ServicePersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long CATEGORY_ID = 990L;
    private static final Long OTRA_CATEGORY_ID = 991L;
    private static final Long TAX_ID = 995L;

    private static final ServiceCategoryRef CONSULTAS = new ServiceCategoryRef(CATEGORY_ID,
            "Consultas");
    private static final TaxRef IVA_19 = new TaxRef(TAX_ID, "IVA 19%", new BigDecimal("19.00"));
    private static final CompanyRef CLINICA = new CompanyRef(COMPANY, "Veterinaria de prueba",
            "900123456");

    @Autowired
    private JpaServiceRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        categoria(CATEGORY_ID, "Consultas", COMPANY);
        categoria(OTRA_CATEGORY_ID, "Cirugias", COMPANY);
        impuesto(TAX_ID, "IVA 19%", "19.00", COMPANY);
        entityManager.flush();
    }

    private void categoria(Long id, String nombre, Long companyId) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO service_categories (id, name, description, company_id,
                                                        created_date, version, enabled)
                VALUES (:id, :nombre, 'Categoria de prueba', :empresa, '2026-01-01 08:00:00', 0,
                        true)
                """).setParameter("id", id).setParameter("nombre", nombre)
                .setParameter("empresa", companyId).executeUpdate();
    }

    private void impuesto(Long id, String nombre, String porcentaje, Long companyId) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO taxes (id, name, percentage, tax_scheme, company_id,
                                          created_date, version, enabled)
                VALUES (:id, :nombre, :porcentaje, 'IVA', :empresa, '2026-01-01 08:00:00', 0, true)
                """).setParameter("id", id).setParameter("nombre", nombre)
                .setParameter("porcentaje", new BigDecimal(porcentaje))
                .setParameter("empresa", companyId).executeUpdate();
    }

    private Service consultaGravada() {
        return repository.save(Service.create("Consulta general", new BigDecimal("50000.00"),
                TaxTreatment.GRAVADO, "notas", CONSULTAS, IVA_19, CLINICA));
    }

    private Service servicioExento(String nombre) {
        return repository.save(Service.create(nombre, new BigDecimal("30000.00"),
                TaxTreatment.EXENTO, null, CONSULTAS, null, CLINICA));
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id y las tres referencias vuelven completas")
        void guardar_asigna_id_y_las_referencias_vuelven_completas() {
            Service guardado = consultaGravada();

            assertThat(guardado.getId()).isNotNull();

            Service leido = repository.findByIdAndCompanyId(guardado.getId(), COMPANY)
                    .orElseThrow();
            assertThat(leido.getName()).isEqualTo("Consulta general");
            assertThat(leido.getPrice()).isEqualByComparingTo("50000.00");
            assertThat(leido.getServiceCategory()).isEqualTo(CONSULTAS);
            assertThat(leido.getTax()).isEqualTo(IVA_19);
            assertThat(leido.getCompany()).isEqualTo(CLINICA);
            assertThat(leido.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un servicio EXENTO guarda la FK de impuesto en null y vuelve sin ref")
        void servicio_exento_guarda_la_fk_de_impuesto_en_null() {
            Service guardado = servicioExento("Vacunacion");

            Service leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getTax()).isNull();
            assertThat(leido.getTaxTreatment()).isEqualTo(TaxTreatment.EXENTO);
        }

        @Test
        @DisplayName("actualizar conserva el id y refleja los campos nuevos")
        void actualizar_conserva_el_id_y_refleja_los_campos_nuevos() {
            Service guardado = consultaGravada();
            Service recuperado = repository.findById(guardado.getId()).orElseThrow();

            recuperado.update("Consulta especializada", new BigDecimal("70000.00"),
                    TaxTreatment.GRAVADO, "notas nuevas", CONSULTAS, IVA_19, CLINICA, 940L, 0L);
            repository.save(recuperado);

            Service leido = repository.findById(guardado.getId()).orElseThrow();
            assertThat(leido.getId()).isEqualTo(guardado.getId());
            assertThat(leido.getName()).isEqualTo("Consulta especializada");
            assertThat(leido.getPrice()).isEqualByComparingTo("70000.00");
            assertThat(leido.getUpdatedBy()).isEqualTo(940L);
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class Tenancy {

        @Test
        @DisplayName("un servicio de otra empresa no se lee con findByIdAndCompanyId")
        void un_servicio_de_otra_empresa_no_se_lee_por_id() {
            CompanyRef otraClinica = new CompanyRef(OTRA_COMPANY, "Veterinaria ajena", "900654321");
            Service ajeno = repository.save(Service.create("Consulta ajena", BigDecimal.TEN,
                    TaxTreatment.EXENTO, null, CONSULTAS, null, otraClinica));

            assertThat(repository.findByIdAndCompanyId(ajeno.getId(), COMPANY)).isEmpty();
            assertThat(repository.findByIdAndCompanyId(ajeno.getId(), OTRA_COMPANY)).isPresent();
        }

        @Test
        @DisplayName("el listado por empresa no ve los servicios ajenos")
        void el_listado_por_empresa_no_ve_los_servicios_ajenos() {
            Service propio = consultaGravada();
            CompanyRef otraClinica = new CompanyRef(OTRA_COMPANY, "Veterinaria ajena", "900654321");
            repository.save(Service.create("Consulta ajena", BigDecimal.TEN, TaxTreatment.EXENTO,
                    null, CONSULTAS, null, otraClinica));

            assertThat(repository.findAllByCompanyId(COMPANY)).extracting(Service::getId)
                    .containsExactly(propio.getId());
        }
    }

    @Nested
    @DisplayName("baja logica y reactivacion")
    class BajaYReactivacion {

        @Test
        @DisplayName("borrar esconde la fila pero no la elimina")
        void borrar_esconde_la_fila_pero_no_la_elimina() {
            Service guardado = consultaGravada();
            entityManager.flush();

            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).isEmpty();
            assertThat(filasCrudas(guardado.getId())).isEqualTo(1L);
        }

        @Test
        @DisplayName("findAllDisabledByCompanyId sortea el @SQLRestriction y lista los pausados")
        void find_all_disabled_sortea_la_restriccion() {
            Service pausado = consultaGravada();
            Service activo = servicioExento("Vacunacion");
            entityManager.flush();
            repository.delete(pausado.getId());
            entityManager.flush();
            entityManager.clear();

            List<Service> pausados = repository.findAllDisabledByCompanyId(COMPANY);

            assertThat(pausados).extracting(Service::getId).containsExactly(pausado.getId());
            assertThat(repository.findAllByCompanyId(COMPANY)).extracting(Service::getId)
                    .containsExactly(activo.getId());
        }

        @Test
        @DisplayName("reactivate exige la empresa correcta")
        void reactivate_exige_la_empresa_correcta() {
            Service guardado = consultaGravada();
            entityManager.flush();
            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.reactivate(guardado.getId(), OTRA_COMPANY)).isZero();
            assertThat(repository.reactivate(guardado.getId(), COMPANY)).isEqualTo(1);
            assertThat(repository.findById(guardado.getId())).isPresent();
        }

        private long filasCrudas(Long id) {
            Number filas = (Number) entityManager
                    .createNativeQuery("SELECT COUNT(*) FROM services WHERE id = :id")
                    .setParameter("id", id).getSingleResult();
            return filas.longValue();
        }
    }

    @Nested
    @DisplayName("busqueda filtrada y paginada")
    class Busqueda {

        @Test
        @DisplayName("filtra por nombre sin distinguir mayusculas, acotado a la empresa")
        void filtra_por_nombre_sin_distinguir_mayusculas() {
            Service consulta = consultaGravada();
            servicioExento("Vacunacion antirrabica");

            PageResult<Service> pagina = repository
                    .search(new SearchServicesCommand(COMPANY, "CONSULTA", null, null, 0, 20));

            assertThat(pagina.content()).extracting(Service::getId)
                    .containsExactly(consulta.getId());
        }

        @Test
        @DisplayName("filtra por categoria e impuesto sin duplicar filas por el fetch-join")
        void filtra_por_categoria_e_impuesto_sin_duplicar_filas() {
            Service consulta = consultaGravada();

            PageResult<Service> pagina = repository
                    .search(new SearchServicesCommand(COMPANY, null, CATEGORY_ID, TAX_ID, 0, 20));

            // El fetch-join de tres asociaciones sobre una unica fila raiz duplicaria el
            // resultado sin el `distinct(true)` de la especificacion cuando la query no
            // es de conteo.
            assertThat(pagina.content()).extracting(Service::getId)
                    .containsExactly(consulta.getId());
            assertThat(pagina.totalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("una categoria de otro servicio no aparece en el filtro")
        void una_categoria_ajena_no_aparece_en_el_filtro() {
            consultaGravada();

            PageResult<Service> pagina = repository.search(
                    new SearchServicesCommand(COMPANY, null, OTRA_CATEGORY_ID, null, 0, 20));

            assertThat(pagina.content()).isEmpty();
        }

        @Test
        @DisplayName("los resultados no se mezclan entre empresas")
        void los_resultados_no_se_mezclan_entre_empresas() {
            consultaGravada();
            CompanyRef otraClinica = new CompanyRef(OTRA_COMPANY, "Veterinaria ajena", "900654321");
            repository.save(Service.create("Consulta ajena", BigDecimal.TEN, TaxTreatment.EXENTO,
                    null, CONSULTAS, null, otraClinica));

            PageResult<Service> pagina = repository
                    .search(new SearchServicesCommand(OTRA_COMPANY, null, null, null, 0, 20));

            assertThat(pagina.content()).hasSize(1);
            assertThat(pagina.content().get(0).getCompany().id()).isEqualTo(OTRA_COMPANY);
        }
    }
}
