package com.vetsoftware.app.baserole.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.baserole.domain.BaseRole;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({JpaBaseRoleRepository.class, BaseRoleJpaMapper.class})
@DisplayName("JpaBaseRoleRepository — persistencia real contra MySQL")
class BaseRolePersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaBaseRoleRepository repository;

    private BaseRole guardarVeterinario() {
        return repository.save(BaseRole.create("Veterinario", "VET-" + System.nanoTime(), false));
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva cada campo")
        void guardar_y_releer_conserva_cada_campo() {
            BaseRole guardado = guardarVeterinario();

            Optional<BaseRole> leido = repository.findById(guardado.getId());

            assertThat(leido).isPresent();
            assertThat(leido.get().getName()).isEqualTo("Veterinario");
            assertThat(leido.get().getMandatory()).isFalse();
            assertThat(leido.get().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un id inexistente no aparece")
        void un_id_inexistente_no_aparece() {
            assertThat(repository.findById(-1L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("findAll incluye los roles guardados")
        void find_all_incluye_los_roles_guardados() {
            BaseRole guardado = guardarVeterinario();

            List<BaseRole> todos = repository.findAll();

            assertThat(todos).extracting(BaseRole::getId).contains(guardado.getId());
        }
    }

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("delete es un soft-delete: no vuelve a aparecer por findById")
        void delete_es_un_soft_delete() {
            BaseRole guardado = guardarVeterinario();

            repository.delete(guardado.getId());

            assertThat(repository.findById(guardado.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactivate vuelve a habilitar un rol borrado")
        void reactivate_vuelve_a_habilitar_un_rol_borrado() {
            BaseRole guardado = guardarVeterinario();
            repository.delete(guardado.getId());

            int filas = repository.reactivate(guardado.getId());

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardado.getId())).isPresent();
        }

        @Test
        @DisplayName("reactivar un id inexistente no afecta filas")
        void reactivar_un_id_inexistente_no_afecta_filas() {
            assertThat(repository.reactivate(-1L)).isZero();
        }
    }
}
