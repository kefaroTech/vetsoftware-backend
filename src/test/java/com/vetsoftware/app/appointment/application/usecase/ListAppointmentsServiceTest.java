package com.vetsoftware.app.appointment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.application.query.ListAppointmentsQuery;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.testsupport.AppointmentMother;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El servicio traduce el rango de fechas del filtro a un rango de instantes:
 * {@code from} al inicio del dia y {@code to} al ultimo instante del dia, para
 * que la agenda del ultimo dia no se pierda. Los stubs se declaran con los
 * valores exactos esperados: con {@code STRICT_STUBS}, cualquier otra
 * traduccion hace fallar el test en vez de devolver una lista vacia silenciosa.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListAppointmentsService — filtros de la agenda")
class ListAppointmentsServiceTest {

    private static final Long COMPANY = AppointmentMother.COMPANY_ID;
    private static final LocalDate DESDE = LocalDate.of(2026, 8, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 8, 31);

    @Mock
    private AppointmentRepository repository;
    @InjectMocks
    private ListAppointmentsService service;

    @Test
    @DisplayName("abre el rango al inicio del primer dia y lo cierra al final del ultimo")
    void abre_y_cierra_el_rango_en_los_extremos_del_dia() {
        when(repository.findByFilters(COMPANY, DESDE.atStartOfDay(), HASTA.atTime(LocalTime.MAX),
                null, null, null)).thenReturn(List.of(AppointmentMother.solicitada()));

        List<AppointmentDto> agenda = service
                .execute(new ListAppointmentsQuery(COMPANY, DESDE, HASTA, null, null, null));

        assertThat(agenda).hasSize(1);
        assertThat(agenda.getFirst().id()).isEqualTo(AppointmentMother.APPOINTMENT_ID);
    }

    @Test
    @DisplayName("sin fechas consulta sin acotar el rango")
    void sin_fechas_consulta_sin_acotar_el_rango() {
        when(repository.findByFilters(COMPANY, null, null, null, null, null))
                .thenReturn(List.of(AppointmentMother.solicitada()));

        assertThat(
                service.execute(new ListAppointmentsQuery(COMPANY, null, null, null, null, null)))
                .hasSize(1);
    }

    @Test
    @DisplayName("con solo fecha inicial deja el extremo superior abierto")
    void con_solo_fecha_inicial_deja_el_extremo_superior_abierto() {
        when(repository.findByFilters(COMPANY, DESDE.atStartOfDay(), null, null, null, null))
                .thenReturn(List.of());

        assertThat(
                service.execute(new ListAppointmentsQuery(COMPANY, DESDE, null, null, null, null)))
                .isEmpty();
    }

    @Test
    @DisplayName("con solo fecha final deja el extremo inferior abierto")
    void con_solo_fecha_final_deja_el_extremo_inferior_abierto() {
        when(repository.findByFilters(COMPANY, null, HASTA.atTime(LocalTime.MAX), null, null, null))
                .thenReturn(List.of());

        assertThat(
                service.execute(new ListAppointmentsQuery(COMPANY, null, HASTA, null, null, null)))
                .isEmpty();
    }

    @Test
    @DisplayName("traslada tal cual los filtros de veterinario, estado y sede")
    void traslada_los_filtros_de_veterinario_estado_y_sede() {
        when(repository.findByFilters(COMPANY, null, null, AppointmentMother.EMPLOYEE_ID,
                AppointmentStatus.CONFIRMED, AppointmentMother.BRANCH_ID))
                .thenReturn(List.of(AppointmentMother.conEstado(AppointmentStatus.CONFIRMED)));

        List<AppointmentDto> agenda = service.execute(
                new ListAppointmentsQuery(COMPANY, null, null, AppointmentMother.EMPLOYEE_ID,
                        AppointmentStatus.CONFIRMED, AppointmentMother.BRANCH_ID));

        assertThat(agenda).singleElement().satisfies(
                cita -> assertThat(cita.status()).isEqualTo(AppointmentStatus.CONFIRMED));
    }

    @Test
    @DisplayName("devuelve una lista vacia cuando la agenda no tiene citas")
    void devuelve_una_lista_vacia_cuando_no_hay_citas() {
        when(repository.findByFilters(COMPANY, null, null, null, null, null)).thenReturn(List.of());

        assertThat(
                service.execute(new ListAppointmentsQuery(COMPANY, null, null, null, null, null)))
                .isEmpty();
    }

    @Test
    @DisplayName("proyecta cada cita del listado sin avisos de solape")
    void proyecta_cada_cita_sin_avisos_de_solape() {
        when(repository.findByFilters(COMPANY, null, null, null, null, null)).thenReturn(
                List.of(AppointmentMother.solicitada(), AppointmentMother.deContactoLibre(null)));

        List<AppointmentDto> agenda = service
                .execute(new ListAppointmentsQuery(COMPANY, null, null, null, null, null));

        assertThat(agenda).hasSize(2)
                .allSatisfy(cita -> assertThat(cita.overlappingAppointmentIds()).isEmpty());
    }

    @Test
    @DisplayName("el corte superior es el ultimo nanosegundo del dia, no su medianoche")
    void el_corte_superior_es_el_ultimo_nanosegundo_del_dia() {
        LocalDateTime finDelDia = LocalDateTime.of(2026, 8, 31, 23, 59, 59, 999_999_999);

        when(repository.findByFilters(COMPANY, DESDE.atStartOfDay(), finDelDia, null, null, null))
                .thenReturn(List.of(AppointmentMother.solicitada()));

        assertThat(
                service.execute(new ListAppointmentsQuery(COMPANY, DESDE, HASTA, null, null, null)))
                .hasSize(1);
    }
}
