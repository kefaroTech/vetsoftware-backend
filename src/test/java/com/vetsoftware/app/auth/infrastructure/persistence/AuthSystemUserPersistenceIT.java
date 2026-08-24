package com.vetsoftware.app.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.auth.application.port.out.AuthSystemUserRepository.AuthSystemUser;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia del estado autenticable de un usuario de sistema
 * contra MySQL real.
 *
 * <p>
 * <b>Aqui no hay ni un {@code WHERE enabled} escrito a mano.</b>
 * {@code findActiveById} llama a {@code findById} a secas, asi que lo unico que
 * impide que un superadministrador dado de baja siga entrando es el
 * {@code @SQLRestriction("enabled = true")} de {@code SystemUserJpaEntity} —una
 * anotacion, en otra feature, que cualquiera puede quitar al refactorizar sin
 * que el compilador ni ninguna regla de arquitectura digan nada—. Leyendo el
 * adaptador la invariante es invisible; solo se ve ejecutando el SQL.
 *
 * <p>
 * Lo mismo vale para {@code findByIdForUpdate}: es JPQL con bloqueo pesimista,
 * y que la restriccion se aplique tambien ahi es lo que evita que la rotacion
 * de version le devuelva sesion valida a una cuenta desactivada.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaAuthSystemUserRepository — un superadministrador desactivado deja de autenticar")
class AuthSystemUserPersistenceIT extends AbstractDataJpaTest {

    private static final Long USUARIO = SchemaSeed.SYSTEM_USER_ID;

    @Autowired
    private JpaAuthSystemUserRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    private void vaciarContexto() {
        entityManager.flush();
        entityManager.clear();
    }

    /** Deja la fila en un estado conocido: sin esto, los defaults del schema. */
    private void fijarVersiones(long authVersion, long version) {
        entityManager
                .createNativeQuery("UPDATE system_users SET auth_version = :auth,"
                        + " version = :ver WHERE id = :id")
                .setParameter("auth", authVersion).setParameter("ver", version)
                .setParameter("id", USUARIO).executeUpdate();
        vaciarContexto();
    }

    private void desactivar() {
        entityManager.createNativeQuery("UPDATE system_users SET enabled = false WHERE id = :id")
                .setParameter("id", USUARIO).executeUpdate();
        vaciarContexto();
    }

    private long columna(String columna) {
        return ((Number) entityManager
                .createNativeQuery("SELECT " + columna + " FROM system_users WHERE id = :id")
                .setParameter("id", USUARIO).getSingleResult()).longValue();
    }

    @Nested
    @DisplayName("Lectura del usuario activo — la sostiene el @SQLRestriction")
    class LecturaDelUsuarioActivo {

        @Test
        @DisplayName("devuelve el id y la authVersion de una cuenta habilitada")
        void devuelve_el_id_y_la_auth_version_de_una_cuenta_habilitada() {
            fijarVersiones(4L, 2L);

            assertThat(repository.findActiveById(USUARIO)).get().satisfies(auth -> {
                assertThat(auth.id()).isEqualTo(USUARIO);
                assertThat(auth.authVersion()).isEqualTo(4L);
            });
        }

        @Test
        @DisplayName("una cuenta desactivada desaparece, aunque el adaptador no filtre por enabled")
        void una_cuenta_desactivada_desaparece() {
            // La fila sigue ahi entera y el adaptador consulta por id a secas. Si el
            // @SQLRestriction de SystemUserJpaEntity desaparece, desactivar a un
            // superadministrador deja de cortarle el acceso y hoy nada mas lo detecta.
            fijarVersiones(4L, 2L);
            desactivar();

            assertThat(repository.findActiveById(USUARIO)).isEmpty();
        }

        @Test
        @DisplayName("un id inexistente también devuelve vacío: no se distingue de uno desactivado")
        void un_id_inexistente_devuelve_vacio() {
            assertThat(repository.findActiveById(-1L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Rotación de la versión bajo bloqueo pesimista")
    class RotacionDeVersion {

        @Test
        @DisplayName("sube la authVersion exactamente 1 y devuelve el mismo número que queda escrito")
        void sube_la_auth_version_exactamente_uno_y_devuelve_lo_que_queda_escrito() {
            fijarVersiones(4L, 2L);

            Optional<AuthSystemUser> rotado = repository.rotateAuthVersion(USUARIO);
            vaciarContexto();

            assertThat(rotado).get().extracting(AuthSystemUser::authVersion).isEqualTo(5L);
            assertThat(columna("auth_version")).isEqualTo(5L);
        }

        @Test
        @DisplayName("el @SQLRestriction aplica TAMBIÉN al SELECT ... FOR UPDATE: cuenta desactivada, vacío")
        void el_sql_restriction_aplica_tambien_al_select_for_update() {
            // findByIdForUpdate es JPQL con PESSIMISTIC_WRITE y tampoco escribe un
            // WHERE enabled. Si la restriccion no llegara hasta aqui, una cuenta dada
            // de baja obtendria una authVersion nueva y con ella un JWT valido.
            fijarVersiones(4L, 2L);
            desactivar();

            assertThat(repository.rotateAuthVersion(USUARIO)).isEmpty();
        }

        @Test
        @DisplayName("sobre una cuenta desactivada NO escribe nada")
        void sobre_una_cuenta_desactivada_no_escribe_nada() {
            fijarVersiones(4L, 2L);
            desactivar();

            repository.rotateAuthVersion(USUARIO);
            vaciarContexto();

            assertThat(columna("auth_version")).isEqualTo(4L);
            assertThat(columna("version")).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("Invalidación de sesiones por UPDATE nativo")
    class InvalidacionDeSesiones {

        @Test
        @DisplayName("mueve authVersion Y version: sin la segunda, un save concurrente revive la sesión")
        void mueve_las_dos_columnas() {
            fijarVersiones(4L, 2L);

            repository.bumpAuthVersion(USUARIO);
            vaciarContexto();

            assertThat(columna("auth_version")).isEqualTo(5L);
            assertThat(columna("version")).isEqualTo(3L);
        }

        @Test
        @DisplayName("sobre una cuenta ya desactivada sigue invalidando: el UPDATE es nativo y salta el filtro")
        void sobre_una_cuenta_desactivada_sigue_invalidando() {
            fijarVersiones(4L, 2L);
            desactivar();
            long trasLaBaja = columna("auth_version");

            repository.bumpAuthVersion(USUARIO);
            vaciarContexto();

            assertThat(columna("auth_version")).isEqualTo(trasLaBaja + 1L);
        }
    }
}
