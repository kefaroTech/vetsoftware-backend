package com.vetsoftware.app.publicholiday.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.publicholiday.application.command.ResolveBusinessDayDeadlineCommand;
import com.vetsoftware.app.publicholiday.application.dto.BusinessDayDeadlineDto;
import com.vetsoftware.app.publicholiday.application.port.out.PublicHolidayRepository;
import com.vetsoftware.app.publicholiday.domain.HolidayCalendar;
import com.vetsoftware.app.publicholiday.domain.HolidayCalendarGapException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResolveBusinessDayDeadlineService — el plazo contra el calendario real")
class ResolveBusinessDayDeadlineServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    /**
     * Las 19:30 del 7 de julio de 2026 en Bogota son las 00:30 del 8 en UTC. Con un
     * reloj sin zona, «hoy» seria el 8 y todo el plazo saldria corrido una jornada.
     */
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-07-08T00:30:00Z"), BOGOTA);

    private static final Set<LocalDate> FESTIVOS_JULIO = Set.of(LocalDate.of(2026, 7, 13),
            LocalDate.of(2026, 7, 20));

    @Mock
    private PublicHolidayRepository repository;

    private ResolveBusinessDayDeadlineService service;

    private static HolidayCalendar calendario(LocalDate desde, LocalDate hasta) {
        return new HolidayCalendar(desde, hasta, Set.of(2026), FESTIVOS_JULIO);
    }

    @Nested
    @DisplayName("Calculo")
    class Calculo {

        @Test
        @DisplayName("devuelve el vencimiento y cuantos festivos entre semana se salto")
        void devuelve_el_vencimiento_y_los_festivos_saltados() {
            service = new ResolveBusinessDayDeadlineService(repository, RELOJ);
            when(repository.loadCalendar(any(), any()))
                    .thenReturn(calendario(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31)));

            BusinessDayDeadlineDto resultado = service.resolve(
                    new ResolveBusinessDayDeadlineCommand(LocalDate.of(2026, 7, 1), 15, 9L));

            assertThat(resultado.startDate()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(resultado.businessDays()).isEqualTo(15);
            assertThat(resultado.dueDate()).isEqualTo(LocalDate.of(2026, 7, 24));
            assertThat(resultado.weekdayHolidaysSkipped()).isEqualTo(2);
        }

        @Test
        @DisplayName("sin fecha de partida usa hoy en Bogota, no el dia siguiente en UTC")
        void sin_fecha_de_partida_usa_hoy_en_bogota() {
            service = new ResolveBusinessDayDeadlineService(repository, RELOJ);
            when(repository.loadCalendar(any(), any()))
                    .thenReturn(calendario(LocalDate.of(2026, 7, 7), LocalDate.of(2026, 8, 31)));

            BusinessDayDeadlineDto resultado = service
                    .resolve(new ResolveBusinessDayDeadlineCommand(null, 1, 9L));

            // 19:30 del martes 7 en Bogota. Con el reloj sin zona seria ya el miercoles 8
            // y el vencimiento saldria el jueves 9.
            assertThat(resultado.startDate()).isEqualTo(LocalDate.of(2026, 7, 7));
            assertThat(resultado.dueDate()).isEqualTo(LocalDate.of(2026, 7, 8));
        }

        @Test
        @DisplayName("pide un tramo con margen sobrado para no quedarse corto")
        void pide_un_tramo_con_margen() {
            service = new ResolveBusinessDayDeadlineService(repository, RELOJ);
            when(repository.loadCalendar(any(), any()))
                    .thenReturn(calendario(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31)));

            service.resolve(
                    new ResolveBusinessDayDeadlineCommand(LocalDate.of(2026, 7, 1), 15, 9L));

            ArgumentCaptor<LocalDate> desde = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<LocalDate> hasta = ArgumentCaptor.forClass(LocalDate.class);
            verify(repository).loadCalendar(desde.capture(), hasta.capture());
            assertThat(desde.getValue()).isEqualTo(LocalDate.of(2026, 7, 1));
            // 15 * 3 + 30 = 75 dias naturales: mas del triple de lo que 15 habiles
            // pueden llegar a ocupar, incluso con una semana entera de festivos.
            assertThat(hasta.getValue()).isEqualTo(LocalDate.of(2026, 9, 14));
        }
    }

    @Nested
    @DisplayName("Cuando el calendario no llega")
    class CalendarioIncompleto {

        @Test
        @DisplayName("propaga el hueco en vez de devolver una fecha optimista")
        void propaga_el_hueco_del_calendario() {
            service = new ResolveBusinessDayDeadlineService(repository, RELOJ);
            // El tramo devuelto no cubre 2027: el ano no esta sembrado.
            when(repository.loadCalendar(any(), any())).thenReturn(new HolidayCalendar(
                    LocalDate.of(2026, 12, 20), LocalDate.of(2027, 3, 31), Set.of(2026), Set.of()));

            assertThatThrownBy(() -> service.resolve(
                    new ResolveBusinessDayDeadlineCommand(LocalDate.of(2026, 12, 20), 15, 9L)))
                    .isInstanceOf(HolidayCalendarGapException.class)
                    .hasMessageContaining("seed that year");
        }
    }

    @Nested
    @DisplayName("Validaciones del command")
    class Validaciones {

        @Test
        @DisplayName("un plazo de cero dias habiles se rechaza al construir el command")
        void rechaza_el_plazo_de_cero() {
            assertThatThrownBy(
                    () -> new ResolveBusinessDayDeadlineCommand(LocalDate.of(2026, 7, 1), 0, 9L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("businessDays must be between 1 and 730");
        }

        @Test
        @DisplayName("un plazo mayor que dos anos se rechaza: ningun plazo legal llega ahi")
        void rechaza_el_plazo_desmedido() {
            assertThatThrownBy(
                    () -> new ResolveBusinessDayDeadlineCommand(LocalDate.of(2026, 7, 1), 731, 9L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("businessDays must be between 1 and 730");
        }
    }
}
