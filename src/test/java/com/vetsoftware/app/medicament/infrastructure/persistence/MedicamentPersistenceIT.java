package com.vetsoftware.app.medicament.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.medicament.domain.CompanyRef;
import com.vetsoftware.app.medicament.domain.Medicament;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
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
 * soft-delete, el nativo que ve los pausados y la guarda de unicidad de nombre
 * por ambito (#559).
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaMedicamentRepository — catalogo de medicamentos contra MySQL real")
class MedicamentPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;

    /**
     * Nombres inventados a proposito. Esta rodaja corre contra las migraciones
     * REALES y el catalogo global NO esta vacio: 173b sembro seis moleculas y 299
     * anadio 154 mas (docs/db/semilla-medicamentos.md), entre ellas «Amoxicilina»,
     * que es como se llamaba el medicamento de prueba de estos casos. Bajo el
     * indice unico por propietario que introduce 285
     * ({@code uq_medicaments_owner_active_name} sobre {@code owner_scope} +
     * {@code active_name}) ese choque de nombre ya no es un detalle estetico: el
     * INSERT falla. Alfamicina y Zetamicina no existen en la semilla y ademas
     * enmarcan el alfabeto, que es lo que necesita el caso de la paginacion.
     */
    private static final String ALFA = "Alfamicina";
    private static final String ZETA = "Zetamicina";

    @Autowired
    private JpaMedicamentRepository repository;
    @Autowired
    private CompanyJpaRepository companyJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private CompanyRef companyRef;

    /**
     * Linea base del catalogo, MEDIDA y no fijada a mano. El numero de filas
     * globales sembradas ha cambiado dos veces (6 con 173b, 160 con 299) y cada
     * cambio rompio estos casos. Contar antes de escribir hace que las aserciones
     * hablen de lo que el test aporta —«dos filas mas»— y no del tamano del
     * catalogo, que no es asunto suyo.
     */
    private long catalogoBase;

    @BeforeEach
    void sembrarLaEmpresa() {
        SchemaSeed.seed(entityManager);
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(COMPANY);
        companyRef = new CompanyRef(COMPANY, company.getName(), company.getIdentifier());
        catalogoBase = repository.findAll(0, 1).totalElements();
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    private CompanyRef otraCompanyRef() {
        CompanyJpaEntity otra = companyJpaRepository.getReferenceById(OTRA_COMPANY);
        return new CompanyRef(OTRA_COMPANY, otra.getName(), otra.getIdentifier());
    }

    @Nested
    @DisplayName("save")
    class Guardado {

        @Test
        @DisplayName("persiste un medicamento general sin empresa")
        void persiste_un_medicamento_general() {
            Medicament guardado = repository
                    .save(Medicament.create(ALFA, "Antibiotico", null, true));
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
            Medicament guardado = repository.save(Medicament.create(ALFA, null, null, true));
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
            repository.save(Medicament.create(ALFA, null, null, true));
            repository.save(Medicament.create("Suero", null, companyRef, false));
            repository.save(Medicament.create("Exclusivo de otra", null, otraCompanyRef(), false));
            releerDesdeLaBase();

            var disponibles = repository.findAllAvailableForCompany(COMPANY);

            // Lo que se prueba es el criterio de la consulta, no el tamano del catalogo:
            // entran el general recien creado y el propio, y NO entra el de la otra
            // empresa. Los generales sembrados por las migraciones entran por definicion
            // —son generales— y por eso el total se cuenta sobre la linea base medida.
            assertThat(disponibles).extracting(Medicament::getName).contains(ALFA, "Suero")
                    .doesNotContain("Exclusivo de otra");
            assertThat(disponibles).hasSize((int) catalogoBase + 2);
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

    /**
     * La guarda de unicidad de nombre de #559. Nada de esto lo puede probar un
     * unitario: la fila pausada la oculta el {@code @SQLRestriction}, la igualdad
     * de nombres la decide la collation de MySQL y el reparto entre las dos ramas
     * del adaptador —empresa contra vademecum de plataforma— solo se ve contra la
     * base.
     */
    @Nested
    @DisplayName("findByNameAndCompanyIdIncludingDisabled — la consulta que SI ve los pausados")
    class BusquedaPorNombre {

        @Test
        @DisplayName("ve la fila PAUSADA que el SQLRestriction esconde a todo lo demas")
        void ve_la_fila_pausada() {
            Medicament guardado = repository
                    .save(Medicament.create("Suero", "Formula vieja", companyRef, false));
            releerDesdeLaBase();
            repository.delete(guardado.getId());
            releerDesdeLaBase();

            // El resto del adaptador ya no la ve: por eso el alta chocaba contra un
            // nombre invisible en el catalogo activo de la clinica (#432).
            assertThat(repository.findById(guardado.getId())).isEmpty();

            Medicament encontrado = repository
                    .findByNameAndCompanyIdIncludingDisabled("Suero", COMPANY).orElseThrow();

            assertThat(encontrado.getId()).isEqualTo(guardado.getId());
            assertThat(encontrado.isEnabled()).isFalse();
            assertThat(encontrado.getCompany().id()).isEqualTo(COMPANY);
        }

        /**
         * La collation de la columna es {@code utf8mb4_0900_ai_ci}: insensible a
         * acentos y a caja, que es el MISMO criterio con el que el indice unico decide
         * si dos nombres chocan. Comparar en Java diria que el nombre esta libre y la
         * base lo rechazaria despues — y en nomenclatura farmacologica, llena de
         * tildes, ese es el fallo mas facil de introducir aqui (#557).
         */
        @Test
        @DisplayName("la igualdad de nombre ignora acentos y caja, como el indice unico")
        void la_igualdad_ignora_acentos_y_caja() {
            Medicament guardado = repository.save(Medicament
                    .create("Amoxicilina + Ácido clavulánico", "Antibiotico", companyRef, false));
            releerDesdeLaBase();

            assertThat(repository.findByNameAndCompanyIdIncludingDisabled(
                    "amoxicilina + acido clavulanico", COMPANY)).get().extracting(Medicament::getId)
                    .isEqualTo(guardado.getId());
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled(
                    "AMOXICILINA + ÁCIDO CLAVULÁNICO", COMPANY)).get().extracting(Medicament::getId)
                    .isEqualTo(guardado.getId());
        }

        /**
         * El defecto #580. El indice unico cubre solo las filas ACTIVAS
         * —{@code active_name} vale NULL con {@code enabled = false} y MySQL no
         * deduplica NULL—, asi que la tabla admite UNA activa y N dadas de baja con el
         * mismo nombre. Sin el {@code ORDER BY enabled DESC, id DESC} + {@code LIMIT 1}
         * esta consulta devolvia dos filas a un {@code Optional} y reventaba con
         * {@code IncorrectResultSizeDataAccessException}: un 500 que dejaba ese nombre
         * inutilizable para siempre, porque ni el alta ni la reactivacion podian pasar
         * de ahi. Se llega dando de alta y de baja el mismo nombre dos veces, que en un
         * catalogo clinico no es raro.
         */
        @Test
        @DisplayName("con una activa y dos homonimas dadas de baja devuelve la ACTIVA, sin reventar")
        void con_varias_homonimas_devuelve_la_activa() {
            Medicament primeraBaja = repository
                    .save(Medicament.create("Suero", "v1", companyRef, false));
            releerDesdeLaBase();
            repository.delete(primeraBaja.getId());
            releerDesdeLaBase();

            Medicament segundaBaja = repository
                    .save(Medicament.create("Suero", "v2", companyRef, false));
            releerDesdeLaBase();
            repository.delete(segundaBaja.getId());
            releerDesdeLaBase();

            Medicament activa = repository
                    .save(Medicament.create("Suero", "v3", companyRef, false));
            releerDesdeLaBase();

            Medicament encontrado = repository
                    .findByNameAndCompanyIdIncludingDisabled("Suero", COMPANY).orElseThrow();

            // La activa es la unica que de verdad ocupa el nombre, y es la que tiene que
            // hacer saltar el conflicto del alta.
            assertThat(encontrado.getId()).isEqualTo(activa.getId());
            assertThat(encontrado.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("sin ninguna activa y dos homonimas dadas de baja devuelve la de id MAYOR")
        void sin_activas_devuelve_la_baja_mas_reciente() {
            Medicament antigua = repository
                    .save(Medicament.create("Suero", "v1", companyRef, false));
            releerDesdeLaBase();
            repository.delete(antigua.getId());
            releerDesdeLaBase();

            Medicament reciente = repository
                    .save(Medicament.create("Suero", "v2", companyRef, false));
            releerDesdeLaBase();
            repository.delete(reciente.getId());
            releerDesdeLaBase();

            Medicament encontrado = repository
                    .findByNameAndCompanyIdIncludingDisabled("Suero", COMPANY).orElseThrow();

            // La mas reciente es la que la usuaria espera recuperar al volver a dar de
            // alta ese nombre; resucitar la de hace dos años seria una sorpresa.
            assertThat(encontrado.getId()).isEqualTo(reciente.getId());
            assertThat(encontrado.getId()).isGreaterThan(antigua.getId());
            assertThat(encontrado.isEnabled()).isFalse();
            assertThat(encontrado.getDescription()).isEqualTo("v2");
        }

        @Test
        @DisplayName("la rama global tambien tolera homonimas dadas de baja")
        void la_rama_global_tolera_homonimas_dadas_de_baja() {
            Medicament antigua = repository.save(Medicament.create(ALFA, "v1", null, true));
            releerDesdeLaBase();
            repository.delete(antigua.getId());
            releerDesdeLaBase();

            Medicament reciente = repository.save(Medicament.create(ALFA, "v2", null, true));
            releerDesdeLaBase();
            repository.delete(reciente.getId());
            releerDesdeLaBase();

            Medicament encontrado = repository.findByNameAndCompanyIdIncludingDisabled(ALFA, null)
                    .orElseThrow();

            assertThat(encontrado.getId()).isEqualTo(reciente.getId());
            assertThat(encontrado.getCompany()).isNull();
        }

        /**
         * Las dos ramas de la ternaria del adaptador. {@code companyId == null} tiene
         * que ir a la consulta de {@code company_id IS NULL} y no a la parametrizada:
         * un {@code = NULL} no casa nunca en SQL, asi que el ambito global se quedaria
         * sin guarda EN SILENCIO, que es la peor forma de perderla.
         */
        @Test
        @DisplayName("el ambito nulo busca en el vademecum global y el no nulo en la empresa")
        void las_dos_ramas_del_ambito_no_se_cruzan() {
            Medicament global = repository
                    .save(Medicament.create(ALFA, "De plataforma", null, true));
            Medicament deEmpresa = repository
                    .save(Medicament.create(ALFA, "De la clinica", companyRef, false));
            releerDesdeLaBase();

            assertThat(repository.findByNameAndCompanyIdIncludingDisabled(ALFA, null)).get()
                    .extracting(Medicament::getId).isEqualTo(global.getId());
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled(ALFA, COMPANY)).get()
                    .extracting(Medicament::getId).isEqualTo(deEmpresa.getId());
            // La empresa ajena no tiene ninguno con ese nombre: el mismo nombre puede
            // vivir en tantos ambitos como empresas haya.
            assertThat(repository.findByNameAndCompanyIdIncludingDisabled(ALFA, OTRA_COMPANY))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("existsActiveByNameAndCompanyIdExcludingId — la guarda de la edicion")
    class ExistenciaPorNombre {

        @Test
        @DisplayName("no cuenta la fila que se esta editando ni las pausadas")
        void no_cuenta_la_fila_editada_ni_las_pausadas() {
            Medicament propio = repository
                    .save(Medicament.create("Suero", null, companyRef, false));
            Medicament pausado = repository
                    .save(Medicament.create("Analgesico", null, companyRef, false));
            releerDesdeLaBase();
            repository.delete(pausado.getId());
            releerDesdeLaBase();

            // Renombrarse a si mismo no es un choque.
            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId("Suero", COMPANY,
                    propio.getId())).isFalse();
            // Una fila pausada no ocupa el nombre: el indice unico solo cubre las activas.
            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId("Analgesico", COMPANY,
                    propio.getId())).isFalse();
        }

        @Test
        @DisplayName("otra fila activa de la MISMA empresa con ese nombre si es un choque")
        void otra_fila_activa_de_la_misma_empresa_choca() {
            Medicament ocupante = repository
                    .save(Medicament.create("Analgesico", null, companyRef, false));
            Medicament editado = repository
                    .save(Medicament.create("Suero", null, companyRef, false));
            releerDesdeLaBase();

            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId("Analgesico", COMPANY,
                    editado.getId())).isTrue();
            assertThat(ocupante.getId()).isNotEqualTo(editado.getId());
        }

        @Test
        @DisplayName("las dos ramas del ambito no se cruzan: global y empresa son catalogos distintos")
        void las_dos_ramas_del_ambito_no_se_cruzan() {
            repository.save(Medicament.create(ALFA, "De plataforma", null, true));
            Medicament deOtraEmpresa = repository
                    .save(Medicament.create(ALFA, "De la otra clinica", otraCompanyRef(), false));
            releerDesdeLaBase();

            // Rama global (companyId == null): ve la fila de plataforma...
            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId(ALFA, null, 999_999L))
                    .isTrue();
            // ...y NO ve la de una empresa.
            assertThat(
                    repository.existsActiveByNameAndCompanyIdExcludingId(ALFA, COMPANY, 999_999L))
                    .isFalse();
            assertThat(repository.existsActiveByNameAndCompanyIdExcludingId(ALFA, OTRA_COMPANY,
                    deOtraEmpresa.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("reactivateWithDetails — el alta que se encuentra el nombre ocupado por una pausada")
    class ReactivacionConDetalles {

        @Test
        @DisplayName("reactiva, aplica la descripcion nueva y sube la version")
        void reactiva_aplica_la_descripcion_y_sube_la_version() {
            Medicament guardado = repository.save(
                    Medicament.create("Suero", "Formula de hace dos años", companyRef, false));
            releerDesdeLaBase();
            // La version se lee RELEIDA de la base y no del objeto que devuelve save():
            // el bloqueo optimista lo asigna el motor, y afirmar sobre el valor en
            // memoria probaria a Hibernate, no al UPDATE nativo.
            Long versionAntes = repository.findById(guardado.getId()).orElseThrow().getVersion();
            repository.delete(guardado.getId());
            releerDesdeLaBase();

            int filas = repository.reactivateWithDetails(guardado.getId(), COMPANY, "Suero",
                    "Formula revisada");
            releerDesdeLaBase();

            assertThat(filas).isEqualTo(1);
            Medicament releido = repository.findById(guardado.getId()).orElseThrow();
            assertThat(releido.isEnabled()).isTrue();
            assertThat(releido.getDescription()).isEqualTo("Formula revisada");
            // La version se mueve a proposito: una consulta nativa ni comprueba ni
            // incrementa el bloqueo optimista, y sin el bump un save cargado antes
            // reescribe la fila entera con su enabled = false y deshace la reactivacion
            // en silencio.
            assertThat(releido.getVersion()).isGreaterThan(versionAntes);
        }

        /**
         * La rama del vademecum de plataforma. El {@code WHERE} nombra igualmente
         * {@code company_id IS NULL}: acotar por «no tiene empresa» es lo que impide
         * que este camino alcance el medicamento privado de un tenant.
         */
        @Test
        @DisplayName("la rama global reactiva la fila sin empresa")
        void la_rama_global_reactiva_la_fila_sin_empresa() {
            Medicament global = repository.save(Medicament.create(ALFA, "Retirado", null, true));
            releerDesdeLaBase();
            repository.delete(global.getId());
            releerDesdeLaBase();

            int filas = repository.reactivateWithDetails(global.getId(), null, ALFA, "Revisado");
            releerDesdeLaBase();

            assertThat(filas).isEqualTo(1);
            Medicament releido = repository.findById(global.getId()).orElseThrow();
            assertThat(releido.isEnabled()).isTrue();
            assertThat(releido.getDescription()).isEqualTo("Revisado");
            assertThat(releido.getCompany()).isNull();
        }

        @Test
        @DisplayName("la rama global NO alcanza la fila pausada de una empresa")
        void la_rama_global_no_alcanza_la_fila_de_una_empresa() {
            Medicament deEmpresa = repository
                    .save(Medicament.create("Suero", "Formula propia", companyRef, false));
            releerDesdeLaBase();
            repository.delete(deEmpresa.getId());
            releerDesdeLaBase();

            int filas = repository.reactivateWithDetails(deEmpresa.getId(), null, "Suero",
                    "Robado");
            releerDesdeLaBase();

            assertThat(filas).isZero();
            assertThat(repository.findById(deEmpresa.getId())).isEmpty();
        }

        @Test
        @DisplayName("la rama de empresa NO alcanza la fila pausada de otra empresa")
        void la_rama_de_empresa_no_alcanza_la_de_otra_empresa() {
            Medicament deEmpresa = repository
                    .save(Medicament.create("Suero", "Formula propia", companyRef, false));
            releerDesdeLaBase();
            repository.delete(deEmpresa.getId());
            releerDesdeLaBase();

            int filas = repository.reactivateWithDetails(deEmpresa.getId(), OTRA_COMPANY, "Suero",
                    "Robado");
            releerDesdeLaBase();

            assertThat(filas).isZero();
            assertThat(repository.findById(deEmpresa.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll — catalogo global paginado")
    class Listado {

        /**
         * Se pide en dos paginas de cinco a proposito: lo que hay que demostrar es que
         * el orden es total y que ninguna fila aparece en las dos paginas, y con una
         * sola pagina que abarque el catalogo entero eso no se puede ver.
         *
         * <p>
         * La secuencia de las dos paginas se contrasta contra el listado completo y no
         * contra un {@code isSorted()} de Java: la base ordena con
         * {@code utf8mb4_0900_ai_ci} y {@code String.compareTo} no, asi que con un
         * catalogo de moleculas acentuadas ese {@code isSorted()} fallaria sobre una
         * consulta correcta.
         */
        @Test
        @DisplayName("ordena por nombre y no repite entre paginas")
        void ordena_por_nombre_sin_repetir() {
            repository.save(Medicament.create(ZETA, null, null, true));
            repository.save(Medicament.create(ALFA, null, null, true));
            releerDesdeLaBase();

            PageResult<Medicament> primera = repository.findAll(0, 5);
            PageResult<Medicament> segunda = repository.findAll(1, 5);
            List<String> catalogoEntero = repository.findAll(0, Pages.MAX_SIZE).content().stream()
                    .map(Medicament::getName).toList();
            List<String> nombresDeLasDos = Stream
                    .concat(primera.content().stream(), segunda.content().stream())
                    .map(Medicament::getName).toList();

            assertThat(primera.totalElements()).isEqualTo(catalogoBase + 2);
            // El caso compara contra el catalogo completo, asi que deja de ser valido si
            // el catalogo desborda una pagina: se afirma para que ese dia falle diciendo
            // por que, en vez de degradarse en silencio.
            assertThat(catalogoBase + 2)
                    .as("el catalogo tiene que caber en una pagina de Pages.MAX_SIZE")
                    .isLessThanOrEqualTo(Pages.MAX_SIZE);
            assertThat(nombresDeLasDos).doesNotHaveDuplicates().hasSize(10)
                    .isEqualTo(catalogoEntero.subList(0, 10));
            assertThat(catalogoEntero).containsSubsequence(ALFA, ZETA).doesNotHaveDuplicates();
        }
    }
}
