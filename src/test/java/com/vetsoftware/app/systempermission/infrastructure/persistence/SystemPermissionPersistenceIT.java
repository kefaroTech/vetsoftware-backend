package com.vetsoftware.app.systempermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.systempermission.domain.SystemPermission;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia del catalogo global de permisos de sistema contra
 * MySQL real. {@code SystemPermissionJpaEntity} no tiene FKs: no hace falta
 * {@code SchemaSeed}.
 */
@Import({JpaSystemPermissionRepository.class, SystemPermissionJpaMapper.class})
@DisplayName("JpaSystemPermissionRepository — catalogo de permisos contra MySQL real")
class SystemPermissionPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaSystemPermissionRepository repository;

    private SystemPermission nuevoPermiso(String nombre, String codigo) {
        return repository.save(SystemPermission.create(nombre, codigo));
    }

    @Nested
    @DisplayName("guardar y releer")
    class GuardarYReleer {

        @Test
        @DisplayName("guarda el permiso y lo relee con cada campo intacto")
        void guarda_y_relee_con_cada_campo_intacto() {
            SystemPermission guardado = nuevoPermiso("Administrar usuarios-IT", "admin.users.it");

            Optional<SystemPermission> releido = repository.findById(guardado.getId());

            assertThat(releido).isPresent();
            assertThat(releido.get().getName()).isEqualTo("Administrar usuarios-IT");
            assertThat(releido.get().getCode()).isEqualTo("admin.users.it");
            assertThat(releido.get().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un id inexistente devuelve vacio")
        void un_id_inexistente_devuelve_vacio() {
            assertThat(repository.findById(999_999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("incluye los permisos guardados")
        void incluye_los_permisos_guardados() {
            SystemPermission guardado = nuevoPermiso("Administrar roles-IT", "admin.roles.it");

            List<SystemPermission> todos = repository.findAll();

            assertThat(todos).extracting(SystemPermission::getId).contains(guardado.getId());
        }
    }

    @Nested
    @DisplayName("borrado logico y reactivacion")
    class BorradoYReactivacion {

        @Test
        @DisplayName("delete deshabilita la fila: deja de aparecer por id")
        void delete_deshabilita_la_fila() {
            SystemPermission guardado = nuevoPermiso("Administrar reportes-IT", "admin.reports.it");

            repository.delete(guardado.getId());

            assertThat(repository.findById(guardado.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate vuelve a habilitar la fila borrada")
        void reactivate_vuelve_a_habilitar_la_fila() {
            SystemPermission guardado = nuevoPermiso("Administrar compras-IT",
                    "admin.purchases.it");
            repository.delete(guardado.getId());

            int filas = repository.reactivate(guardado.getId());

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardado.getId())).isPresent();
        }

        @Test
        @DisplayName("reactivate sobre un id inexistente no afecta ninguna fila")
        void reactivate_sobre_id_inexistente_no_afecta_filas() {
            assertThat(repository.reactivate(999_999L)).isZero();
        }
    }
}
