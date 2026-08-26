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
    /**
     * Tercera molecula inventada, para el desempate por id entre pausadas
     * homonimas.
     */
    private static final String BETA = "Betamicina";

    /**
     * Los dos nombres del buscador, inventados por el mismo motivo que los de
     * arriba y con una exigencia mas: la semilla contiene «Amoxicilina +
     * Clavulánico», asi que buscar «clavul» contra la base real encontraria ademas
     * esa fila y el caso dejaria de poder afirmar el contenido exacto.
     * «Betavulanico» no existe en ningun sitio.
     *
     * <p>
     * {@link #COMPUESTO} es un nombre compuesto cuyo termino discriminante NO esta
     * al principio: es lo que separa una busqueda por subcadena de una por prefijo,
     * y media nomenclatura farmacologica es asi. {@link #ACENTUADO} lleva la tilde
     * dentro de ese mismo termino, que es lo que pone a prueba la collation.
     */
    private static final String COMPUESTO = "Alfamicina con betavulanico sintetico";
    private static final String ACENTUADO = "Gammamicina con Ácido betavulánico";

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

    /**
     * La misma linea base, pero del vademecum de PLATAFORMA: las filas que
     * {@code findAllGlobal} devuelve antes de que el caso escriba nada. Se mide
     * aparte de {@link #catalogoBase} porque la diferencia entre las dos —el numero
     * de medicamentos privados de las empresas— es justamente lo que separa las dos
     * consultas, y fijarla a mano volveria a atar estos casos al tamano de la
     * semilla.
     */
    private long catalogoGlobalBase;

    @BeforeEach
    void sembrarLaEmpresa() {
        SchemaSeed.seed(entityManager);
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(COMPANY);
        companyRef = new CompanyRef(COMPANY, company.getName(), company.getIdentifier());
        catalogoBase = repository.findAll(null, 0, 1).totalElements();
        catalogoGlobalBase = repository.findAllGlobal(null, 0, 1).totalElements();
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

            PageResult<Medicament> primera = repository.findAll(null, 0, 5);
            PageResult<Medicament> segunda = repository.findAll(null, 1, 5);
            List<String> catalogoEntero = repository.findAll(null, 0, Pages.MAX_SIZE).content()
                    .stream().map(Medicament::getName).toList();
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

    /**
     * {@code findAllGlobal} — el catalogo que administra la consola de plataforma,
     * frente a {@code findAll}, que devuelve ademas los privados de cada empresa
     * para dar contexto. La diferencia entre los dos totales es la unica forma de
     * ver que el {@code general = true} de la consulta esta puesto y funciona:
     * contra una base sin filas privadas los dos finders devolverian lo mismo y el
     * caso pasaria en verde con el filtro quitado.
     */
    @Nested
    @DisplayName("findAllGlobal — el vademecum de plataforma, paginado")
    class ListadoGlobal {

        @Test
        @DisplayName("trae los generales y deja fuera los privados de cualquier empresa")
        void solo_trae_los_generales() {
            repository.save(Medicament.create(ALFA, "Antibiotico", null, true));
            repository.save(Medicament.create("Suero", "Formula propia", companyRef, false));
            repository.save(Medicament.create("Exclusivo de otra", null, otraCompanyRef(), false));
            releerDesdeLaBase();

            PageResult<Medicament> globales = repository.findAllGlobal(null, 0, Pages.MAX_SIZE);

            assertThat(globales.content()).extracting(Medicament::getName).contains(ALFA)
                    .doesNotContain("Suero", "Exclusivo de otra");
            assertThat(globales.content()).allSatisfy(m -> {
                assertThat(m.isGeneral()).isTrue();
                assertThat(m.getCompany()).isNull();
            });
            // Una fila global mas; las dos privadas no cuentan. El contraste con
            // findAll -que suma las tres- es lo que demuestra el filtro.
            assertThat(globales.totalElements()).isEqualTo(catalogoGlobalBase + 1);
            assertThat(repository.findAll(null, 0, 1).totalElements()).isEqualTo(catalogoBase + 3);
        }

        /**
         * Mismo criterio que el caso gemelo de {@code findAll}: se piden dos paginas de
         * cinco porque con una sola que abarque el catalogo entero no se puede ver ni
         * que el orden sea total ni que ninguna fila aparezca en las dos. El contraste
         * va contra el listado completo y no contra un {@code isSorted()} de Java,
         * porque la base ordena con {@code utf8mb4_0900_ai_ci} y
         * {@code String.compareTo} no.
         */
        @Test
        @DisplayName("ordena por nombre y no repite entre paginas")
        void ordena_por_nombre_sin_repetir() {
            repository.save(Medicament.create(ZETA, null, null, true));
            repository.save(Medicament.create(ALFA, null, null, true));
            releerDesdeLaBase();

            PageResult<Medicament> primera = repository.findAllGlobal(null, 0, 5);
            PageResult<Medicament> segunda = repository.findAllGlobal(null, 1, 5);
            List<String> catalogoEntero = repository.findAllGlobal(null, 0, Pages.MAX_SIZE)
                    .content().stream().map(Medicament::getName).toList();
            List<String> nombresDeLasDos = Stream
                    .concat(primera.content().stream(), segunda.content().stream())
                    .map(Medicament::getName).toList();

            assertThat(primera.totalElements()).isEqualTo(catalogoGlobalBase + 2);
            assertThat(catalogoGlobalBase + 2)
                    .as("el vademecum global tiene que caber en una pagina de Pages.MAX_SIZE")
                    .isLessThanOrEqualTo(Pages.MAX_SIZE);
            assertThat(nombresDeLasDos).doesNotHaveDuplicates().hasSize(10)
                    .isEqualTo(catalogoEntero.subList(0, 10));
            assertThat(catalogoEntero).containsSubsequence(ALFA, ZETA).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("un global pausado sale del catalogo activo por el SQLRestriction")
        void un_global_pausado_no_esta_en_el_catalogo_activo() {
            Medicament global = repository.save(Medicament.create(ALFA, null, null, true));
            releerDesdeLaBase();
            repository.delete(global.getId());
            releerDesdeLaBase();

            assertThat(repository.findAllGlobal(null, 0, Pages.MAX_SIZE).content())
                    .extracting(Medicament::getName).doesNotContain(ALFA);
            assertThat(repository.findAllGlobal(null, 0, 1).totalElements())
                    .isEqualTo(catalogoGlobalBase);
        }
    }

    /**
     * {@code findAllDisabledGlobal} — la mitad del defecto que se cerro. La
     * consulta va aparte de {@code findAllDisabledForCompany} y no como un
     * {@code companyId} nulable porque en SQL {@code company_id = NULL} no casa
     * NUNCA, ni siquiera con las filas que tienen esa columna nula: con la consulta
     * acotada esta lista salia siempre vacia y un global pausado se quedaba sin
     * ninguna pantalla desde la que recuperarlo. Eso solo se puede probar contra
     * MySQL: para un mock, {@code = null} e {@code IS NULL} son la misma cadena.
     */
    @Nested
    @DisplayName("findAllDisabledGlobal — los globales pausados, que el IS NULL si encuentra")
    class ListadoGlobalPausados {

        /**
         * Estos casos fijan el CONTENIDO EXACTO de la lista, no solo que contenga lo
         * suyo, y eso solo es legitimo si la semilla no trae ningun global pausado. Se
         * comprueba en vez de suponerse: el dia que una migracion retire una molecula,
         * el fallo dira exactamente eso y no «el orden es otro».
         */
        @BeforeEach
        void laSemillaNoTraeGlobalesPausados() {
            assertThat(repository.findAllDisabledGlobal())
                    .as("la semilla no debe traer globales pausados; si los trae, "
                            + "estos casos dejan de poder afirmar el contenido exacto")
                    .isEmpty();
        }

        private Medicament pausarGlobal(String nombre) {
            Medicament guardado = repository
                    .save(Medicament.create(nombre, "Retirado", null, true));
            releerDesdeLaBase();
            repository.delete(guardado.getId());
            releerDesdeLaBase();
            return guardado;
        }

        @Test
        @DisplayName("ve el global pausado que el SQLRestriction esconde a todo lo demas")
        void ve_el_global_pausado() {
            Medicament global = pausarGlobal(ALFA);

            assertThat(repository.findById(global.getId())).isEmpty();
            assertThat(repository.findAllDisabledGlobal()).singleElement().satisfies(m -> {
                assertThat(m.getId()).isEqualTo(global.getId());
                assertThat(m.getName()).isEqualTo(ALFA);
                assertThat(m.isEnabled()).isFalse();
                assertThat(m.isGeneral()).isTrue();
                assertThat(m.getCompany()).isNull();
            });
        }

        /**
         * El orden lo pone ahora la consulta ({@code ORDER BY name ASC, id ASC}), asi
         * que la asercion pasa a ser el contenido EXACTO y en secuencia: sin el
         * {@code ORDER BY}, InnoDB no promete nada y la pantalla podia reordenarse
         * entre dos recargas, con la operadora reactivando la fila equivocada (#594).
         *
         * <p>
         * El desempate por id no es adorno: el indice unico cubre solo las filas
         * activas —{@code active_name} vale NULL con {@code enabled = false}—, asi que
         * la tabla admite N pausadas con el MISMO nombre. Este caso crea dos homonimas
         * a proposito, que es la unica forma de que el desempate se note.
         */
        @Test
        @DisplayName("ordena por nombre y desempata por id entre pausados homonimos")
        void ordena_por_nombre_y_desempata_por_id() {
            Medicament zeta = pausarGlobal(ZETA);
            Medicament betaPrimera = pausarGlobal(BETA);
            // La segunda «Betamicina» solo puede existir porque la primera esta
            // pausada y por tanto no ocupa el nombre.
            Medicament betaSegunda = pausarGlobal(BETA);
            Medicament alfa = pausarGlobal(ALFA);

            assertThat(betaSegunda.getId()).isGreaterThan(betaPrimera.getId());
            assertThat(repository.findAllDisabledGlobal()).extracting(Medicament::getId)
                    .containsExactly(alfa.getId(), betaPrimera.getId(), betaSegunda.getId(),
                            zeta.getId());
            assertThat(repository.findAllDisabledGlobal()).extracting(Medicament::getName)
                    .containsExactly(ALFA, BETA, BETA, ZETA);
        }

        @Test
        @DisplayName("no trae el pausado de una empresa: son dos catalogos distintos")
        void no_trae_el_pausado_de_una_empresa() {
            Medicament deEmpresa = repository
                    .save(Medicament.create("Suero", "Formula propia", companyRef, false));
            releerDesdeLaBase();
            repository.delete(deEmpresa.getId());
            releerDesdeLaBase();

            assertThat(repository.findAllDisabledGlobal()).isEmpty();
            // Y la gemela acotada si lo ve: no es que la fila no este pausada.
            assertThat(repository.findAllDisabledForCompany(COMPANY))
                    .extracting(Medicament::getName).containsExactly("Suero");
        }

        @Test
        @DisplayName("no trae los globales activos")
        void no_trae_los_globales_activos() {
            repository.save(Medicament.create(ZETA, null, null, true));
            releerDesdeLaBase();

            assertThat(repository.findAllDisabledGlobal()).isEmpty();
        }
    }

    /**
     * {@code reactivateGlobal} — la otra mitad del defecto. Aqui no hay lectura
     * previa que valide nada: el numero de filas afectadas ES la comprobacion de
     * existencia, y el {@code company_id IS NULL} del {@code WHERE} es lo unico que
     * impide que este camino resucite el medicamento privado de un tenant.
     */
    @Nested
    @DisplayName("reactivateGlobal — recuperar un global pausado")
    class ReactivacionGlobal {

        /**
         * El escenario que estaba roto, de punta a punta: se pausa un global, se
         * comprueba que la pantalla de reactivacion lo LISTA, se reactiva y se
         * comprueba que vuelve al catalogo activo. Los dos pasos van juntos a
         * proposito: cualquiera de los dos por separado podia estar bien mientras el
         * global seguia siendo irrecuperable.
         */
        @Test
        @DisplayName("un global pausado se lista en /disabled y se recupera")
        void un_global_pausado_se_lista_y_se_recupera() {
            Medicament global = repository.save(Medicament.create(ALFA, "Retirado", null, true));
            releerDesdeLaBase();
            // La version se lee RELEIDA de la base: el bloqueo optimista lo asigna el
            // motor, y afirmar sobre el valor en memoria probaria a Hibernate y no al
            // UPDATE nativo.
            Long versionAntes = repository.findById(global.getId()).orElseThrow().getVersion();
            repository.delete(global.getId());
            releerDesdeLaBase();

            assertThat(repository.findAllDisabledGlobal()).extracting(Medicament::getId)
                    .contains(global.getId());

            int filas = repository.reactivateGlobal(global.getId());
            releerDesdeLaBase();

            assertThat(filas).isEqualTo(1);
            Medicament releido = repository.findById(global.getId()).orElseThrow();
            assertThat(releido.isEnabled()).isTrue();
            assertThat(releido.isGeneral()).isTrue();
            assertThat(releido.getCompany()).isNull();
            // El bump de version va en el SET y no en el WHERE: sin el, un save cargado
            // antes reescribe la fila con su enabled = false y deshace la reactivacion
            // en silencio.
            assertThat(releido.getVersion()).isGreaterThan(versionAntes);
            assertThat(repository.findAllGlobal(null, 0, Pages.MAX_SIZE).content())
                    .extracting(Medicament::getName).contains(ALFA);
            assertThat(repository.findAllDisabledGlobal()).extracting(Medicament::getId)
                    .doesNotContain(global.getId());
        }

        /**
         * Por que la gemela existe, medido y no razonado: la consulta acotada con
         * {@code company_id = :companyId} y {@code null} compara {@code company_id =
         * NULL} y no casa con la fila, que TIENE la columna nula. El sintoma era un 404
         * permanente sobre un global que existe. Este caso falla si alguien
         * «simplifica» el adaptador reusando {@code reactivate(id, null)}.
         */
        @Test
        @DisplayName("la consulta acotada con empresa nula no recupera nada, la gemela si")
        void la_acotada_con_empresa_nula_no_recupera_nada() {
            Medicament global = repository.save(Medicament.create(ALFA, "Retirado", null, true));
            releerDesdeLaBase();
            repository.delete(global.getId());
            releerDesdeLaBase();

            int porLaAcotada = repository.reactivate(global.getId(), null);
            releerDesdeLaBase();

            assertThat(porLaAcotada).isZero();
            assertThat(repository.findById(global.getId())).isEmpty();

            int porLaGemela = repository.reactivateGlobal(global.getId());
            releerDesdeLaBase();

            assertThat(porLaGemela).isEqualTo(1);
            assertThat(repository.findById(global.getId())).isPresent();
        }

        @Test
        @DisplayName("NO alcanza el medicamento pausado de una empresa")
        void no_alcanza_el_pausado_de_una_empresa() {
            Medicament deEmpresa = repository
                    .save(Medicament.create("Suero", "Formula propia", companyRef, false));
            releerDesdeLaBase();
            repository.delete(deEmpresa.getId());
            releerDesdeLaBase();

            int filas = repository.reactivateGlobal(deEmpresa.getId());
            releerDesdeLaBase();

            assertThat(filas).isZero();
            assertThat(repository.findById(deEmpresa.getId())).isEmpty();
        }

        @Test
        @DisplayName("sobre un id inexistente no afecta filas")
        void sobre_un_id_inexistente_no_afecta_filas() {
            assertThat(repository.reactivateGlobal(999_999L)).isZero();
        }

        /**
         * El {@code AND enabled = false} del {@code WHERE}. Reactivar un global que YA
         * esta activo no alcanza ninguna fila y, sobre todo, NO mueve su
         * {@code version} — que era el dano real: quien tuviera abierto el formulario
         * de esa molecula recibia un 409 CONCURRENT_MODIFICATION al guardar sin que
         * nadie hubiera cambiado un solo dato.
         *
         * <p>
         * La contrapartida asumida es que el caso de uso traduce «cero filas» a un 404
         * sobre una fila que existe y esta activa. Es peor mensaje que «ya estaba
         * activo», y es justo la decision transversal que #484 sigue teniendo abierta
         * para los {@code reactivate} del repositorio. Aqui apenas se paga: la consola
         * solo ofrece reactivar sobre lo que devuelve {@code findAllDisabledGlobal()},
         * que por definicion esta pausado, asi que este 404 solo aparece en una carrera
         * de dos clics.
         */
        @Test
        @DisplayName("sobre un global YA activo no afecta ninguna fila y no le mueve la version")
        void sobre_un_global_ya_activo_no_afecta_filas() {
            Medicament global = repository.save(Medicament.create(ALFA, null, null, true));
            releerDesdeLaBase();
            Long versionAntes = repository.findById(global.getId()).orElseThrow().getVersion();

            int filas = repository.reactivateGlobal(global.getId());
            releerDesdeLaBase();

            assertThat(filas).isZero();
            Medicament releido = repository.findById(global.getId()).orElseThrow();
            assertThat(releido.isEnabled()).isTrue();
            assertThat(releido.getVersion()).isEqualTo(versionAntes);
        }
    }

    /**
     * El buscador por nombre de los dos listados paginados ({@code search} y
     * {@code searchGlobal}).
     *
     * <p>
     * <b>Por que va aqui y no en un unitario.</b> Todo lo que decide si un termino
     * encuentra una fila —que sea subcadena y no prefijo, la caja y sobre todo los
     * acentos— lo resuelve MySQL con la collation de la columna. Para un doble,
     * «acido» y «Ácido» son dos cadenas distintas y no hay nada que preguntar; la
     * pregunta solo existe contra el motor. El adaptador aporta exactamente una
     * decision propia —recortar y traducir «en blanco» a «sin filtro»— y tambien se
     * ejercita aqui de punta a punta.
     */
    @Nested
    @DisplayName("search / searchGlobal — la busqueda por nombre")
    class Busqueda {

        @Test
        @DisplayName("sin termino devuelve lo mismo que antes de existir la busqueda")
        void sin_termino_no_hay_regresion() {
            repository.save(Medicament.create(COMPUESTO, null, null, true));
            repository.save(Medicament.create("Suero", null, companyRef, false));
            releerDesdeLaBase();

            // Un global mas en el vademecum de plataforma; dos filas mas en el
            // catalogo sin acotar. Es exactamente lo que estos dos finders devolvian
            // antes de que la consulta tuviera un :q.
            assertThat(repository.findAllGlobal(null, 0, 1).totalElements())
                    .isEqualTo(catalogoGlobalBase + 1);
            assertThat(repository.findAll(null, 0, 1).totalElements()).isEqualTo(catalogoBase + 2);
        }

        /**
         * El {@code termino(q)} del adaptador. Un campo de texto vacio en el front no
         * debe cambiar nada, y lo que la consulta necesita para no filtrar es
         * {@code null}: {@code LIKE '%%'} tampoco filtraria, pero no es lo mismo —
         * dejaria fuera las filas con {@code name} nulo el dia que las hubiera, y
         * ademas impide que el plan use el camino sin predicado.
         */
        @Test
        @DisplayName("la cadena vacia y la de solo espacios equivalen a no buscar")
        void blanco_y_vacio_equivalen_a_no_buscar() {
            repository.save(Medicament.create(COMPUESTO, null, null, true));
            releerDesdeLaBase();

            long globalSinTermino = repository.findAllGlobal(null, 0, 1).totalElements();
            assertThat(repository.findAllGlobal("", 0, 1).totalElements())
                    .isEqualTo(globalSinTermino);
            assertThat(repository.findAllGlobal("   ", 0, 1).totalElements())
                    .isEqualTo(globalSinTermino);

            long sinAcotarSinTermino = repository.findAll(null, 0, 1).totalElements();
            assertThat(repository.findAll("", 0, 1).totalElements()).isEqualTo(sinAcotarSinTermino);
            assertThat(repository.findAll("  ", 0, 1).totalElements())
                    .isEqualTo(sinAcotarSinTermino);
        }

        /**
         * El caso que obligo a elegir SUBCADENA y no prefijo: media nomenclatura
         * farmacologica es compuesta, asi que el termino que la usuaria recuerda casi
         * nunca es la primera palabra del nombre.
         */
        @Test
        @DisplayName("encuentra por una subcadena que NO esta al principio del nombre")
        void encuentra_por_subcadena_en_medio() {
            Medicament compuesto = repository.save(Medicament.create(COMPUESTO, null, null, true));
            repository.save(Medicament.create(ZETA, null, null, true));
            releerDesdeLaBase();

            assertThat(COMPUESTO)
                    .as("el termino tiene que estar en medio, o el caso no prueba nada")
                    .doesNotStartWith("betavulanico").contains("betavulanico");
            assertThat(repository.findAllGlobal("betavulanico", 0, Pages.MAX_SIZE).content())
                    .extracting(Medicament::getId).containsExactly(compuesto.getId());
        }

        @Test
        @DisplayName("ignora la caja, la del termino y la del nombre")
        void ignora_la_caja() {
            Medicament compuesto = repository.save(Medicament.create(COMPUESTO, null, null, true));
            releerDesdeLaBase();

            assertThat(repository.findAllGlobal("BETAVULANICO", 0, Pages.MAX_SIZE).content())
                    .extracting(Medicament::getId).containsExactly(compuesto.getId());
            assertThat(repository.findAllGlobal("BeTaVuLaNiCo", 0, Pages.MAX_SIZE).content())
                    .extracting(Medicament::getId).containsExactly(compuesto.getId());
            assertThat(repository.findAllGlobal("alfamicina", 0, Pages.MAX_SIZE).content())
                    .extracting(Medicament::getId).containsExactly(compuesto.getId());
        }

        @Test
        @DisplayName("los espacios de alrededor del termino no cambian el resultado")
        void recorta_el_termino() {
            Medicament compuesto = repository.save(Medicament.create(COMPUESTO, null, null, true));
            releerDesdeLaBase();

            assertThat(repository.findAllGlobal("   betavulanico   ", 0, Pages.MAX_SIZE).content())
                    .extracting(Medicament::getId).containsExactly(compuesto.getId());
        }

        /**
         * El caso que importa: que el buscador no se convierta en la fuga que se ha
         * cerrado por todos los demas lados. Un termino que casa con el medicamento
         * PRIVADO de una clinica no lo devuelve por {@code /admin/medicaments}, que es
         * la superficie de plataforma.
         *
         * <p>
         * El contraste con {@code findAll} —el catalogo sin acotar de
         * {@code GET /medicaments}, que es SYSTEM desde BE-29 y existe justamente para
         * dar contexto— es lo que demuestra que el corte lo pone el ambito de cada
         * consulta y no el termino: la misma busqueda, dos alcances distintos.
         */
        @Test
        @DisplayName("la busqueda global NO devuelve el medicamento privado de una clinica")
        void la_busqueda_global_no_alcanza_lo_privado() {
            Medicament global = repository.save(Medicament.create(COMPUESTO, null, null, true));
            Medicament privado = repository.save(Medicament
                    .create("Suero con betavulanico de la clinica", null, companyRef, false));
            releerDesdeLaBase();

            assertThat(repository.findAllGlobal("betavulanico", 0, Pages.MAX_SIZE).content())
                    .extracting(Medicament::getId).containsExactly(global.getId());
            assertThat(repository.findAll("betavulanico", 0, Pages.MAX_SIZE).content())
                    .extracting(Medicament::getId)
                    .containsExactlyInAnyOrder(global.getId(), privado.getId());
        }

        /**
         * <b>La prueba de los acentos, medida contra MySQL y no razonada.</b>
         *
         * <p>
         * El backend NO normaliza en Java a proposito, y su argumento es que el indice
         * unico y este {@code LIKE} comparan sobre la MISMA columna y por tanto con la
         * misma collation, asi que buscar y chocar responden al mismo criterio sea cual
         * sea. El argumento es correcto en su forma, pero lo que hay que saber es el
         * criterio CONCRETO: si la collation no fuera insensible a acentos, existiria
         * el 409 fantasma que se queria evitar —buscar «betavulanico», no encontrarlo,
         * darlo de alta y recibir un 409 sobre exactamente lo que se acaba de buscar
         * sin exito—.
         *
         * <p>
         * Este caso lo resuelve: si falla, ese 409 fantasma existe y hay que
         * reportarlo, no taparlo normalizando en Java. Su gemelo de la guarda de
         * unicidad es {@code la_igualdad_ignora_acentos_y_caja}, unos casos mas arriba,
         * que prueba lo mismo para el {@code =} del indice.
         */
        @Test
        @DisplayName("un termino SIN tilde encuentra el nombre CON tilde")
        void un_termino_sin_tilde_encuentra_el_nombre_con_tilde() {
            Medicament acentuado = repository.save(Medicament.create(ACENTUADO, null, null, true));
            releerDesdeLaBase();

            assertThat(ACENTUADO).as("el nombre tiene que llevar tildes, o el caso no prueba nada")
                    .contains("Ácido betavulánico");
            assertThat(repository.findAllGlobal("betavulanico", 0, Pages.MAX_SIZE).content())
                    .extracting(Medicament::getId).containsExactly(acentuado.getId());
            assertThat(repository.findAllGlobal("acido betavulanico", 0, Pages.MAX_SIZE).content())
                    .extracting(Medicament::getId).containsExactly(acentuado.getId());
        }

        /**
         * La direccion contraria, que es la que de verdad usa quien copia el nombre de
         * un envase: el termino lleva la tilde y el nombre tambien. El segundo caso
         * suma la caja a la tilde, que es la combinacion completa.
         *
         * <p>
         * <b>Cuidado con el termino que se elige.</b> Aqui se busca «betavulanico» y no
         * «Ácido», que seria lo natural: la semilla del changeset 299 trae CUATRO
         * moleculas cuyo nombre contiene «Ácido», asi que ese termino devuelve cinco
         * filas y el caso no puede afirmar el contenido exacto. Se descubrio
         * ejecutandolo. El termino de un caso que usa {@code containsExactly} tiene que
         * ser una palabra que la semilla no contenga.
         */
        @Test
        @DisplayName("un termino CON tilde, y con tilde y mayusculas, tambien encuentra")
        void un_termino_con_tilde_tambien_encuentra() {
            Medicament acentuado = repository.save(Medicament.create(ACENTUADO, null, null, true));
            releerDesdeLaBase();

            assertThat(repository.findAllGlobal("betavulánico", 0, Pages.MAX_SIZE).content())
                    .extracting(Medicament::getId).containsExactly(acentuado.getId());
            assertThat(repository.findAllGlobal("BETAVULÁNICO", 0, Pages.MAX_SIZE).content())
                    .extracting(Medicament::getId).containsExactly(acentuado.getId());
        }

        /**
         * La otra cara de que la collation sea insensible a acentos: dos moleculas que
         * solo se diferencian en la tilde caen las dos bajo el mismo termino. Es el
         * comportamiento que se quiere —quien busca «acido» no sabe si el catalogo lo
         * escribio con tilde— y es tambien la razon de que un termino corto y comun
         * filtre poco. Va con filas propias y no contra la semilla para no atarse a
         * cuantas moleculas acentuadas traiga el changeset de turno.
         */
        @Test
        @DisplayName("un termino sin tilde alcanza a la vez la fila con tilde y la que no la lleva")
        void un_termino_sin_tilde_alcanza_las_dos_grafias() {
            Medicament conTilde = repository.save(Medicament.create(ACENTUADO, null, null, true));
            Medicament sinTilde = repository.save(
                    Medicament.create("Deltamicina con acido betavulanico", null, null, true));
            releerDesdeLaBase();

            assertThat(repository.findAllGlobal("acido betavulanico", 0, Pages.MAX_SIZE).content())
                    .extracting(Medicament::getId)
                    .containsExactlyInAnyOrder(conTilde.getId(), sinTilde.getId());
        }

        @Test
        @DisplayName("un termino que no casa devuelve pagina vacia con total cero")
        void termino_sin_resultados() {
            repository.save(Medicament.create(COMPUESTO, null, null, true));
            releerDesdeLaBase();

            PageResult<Medicament> vacia = repository.findAllGlobal("no-existe-esta-molecula", 0,
                    Pages.MAX_SIZE);

            assertThat(vacia.content()).isEmpty();
            assertThat(vacia.totalElements()).isZero();
            assertThat(vacia.totalPages()).isZero();
        }

        /**
         * La busqueda no se lleva por delante el orden ni la paginacion: el
         * {@code PAGE_ORDER} del adaptador se aplica igual con termino que sin el.
         */
        @Test
        @DisplayName("los resultados siguen ordenados por nombre y siguen paginando")
        void los_resultados_siguen_ordenados_y_paginados() {
            repository.save(Medicament.create("Zetamicina betavulanico", null, null, true));
            repository.save(Medicament.create("Alfamicina betavulanico", null, null, true));
            repository.save(Medicament.create("Gammamicina betavulanico", null, null, true));
            releerDesdeLaBase();

            PageResult<Medicament> primera = repository.findAllGlobal("betavulanico", 0, 2);
            PageResult<Medicament> segunda = repository.findAllGlobal("betavulanico", 1, 2);

            assertThat(primera.totalElements()).isEqualTo(3L);
            assertThat(primera.totalPages()).isEqualTo(2);
            assertThat(primera.content()).extracting(Medicament::getName)
                    .containsExactly("Alfamicina betavulanico", "Gammamicina betavulanico");
            assertThat(segunda.content()).extracting(Medicament::getName)
                    .containsExactly("Zetamicina betavulanico");
        }
    }
}
