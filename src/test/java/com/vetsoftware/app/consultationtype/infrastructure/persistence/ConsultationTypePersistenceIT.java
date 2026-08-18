package com.vetsoftware.app.consultationtype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.consultationtype.domain.ConsultationType;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
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
 */
@Import({JpaConsultationTypeRepository.class, ConsultationTypeJpaMapper.class})
@DisplayName("JpaConsultationTypeRepository — contra MySQL real")
class ConsultationTypePersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaConsultationTypeRepository repository;

    private ConsultationType nuevo(String nombre) {
        return ConsultationType.create(nombre, "Descripcion de prueba para persistencia");
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
}
