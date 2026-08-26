package com.vetsoftware.app.spatype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.spatype.domain.SpaType;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia de {@code JpaSpaTypeRepository} contra MySQL real.
 *
 * <p>
 * <b>Es el primer fichero de test que ha tenido nunca la rodaja
 * {@code spatype}</b> (#426): ni dominio, ni caso de uso, ni adaptador, ni
 * controller. El adaptador es de cuatro lineas y parece no tener nada que
 * probar; lo que tiene que probarse no esta en el, esta en las tres anotaciones
 * de {@link SpaTypeJpaEntity} y en el {@code UPDATE} nativo de
 * {@link SpaTypeJpaRepository#reactivate(Long)}, y ninguna de las cuatro se
 * puede falsear con un doble porque las ejecuta el motor, no el codigo Java:
 *
 * <ul>
 * <li><b>{@code name} es {@code UNIQUE} y es GLOBAL.</b> {@code spa_types} es
 * catalogo maestro de plataforma: no tiene {@code company_id}, asi que aqui no
 * cabe la comprobacion de dos caras «dos empresas pueden repetir la llave» — no
 * hay dos empresas. Lo que si cabe, y es la mitad que importa, es que la llave
 * siga deduplicando: ver {@link Unicidad}.
 * <li><b>El borrado es logico y la clave unica no lo sabe.</b> El
 * {@code @SQLDelete} apaga el {@code enabled} y el {@code @SQLRestriction} hace
 * desaparecer la fila de toda lectura — pero la fila sigue en la tabla y sigue
 * ocupando el {@code name}. Ver {@link BorradoLogicoYClaveUnica}, que es donde
 * esta el hallazgo de esta rodaja.
 * <li><b>El {@code @SQLDelete} liga DOS parametros</b> —{@code id} y
 * {@code version}— desde que la entidad se versiono. Se lee perfecto y el
 * compilador calla; solo se nota al borrar de verdad.
 * <li><b>{@code reactivate} es {@code UPDATE} nativo</b>: va directo a la base,
 * salta el {@code @SQLRestriction} —que es justo lo que necesita, porque la
 * fila que resucita es invisible— y mueve la {@code version} a mano.
 * </ul>
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSpaTypeRepository — catálogo maestro de tipos de spa contra MySQL real")
class SpaTypePersistenceIT extends AbstractDataJpaTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 8, 23, 10, 0);

    /**
     * <b>En un catálogo global los fixtures ya no pueden usar nombres del mundo
     * real, porque el mundo real está sembrado.</b> Esta constante existe para
     * dejar esa regla escrita donde se tropieza con ella.
     *
     * <p>
     * {@link com.vetsoftware.app.testsupport.AbstractDataJpaTest} levanta MySQL de
     * verdad y corre Liquibase entero, así que la rodaja arranca con las 32 filas
     * de {@code 298_seed_spa_types_catalog.xml} ya dentro — y «Baño medicado» es
     * una de ellas. Además, en {@code spa_types} <b>todo fixture es global</b>: la
     * entidad no mapea {@code company_id} ni {@code general}, así que cada fila que
     * guarda un test nace con {@code company_id = NULL} y {@code general = TRUE}
     * (el {@code DEFAULT} del changeset 288), es decir con el mismo
     * {@code owner_scope = 0} que la semilla. Un fixture con un nombre sembrado
     * choca contra {@code uq_spa_types_owner_active_name} en el propio INSERT: el
     * test no falla por lo que afirma, falla al montarse, y el error apunta a la
     * constraint y no al descuido.
     *
     * <p>
     * <b>Quitar la tilde no es escapatoria.</b> La igualdad la decide la collation
     * {@code utf8mb4_0900_ai_ci}, insensible a acentos y a caja: «bano medicado»
     * colisiona exactamente igual que «Baño medicado». El sufijo «de prueba» sí
     * separa, porque cambia la cadena y no solo su acentuación.
     */
    private static final String BANO_MEDICADO = "Baño medicado de prueba";

    @Autowired
    private JpaSpaTypeRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Construye por el constructor publico y no por {@code SpaType.create(...)}:
     * ese llama a {@code LocalDateTime.now()} y dejaria el test dependiendo del
     * reloj de la maquina. Deuda ya registrada del repo, no excusa para propagarla.
     */
    private SpaType nuevo(String nombre, String descripcion) {
        return new SpaType(null, nombre, descripcion, CREADO, null, true);
    }

    private SpaType guardar(String nombre, String descripcion) {
        return repository.save(nuevo(nombre, descripcion));
    }

    /** Guarda y sincroniza: sin el flush el INSERT no ha llegado al motor. */
    private SpaType guardarYSincronizar(String nombre, String descripcion) {
        SpaType guardado = guardar(nombre, descripcion);
        entityManager.flush();
        return guardado;
    }

    private void vaciarContexto() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Lee saltando el {@code @SQLRestriction}: es la unica forma de ver una baja.
     */
    private List<Object[]> filaCruda(Long id) {
        @SuppressWarnings("unchecked")
        List<Object[]> filas = entityManager
                .createNativeQuery("SELECT name, enabled, version FROM spa_types WHERE id = :id")
                .setParameter("id", id).getResultList();
        return filas;
    }

    private long contarFilasCrudasCon(String nombre) {
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM spa_types WHERE name = :name")
                .setParameter("name", nombre).getSingleResult()).longValue();
    }

    @Nested
    @DisplayName("Guardado y relectura")
    class Guardado {

        @Test
        @DisplayName("guarda el tipo y lo devuelve con id e ida y vuelta por el mapper")
        void guarda_y_relee() {
            SpaType guardado = guardarYSincronizar(BANO_MEDICADO, "Baño con champú medicado");
            vaciarContexto();

            assertThat(guardado.getId()).isNotNull();
            assertThat(repository.findById(guardado.getId())).get().satisfies(leido -> {
                assertThat(leido.getName()).isEqualTo(BANO_MEDICADO);
                assertThat(leido.getDescription()).isEqualTo("Baño con champú medicado");
                assertThat(leido.getCreatedDate()).isEqualTo(CREADO);
                assertThat(leido.isEnabled()).isTrue();
            });
        }

        @Test
        @DisplayName("la fila nueva nace con version 0: el bloqueo optimista arranca")
        void nace_con_version_cero() {
            SpaType guardado = guardarYSincronizar("Corte de pelo", "Corte y peinado");
            vaciarContexto();

            assertThat(repository.findById(guardado.getId())).get().extracting(SpaType::getVersion)
                    .isEqualTo(0L);
        }

        @Test
        @DisplayName("un id que no existe devuelve vacío en vez de lanzar")
        void id_inexistente_devuelve_vacio() {
            assertThat(repository.findById(8_888_888L)).isEmpty();
        }

        @Test
        @DisplayName("findAll devuelve solo los habilitados: el @SQLRestriction filtra en el SELECT")
        void find_all_solo_devuelve_habilitados() {
            SpaType vivo = guardarYSincronizar("Spa vivo", "Sigue activo");
            SpaType muerto = guardarYSincronizar("Spa dado de baja", "Se da de baja");
            repository.delete(muerto.getId());
            vaciarContexto();

            assertThat(repository.findAll()).extracting(SpaType::getId).contains(vivo.getId())
                    .doesNotContain(muerto.getId());
        }
    }

    @Nested
    @DisplayName("Unicidad del nombre")
    class Unicidad {

        @Test
        @DisplayName("dos nombres distintos conviven sin problema")
        void dos_nombres_distintos_conviven() {
            guardarYSincronizar("Baño simple", "Baño básico");
            guardarYSincronizar("Baño premium", "Baño con tratamiento");
            vaciarContexto();

            assertThat(repository.findAll()).extracting(SpaType::getName).contains("Baño simple",
                    "Baño premium");
        }

        @Test
        @DisplayName("repetir el nombre de un tipo activo es conflicto: el índice único deduplica")
        void repetir_el_nombre_activo_es_conflicto() {
            guardarYSincronizar("Baño duplicado", "El primero");

            assertThatThrownBy(() -> guardarYSincronizar("Baño duplicado", "El segundo"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("Borrado lógico y clave única")
    class BorradoLogicoYClaveUnica {

        @Test
        @DisplayName("borrar no borra: apaga enabled y la fila sigue en la tabla")
        void borrar_apaga_enabled_y_conserva_la_fila() {
            SpaType guardado = guardarYSincronizar("Masaje", "Masaje relajante");
            repository.delete(guardado.getId());
            vaciarContexto();

            // enabled se compara como numero, no como boolean: la columna es BOOLEAN en
            // Liquibase, que MySQL materializa como TINYINT pelado, y una consulta
            // NATIVA sin mapeo de tipo lo devuelve como Number — no como Boolean.
            assertThat(filaCruda(guardado.getId())).singleElement()
                    .satisfies(fila -> assertThat(((Number) fila[1]).intValue()).isZero());
        }

        @Test
        @DisplayName("tras la baja el tipo desaparece de findById: el @SQLRestriction lo oculta")
        void tras_la_baja_desaparece_de_las_lecturas() {
            SpaType guardado = guardarYSincronizar("Masaje oculto", "Se va a dar de baja");
            repository.delete(guardado.getId());
            vaciarContexto();

            assertThat(repository.findById(guardado.getId())).isEmpty();
        }

        /**
         * <b>Este caso afirmaba lo contrario hasta el changeset 288 y se ha invertido a
         * conciencia.</b>
         *
         * <p>
         * Era el hallazgo que justificaba la rodaja entera y lo que denunciaba #482:
         * «dar de baja un tipo de spa quema su nombre para siempre». La fila dada de
         * baja era invisible para {@code findById} y para {@code findAll} —el
         * {@code @SQLRestriction} la oculta— pero el {@code UNIQUE (name)} original de
         * {@code 043_create_spa_types} no sabia nada de {@code enabled} y la seguia
         * contando. El administrador daba de baja «Baño terapéutico», intentaba
         * recrearlo y recibia un error que ni siquiera decia que el nombre estuviera
         * ocupado, porque para el sistema ese tipo ya no existia.
         *
         * <p>
         * {@code 288_add_company_scope_to_global_catalogs} cambio el mecanismo: hace
         * {@code DROP INDEX `name`} y lo sustituye por
         * {@code UNIQUE (owner_scope, active_name)}, donde {@code active_name} es una
         * columna generada {@code STORED} que vale {@code NULL} cuando
         * {@code enabled = FALSE}. MySQL no deduplica {@code NULL}, asi que <b>una fila
         * dada de baja ya no ocupa el nombre</b>: la baja logica lo LIBERA, que es el
         * comportamiento correcto y el que la rama de reactivacion de #559 da por
         * bueno.
         *
         * <p>
         * Lo que sigue siendo cierto —y lo fija
         * {@link #la_fila_oculta_sigue_contando_en_la_tabla()}— es que la fila no
         * desaparece: sigue en la tabla. Por eso el alta la busca con
         * {@code findByNameIncludingDisabled} y la reactiva en vez de insertar una
         * segunda, aunque la base ya le dejaria insertarla.
         */
        @Test
        @DisplayName("una fila dada de baja YA NO ocupa el nombre: la baja lógica lo libera (#482)")
        void la_fila_dada_de_baja_ya_no_ocupa_el_nombre() {
            SpaType guardado = guardarYSincronizar("Baño terapéutico", "Original");
            repository.delete(guardado.getId());
            vaciarContexto();

            SpaType recreado = guardarYSincronizar("Baño terapéutico", "Intento de recrear");
            vaciarContexto();

            assertThat(recreado.getId()).isNotNull().isNotEqualTo(guardado.getId());
            assertThat(repository.findById(recreado.getId())).get().extracting(SpaType::getName)
                    .isEqualTo("Baño terapéutico");
        }

        @Test
        @DisplayName("y la fila oculta sigue contando en la tabla: es una sola, no cero")
        void la_fila_oculta_sigue_contando_en_la_tabla() {
            SpaType guardado = guardarYSincronizar("Baño censado", "Original");
            repository.delete(guardado.getId());
            vaciarContexto();

            assertThat(repository.findById(guardado.getId())).isEmpty();
            assertThat(contarFilasCrudasCon("Baño censado")).isEqualTo(1L);
        }

        /**
         * <b>En el borrado logico la version es el candado, no la carga.</b> El
         * {@code @SQLDelete} de {@link SpaTypeJpaEntity} es
         * {@code UPDATE spa_types SET enabled = false WHERE id = ? AND version = ?}:
         * desde que la entidad se versiono Hibernate liga <b>dos</b> parametros
         * —primero el {@code id}, despues la {@code version}— y el {@code SET} toca
         * unicamente {@code enabled}.
         *
         * <p>
         * Por eso este caso afirma que la version <b>NO se mueve</b>, que es lo
         * contrario de lo que hace {@code reactivate} (ver
         * {@link Reactivacion#reactivar_mueve_la_version()}). Esa diferencia es toda la
         * intencion de las dos reglas: aqui el SQL lo emite Hibernate para una fila que
         * alguien acaba de leer, y la version en el {@code WHERE} es lo que impide
         * borrar sobre una lectura vieja; alli es un {@code UPDATE} de conjunto que
         * nadie leyo antes, y la version va en el {@code SET} porque ponerla en el
         * {@code WHERE} solo conseguiria afectar cero filas (#53).
         *
         * <p>
         * <b>Este caso nace de un error propio, y por eso lleva el javadoc.</b> La
         * primera version afirmaba que el borrado <em>subia</em> la version, por
         * analogia con {@code reactivate}. Es falso, y contra base real se habria caido
         * en la primera ejecucion: ningun {@code SET} la escribe.
         */
        @Test
        @DisplayName("el borrado lógico NO mueve la versión: en el @SQLDelete la versión es la"
                + " guarda del WHERE, no algo que el SET escriba")
        void el_borrado_logico_no_mueve_la_version() {
            SpaType guardado = guardarYSincronizar("Baño versionado", "Original");
            long versionAntes = ((Number) filaCruda(guardado.getId()).getFirst()[2]).longValue();

            repository.delete(guardado.getId());
            vaciarContexto();

            assertThat(filaCruda(guardado.getId())).singleElement().satisfies(fila -> {
                assertThat(((Number) fila[1]).intValue()).isZero();
                assertThat(((Number) fila[2]).longValue()).isEqualTo(versionAntes);
            });
        }
    }

    @Nested
    @DisplayName("Reactivación por UPDATE nativo")
    class Reactivacion {

        @Test
        @DisplayName("reactivar devuelve la fila afectada y la vuelve visible")
        void reactivar_vuelve_a_hacer_visible_la_fila() {
            SpaType guardado = guardarYSincronizar("Baño resucitado", "Original");
            repository.delete(guardado.getId());
            vaciarContexto();

            assertThat(repository.reactivate(guardado.getId())).isEqualTo(1);
            vaciarContexto();
            assertThat(repository.findById(guardado.getId())).get().extracting(SpaType::isEnabled)
                    .isEqualTo(true);
        }

        @Test
        @DisplayName("reactivar mueve la versión: el UPDATE nativo no pasa por @Version")
        void reactivar_mueve_la_version() {
            SpaType guardado = guardarYSincronizar("Baño con versión", "Original");
            repository.delete(guardado.getId());
            vaciarContexto();
            long versionTrasLaBaja = ((Number) filaCruda(guardado.getId()).getFirst()[2])
                    .longValue();

            repository.reactivate(guardado.getId());
            vaciarContexto();

            assertThat(((Number) filaCruda(guardado.getId()).getFirst()[2]).longValue())
                    .isEqualTo(versionTrasLaBaja + 1);
        }

        @Test
        @DisplayName("reactivar un id inexistente no afecta ninguna fila")
        void reactivar_un_id_inexistente_no_afecta_nada() {
            assertThat(repository.reactivate(7_777_777L)).isZero();
        }

        /**
         * <b>Divergencia con el resto del repositorio, deliberadamente afirmada.</b>
         * {@code employees.reactivate}, {@code permissions.reactivate} y las demas
         * reactivaciones de la campana llevan {@code AND enabled = false} en el
         * {@code WHERE}; esta no. La consecuencia es que reactivar un tipo que ya
         * estaba activo devuelve 1 —el servicio lo lee como exito— y sube la
         * {@code version} sin que nada haya cambiado, tumbando con un 409 la edicion de
         * quien tuviera la ficha cargada. Este test fija el comportamiento REAL de hoy,
         * no el deseable: si alguien anade el {@code AND}, este caso falla y le obliga
         * a decidir a conciencia. Queda registrado como issue.
         */
        @Test
        @DisplayName("reactivar un tipo YA activo devuelve 1 y sube la versión igualmente")
        void reactivar_un_tipo_ya_activo_sube_la_version_igualmente() {
            SpaType guardado = guardarYSincronizar("Baño ya activo", "Nunca se dio de baja");
            vaciarContexto();
            long versionInicial = ((Number) filaCruda(guardado.getId()).getFirst()[2]).longValue();

            assertThat(repository.reactivate(guardado.getId())).isEqualTo(1);
            vaciarContexto();

            assertThat(((Number) filaCruda(guardado.getId()).getFirst()[2]).longValue())
                    .isEqualTo(versionInicial + 1);
        }
    }

    /**
     * La guarda de nombre que #559 puso delante del alta y de la edicion. Es el
     * unico sitio donde puede probarse: las dos consultas que la sostienen las
     * resuelve el motor, no el codigo Java.
     */
    @Nested
    @DisplayName("Guarda de nombre contra la base")
    class GuardaDeNombre {

        /**
         * El caso que justifica el finder entero, y la prueba de que #482 dejo de
         * doler. La fila dada de baja es invisible para {@code findById} y para
         * {@code findAll} —el {@code @SQLRestriction("enabled = true")} la borra de
         * toda lectura generada—, pero sigue en la tabla.
         * {@code findByNameIncludingDisabled} es NATIVA justamente para saltarse esa
         * restriccion y poder verla: sin ella el alta insertaria una segunda fila y
         * dejaria dos tipos con el mismo nombre, uno visible y otro no.
         */
        @Test
        @DisplayName("findByNameIncludingDisabled VE una fila dada de baja que findById ya no ve")
        void find_by_name_including_disabled_ve_una_fila_dada_de_baja() {
            SpaType guardado = guardarYSincronizar("Baño enterrado", "Se va a dar de baja");
            repository.delete(guardado.getId());
            vaciarContexto();

            assertThat(repository.findById(guardado.getId())).isEmpty();
            assertThat(repository.findByNameIncludingDisabled("Baño enterrado")).get()
                    .satisfies(leido -> {
                        assertThat(leido.getId()).isEqualTo(guardado.getId());
                        assertThat(leido.isEnabled()).isFalse();
                    });
        }

        @Test
        @DisplayName("findByNameIncludingDisabled tambien ve las activas")
        void find_by_name_including_disabled_ve_las_activas() {
            SpaType guardado = guardarYSincronizar("Baño bien vivo", "Sigue activo");
            vaciarContexto();

            assertThat(repository.findByNameIncludingDisabled("Baño bien vivo")).get()
                    .satisfies(leido -> {
                        assertThat(leido.getId()).isEqualTo(guardado.getId());
                        assertThat(leido.isEnabled()).isTrue();
                    });
        }

        @Test
        @DisplayName("un nombre libre no devuelve nada: el alta sigue su camino de INSERT")
        void un_nombre_libre_no_devuelve_nada() {
            assertThat(repository.findByNameIncludingDisabled("Nombre que nadie usó")).isEmpty();
        }

        /**
         * <b>El fallo mas facil de introducir en esta guarda.</b> La igualdad la
         * resuelve MySQL con la collation de la columna ({@code utf8mb4_0900_ai_ci}):
         * insensible a acentos y a caja, exactamente el mismo criterio que usa el
         * indice unico. Si alguien mueve la comparacion a Java —un {@code equals}, un
         * {@code Map} de nombres, un filtro en memoria— la guarda dara «libre» a un
         * nombre que la base considera ocupado, el INSERT llegara al motor y el usuario
         * recibira de vuelta el 409 crudo de constraint que #559 venia justamente a
         * eliminar. Con nombres como «Baño terapéutico» esto no es un caso de
         * laboratorio: es el dia a dia del catalogo.
         */
        @Test
        @DisplayName("la comparacion de nombre ignora acentos y caja: la decide la collation")
        void la_comparacion_de_nombre_ignora_acentos_y_caja() {
            SpaType guardado = guardarYSincronizar("Baño terapéutico premium",
                    "Con tilde y mayúsculas");
            vaciarContexto();

            assertThat(repository.findByNameIncludingDisabled("bano terapeutico premium")).get()
                    .satisfies(leido -> {
                        assertThat(leido.getId()).isEqualTo(guardado.getId());
                        assertThat(leido.getName()).isEqualTo("Baño terapéutico premium");
                    });
        }

        @Test
        @DisplayName("existsActiveByNameExcludingId no cuenta la propia fila")
        void exists_active_by_name_excluding_id_no_cuenta_la_propia_fila() {
            SpaType guardado = guardarYSincronizar("Baño propio", "El que se edita");
            vaciarContexto();

            assertThat(repository.existsActiveByNameExcludingId("Baño propio", guardado.getId()))
                    .isFalse();
        }

        @Test
        @DisplayName("existsActiveByNameExcludingId si cuenta a OTRA fila activa con ese nombre")
        void exists_active_by_name_excluding_id_cuenta_a_otra_fila_activa() {
            guardarYSincronizar("Baño ocupado", "El que estorba");
            SpaType editado = guardarYSincronizar("Baño que se edita", "El otro");
            vaciarContexto();

            assertThat(repository.existsActiveByNameExcludingId("Baño ocupado", editado.getId()))
                    .isTrue();
        }

        /**
         * La otra mitad de la regla: la guarda de la edicion cuenta solo filas ACTIVAS,
         * igual que el indice unico. Una fila dada de baja no estorba, y por eso la
         * edicion puede reutilizar su nombre.
         */
        @Test
        @DisplayName("existsActiveByNameExcludingId IGNORA una fila dada de baja con ese nombre")
        void exists_active_by_name_excluding_id_ignora_una_fila_dada_de_baja() {
            SpaType enterrado = guardarYSincronizar("Baño liberado", "Se da de baja");
            repository.delete(enterrado.getId());
            SpaType editado = guardarYSincronizar("Baño que se edita 2", "El otro");
            vaciarContexto();

            assertThat(repository.existsActiveByNameExcludingId("Baño liberado", editado.getId()))
                    .isFalse();
        }

        /**
         * <b>El caso de #580, y la razón de ser del {@code ORDER BY … LIMIT 1}.</b>
         *
         * <p>
         * El índice único cubre solo las filas ACTIVAS —{@code active_name} vale
         * {@code NULL} cuando {@code enabled = false} y MySQL no deduplica
         * {@code NULL}—, así que la tabla admite UNA activa y N dadas de baja con el
         * mismo nombre. Este método devuelve {@code Optional}: sin orden ni límite, la
         * segunda baja homónima lo convertía en un
         * {@code IncorrectResultSizeDataAccessException} —un 500— y dejaba ese nombre
         * inutilizable para siempre.
         *
         * <p>
         * Que devuelva la ACTIVA no es una preferencia, es lo único correcto: es la
         * fila cuyo {@code active_name} está en el índice y por tanto la única que de
         * verdad ocupa el nombre. Si devolviera una de las bajas, el alta se iría por
         * la rama de reactivación, {@code reactivateWithDetails} pondría
         * {@code enabled = true} en esa fila, su {@code active_name} pasaría a valer el
         * nombre y chocaría contra la activa que ya lo tiene: vuelta al 409 genérico en
         * inglés que #559 vino a quitar.
         */
        @Test
        @DisplayName("con una activa y varias bajas homónimas devuelve la ACTIVA y no revienta")
        void con_varias_filas_homonimas_devuelve_la_activa() {
            SpaType primeraBaja = guardarYSincronizar("Baño repetido", "El primero");
            repository.delete(primeraBaja.getId());
            vaciarContexto();
            SpaType segundaBaja = guardarYSincronizar("Baño repetido", "El segundo");
            repository.delete(segundaBaja.getId());
            vaciarContexto();
            SpaType activo = guardarYSincronizar("Baño repetido", "El vivo");
            vaciarContexto();

            assertThat(repository.findByNameIncludingDisabled("Baño repetido")).get()
                    .satisfies(leido -> {
                        assertThat(leido.getId()).isEqualTo(activo.getId());
                        assertThat(leido.isEnabled()).isTrue();
                    });
        }

        /**
         * El gemelo del anterior sin ninguna activa. Aquí cualquiera de las dos bajas
         * sería segura de reactivar —las dos tienen {@code active_name = NULL}, ninguna
         * chocaría—, así que el criterio no es de corrección sino de expectativa: se
         * devuelve la de id mayor, la última que se creó, que es la que el usuario cree
         * estar recuperando al volver a dar de alta ese nombre. Sin el {@code id DESC}
         * el motor puede devolver cualquiera de las dos y el alta resucitaría una fila
         * distinta en cada ejecución.
         */
        @Test
        @DisplayName("sin ninguna activa y con dos bajas homónimas devuelve la de id mayor")
        void sin_activa_devuelve_la_baja_mas_reciente() {
            SpaType antiguo = guardarYSincronizar("Baño enterrado dos veces", "El antiguo");
            repository.delete(antiguo.getId());
            vaciarContexto();
            SpaType reciente = guardarYSincronizar("Baño enterrado dos veces", "El reciente");
            repository.delete(reciente.getId());
            vaciarContexto();

            assertThat(reciente.getId()).isGreaterThan(antiguo.getId());
            assertThat(repository.findByNameIncludingDisabled("Baño enterrado dos veces")).get()
                    .satisfies(leido -> {
                        assertThat(leido.getId()).isEqualTo(reciente.getId());
                        assertThat(leido.isEnabled()).isFalse();
                    });
        }
    }

    /**
     * {@code reactivateWithDetails} es la rama de reactivacion del alta: resucita
     * la fila y le aplica el nombre y la descripcion de la peticion en un solo
     * statement. Es {@code UPDATE} nativo, con las mismas dos propiedades que
     * {@link Reactivacion}: salta el {@code @SQLRestriction} —imprescindible,
     * porque la fila que resucita es invisible— y mueve la {@code version} a mano.
     */
    @Nested
    @DisplayName("Reactivación con datos por UPDATE nativo")
    class ReactivacionConDatos {

        @Test
        @DisplayName("vuelve a habilitar la fila y la hace visible de nuevo")
        void vuelve_a_habilitar_la_fila() {
            SpaType guardado = guardarYSincronizar("Baño a revivir", "Original");
            repository.delete(guardado.getId());
            vaciarContexto();

            assertThat(repository.reactivateWithDetails(guardado.getId(), "Baño a revivir",
                    "Descripción nueva")).isEqualTo(1);
            vaciarContexto();

            assertThat(repository.findById(guardado.getId())).get().extracting(SpaType::isEnabled)
                    .isEqualTo(true);
        }

        @Test
        @DisplayName("aplica la descripción nueva y no deja la vieja")
        void aplica_la_descripcion_nueva() {
            SpaType guardado = guardarYSincronizar("Baño redescrito", "Descripción vieja");
            repository.delete(guardado.getId());
            vaciarContexto();

            repository.reactivateWithDetails(guardado.getId(), "Baño redescrito",
                    "Descripción nueva de verdad");
            vaciarContexto();

            assertThat(repository.findById(guardado.getId())).get()
                    .extracting(SpaType::getDescription).isEqualTo("Descripción nueva de verdad");
        }

        @Test
        @DisplayName("también reescribe el nombre: el alta puede corregir la caja o la tilde")
        void tambien_reescribe_el_nombre() {
            SpaType guardado = guardarYSincronizar("bano sin tilde", "Original");
            repository.delete(guardado.getId());
            vaciarContexto();

            repository.reactivateWithDetails(guardado.getId(), "Baño Sin Tilde",
                    "Descripción nueva");
            vaciarContexto();

            assertThat(repository.findById(guardado.getId())).get().extracting(SpaType::getName)
                    .isEqualTo("Baño Sin Tilde");
        }

        /**
         * <b>La subida de version es deliberada y es lo que hay que proteger.</b> Una
         * consulta nativa va directa a la base: ni comprueba ni incrementa el bloqueo
         * optimista, que {@code @Version} solo aplica en el ciclo
         * leer-modificar-guardar. Sin el bump, un {@code save} concurrente cargado
         * antes reescribe la fila entera desde el dominio —con su
         * {@code enabled = false}— y su {@code WHERE version = ?} casa igual: la
         * reactivacion se deshace en silencio y el tipo desaparece otra vez sin que
         * nadie vea un error. Movida la version, ese save no encuentra fila y salta un
         * 409 {@code CONCURRENT_MODIFICATION}, que es un fallo visible y reintentable.
         *
         * <p>
         * Es la misma regla que {@link Reactivacion#reactivar_mueve_la_version()} y la
         * contraria a la del {@code @SQLDelete}, donde la version es la guarda del
         * {@code WHERE} y no algo que el {@code SET} escriba (#53).
         */
        @Test
        @DisplayName("sube la versión: sin ese bump un save concurrente deshace la reactivación")
        void sube_la_version() {
            SpaType guardado = guardarYSincronizar("Baño con versión nueva", "Original");
            repository.delete(guardado.getId());
            vaciarContexto();
            long versionTrasLaBaja = ((Number) filaCruda(guardado.getId()).getFirst()[2])
                    .longValue();

            repository.reactivateWithDetails(guardado.getId(), "Baño con versión nueva",
                    "Descripción nueva");
            vaciarContexto();

            assertThat(((Number) filaCruda(guardado.getId()).getFirst()[2]).longValue())
                    .isEqualTo(versionTrasLaBaja + 1);
        }

        @Test
        @DisplayName("sobre un id inexistente no afecta ninguna fila")
        void sobre_un_id_inexistente_no_afecta_nada() {
            assertThat(repository.reactivateWithDetails(6_666_666L, "Da igual", "Da igual"))
                    .isZero();
        }
    }

    @Nested
    @DisplayName("Lo que el esquema exige y el dominio no")
    class InvariantesDelEsquema {

        /**
         * {@code spa_types.description} es {@code VARCHAR(500) NOT NULL} —changeset
         * {@code 043_create_spa_types}— pero {@code SpaType.validate} solo mide la
         * longitud <em>si no es null</em>, y {@code CreateSpaTypeRequest.description}
         * no lleva {@code @NotBlank}. Es decir: los tres filtros que preceden a la base
         * dejan pasar el null y quien lo para es la columna, en tiempo de ejecucion.
         * Este test es la primera vez que ese camino se recorre. Queda registrado como
         * issue.
         */
        @Test
        @DisplayName("una descripción nula pasa el dominio y la para la columna NOT NULL")
        void la_descripcion_nula_solo_la_para_la_columna() {
            assertThatThrownBy(() -> guardarYSincronizar("Baño sin descripción", null))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
