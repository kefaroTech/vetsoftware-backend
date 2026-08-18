package com.vetsoftware.app.membership.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.membership.domain.Membership;
import com.vetsoftware.app.membership.domain.MembershipStatus;
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
 * Rodaja de persistencia de {@link JpaMembershipRepository} contra MySQL real:
 * el {@code UPDATE memberships SET enabled = true} nativo de
 * {@code reactivate()} y el {@code @SQLRestriction} de soft-delete, que ningun
 * test con dobles puede ver.
 */
@Import({JpaMembershipRepository.class, MembershipJpaMapper.class})
@DisplayName("JpaMembershipRepository — membresias contra MySQL real")
class MembershipPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaMembershipRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    private Membership membresiaNueva(String nombre) {
        return Membership.create(nombre, MembershipStatus.ACTIVE, false);
    }

    @Nested
    @DisplayName("save y findById")
    class Guardado {

        @Test
        @DisplayName("persiste la membresia y devuelve el id asignado")
        void persiste_la_membresia_y_devuelve_el_id() {
            Membership guardada = repository.save(membresiaNueva("Plan Oro"));
            releerDesdeLaBase();

            assertThat(guardada.getId()).isNotNull();
            Membership releida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(releida.getName()).isEqualTo("Plan Oro");
            assertThat(releida.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
            assertThat(releida.isMandatory()).isFalse();
            assertThat(releida.isEnabled()).isTrue();
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
        @DisplayName("trae las membresias habilitadas guardadas")
        void trae_las_membresias_habilitadas_guardadas() {
            repository.save(membresiaNueva("Plan Oro"));
            repository.save(membresiaNueva("Plan Plata"));
            releerDesdeLaBase();

            List<Membership> membresias = repository.findAll();

            assertThat(membresias).extracting(Membership::getName).contains("Plan Oro",
                    "Plan Plata");
        }

        @Test
        @DisplayName("una membresia deshabilitada no aparece en el listado (SQLRestriction)")
        void una_membresia_deshabilitada_no_aparece() {
            Membership guardada = repository.save(membresiaNueva("Plan Oro"));
            releerDesdeLaBase();
            repository.delete(guardada.getId());
            releerDesdeLaBase();

            assertThat(repository.findAll()).extracting(Membership::getId)
                    .doesNotContain(guardada.getId());
        }
    }

    @Nested
    @DisplayName("delete y reactivate")
    class BorradoYReactivacion {

        @Test
        @DisplayName("una membresia borrada desaparece de findById")
        void membresia_borrada_desaparece() {
            Membership guardada = repository.save(membresiaNueva("Plan Oro"));
            releerDesdeLaBase();

            repository.delete(guardada.getId());
            releerDesdeLaBase();

            assertThat(repository.findById(guardada.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate() vuelve a hacer visible una membresia borrada")
        void reactivate_vuelve_a_hacer_visible() {
            Membership guardada = repository.save(membresiaNueva("Plan Oro"));
            releerDesdeLaBase();
            repository.delete(guardada.getId());
            releerDesdeLaBase();

            int filas = repository.reactivate(guardada.getId());
            releerDesdeLaBase();

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardada.getId())).isPresent();
        }

        @Test
        @DisplayName("reactivate() sobre un id inexistente no afecta filas")
        void reactivate_sobre_id_inexistente_no_afecta_filas() {
            assertThat(repository.reactivate(999_999L)).isZero();
        }
    }

    @Nested
    @DisplayName("update")
    class Actualizacion {

        @Test
        @DisplayName("save sobre una membresia existente actualiza sus columnas")
        void save_sobre_membresia_existente_actualiza_sus_columnas() {
            Membership guardada = repository.save(membresiaNueva("Plan Oro"));
            releerDesdeLaBase();
            Membership recargada = repository.findById(guardada.getId()).orElseThrow();

            recargada.update("Plan Platino", MembershipStatus.DEPRECATED, true);
            repository.save(recargada);
            releerDesdeLaBase();

            Membership releida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(releida.getName()).isEqualTo("Plan Platino");
            assertThat(releida.getStatus()).isEqualTo(MembershipStatus.DEPRECATED);
            assertThat(releida.isMandatory()).isTrue();
        }
    }
}
