package com.vetsoftware.app.systemuser.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.systemuser.domain.SystemUser;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de {@link JpaSystemUserRepository} contra MySQL real:
 * el soft-delete via {@code @SQLDelete}/{@code @SQLRestriction} y el
 * {@code reactivate()} nativo — ambos rotan {@code auth_version}, y eso ningun
 * test con dobles puede verlo.
 */
@Import({JpaSystemUserRepository.class, SystemUserJpaMapper.class})
@DisplayName("JpaSystemUserRepository — usuarios de sistema contra MySQL real")
class SystemUserPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaSystemUserRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private SystemUser usuarioNuevo(String code) {
        return SystemUser.create(code, "hash-almacenado-de-prueba");
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
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
}
