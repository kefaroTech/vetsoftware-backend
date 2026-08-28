package com.vetsoftware.app.uvtvalue.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.uvtvalue.domain.UvtValue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaUvtValueRepository — la UVT por ano contra MySQL real")
class UvtValuePersistenceIT extends AbstractDataJpaTest {

    // 2030 y 2031: el changeset 360 ya siembra 2025 y 2026 y uq_uvt_values_year
    // rechazaria repetirlos.
    private static final int ANO = 2030;
    private static final int OTRO_ANO = 2031;
    private static final BigDecimal VALOR = new BigDecimal("60000.00");
    private static final BigDecimal OTRO_VALOR = new BigDecimal("63000.00");
    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2029, 12, 15, 8, 0, 0);

    @Autowired
    private UvtValueJpaRepository springDataRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private JpaUvtValueRepository repository;

    @BeforeEach
    void adaptador() {
        repository = new JpaUvtValueRepository(springDataRepository, new UvtValueJpaMapper());
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda el ano con su cifra y su resolucion, y los recupera campo a campo")
        void guarda_el_ano_con_su_cifra_y_su_resolucion() {
            UvtValue guardado = repository.save(UvtValue.create(ANO, VALOR,
                    "Resolucion DIAN de fijacion de la UVT 2030", CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get().satisfies(recuperado -> {
                assertThat(recuperado.getFiscalYear()).isEqualTo(ANO);
                assertThat(recuperado.getValueAmount()).isEqualByComparingTo(VALOR);
                assertThat(recuperado.getLegalReference()).contains("Resolucion DIAN");
                assertThat(recuperado.getCreatedDate()).isEqualTo(CREADO_EL);
                assertThat(recuperado.isEnabled()).isTrue();
            });
        }

        @Test
        @DisplayName("el SMALLINT del ano no pierde el valor al pasar por short")
        void el_smallint_del_ano_conserva_el_valor() {
            repository.save(UvtValue.create(OTRO_ANO, OTRO_VALOR, "Resolucion 2031", CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByFiscalYear(OTRO_ANO)).get()
                    .extracting(UvtValue::getFiscalYear).isEqualTo(OTRO_ANO);
        }
    }

    @Nested
    @DisplayName("Resolucion por ano")
    class ResolucionPorAno {

        @Test
        @DisplayName("cada ano devuelve SU cifra, nunca la del otro")
        void cada_ano_devuelve_su_cifra() {
            repository.save(UvtValue.create(ANO, VALOR, "Resolucion 2030", CREADO_EL));
            repository.save(UvtValue.create(OTRO_ANO, OTRO_VALOR, "Resolucion 2031", CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByFiscalYear(ANO)).get().extracting(UvtValue::getValueAmount)
                    .isEqualTo(VALOR);
            assertThat(repository.findByFiscalYear(OTRO_ANO)).get()
                    .extracting(UvtValue::getValueAmount).isEqualTo(OTRO_VALOR);
        }

        @Test
        @DisplayName("un ano sin publicar devuelve vacio: no cae en el ano anterior")
        void un_ano_sin_publicar_devuelve_vacio() {
            repository.save(UvtValue.create(ANO, VALOR, "Resolucion 2030", CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByFiscalYear(2099)).isEmpty();
            assertThat(repository.existsByFiscalYear(2099)).isFalse();
        }
    }

    @Nested
    @DisplayName("Paginacion")
    class Paginacion {

        @Test
        @DisplayName("findAll ordena del ano mas reciente al mas viejo")
        void find_all_ordena_del_mas_reciente_al_mas_viejo() {
            repository.save(UvtValue.create(ANO, VALOR, "Resolucion 2030", CREADO_EL));
            repository.save(UvtValue.create(OTRO_ANO, OTRO_VALOR, "Resolucion 2031", CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            var pagina = repository.findAll(0, 200);

            assertThat(pagina.content()).extracting(UvtValue::getFiscalYear)
                    .isSortedAccordingTo(java.util.Comparator.reverseOrder());
        }
    }
}
