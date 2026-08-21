package com.vetsoftware.app.medicament.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.medicament.domain.CompanyRef;
import com.vetsoftware.app.medicament.domain.Medicament;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de {@link JpaMedicamentRepository} contra MySQL real:
 * el catalogo global (general = true) frente al propio de empresa, el
 * soft-delete y el nativo que ve los pausados.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaMedicamentRepository — catalogo de medicamentos contra MySQL real")
class MedicamentPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;

    /**
     * El changeset {@code 173b_seed_global_medicaments} siembra seis medicamentos
     * generales de plataforma (Amoxicilina + Clavulanico, Meloxicam, Metronidazol,
     * Omeprazol, Sucralfato y Maropitant). Esta rodaja corre contra las migraciones
     * REALES, asi que el catalogo global nunca esta vacio: los listados se afirman
     * sobre esa linea base en vez de fijar la lista entera, que es lo que hacia que
     * estos dos casos fallaran.
     */
    private static final int GENERALES_SEMBRADOS = 6;

    @Autowired
    private JpaMedicamentRepository repository;
    @Autowired
    private CompanyJpaRepository companyJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private CompanyRef companyRef;

    @BeforeEach
    void sembrarLaEmpresa() {
        SchemaSeed.seed(entityManager);
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(COMPANY);
        companyRef = new CompanyRef(COMPANY, company.getName(), company.getIdentifier());
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("save")
    class Guardado {

        @Test
        @DisplayName("persiste un medicamento general sin empresa")
        void persiste_un_medicamento_general() {
            Medicament guardado = repository
                    .save(Medicament.create("Amoxicilina", "Antibiotico", null, true));
            releerDesdeLaBase();

            Medicament releido = repository.findById(guardado.getId()).orElseThrow();
            assertThat(releido.isGeneral()).isTrue();
            assertThat(releido.getCompany()).isNull();
        }

        @Test
        @DisplayName("persiste un medicamento propio resolviendo la empresa por getReferenceById")
        void persiste_un_medicamento_propio() {
            Medicament guardado = repository
                    .save(Medicament.create("Suero", "Formula propia", companyRef, false));
            releerDesdeLaBase();

            Medicament releido = repository.findById(guardado.getId()).orElseThrow();
            assertThat(releido.isGeneral()).isFalse();
            assertThat(releido.getCompany().id()).isEqualTo(COMPANY);
        }
    }

    /**
     * Los dos finders acotados no son intercambiables y la diferencia es de
     * autorizacion: {@code findAvailableByIdAndCompanyId} es la vista de LECTURA
     * (generales de la plataforma + propios) y {@code findByIdAndCompanyId} la de
     * ESCRITURA (solo propios), porque un general no lo edita ningun tenant.
     */
    @Nested
    @DisplayName("finders acotados — disponibilidad de lectura vs. propiedad de escritura")
    class Disponibilidad {

        @Test
        @DisplayName("un general esta disponible para cualquier empresa, pero no es de ninguna")
        void medicamento_general_disponible_pero_de_nadie() {
            Medicament guardado = repository
                    .save(Medicament.create("Amoxicilina", null, null, true));
            releerDesdeLaBase();

            assertThat(repository.findAvailableByIdAndCompanyId(guardado.getId(), COMPANY))
                    .isPresent();
            assertThat(repository.findAvailableByIdAndCompanyId(guardado.getId(), OTRA_COMPANY))
                    .isPresent();
            // Ninguna empresa lo tiene como propio: por eso no puede escribirlo.
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), COMPANY)).isEmpty();
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), OTRA_COMPANY)).isEmpty();
        }

        @Test
        @DisplayName("un medicamento propio no esta disponible ni es propio para otra empresa")
        void medicamento_propio_no_disponible_para_otra_empresa() {
            Medicament guardado = repository
                    .save(Medicament.create("Suero", null, companyRef, false));
            releerDesdeLaBase();

            assertThat(repository.findAvailableByIdAndCompanyId(guardado.getId(), COMPANY))
                    .isPresent();
            assertThat(repository.findAvailableByIdAndCompanyId(guardado.getId(), OTRA_COMPANY))
                    .isEmpty();
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), COMPANY)).isPresent();
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), OTRA_COMPANY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllAvailableForCompany")
    class ListadoDisponible {

        @Test
        @DisplayName("mezcla los generales con los propios, sin traer los de otra empresa")
        void mezcla_generales_con_propios() {
            repository.save(Medicament.create("Amoxicilina", null, null, true));
            repository.save(Medicament.create("Suero", null, companyRef, false));
            CompanyJpaEntity otra = companyJpaRepository.getReferenceById(OTRA_COMPANY);
            repository.save(Medicament.create("Exclusivo de otra", null,
                    new CompanyRef(OTRA_COMPANY, otra.getName(), otra.getIdentifier()), false));
            releerDesdeLaBase();

            var disponibles = repository.findAllAvailableForCompany(COMPANY);

            // Lo que se prueba es el criterio de la consulta, no el tamano del catalogo:
            // entran el general recien creado y el propio, y NO entra el de la otra
            // empresa. Los seis generales sembrados por la migracion entran por
            // definicion —son generales— y por eso el total se cuenta sobre ellos.
            assertThat(disponibles).extracting(Medicament::getName).contains("Amoxicilina", "Suero")
                    .doesNotContain("Exclusivo de otra");
            assertThat(disponibles).hasSize(GENERALES_SEMBRADOS + 2);
        }
    }

    @Nested
    @DisplayName("findAllDisabledForCompany — nativo que ve los pausados")
    class ListadoPausados {

        @Test
        @DisplayName("solo trae los pausados de la empresa pedida")
        void solo_trae_los_pausados_de_la_empresa() {
            Medicament activo = repository
                    .save(Medicament.create("Suero", null, companyRef, false));
            Medicament pausado = repository
                    .save(Medicament.create("Analgesico", null, companyRef, false));
            releerDesdeLaBase();
            repository.delete(pausado.getId());
            releerDesdeLaBase();

            var pausados = repository.findAllDisabledForCompany(COMPANY);

            assertThat(pausados).extracting(Medicament::getName).containsExactly("Analgesico");
            assertThat(pausados).extracting(Medicament::getId).doesNotContain(activo.getId());
        }
    }

    @Nested
    @DisplayName("delete y reactivate")
    class BorradoYReactivacion {

        @Test
        @DisplayName("un medicamento borrado desaparece de findById (SQLRestriction)")
        void medicamento_borrado_desaparece() {
            Medicament guardado = repository
                    .save(Medicament.create("Suero", null, companyRef, false));
            releerDesdeLaBase();

            repository.delete(guardado.getId());
            releerDesdeLaBase();

            assertThat(repository.findById(guardado.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate() vuelve a hacer visible un medicamento borrado")
        void reactivate_vuelve_a_hacer_visible() {
            Medicament guardado = repository
                    .save(Medicament.create("Suero", null, companyRef, false));
            releerDesdeLaBase();
            repository.delete(guardado.getId());
            releerDesdeLaBase();

            int filas = repository.reactivate(guardado.getId(), COMPANY);
            releerDesdeLaBase();

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardado.getId())).isPresent();
        }

        @Test
        @DisplayName("reactivate() sobre un id inexistente no afecta filas")
        void reactivate_sobre_id_inexistente() {
            assertThat(repository.reactivate(999_999L, COMPANY)).isZero();
        }

        /**
         * El {@code AND company_id} del UPDATE es LA defensa de la reactivacion: no hay
         * lectura previa que valide la propiedad, el servicio decide si existe mirando
         * las filas afectadas. Con el companyId de otra empresa el UPDATE tiene que
         * tocar cero filas y el medicamento seguir borrado.
         */
        @Test
        @DisplayName("reactivate() con el companyId de otra empresa afecta 0 filas")
        void reactivate_con_otra_empresa_no_afecta_filas() {
            Medicament guardado = repository
                    .save(Medicament.create("Suero", null, companyRef, false));
            releerDesdeLaBase();
            repository.delete(guardado.getId());
            releerDesdeLaBase();

            int filas = repository.reactivate(guardado.getId(), OTRA_COMPANY);
            releerDesdeLaBase();

            assertThat(filas).isZero();
            assertThat(repository.findById(guardado.getId())).isEmpty();
        }

        @Test
        @DisplayName("findByIdAndCompanyId no ve el medicamento de otra empresa")
        void find_por_empresa_no_ve_el_de_otra_empresa() {
            Medicament propio = repository
                    .save(Medicament.create("Suero", null, companyRef, false));
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(propio.getId(), COMPANY)).isPresent();
            assertThat(repository.findByIdAndCompanyId(propio.getId(), OTRA_COMPANY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll — catalogo global paginado")
    class Listado {

        /**
         * Se pide en dos paginas de cinco a proposito: lo que hay que demostrar es que
         * el orden es total y que ninguna fila aparece en las dos paginas, y con una
         * sola pagina que abarque el catalogo entero eso no se puede ver.
         */
        @Test
        @DisplayName("ordena por nombre y no repite entre paginas")
        void ordena_por_nombre_sin_repetir() {
            repository.save(Medicament.create("Zaragatona", null, null, true));
            repository.save(Medicament.create("Amoxicilina", null, null, true));
            releerDesdeLaBase();

            PageResult<Medicament> primera = repository.findAll(0, 5);
            PageResult<Medicament> segunda = repository.findAll(1, 5);

            List<String> nombresDeLaPrimera = primera.content().stream().map(Medicament::getName)
                    .toList();
            List<String> nombresDeLasDos = Stream
                    .concat(primera.content().stream(), segunda.content().stream())
                    .map(Medicament::getName).toList();

            assertThat(nombresDeLaPrimera).isSorted();
            assertThat(nombresDeLasDos).containsSubsequence("Amoxicilina", "Zaragatona")
                    .doesNotHaveDuplicates().hasSize(GENERALES_SEMBRADOS + 2);
            assertThat(primera.totalElements()).isEqualTo(GENERALES_SEMBRADOS + 2L);
        }
    }
}
