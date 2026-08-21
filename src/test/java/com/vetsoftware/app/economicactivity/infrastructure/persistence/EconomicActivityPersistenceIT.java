package com.vetsoftware.app.economicactivity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.economicactivity.domain.EconomicActivity;
import com.vetsoftware.app.economicactivity.testsupport.EconomicActivityMother;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia de actividades economicas contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b> El soft delete lo hacen dos
 * anotaciones de Hibernate en {@link EconomicActivityJpaEntity}: el
 * {@code @SQLDelete} convierte el borrado en {@code UPDATE enabled = false} y
 * el {@code @SQLRestriction} esconde la fila de TODAS las consultas de entidad
 * (incluida la que arma {@code existsByCode}), menos de la nativa que usa
 * {@code reactivate}. Ese contraste, y el choque con el indice unico de
 * {@code code} que sobrevive al soft delete, solo se ve pasando por el motor.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaEconomicActivityRepository — soft delete y unicidad de codigo contra MySQL real")
class EconomicActivityPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaEconomicActivityRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private EconomicActivity guardar(String code, String name) {
        return repository.save(EconomicActivityMother.nueva(code, name));
    }

    /** Soft delete + flush: el UPDATE tiene que llegar a la BD antes de releer. */
    private void deshabilitar(Long id) {
        repository.delete(id);
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva cada campo")
        void guardar_y_releer_conserva_cada_campo() {
            EconomicActivity guardada = guardar("0111", "Cultivo de cereales");

            EconomicActivity leida = repository.findById(guardada.getId()).orElseThrow();

            assertThat(leida.getCode()).isEqualTo("0111");
            assertThat(leida.getName()).isEqualTo("Cultivo de cereales");
            assertThat(leida.getCreatedDate()).isEqualTo(EconomicActivityMother.CREADO);
            assertThat(leida.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("unicidad de codigo")
    class UnicidadDeCodigo {

        @Test
        @DisplayName("existsByCode es true tras guardar y false para otro codigo")
        void exists_by_code_distingue_codigos() {
            guardar("0111", "Cultivo de cereales");

            assertThat(repository.existsByCode("0111")).isTrue();
            assertThat(repository.existsByCode("9999")).isFalse();
        }

        @Test
        @DisplayName("un codigo deshabilitado se reporta libre, pero el indice unico lo sigue bloqueando")
        void codigo_deshabilitado_se_reporta_libre_pero_el_indice_lo_bloquea() {
            EconomicActivity guardada = guardar("0111", "Cultivo de cereales");
            deshabilitar(guardada.getId());

            // El @SQLRestriction filtra "enabled = true" en la consulta que arma
            // existsByCode, asi que el codigo parece libre...
            assertThat(repository.existsByCode("0111")).isFalse();

            // ...pero la columna `code` es un unique llano (sin la columna generada tipo
            // active_name de supplier): el indice fisico sigue viendo la fila deshabilitada
            // y el INSERT choca igual. Un CreateEconomicActivityService que confio en
            // existsByCode como unica guarda dejaria pasar este choque hasta la BD.
            // El save y el flush van DENTRO de la misma guarda a proposito: el id es
            // GenerationType.IDENTITY, asi que Hibernate no puede diferir el INSERT —
            // necesita la clave generada— y lo ejecuta ya en persist(). La violacion del
            // unico saltaba por tanto en el save, una linea por encima del
            // assertThatThrownBy, y se escapaba del test como error en vez de cumplirlo.
            // Guardando el bloque entero, la asercion vale con cualquier estrategia de id.
            assertThatThrownBy(() -> {
                repository.save(EconomicActivityMother.nueva("0111", "Otra actividad"));
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("findAll no ve las actividades deshabilitadas")
        void find_all_no_ve_las_deshabilitadas() {
            EconomicActivity activa = guardar("0111", "Cultivo de cereales");
            EconomicActivity pausada = guardar("0112", "Cultivo de hortalizas");
            deshabilitar(pausada.getId());

            assertThat(repository.findAll()).extracting(EconomicActivity::getId)
                    .containsExactly(activa.getId());
        }
    }

    @Nested
    @DisplayName("soft delete y reactivacion")
    class SoftDeleteYReactivacion {

        @Test
        @DisplayName("findById no ve una actividad deshabilitada")
        void find_by_id_no_ve_la_deshabilitada() {
            EconomicActivity guardada = guardar("0111", "Cultivo de cereales");

            deshabilitar(guardada.getId());

            assertThat(repository.findById(guardada.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate devuelve la actividad al listado y cuenta una fila tocada")
        void reactivate_devuelve_la_actividad_al_listado() {
            EconomicActivity guardada = guardar("0111", "Cultivo de cereales");
            deshabilitar(guardada.getId());

            int filas = repository.reactivate(guardada.getId());
            entityManager.clear();

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardada.getId())).map(EconomicActivity::isEnabled)
                    .contains(true);
        }

        @Test
        @DisplayName("reactivate sobre un id inexistente no toca ninguna fila")
        void reactivate_sobre_id_inexistente_no_toca_filas() {
            assertThat(repository.reactivate(999_999L)).isZero();
        }
    }
}
