package com.vetsoftware.app.systemuser.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.systemuser.domain.SystemUser;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * Rodaja de persistencia de {@link JpaSystemUserRepository} contra MySQL real:
 * el soft-delete via {@code @SQLDelete}/{@code @SQLRestriction} y el
 * {@code reactivate()} nativo — ambos rotan {@code auth_version}, y eso ningun
 * test con dobles puede verlo.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSystemUserRepository — usuarios de sistema contra MySQL real")
class SystemUserPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaSystemUserRepository repository;

    @Autowired
    private SystemUserJpaRepository jpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private SystemUser usuarioNuevo(String code) {
        return SystemUser.create(code, "hash-almacenado-de-prueba");
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Lee una columna numerica saltandose el mapper y el contexto de persistencia.
     * Es la unica forma de afirmar que {@code version} y {@code auth_version} —dos
     * {@code BIGINT} indistinguibles en Java— quedaron cada una en su sitio: por el
     * dominio ambas son un {@code Long} y confundirlas no da error de compilacion.
     * El {@code CAST(... AS SIGNED)} normaliza tambien el {@code TINYINT} de
     * {@code enabled}, que el driver no devuelve como el mismo tipo.
     */
    private long columna(String nombre, Long id) {
        return ((Number) entityManager.createNativeQuery(
                "SELECT CAST(" + nombre + " AS SIGNED) FROM system_users" + " WHERE id = :id")
                .setParameter("id", id).getSingleResult()).longValue();
    }

    private long filasConCode(String code) {
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM system_users WHERE code = :code")
                .setParameter("code", code).getSingleResult()).longValue();
    }

    @Nested
    @DisplayName("save y findById")
    class Guardado {

        @Test
        @DisplayName("persiste el usuario y devuelve el id asignado")
        void persiste_el_usuario_y_devuelve_el_id() {
            SystemUser guardado = repository.save(usuarioNuevo("svc-integracion-1"));
            releerDesdeLaBase();

            assertThat(guardado.getId()).isNotNull();
            SystemUser releido = repository.findById(guardado.getId()).orElseThrow();
            assertThat(releido.getCode()).isEqualTo("svc-integracion-1");
            assertThat(releido.getHashPassword()).isEqualTo("hash-almacenado-de-prueba");
            assertThat(releido.isEnabled()).isTrue();
            assertThat(releido.getAuthVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("un id inexistente devuelve vacio")
        void un_id_inexistente_devuelve_vacio() {
            assertThat(repository.findById(999_999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class Listado {

        @Test
        @DisplayName("trae todos los usuarios habilitados")
        void trae_todos_los_usuarios_habilitados() {
            repository.save(usuarioNuevo("svc-integracion-2"));
            repository.save(usuarioNuevo("svc-integracion-3"));
            releerDesdeLaBase();

            List<SystemUser> todos = repository.findAll();

            assertThat(todos).extracting(SystemUser::getCode).contains("svc-integracion-2",
                    "svc-integracion-3");
        }
    }

    @Nested
    @DisplayName("delete y reactivate")
    class BorradoYReactivacion {

        @Test
        @DisplayName("un usuario borrado desaparece de findById — SQLDelete + SQLRestriction")
        void usuario_borrado_desaparece() {
            SystemUser guardado = repository.save(usuarioNuevo("svc-integracion-4"));
            releerDesdeLaBase();

            repository.delete(guardado.getId());
            releerDesdeLaBase();

            assertThat(repository.findById(guardado.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate() vuelve a hacer visible un usuario borrado y rota auth_version dos veces")
        void reactivate_vuelve_a_hacer_visible() {
            SystemUser guardado = repository.save(usuarioNuevo("svc-integracion-5"));
            releerDesdeLaBase();
            repository.delete(guardado.getId());
            releerDesdeLaBase();

            int filas = repository.reactivate(guardado.getId());
            releerDesdeLaBase();

            assertThat(filas).isEqualTo(1);
            SystemUser releido = repository.findById(guardado.getId()).orElseThrow();
            assertThat(releido.isEnabled()).isTrue();
            // SQLDelete rota auth_version al borrar y reactivate() la rota otra vez: dos
            // rotaciones invalidan cualquier sesion emitida antes del ciclo completo.
            assertThat(releido.getAuthVersion()).isEqualTo(2L);
        }

        @Test
        @DisplayName("reactivate() sobre un id inexistente no afecta filas")
        void reactivate_sobre_id_inexistente_no_afecta_filas() {
            assertThat(repository.reactivate(999_999L)).isZero();
        }
    }

    /**
     * BE-26. {@code system_users} es, junto con {@code employees}, la unica tabla
     * con dos columnas de version del mismo tipo: la {@code version} de bloqueo
     * optimista que gestiona Hibernate y la {@code auth_version} preexistente que
     * invalida sesiones. Aqui se fija cual mueve cada camino.
     */
    @Nested
    @DisplayName("bloqueo optimista y auth_version")
    class BloqueoOptimista {

        @Test
        @DisplayName("dos copias de la misma fila: la segunda en guardar choca por version obsoleta")
        void la_segunda_copia_choca_por_version_obsoleta() {
            SystemUser guardado = repository.save(usuarioNuevo("svc-opt-1"));
            releerDesdeLaBase();

            SystemUser primeraCopia = repository.findById(guardado.getId()).orElseThrow();
            SystemUser segundaCopia = repository.findById(guardado.getId()).orElseThrow();

            primeraCopia.update("svc-opt-1-uno");
            repository.save(primeraCopia);
            releerDesdeLaBase();

            segundaCopia.update("svc-opt-1-dos");

            assertThatThrownBy(() -> repository.save(segundaCopia))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                    .hasMessageContaining("SystemUserJpaEntity");
        }

        @Test
        @DisplayName("una edicion normal sube version y deja auth_version intacta")
        void una_edicion_normal_sube_version_y_deja_auth_version_intacta() {
            SystemUser guardado = repository.save(usuarioNuevo("svc-opt-2"));
            releerDesdeLaBase();

            SystemUser leido = repository.findById(guardado.getId()).orElseThrow();
            leido.update("svc-opt-2-editado");
            repository.save(leido);
            releerDesdeLaBase();

            assertThat(columna("version", guardado.getId())).isEqualTo(1L);
            assertThat(columna("auth_version", guardado.getId()))
                    .as("editar el code no revoca la sesion de nadie").isZero();
        }

        @Test
        @DisplayName("guardar un usuario existente actualiza la fila y no inserta otra")
        void guardar_un_usuario_existente_actualiza_y_no_inserta_otra() {
            SystemUser guardado = repository.save(usuarioNuevo("svc-opt-3"));
            releerDesdeLaBase();

            SystemUser leido = repository.findById(guardado.getId()).orElseThrow();
            leido.update("svc-opt-3");
            repository.save(leido);
            releerDesdeLaBase();

            assertThat(filasConCode("svc-opt-3")).isEqualTo(1L);
            assertThat(repository.findById(guardado.getId())).map(SystemUser::getId)
                    .contains(guardado.getId());
        }

        @Test
        @DisplayName("el borrado por @SQLDelete sube auth_version pero NO version, al reves que reactivate")
        void el_borrado_por_sql_delete_sube_auth_version_pero_no_version() {
            SystemUser guardado = repository.save(usuarioNuevo("svc-opt-4"));
            releerDesdeLaBase();

            // Aqui `delete` SI es el @SQLDelete de la entidad: el puerto llama a
            // `deleteById` y no hay UPDATE nativo de baja para esta tabla (a diferencia
            // de `employees`, donde la baja va por `deactivate` y si mueve `version`).
            //
            // Su SQL es "SET enabled = false, auth_version = auth_version + 1
            // WHERE id = ? AND version = ?": dos parametros de tipos indistinguibles. Si
            // Hibernate ligara el id donde va la version el UPDATE afectaria 0 filas y
            // explotaria por StaleState; que enabled y auth_version hayan cambiado prueba
            // que los ligo en el orden correcto contra MySQL real.
            repository.delete(guardado.getId());
            releerDesdeLaBase();

            assertThat(columna("enabled", guardado.getId())).isZero();
            assertThat(columna("auth_version", guardado.getId())).isEqualTo(1L);
            assertThat(columna("version", guardado.getId()))
                    .as("un DELETE no incrementa la version de bloqueo, solo la lee").isZero();
        }

        @Test
        @DisplayName("la reactivacion sube auth_version y tambien version, al reves que el @SQLDelete")
        void la_reactivacion_sube_auth_version_y_tambien_version() {
            SystemUser guardado = repository.save(usuarioNuevo("svc-opt-7"));
            releerDesdeLaBase();
            repository.delete(guardado.getId());
            releerDesdeLaBase();

            repository.reactivate(guardado.getId());
            releerDesdeLaBase();

            // Los dos pasos dejan la fila deshabilitada y luego habilitada, y los dos
            // rotan auth_version — pero solo el segundo mueve `version`. Es justo la
            // asimetria donde alguien se confundira: el borrado va por el @SQLDelete de
            // la entidad (un DELETE, que solo lee la version) y la reactivacion por un
            // UPDATE nativo (que la incrementa como cualquier otra escritura).
            assertThat(columna("enabled", guardado.getId())).isEqualTo(1L);
            assertThat(columna("auth_version", guardado.getId())).isEqualTo(2L);
            assertThat(columna("version", guardado.getId()))
                    .as("solo la reactivacion la movio; el @SQLDelete la dejo igual").isEqualTo(1L);
        }

        @Test
        @DisplayName("el bump de auth_version sube tambien la version de bloqueo")
        void el_bump_de_auth_version_sube_tambien_la_version_de_bloqueo() {
            SystemUser guardado = repository.save(usuarioNuevo("svc-opt-5"));
            releerDesdeLaBase();

            jpaRepository.bumpAuthVersion(guardado.getId());
            releerDesdeLaBase();

            assertThat(columna("auth_version", guardado.getId())).isEqualTo(1L);
            assertThat(columna("version", guardado.getId()))
                    .as("el UPDATE de revocacion mueve las dos columnas, no solo auth_version")
                    .isEqualTo(1L);
        }

        @Test
        @DisplayName("una copia leida antes del bump de auth_version ya no puede pisar la revocacion")
        void una_copia_previa_al_bump_ya_no_puede_pisar_la_revocacion() {
            SystemUser guardado = repository.save(usuarioNuevo("svc-opt-6"));
            releerDesdeLaBase();
            SystemUser copiaPrevia = repository.findById(guardado.getId()).orElseThrow();

            jpaRepository.bumpAuthVersion(guardado.getId());
            releerDesdeLaBase();

            copiaPrevia.update("svc-opt-6-editado");

            // Incidencia #54. Esto es lo que protege el `version = version + 1` del
            // UPDATE de revocacion, y por eso ese incremento no es decorativo: el
            // UPDATE es nativo y no pasa por Hibernate, asi que sin el la columna
            // `version` no se movia y el candado optimista no veia ningun conflicto.
            // Una edicion cargada ANTES del logout casaba su `WHERE version = ?` y
            // reescribia `auth_version` con el valor viejo que llevaba en el dominio
            // —el mapper la copia campo a campo—, revalidando en silencio un token ya
            // revocado. Movida la version, esa edicion pierde la carrera aqui.
            assertThatThrownBy(() -> repository.save(copiaPrevia))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                    .hasMessageContaining("SystemUserJpaEntity");

            entityManager.clear();
            assertThat(columna("auth_version", guardado.getId()))
                    .as("la revocacion sobrevive: la edicion perdedora no llego a escribir")
                    .isEqualTo(1L);
        }
    }
}
