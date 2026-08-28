package com.vetsoftware.app.publicholiday.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.publicholiday.domain.HolidayCalendar;
import com.vetsoftware.app.publicholiday.domain.PublicHoliday;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaPublicHolidayRepository — el calendario contra MySQL real")
class PublicHolidayPersistenceIT extends AbstractDataJpaTest {

    // 2028 y no 2026: el changeset 360 ya siembra 2026 y 2027, y
    // uq_public_holidays_date
    // rechazaria una fecha repetida. Escribir en un ano virgen deja el test
    // independiente
    // de la siembra.
    private static final LocalDate LUNES_OBSERVADO = LocalDate.of(2028, 1, 10);
    private static final LocalDate NOMINAL_TRASLADADA = LocalDate.of(2028, 1, 6);
    private static final LocalDate SIN_TRASLADO = LocalDate.of(2028, 1, 1);
    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2027, 12, 1, 9, 0, 0);

    @Autowired
    private PublicHolidayJpaRepository springDataRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private JpaPublicHolidayRepository repository;

    @BeforeEach
    void adaptador() {
        repository = new JpaPublicHolidayRepository(springDataRepository,
                new PublicHolidayJpaMapper());
    }

    private static PublicHoliday trasladado() {
        return PublicHoliday.create(LUNES_OBSERVADO, "Reyes Magos (Epifania)", NOMINAL_TRASLADADA,
                true, "Ley 51 de 1983", CREADO_EL);
    }

    private static PublicHoliday sinTraslado() {
        return PublicHoliday.create(SIN_TRASLADO, "Ano Nuevo", SIN_TRASLADO, false, "Art. 177 CST",
                CREADO_EL);
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda el festivo trasladado con las dos fechas en su sitio")
        void guarda_el_festivo_trasladado() {
            PublicHoliday guardado = repository.save(trasladado());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get().satisfies(recuperado -> {
                assertThat(recuperado.getHolidayDate()).isEqualTo(LUNES_OBSERVADO);
                assertThat(recuperado.getNominalDate()).isEqualTo(NOMINAL_TRASLADADA);
                assertThat(recuperado.isMoved()).isTrue();
                assertThat(recuperado.getLegalReference()).isEqualTo("Ley 51 de 1983");
                assertThat(recuperado.getCreatedDate()).isEqualTo(CREADO_EL);
                assertThat(recuperado.isEnabled()).isTrue();
            });
        }

        @Test
        @DisplayName("existsByHolidayDate mira la fecha observada, que es la unica unica")
        void exists_mira_la_fecha_observada() {
            repository.save(trasladado());
            entityManager.flush();

            assertThat(repository.existsByHolidayDate(LUNES_OBSERVADO)).isTrue();
            // La nominal NO identifica: dos efemerides pueden compartir el lunes
            // observado y por eso no lleva unicidad.
            assertThat(repository.existsByHolidayDate(NOMINAL_TRASLADADA)).isFalse();
        }
    }

    @Nested
    @DisplayName("Consultas del calendario")
    class Consultas {

        @Test
        @DisplayName("findByYear devuelve el ano completo ordenado por fecha")
        void find_by_year_ordena_por_fecha() {
            repository.save(trasladado());
            repository.save(sinTraslado());
            entityManager.flush();
            entityManager.clear();

            List<PublicHoliday> del2028 = repository.findByYear(2028);

            assertThat(del2028).extracting(PublicHoliday::getHolidayDate)
                    .containsExactly(SIN_TRASLADO, LUNES_OBSERVADO);
        }

        @Test
        @DisplayName("loadCalendar trae las fechas observadas del tramo y los anos sembrados")
        void load_calendar_trae_tramo_y_anos() {
            repository.save(trasladado());
            repository.save(sinTraslado());
            entityManager.flush();
            entityManager.clear();

            HolidayCalendar calendario = repository.loadCalendar(LocalDate.of(2028, 1, 1),
                    LocalDate.of(2028, 1, 31));

            assertThat(calendario.observedHolidays()).contains(SIN_TRASLADO, LUNES_OBSERVADO);
            // Los anos vienen de toda la tabla, no del tramo: 2026 y 2027 los siembra el
            // changeset 360 y 2028 lo acaba de escribir este test.
            assertThat(calendario.coveredYears()).contains(2028);
            assertThat(calendario.coveredFrom()).isEqualTo(LocalDate.of(2028, 1, 1));
            assertThat(calendario.coveredTo()).isEqualTo(LocalDate.of(2028, 1, 31));
        }

        @Test
        @DisplayName("el plazo calculado sobre el calendario real salta el festivo guardado")
        void el_plazo_sobre_el_calendario_real_salta_el_festivo() {
            repository.save(trasladado());
            entityManager.flush();
            entityManager.clear();

            HolidayCalendar calendario = repository.loadCalendar(LocalDate.of(2028, 1, 1),
                    LocalDate.of(2028, 2, 29));

            // Viernes 7 de enero de 2028; el lunes 10 es el festivo trasladado, asi que
            // el primer dia habil es el martes 11.
            assertThat(calendario.deadline(LocalDate.of(2028, 1, 7), 1))
                    .isEqualTo(LocalDate.of(2028, 1, 11));
        }
    }

    @Nested
    @DisplayName("Paginacion")
    class Paginacion {

        @Test
        @DisplayName("findAll ordena de la fecha mas reciente a la mas vieja, con desempate")
        void find_all_ordena_por_fecha_descendente() {
            repository.save(trasladado());
            repository.save(sinTraslado());
            entityManager.flush();
            entityManager.clear();

            var pagina = repository.findAll(0, 200);

            assertThat(pagina.content()).extracting(PublicHoliday::getHolidayDate)
                    .isSortedAccordingTo(java.util.Comparator.reverseOrder());
            assertThat(pagina.totalElements()).isPositive();
        }
    }
}
