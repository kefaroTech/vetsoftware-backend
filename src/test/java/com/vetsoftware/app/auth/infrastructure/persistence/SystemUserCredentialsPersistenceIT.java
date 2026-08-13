package com.vetsoftware.app.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.auth.application.port.out.SystemUserCredentialsRepository.SystemUserCredentials;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia del inicio de sesion de un usuario de sistema.
 *
 * <p>
 * Este adaptador es el UNICO sitio que decide si un SYSTEM_USER puede entrar:
 * el servicio solo compara el hash de lo que le devuelvan. Que una cuenta
 * desactivada no autentique NO se ve leyendo su codigo —consulta por codigo a
 * secas— sino que depende de una sola anotacion,
 * {@code @SQLRestriction("enabled = true")} en {@code SystemUserJpaEntity}.
 *
 * <p>
 * Eso es justo lo que estas pruebas fijan: una invariante de seguridad que hoy
 * sostiene una anotacion que cualquiera podria quitar al refactorizar, sin que
 * nada mas se queje. No se puede probar con un doble del repositorio, porque un
 * mock devolveria lo que se le diga; por eso va contra MySQL real.
 */
@Import(JpaSystemUserCredentialsRepository.class)
@DisplayName("JpaSystemUserCredentialsRepository — una cuenta desactivada no autentica")
class SystemUserCredentialsPersistenceIT extends AbstractDataJpaTest {

    private static final String HASH = "$2a$12$abcdefghijklmnopqrstuv.wxyz0123456789ABCDEFGHIJKLMN";

    @Autowired
    private JpaSystemUserCredentialsRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private void insertarUsuario(String code, boolean enabled) {
        entityManager
                .createNativeQuery("INSERT INTO system_users (code, hash_password, enabled) "
                        + "VALUES (:code, :hash, :enabled)")
                .setParameter("code", code).setParameter("hash", HASH)
                .setParameter("enabled", enabled).executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("devuelve las credenciales de una cuenta habilitada")
    void devuelve_las_credenciales_de_una_cuenta_habilitada() {
        insertarUsuario("op-habilitado", true);

        Optional<SystemUserCredentials> credenciales = repository.findByCode("op-habilitado");

        assertThat(credenciales).isPresent();
        assertThat(credenciales.get().hashPassword()).isEqualTo(HASH);
    }

    @Test
    @DisplayName("NO devuelve nada para una cuenta desactivada, aunque exista y el hash sea valido")
    void no_devuelve_nada_para_una_cuenta_desactivada() {
        // La fila esta ahi y su contrasena es correcta. Lo unico que impide el
        // acceso es la restriccion de la entidad; si desaparece, esta prueba cae.
        insertarUsuario("op-desactivado", false);

        assertThat(repository.findByCode("op-desactivado")).isEmpty();
    }

    @Test
    @DisplayName("no confunde un codigo inexistente con uno desactivado: los dos son vacio")
    void un_codigo_inexistente_devuelve_vacio() {
        assertThat(repository.findByCode("no-existe")).isEmpty();
    }
}
