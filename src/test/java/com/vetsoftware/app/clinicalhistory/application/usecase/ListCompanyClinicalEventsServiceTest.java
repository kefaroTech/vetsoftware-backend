package com.vetsoftware.app.clinicalhistory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalEventRepository;
import com.vetsoftware.app.clinicalhistory.application.query.ListCompanyClinicalEventsQuery;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BE-06. {@code GET /clinical-history} declara el rango OPCIONAL, asi que una
 * llamada sin parametros pedia la historia clinica COMPLETA de la empresa:
 * todas las consultas, vacunas, cirugias y hospitalizaciones de todos los
 * pacientes desde el primer dia.
 *
 * <p>
 * Como en la agenda, la respuesta no es paginar —esto alimenta un calendario, y
 * un periodo se pinta entero o no se pinta— sino garantizar que el rango existe
 * siempre. Lo que estas pruebas fijan es esa garantia.
 *
 * <p>
 * Se afirma sobre el query que LLEGA al repositorio, capturado, porque es el
 * unico sitio donde se ve la ventana ya resuelta.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListCompanyClinicalEventsService — la ventana nunca queda abierta")
class ListCompanyClinicalEventsServiceTest {

    private static final Long COMPANY = 7L;
    private static final LocalDate HOY = LocalDate.of(2026, 8, 12);
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final Clock RELOJ = Clock.fixed(HOY.atStartOfDay(BOGOTA).toInstant(), BOGOTA);

    @Mock
    private ClinicalEventRepository repository;

    private ListCompanyClinicalEventsQuery ventanaUsadaPara(LocalDate desde, LocalDate hasta) {
        when(repository.findByCompany(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        new ListCompanyClinicalEventsService(repository, RELOJ)
                .execute(new ListCompanyClinicalEventsQuery(COMPANY, List.of(), desde, hasta));

        ArgumentCaptor<ListCompanyClinicalEventsQuery> capturado = ArgumentCaptor
                .forClass(ListCompanyClinicalEventsQuery.class);
        org.mockito.Mockito.verify(repository).findByCompany(capturado.capture());
        return capturado.getValue();
    }

    @Test
    @DisplayName("sin fechas consulta el mes siguiente a hoy, no la historia entera")
    void sin_fechas_consulta_el_mes_siguiente_a_hoy() {
        ListCompanyClinicalEventsQuery ventana = ventanaUsadaPara(null, null);

        assertThat(ventana.from()).isEqualTo(HOY);
        assertThat(ventana.to()).isEqualTo(HOY.plusDays(31));
    }

    @Test
    @DisplayName("con solo fecha inicial cierra el extremo superior a 31 dias")
    void con_solo_fecha_inicial_cierra_el_extremo_superior() {
        LocalDate desde = LocalDate.of(2026, 3, 1);

        ListCompanyClinicalEventsQuery ventana = ventanaUsadaPara(desde, null);

        assertThat(ventana.from()).isEqualTo(desde);
        assertThat(ventana.to()).isEqualTo(desde.plusDays(31));
    }

    @Test
    @DisplayName("con solo fecha final mira 31 dias hacia atras desde ella")
    void con_solo_fecha_final_mira_hacia_atras() {
        // Anclarlo en hoy devolveria vacio siempre que la fecha fuese pasada, que
        // es justo cuando alguien consulta un historial «hasta» una fecha.
        LocalDate hasta = LocalDate.of(2026, 3, 31);

        ListCompanyClinicalEventsQuery ventana = ventanaUsadaPara(null, hasta);

        assertThat(ventana.from()).isEqualTo(hasta.minusDays(31));
        assertThat(ventana.to()).isEqualTo(hasta);
    }

    @Test
    @DisplayName("un rango de diez anios se recorta a uno")
    void un_rango_de_diez_anios_se_recorta_a_uno() {
        // La otra puerta al mismo problema: el rango existe, pero es la tabla
        // entera con otro nombre.
        LocalDate desde = LocalDate.of(2020, 1, 1);

        ListCompanyClinicalEventsQuery ventana = ventanaUsadaPara(desde, desde.plusYears(10));

        assertThat(ventana.to()).isEqualTo(desde.plusDays(366));
    }

    @Test
    @DisplayName("un rango que ya cabe se respeta tal cual")
    void un_rango_que_ya_cabe_se_respeta() {
        LocalDate desde = LocalDate.of(2026, 8, 1);
        LocalDate hasta = LocalDate.of(2026, 8, 15);

        ListCompanyClinicalEventsQuery ventana = ventanaUsadaPara(desde, hasta);

        assertThat(ventana.from()).isEqualTo(desde);
        assertThat(ventana.to()).isEqualTo(hasta);
    }
}
