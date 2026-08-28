package com.vetsoftware.app.securityincident.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.publicholiday.domain.HolidayCalendarGapException;
import com.vetsoftware.app.securityincident.application.command.RegisterSecurityIncidentCommand;
import com.vetsoftware.app.securityincident.application.dto.SecurityIncidentDto;
import com.vetsoftware.app.securityincident.application.port.out.BusinessDayDeadlinePort;
import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentRepository;
import com.vetsoftware.app.securityincident.domain.IncidentSeverity;
import com.vetsoftware.app.securityincident.domain.SecurityIncident;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentKind;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El calculo del plazo de reporte a la SIC.
 *
 * <h2>Lo que esta clase existe para sujetar</h2>
 *
 * <p>
 * Quince dias habiles <b>desde el escalamiento interno</b>, no desde la
 * deteccion (Circular Unica de la SIC, Titulo V, 2.1, f, ii). Es una regla que
 * se cuela por las rendijas de todo lo demas: al adaptador de persistencia le
 * llega {@code deadline_at} ya resuelto, asi que ninguna rodaja de persistencia
 * puede verlo mal calculado, y el controller solo mira codigos de estado. Aqui
 * es el unico sitio donde la cuenta es observable.
 *
 * <p>
 * <b>Por eso la deteccion y el escalamiento caen en dias DISTINTOS en todos los
 * casos.</b> Si los dos fueran el mismo dia, el caso pasaria igual con el
 * servicio contando desde {@code detectedAt}, que es exactamente el error que
 * se quiere impedir: la prueba no diria nada y seguiria en verde el dia que
 * alguien cambiara la linea. El {@code ArgumentCaptor} sobre el puerto es quien
 * lo hace observable.
 *
 * <p>
 * <b>El camino de excepcion cuenta tanto como el feliz.</b> Un plazo legal que
 * falla en silencio por falta de festivos sembrados es peor que uno mal
 * calculado: {@code HolidayCalendarGapException} tiene que salir entera y sin
 * dejar fila escrita.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterSecurityIncidentService — quince dias habiles desde el escalamiento")
class RegisterSecurityIncidentServiceTest {

    /** Lunes 2 de marzo de 2026, 08:15. */
    private static final LocalDateTime DETECTADO_EL = LocalDateTime.of(2026, 3, 2, 8, 15);

    /**
     * Jueves 5 de marzo, 17:30. <b>Tres dias despues de la deteccion</b>, que es lo
     * que permite distinguir desde cual de las dos fechas se conto.
     */
    private static final LocalDateTime ESCALADO_EL = LocalDateTime.of(2026, 3, 5, 17, 30);

    /** Lo que devuelve el calendario: quince habiles desde el jueves 5. */
    private static final LocalDate VENCE_EL = LocalDate.of(2026, 3, 27);

    private static final Clock RELOJ = Clock
            .fixed(LocalDateTime.of(2026, 3, 5, 18, 0).toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));

    @Mock
    private SecurityIncidentRepository repository;
    @Mock
    private BusinessDayDeadlinePort businessDayDeadlinePort;

    @Captor
    private ArgumentCaptor<LocalDate> inicioDelPlazo;
    @Captor
    private ArgumentCaptor<Integer> diasHabiles;
    @Captor
    private ArgumentCaptor<SecurityIncident> guardado;

    private RegisterSecurityIncidentService service() {
        return new RegisterSecurityIncidentService(repository, businessDayDeadlinePort, RELOJ);
    }

    private static RegisterSecurityIncidentCommand comando(LocalDateTime escalatedAt) {
        return new RegisterSecurityIncidentCommand(DETECTADO_EL, DETECTADO_EL.minusHours(6),
                escalatedAt, SecurityIncidentKind.DATA_LEAK, IncidentSeverity.HIGH,
                "Exposicion de historias clinicas por una URL firmada sin caducar", 412);
    }

    @Nested
    @DisplayName("El dies a quo")
    class DiesAQuo {

        @Test
        @DisplayName("cuenta desde el escalamiento interno y no desde la deteccion")
        void cuenta_desde_el_escalamiento_y_no_desde_la_deteccion() {
            when(businessDayDeadlinePort.resolve(any(), anyInt())).thenReturn(VENCE_EL);
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service().execute(comando(ESCALADO_EL));

            verify(businessDayDeadlinePort).resolve(inicioDelPlazo.capture(),
                    diasHabiles.capture());
            assertThat(inicioDelPlazo.getValue()).isEqualTo(ESCALADO_EL.toLocalDate())
                    .isNotEqualTo(DETECTADO_EL.toLocalDate());
        }

        @Test
        @DisplayName("pide exactamente quince dias habiles, los de la Circular Unica")
        void pide_exactamente_quince_dias_habiles() {
            when(businessDayDeadlinePort.resolve(any(), anyInt())).thenReturn(VENCE_EL);
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service().execute(comando(ESCALADO_EL));

            verify(businessDayDeadlinePort).resolve(inicioDelPlazo.capture(),
                    diasHabiles.capture());
            assertThat(diasHabiles.getValue())
                    .isEqualTo(SecurityIncident.PLAZO_REPORTE_SIC_DIAS_HABILES).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("De fecha a instante")
    class DeFechaAInstante {

        @Test
        @DisplayName("sella el vencimiento al final de la jornada del dia que da el calendario")
        void sella_el_vencimiento_al_final_de_la_jornada() {
            // Al final y no al principio: atStartOfDay() le quitaria al plazo una
            // jornada entera que la norma concede. Y con la precision exacta de
            // DATETIME(6), porque LocalTime.MAX lleva nanos y MySQL los truncaria.
            when(businessDayDeadlinePort.resolve(any(), anyInt())).thenReturn(VENCE_EL);
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            SecurityIncidentDto dto = service().execute(comando(ESCALADO_EL));

            assertThat(dto.deadlineAt())
                    .isEqualTo(LocalDateTime.of(2026, 3, 27, 23, 59, 59, 999_999_000));
        }

        @Test
        @DisplayName("guarda el incidente con el vencimiento resuelto y el reloj inyectado")
        void guarda_el_incidente_con_el_vencimiento_resuelto() {
            when(businessDayDeadlinePort.resolve(any(), anyInt())).thenReturn(VENCE_EL);
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service().execute(comando(ESCALADO_EL));

            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue()).satisfies(incidente -> {
                assertThat(incidente.getId()).isNull();
                assertThat(incidente.getEscalatedAt()).isEqualTo(ESCALADO_EL);
                assertThat(incidente.getDeadlineAt())
                        .isEqualTo(LocalDateTime.of(2026, 3, 27, 23, 59, 59, 999_999_000));
                assertThat(incidente.getCreatedDate()).isEqualTo(LocalDateTime.now(RELOJ));
                assertThat(incidente.getReportedToAuthorityAt()).isNull();
                assertThat(incidente.getClosedAt()).isNull();
            });
        }
    }

    @Nested
    @DisplayName("Caminos de fallo")
    class CaminosDeFallo {

        @Test
        @DisplayName("sin escalamiento no hay plazo que calcular y no se escribe nada")
        void sin_escalamiento_no_se_escribe_nada() {
            assertThatThrownBy(() -> service().execute(comando(null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("escalatedAt is required");

            verifyNoInteractions(businessDayDeadlinePort, repository);
        }

        @Test
        @DisplayName("el hueco del calendario sale entero y no deja incidente escrito")
        void el_hueco_del_calendario_sale_entero() {
            // La alternativa seria degradar a dias corridos, que da un vencimiento
            // MAS TARDE que el real: se incumple el plazo y nadie se entera. Fallar
            // ruidosamente y pedir que se siembre el ano es la unica salida honesta.
            when(businessDayDeadlinePort.resolve(any(), anyInt()))
                    .thenThrow(new HolidayCalendarGapException(LocalDate.of(2026, 3, 27)));

            assertThatThrownBy(() -> service().execute(comando(ESCALADO_EL)))
                    .isInstanceOf(HolidayCalendarGapException.class)
                    .hasMessageContaining("does not cover 2026-03-27");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("un calendario que devuelve una fecha anterior al escalamiento no cuela")
        void un_vencimiento_anterior_al_escalamiento_no_cuela() {
            // El servicio no valida esto: lo valida el dominio. El caso existe para
            // demostrar que el servicio NO se salta esa comprobacion al construir el
            // incidente, que es la unica forma de que un puerto equivocado -o un
            // calendario mal sembrado que devolviera el propio dia- acabara en una
            // fila con el plazo ya vencido al nacer.
            when(businessDayDeadlinePort.resolve(any(), anyInt()))
                    .thenReturn(ESCALADO_EL.toLocalDate().minusDays(1));

            assertThatThrownBy(() -> service().execute(comando(ESCALADO_EL)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("deadlineAt must be after escalatedAt");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("un escalamiento anterior a la deteccion lo para el dominio")
        void un_escalamiento_anterior_a_la_deteccion_lo_para_el_dominio() {
            LocalDateTime antesDeDetectar = DETECTADO_EL.minusDays(1);
            when(businessDayDeadlinePort.resolve(any(), anyInt())).thenReturn(VENCE_EL);

            assertThatThrownBy(() -> service().execute(comando(antesDeDetectar)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("escalatedAt must not be before detectedAt");

            verifyNoInteractions(repository);
        }
    }
}
