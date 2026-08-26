package com.vetsoftware.app.consultationtype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.consultationtype.domain.ConsultationType;
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
 * Rodaja de persistencia de los tipos de consulta contra MySQL real.
 *
 * <p>
 * Lo que se comprueba aqui no existe en el codigo Java: la unicidad de
 * {@code name} la impone un indice de la base, y el soft delete (
 * {@code @SQLDelete} + {@code @SQLRestriction}) lo aplica Hibernate sobre el
 * SQL real. Con un doble del repositorio las dos cosas darian verde siempre.
 *
 * <p>
 * <b>Los tres metodos que #559 anadio al puerto viven enteros aqui.</b> Ningun
 * unitario puede probarlos porque los tres dependen del motor y no del codigo:
 *
 * <ul>
 * <li>{@code findByNameIncludingDisabled} es una consulta NATIVA cuyo unico
 * proposito es <em>saltarse</em> el {@code @SQLRestriction("enabled = true")}.
 * Un doble devuelve lo que se le diga y no demuestra nada; contra base real
 * demuestra que la fila dada de baja se ve. Ver {@link GuardaDeNombre}.
 * <li>La igualdad de nombres la decide la <b>collation</b> de la columna
 * ({@code utf8mb4_0900_ai_ci}), insensible a acentos y a caja. Comparar en Java
 * daria «libre» a un nombre que la base considera ocupado, y ese es el fallo
 * mas facil de introducir en esta guarda.
 * <li>{@code reactivateWithDetails} es un {@code UPDATE} nativo que mueve
 * {@code version} a mano. Ver {@link ReactivacionConDatos}.
 * </ul>
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaConsultationTypeRepository — contra MySQL real")
class ConsultationTypePersistenceIT extends AbstractDataJpaTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Autowired
    private JpaConsultationTypeRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private ConsultationType nuevo(String nombre) {
        return ConsultationType.create(nombre, "Descripcion de prueba para persistencia");
    }

    /**
     * Construye por el constructor publico y no por
     * {@code ConsultationType.create(...)}: ese llama a {@code LocalDateTime.now()}
     * y dejaria el test dependiendo del reloj de la maquina. Deuda ya registrada
     * del repo, no excusa para propagarla.
     */
    private ConsultationType nuevo(String nombre, String descripcion) {
        return new ConsultationType(null, nombre, descripcion, CREADO, null, true);
    }

    /** Guarda y sincroniza: sin el flush el INSERT no ha llegado al motor. */
    private ConsultationType guardarYSincronizar(String nombre, String descripcion) {
        ConsultationType guardado = repository.save(nuevo(nombre, descripcion));
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
        List<Object[]> filas = entityManager.createNativeQuery(
                "SELECT name, description, enabled, version FROM consultation_types"
                        + " WHERE id = :id")
                .setParameter("id", id).getResultList();
        return filas;
    }

    private long versionCruda(Long id) {
        return ((Number) filaCruda(id).getFirst()[3]).longValue();
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id y releer conserva los campos")
        void guardar_asigna_id_y_releer_conserva_los_campos() {
            ConsultationType guardado = repository.save(nuevo("Vacunacion IT"));

            assertThat(guardado.getId()).isNotNull();
            ConsultationType leido = repository.findById(guardado.getId()).orElseThrow();
            assertThat(leido.getName()).isEqualTo("Vacunacion IT");
            assertThat(leido.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un id inexistente no devuelve nada")
        void un_id_inexistente_no_devuelve_nada() {
            assertThat(repository.findById(-1L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("unicidad de name")
    class Unicidad {

        @Test
        @DisplayName("un nombre duplicado lo rechaza la base")
        void un_nombre_duplicado_lo_rechaza_la_base() {
            repository.save(nuevo("Consulta duplicada IT"));

            assertThatThrownBy(() -> repository.save(nuevo("Consulta duplicada IT")))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("findAll devuelve los tipos guardados")
        void findAll_devuelve_los_tipos_guardados() {
            repository.save(nuevo("Tipo A IT"));
            repository.save(nuevo("Tipo B IT"));

            assertThat(repository.findAll()).extracting(ConsultationType::getName)
                    .contains("Tipo A IT", "Tipo B IT");
        }
    }

    @Nested
    @DisplayName("borrado y reactivacion")
    class BorradoYReactivacion {

        @Test
        @DisplayName("delete deshabilita la fila y deja de verse por el soft delete")
        void delete_deshabilita_la_fila() {
            ConsultationType guardado = repository.save(nuevo("A borrar IT"));

            repository.delete(guardado.getId());

            assertThat(repository.findById(guardado.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate habilita de nuevo y devuelve las filas afectadas")
        void reactivate_habilita_de_nuevo() {
            ConsultationType guardado = repository.save(nuevo("A reactivar IT"));
            repository.delete(guardado.getId());

            int filasAfectadas = repository.reactivate(guardado.getId());

            assertThat(filasAfectadas).isEqualTo(1);
            assertThat(repository.findById(guardado.getId())).isPresent();
        }

        @Test
        @DisplayName("reactivate sobre un id inexistente no afecta ninguna fila")
        void reactivate_sobre_id_inexistente_no_afecta_filas() {
            assertThat(repository.reactivate(-1L)).isZero();
        }
    }

    /**
     * La guarda de nombre que #559 puso delante del alta y de la edicion. Es el
     * unico sitio donde puede probarse: las dos consultas que la sostienen las
     * resuelve el motor.
     */
    @Nested
    @DisplayName("Guarda de nombre contra la base")
    class GuardaDeNombre {

        /**
         * El caso que justifica el finder entero. La fila dada de baja es invisible
         * para {@code findById} y para {@code findAll} —el
         * {@code @SQLRestriction("enabled = true")} la borra de toda lectura generada—,
         * pero sigue en la tabla ocupando su sitio. Sin esta consulta nativa el alta no
         * puede verla, y el usuario recibe un choque contra algo que no existe para el.
         */
        @Test
        @DisplayName("findByNameIncludingDisabled VE una fila dada de baja que findById ya no ve")
        void find_by_name_including_disabled_ve_una_fila_dada_de_baja() {
            ConsultationType guardado = guardarYSincronizar("Consulta enterrada IT",
                    "Se va a dar de baja");
            repository.delete(guardado.getId());
            vaciarContexto();

            assertThat(repository.findById(guardado.getId())).isEmpty();
            assertThat(repository.findByNameIncludingDisabled("Consulta enterrada IT")).get()
                    .satisfies(leido -> {
                        assertThat(leido.getId()).isEqualTo(guardado.getId());
                        assertThat(leido.isEnabled()).isFalse();
                    });
        }

        @Test
        @DisplayName("findByNameIncludingDisabled tambien ve las activas")
        void find_by_name_including_disabled_ve_las_activas() {
            ConsultationType guardado = guardarYSincronizar("Consulta viva IT", "Sigue activa");
            vaciarContexto();

            assertThat(repository.findByNameIncludingDisabled("Consulta viva IT")).get()
                    .satisfies(leido -> {
                        assertThat(leido.getId()).isEqualTo(guardado.getId());
                        assertThat(leido.isEnabled()).isTrue();
                    });
        }

        @Test
        @DisplayName("un nombre libre no devuelve nada: el alta sigue su camino de INSERT")
        void un_nombre_libre_no_devuelve_nada() {
            assertThat(repository.findByNameIncludingDisabled("Nombre que nadie uso IT")).isEmpty();
        }

        /**
         * <b>El fallo mas facil de introducir en esta guarda.</b> La igualdad la
         * resuelve MySQL con la collation de la columna ({@code utf8mb4_0900_ai_ci}):
         * insensible a acentos y a caja, exactamente el mismo criterio que usa el
         * indice unico. Si alguien mueve la comparacion a Java —un {@code equals}, un
         * {@code Map} de nombres, un filtro en memoria— la guarda dara «libre» a un
         * nombre que la base considera ocupado, el INSERT llegara al motor y el usuario
         * recibira de vuelta el 409 crudo de constraint que #559 venia justamente a
         * eliminar.
         */
        @Test
        @DisplayName("la comparacion de nombre ignora acentos y caja: la decide la collation")
        void la_comparacion_de_nombre_ignora_acentos_y_caja() {
            ConsultationType guardado = guardarYSincronizar("Consulta general con revisión IT",
                    "Con tilde y mayusculas");
            vaciarContexto();

            assertThat(repository.findByNameIncludingDisabled("consulta general con revision it"))
                    .get().satisfies(leido -> {
                        assertThat(leido.getId()).isEqualTo(guardado.getId());
                        assertThat(leido.getName()).isEqualTo("Consulta general con revisión IT");
                    });
        }

        @Test
        @DisplayName("existsActiveByNameExcludingId no cuenta la propia fila")
        void exists_active_by_name_excluding_id_no_cuenta_la_propia_fila() {
            ConsultationType guardado = guardarYSincronizar("Consulta propia IT",
                    "La que se edita");
            vaciarContexto();

            assertThat(repository.existsActiveByNameExcludingId("Consulta propia IT",
                    guardado.getId())).isFalse();
        }

        @Test
        @DisplayName("existsActiveByNameExcludingId si cuenta a OTRA fila activa con ese nombre")
        void exists_active_by_name_excluding_id_cuenta_a_otra_fila_activa() {
            guardarYSincronizar("Consulta ocupada IT", "La que estorba");
            ConsultationType editada = guardarYSincronizar("Consulta que se edita IT", "La otra");
            vaciarContexto();

            assertThat(repository.existsActiveByNameExcludingId("Consulta ocupada IT",
                    editada.getId())).isTrue();
        }

        /**
         * La otra mitad de la regla: la guarda de la edicion cuenta solo filas ACTIVAS,
         * igual que el indice unico. Una fila dada de baja no estorba, y por eso la
         * edicion puede reutilizar su nombre.
         */
        @Test
        @DisplayName("existsActiveByNameExcludingId IGNORA una fila dada de baja con ese nombre")
        void exists_active_by_name_excluding_id_ignora_una_fila_dada_de_baja() {
            ConsultationType enterrada = guardarYSincronizar("Consulta liberada IT",
                    "Se da de baja");
            repository.delete(enterrada.getId());
            ConsultationType editada = guardarYSincronizar("Consulta que se edita IT 2", "La otra");
            vaciarContexto();

            assertThat(repository.existsActiveByNameExcludingId("Consulta liberada IT",
                    editada.getId())).isFalse();
        }

        /**
         * <b>El caso de #580, y la razon de ser del {@code ORDER BY … LIMIT 1}.</b>
         *
         * <p>
         * El indice unico cubre solo las filas ACTIVAS —{@code active_name} vale
         * {@code NULL} cuando {@code enabled = false} y MySQL no deduplica
         * {@code NULL}—, asi que la tabla admite UNA activa y N dadas de baja con el
         * mismo nombre. Este metodo devuelve {@code Optional}: sin orden ni limite, la
         * segunda baja homonima lo convertia en un
         * {@code IncorrectResultSizeDataAccessException} —un 500— y dejaba ese nombre
         * inutilizable para siempre.
         *
         * <p>
         * Que devuelva la ACTIVA no es una preferencia, es lo unico correcto: es la
         * fila cuyo {@code active_name} esta en el indice y por tanto la unica que de
         * verdad ocupa el nombre. Si devolviera una de las bajas, el alta se iria por
         * la rama de reactivacion, {@code reactivateWithDetails} pondria
         * {@code enabled = true} en esa fila, su {@code active_name} pasaria a valer el
         * nombre y chocaria contra la activa que ya lo tiene: vuelta al 409 generico en
         * ingles que #559 vino a quitar.
         */
        @Test
        @DisplayName("con una activa y varias bajas homonimas devuelve la ACTIVA y no revienta")
        void con_varias_filas_homonimas_devuelve_la_activa() {
            ConsultationType primeraBaja = guardarYSincronizar("Consulta repetida IT",
                    "La primera");
            repository.delete(primeraBaja.getId());
            vaciarContexto();
            ConsultationType segundaBaja = guardarYSincronizar("Consulta repetida IT",
                    "La segunda");
            repository.delete(segundaBaja.getId());
            vaciarContexto();
            ConsultationType activa = guardarYSincronizar("Consulta repetida IT", "La viva");
            vaciarContexto();

            assertThat(repository.findByNameIncludingDisabled("Consulta repetida IT")).get()
                    .satisfies(leida -> {
                        assertThat(leida.getId()).isEqualTo(activa.getId());
                        assertThat(leida.isEnabled()).isTrue();
                    });
        }

        /**
         * El gemelo del anterior sin ninguna activa. Aqui cualquiera de las dos bajas
         * seria segura de reactivar —las dos tienen {@code active_name = NULL}, ninguna
         * chocaria—, asi que el criterio no es de correccion sino de expectativa: se
         * devuelve la de id mayor, la ultima que se creo, que es la que el usuario cree
         * estar recuperando al volver a dar de alta ese nombre. Sin el {@code id DESC}
         * el motor puede devolver cualquiera de las dos y el alta resucitaria una fila
         * distinta en cada ejecucion.
         */
        @Test
        @DisplayName("sin ninguna activa y con dos bajas homonimas devuelve la de id mayor")
        void sin_activa_devuelve_la_baja_mas_reciente() {
            ConsultationType antigua = guardarYSincronizar("Consulta enterrada dos veces IT",
                    "La antigua");
            repository.delete(antigua.getId());
            vaciarContexto();
            ConsultationType reciente = guardarYSincronizar("Consulta enterrada dos veces IT",
                    "La reciente");
            repository.delete(reciente.getId());
            vaciarContexto();

            assertThat(reciente.getId()).isGreaterThan(antigua.getId());
            assertThat(repository.findByNameIncludingDisabled("Consulta enterrada dos veces IT"))
                    .get().satisfies(leida -> {
                        assertThat(leida.getId()).isEqualTo(reciente.getId());
                        assertThat(leida.isEnabled()).isFalse();
                    });
        }
    }

    /**
     * {@code reactivateWithDetails} es la rama de reactivacion del alta: resucita
     * la fila y le aplica el nombre y la descripcion de la peticion en un solo
     * statement.
     */
    @Nested
    @DisplayName("Reactivacion con datos por UPDATE nativo")
    class ReactivacionConDatos {

        @Test
        @DisplayName("vuelve a habilitar la fila y la hace visible de nuevo")
        void vuelve_a_habilitar_la_fila() {
            ConsultationType guardado = guardarYSincronizar("Consulta a revivir IT", "Original");
            repository.delete(guardado.getId());
            vaciarContexto();

            assertThat(repository.reactivateWithDetails(guardado.getId(), "Consulta a revivir IT",
                    "Descripcion nueva")).isEqualTo(1);
            vaciarContexto();

            assertThat(repository.findById(guardado.getId())).get()
                    .extracting(ConsultationType::isEnabled).isEqualTo(true);
        }

        @Test
        @DisplayName("aplica la descripcion nueva y no deja la vieja")
        void aplica_la_descripcion_nueva() {
            ConsultationType guardado = guardarYSincronizar("Consulta redescrita IT",
                    "Descripcion vieja");
            repository.delete(guardado.getId());
            vaciarContexto();

            repository.reactivateWithDetails(guardado.getId(), "Consulta redescrita IT",
                    "Descripcion nueva de verdad");
            vaciarContexto();

            assertThat(repository.findById(guardado.getId())).get()
                    .extracting(ConsultationType::getDescription)
                    .isEqualTo("Descripcion nueva de verdad");
        }

        @Test
        @DisplayName("tambien reescribe el nombre: el alta puede corregir la caja o la tilde")
        void tambien_reescribe_el_nombre() {
            ConsultationType guardado = guardarYSincronizar("consulta sin tilde it", "Original");
            repository.delete(guardado.getId());
            vaciarContexto();

            repository.reactivateWithDetails(guardado.getId(), "Consulta Sin Tilde IT",
                    "Descripcion nueva");
            vaciarContexto();

            assertThat(repository.findById(guardado.getId())).get()
                    .extracting(ConsultationType::getName).isEqualTo("Consulta Sin Tilde IT");
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
         * 409 {@code CONCURRENT_MODIFICATION}.
         */
        @Test
        @DisplayName("sube la version: sin ese bump un save concurrente deshace la reactivacion")
        void sube_la_version() {
            ConsultationType guardado = guardarYSincronizar("Consulta versionada IT", "Original");
            repository.delete(guardado.getId());
            vaciarContexto();
            long versionTrasLaBaja = versionCruda(guardado.getId());

            repository.reactivateWithDetails(guardado.getId(), "Consulta versionada IT",
                    "Descripcion nueva");
            vaciarContexto();

            assertThat(versionCruda(guardado.getId())).isEqualTo(versionTrasLaBaja + 1);
        }

        @Test
        @DisplayName("sobre un id inexistente no afecta ninguna fila")
        void sobre_un_id_inexistente_no_afecta_nada() {
            assertThat(repository.reactivateWithDetails(-1L, "Da igual IT", "Da igual")).isZero();
        }
    }
}
