package com.vetsoftware.app.vatfilingperiod.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.vatfilingperiod.domain.VatFilingFrequency;
import com.vetsoftware.app.vatfilingperiod.domain.VatFilingPeriod;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaVatFilingPeriodRepository — la periodicidad de IVA contra MySQL real")
class VatFilingPeriodPersistenceIT extends AbstractDataJpaTest {

    private static final int PRIMER_ANO = 2030;
    private static final int SEGUNDO_ANO = 2031;
    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2029, 12, 20, 11, 0, 0);

    @Autowired
    private VatFilingPeriodJpaRepository springDataRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private JpaVatFilingPeriodRepository repository;

    @BeforeEach
    void adaptador() {
        repository = new JpaVatFilingPeriodRepository(springDataRepository,
                new VatFilingPeriodJpaMapper());
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("el enum se guarda como texto y vuelve intacto, igual que en chk_frequency")
        void el_enum_se_guarda_como_texto() {
            VatFilingPeriod guardado = repository.save(VatFilingPeriod.create(PRIMER_ANO,
                    VatFilingFrequency.BIMONTHLY, "Art. 600 num. 1 ET", CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get().satisfies(recuperado -> {
                assertThat(recuperado.getFrequency()).isEqualTo(VatFilingFrequency.BIMONTHLY);
                assertThat(recuperado.getFiscalYear()).isEqualTo(PRIMER_ANO);
                assertThat(recuperado.getLegalReference()).isEqualTo("Art. 600 num. 1 ET");
                assertThat(recuperado.isEnabled()).isTrue();
            });
        }
    }

    @Nested
    @DisplayName("La vigencia por ano")
    class VigenciaPorAno {

        @Test
        @DisplayName("el ano siguiente puede tener otra periodicidad sin tocar la del anterior")
        void cada_ano_conserva_su_periodicidad() {
            // El primer ano es bimestral por ley —no hay ingresos del ano anterior con los
            // que decidir— y el siguiente puede pasar a cuatrimestral. Recalcular el
            // pasado con la periodicidad de hoy moveria los meses en los que se debio
            // declarar.
            repository.save(VatFilingPeriod.create(PRIMER_ANO, VatFilingFrequency.BIMONTHLY,
                    "Art. 600 num. 1 ET", CREADO_EL));
            repository.save(VatFilingPeriod.create(SEGUNDO_ANO, VatFilingFrequency.FOURMONTHLY,
                    "Art. 600 num. 2 ET", CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByFiscalYear(PRIMER_ANO)).get()
                    .extracting(VatFilingPeriod::getFrequency)
                    .isEqualTo(VatFilingFrequency.BIMONTHLY);
            assertThat(repository.findByFiscalYear(SEGUNDO_ANO)).get()
                    .extracting(VatFilingPeriod::getFrequency)
                    .isEqualTo(VatFilingFrequency.FOURMONTHLY);
        }

        @Test
        @DisplayName("un ano sin publicar devuelve vacio en vez de suponer bimestral")
        void un_ano_sin_publicar_devuelve_vacio() {
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
            repository.save(VatFilingPeriod.create(PRIMER_ANO, VatFilingFrequency.BIMONTHLY,
                    "Art. 600 num. 1 ET", CREADO_EL));
            repository.save(VatFilingPeriod.create(SEGUNDO_ANO, VatFilingFrequency.ANNUAL,
                    "Art. 600 ET", CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            var pagina = repository.findAll(0, 200);

            assertThat(pagina.content()).extracting(VatFilingPeriod::getFiscalYear)
                    .isSortedAccordingTo(java.util.Comparator.reverseOrder());
        }
    }
}
