package com.vetsoftware.app.country.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.country.domain.Country;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia del catalogo global de paises contra MySQL real.
 * {@code CountryJpaEntity} no tiene FKs (es la raiz de la jerarquia
 * geografica), asi que no hace falta {@code SchemaSeed}.
 *
 * <p>
 * Lo que un doble no puede comprobar: que el borrado es logico via
 * {@code @SQLDelete}/{@code @SQLRestriction} y que {@code reactivate} es un
 * UPDATE nativo que solo el motor ejecuta de verdad.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCountryRepository — catalogo de paises contra MySQL real")
class CountryPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaCountryRepository repository;

    private Country nuevoPais(String nombre) {
        return repository.save(Country.create(nombre));
    }

    @Nested
    @DisplayName("guardar y releer")
    class GuardarYReleer {

        @Test
        @DisplayName("guarda el pais y lo relee con cada campo intacto")
        void guarda_y_relee_con_cada_campo_intacto() {
            Country guardado = nuevoPais("Colombia-IT");

            Optional<Country> releido = repository.findById(guardado.getId());

            assertThat(releido).isPresent();
            assertThat(releido.get().getName()).isEqualTo("Colombia-IT");
            assertThat(releido.get().isEnabled()).isTrue();
            assertThat(releido.get().getCreatedDate()).isNotNull();
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
        @DisplayName("incluye los paises guardados")
        void incluye_los_paises_guardados() {
            Country guardado = nuevoPais("Ecuador-IT");

            List<Country> todos = repository.findAll();

            assertThat(todos).extracting(Country::getId).contains(guardado.getId());
        }
    }

    @Nested
    @DisplayName("borrado logico y reactivacion")
    class BorradoYReactivacion {

        @Test
        @DisplayName("delete deshabilita la fila: deja de aparecer por id")
        void delete_deshabilita_la_fila() {
            Country guardado = nuevoPais("Peru-IT");

            repository.delete(guardado.getId());

            assertThat(repository.findById(guardado.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate vuelve a habilitar la fila borrada")
        void reactivate_vuelve_a_habilitar_la_fila() {
            Country guardado = nuevoPais("Chile-IT");
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
