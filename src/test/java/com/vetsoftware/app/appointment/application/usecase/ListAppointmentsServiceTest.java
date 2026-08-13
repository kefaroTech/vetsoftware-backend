package com.vetsoftware.app.appointment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.application.query.ListAppointmentsQuery;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.testsupport.AppointmentMother;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El servicio traduce el rango de fechas del filtro a un rango de instantes:
 * {@code from} al inicio del dia y {@code to} al ultimo instante del dia, para
 * que la agenda del ultimo dia no se pierda.
 *
 * <p>
 * Y, desde BE-06, <strong>garantiza que ese rango existe siempre</strong>.
 * Antes los extremos ausentes viajaban como {@code null} hasta la consulta, asi
 * que un {@code GET /appointments} sin parametros devolvia la agenda COMPLETA
 * de la empresa. Los tests que fijaban aquello se titulaban «sin acotar el
 * rango»: documentaban el defecto, no una decision.
 *
 * <p>
 * Los stubs se declaran con los instantes exactos esperados: con
 * {@code STRICT_STUBS}, una ventana mal calculada hace fallar el test en vez de
 * devolver una lista vacia silenciosa.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListAppointmentsService — filtros y ventana de la agenda")
class ListAppointmentsServiceTest {

    private static final Long COMPANY = AppointmentMother.COMPANY_ID;
    private static final LocalDate HOY = LocalDate.of(2026, 8, 12);
    private static final LocalDate DESDE = LocalDate.of(2026, 8, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 8, 31);

    /** Reloj fijo: la ventana por defecto se calcula desde «hoy». */
    private static final Clock RELOJ = Clock.fixed(
            HOY.atStartOfDay(ZoneId.of("America/Bogota")).toInstant(), ZoneId.of("America/Bogota"));

    @Mock
    private AppointmentRepository repository;

    private ListAppointmentsService service;

    private ListAppointmentsService service() {
        if (service == null) {
            service = new ListAppointmentsService(repository, RELOJ);
        }
        return service;
    }

    private List<AppointmentDto> ejecutar(LocalDate desde, LocalDate hasta) {
        return service()
                .execute(new ListAppointmentsQuery(COMPANY, desde, hasta, null, null, null));
    }

    private void esperaVentana(LocalDate desde, LocalDate hasta) {
        when(repository.findByFilters(COMPANY, desde.atStartOfDay(), hasta.atTime(LocalTime.MAX),
                null, null, null)).thenReturn(List.of(AppointmentMother.solicitada()));
    }

    @Nested
    @DisplayName("traduccion del rango a instantes")
    class Traduccion {

        @Test
        @DisplayName("abre el rango al inicio del primer dia y lo cierra al final del ultimo")
        void abre_y_cierra_el_rango_en_los_extremos_del_dia() {
            esperaVentana(DESDE, HASTA);

            List<AppointmentDto> agenda = ejecutar(DESDE, HASTA);

            assertThat(agenda).hasSize(1);
            assertThat(agenda.getFirst().id()).isEqualTo(AppointmentMother.APPOINTMENT_ID);
        }

        @Test
        @DisplayName("el corte superior es el ultimo nanosegundo del dia, no su medianoche")
        void el_corte_superior_es_el_ultimo_nanosegundo_del_dia() {
            LocalDateTime finDelDia = LocalDateTime.of(2026, 8, 31, 23, 59, 59, 999_999_999);

            when(repository.findByFilters(COMPANY, DESDE.atStartOfDay(), finDelDia, null, null,
                    null)).thenReturn(List.of(AppointmentMother.solicitada()));

            assertThat(ejecutar(DESDE, HASTA)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("la ventana nunca queda abierta (BE-06)")
    class Ventana {

        @Test
        @DisplayName("sin fechas consulta el mes siguiente a hoy, no la agenda entera")
        void sin_fechas_consulta_el_mes_siguiente_a_hoy() {
            esperaVentana(HOY, HOY.plusDays(31));

            assertThat(ejecutar(null, null)).hasSize(1);
        }

        @Test
        @DisplayName("con solo fecha inicial cierra el extremo superior a 31 dias")
        void con_solo_fecha_inicial_cierra_el_extremo_superior() {
            esperaVentana(DESDE, DESDE.plusDays(31));

            assertThat(ejecutar(DESDE, null)).hasSize(1);
        }

        @Test
        @DisplayName("con solo fecha final mira 31 dias hacia atras desde ella")
        void con_solo_fecha_final_mira_hacia_atras() {
            // Anclarlo en hoy devolveria vacio siempre que la fecha fuese pasada,
            // que es justo cuando alguien filtra «hasta».
            esperaVentana(HASTA.minusDays(31), HASTA);

            assertThat(ejecutar(null, HASTA)).hasSize(1);
        }

        @Test
        @DisplayName("un rango de diez anios se recorta a uno")
        void un_rango_de_diez_anios_se_recorta_a_uno() {
            // La otra puerta al mismo problema: el rango existe, pero es la tabla
            // entera con otro nombre.
            esperaVentana(DESDE, DESDE.plusDays(366));

            assertThat(ejecutar(DESDE, DESDE.plusYears(10))).hasSize(1);
        }

        @Test
        @DisplayName("un rango invertido se colapsa al dia de inicio")
        void un_rango_invertido_se_colapsa_al_dia_de_inicio() {
            esperaVentana(HASTA, HASTA);

            assertThat(ejecutar(HASTA, DESDE)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("filtros")
    class Filtros {

        @Test
        @DisplayName("traslada tal cual los filtros de veterinario, estado y sede")
        void traslada_los_filtros_de_veterinario_estado_y_sede() {
            when(repository.findByFilters(COMPANY, DESDE.atStartOfDay(),
                    HASTA.atTime(LocalTime.MAX), AppointmentMother.EMPLOYEE_ID,
                    AppointmentStatus.CONFIRMED, AppointmentMother.BRANCH_ID))
                    .thenReturn(List.of(AppointmentMother.conEstado(AppointmentStatus.CONFIRMED)));

            List<AppointmentDto> agenda = service().execute(
                    new ListAppointmentsQuery(COMPANY, DESDE, HASTA, AppointmentMother.EMPLOYEE_ID,
                            AppointmentStatus.CONFIRMED, AppointmentMother.BRANCH_ID));

            assertThat(agenda).singleElement().satisfies(
                    cita -> assertThat(cita.status()).isEqualTo(AppointmentStatus.CONFIRMED));
        }

        @Test
        @DisplayName("devuelve una lista vacia cuando la agenda no tiene citas")
        void devuelve_una_lista_vacia_cuando_no_hay_citas() {
            when(repository.findByFilters(COMPANY, DESDE.atStartOfDay(),
                    HASTA.atTime(LocalTime.MAX), null, null, null)).thenReturn(List.of());

            assertThat(ejecutar(DESDE, HASTA)).isEmpty();
        }

        @Test
        @DisplayName("proyecta cada cita del listado sin avisos de solape")
        void proyecta_cada_cita_sin_avisos_de_solape() {
            when(repository.findByFilters(COMPANY, DESDE.atStartOfDay(),
                    HASTA.atTime(LocalTime.MAX), null, null, null))
                    .thenReturn(List.of(AppointmentMother.solicitada(),
                            AppointmentMother.deContactoLibre(null)));

            assertThat(ejecutar(DESDE, HASTA)).hasSize(2)
                    .allSatisfy(cita -> assertThat(cita.overlappingAppointmentIds()).isEmpty());
        }
    }
}
