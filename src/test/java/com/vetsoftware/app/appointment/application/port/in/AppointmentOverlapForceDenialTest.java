package com.vetsoftware.app.appointment.application.port.in;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.appointment.application.command.CreateAppointmentCommand;
import com.vetsoftware.app.appointment.application.command.RescheduleAppointmentCommand;
import com.vetsoftware.app.appointment.application.command.UpdateAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import com.vetsoftware.app.auth.application.dto.EmployeeContext;
import com.vetsoftware.app.auth.infrastructure.security.Authz;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * El 403 de verdad del gate de {@code appointment.overlap.force} (BE-17).
 *
 * <p>
 * <b>Por que existe ademas de {@link AppointmentOverlapForceGateTest}.</b>
 * Aquel lee el SpEL de la anotacion; este lo <b>evalua</b>. Un gate puede estar
 * escrito y no aplicarse: basta que el puerto se llame sin pasar por el proxy,
 * que el bean {@code @authz} cambie de nombre, que
 * {@code #command.forceOverlap} deje de resolverse contra el accessor del
 * record —los tres fallan en silencio y dejan pasar la operacion—. Ninguna de
 * esas tres cosas la ve un test de anotaciones, ni {@code @WebMvcTest} (ahi los
 * puertos van mockeados y el {@code @PreAuthorize} ni se mira), ni un test de
 * servicio (llama la clase directa).
 *
 * <p>
 * <b>Por que no es un {@code @SpringBootTest}.</b> El contexto son tres beans:
 * {@code @EnableMethodSecurity}, el {@code Authz} real y un doble de los tres
 * puertos. Sin autoconfiguracion, sin base de datos, sin Redis y sin filtros.
 * Lo que se prueba <em>es</em> el cableado de seguridad, que es el unico caso
 * en el que el CLAUDE.md admite levantar contexto.
 *
 * <p>
 * <b>Como se distingue «denegado» de «ejecutado»</b> sin estado compartido
 * entre tests: el doble no devuelve nada, lanza {@link LlegoAlCasoDeUso}. Ver
 * esa excepcion significa que el gate dejo pasar; ver
 * {@code AccessDeniedException} significa que corto <em>antes</em> de ejecutar
 * el caso de uso.
 */
@SpringJUnitConfig(AppointmentOverlapForceDenialTest.CableadoDeMethodSecurity.class)
@DisplayName("Gate de forceOverlap — denegacion real con el proxy de method security activo")
class AppointmentOverlapForceDenialTest {

    private static final String EDITAR = "appointment.update";
    private static final String CREAR = "appointment.create";
    private static final String FORZAR = "appointment.overlap.force";

    private static final Long EMPRESA = 9L;
    private static final Long OTRA_EMPRESA = 77L;
    private static final Long EMPLEADO = 4L;
    private static final Long SEDE = 1L;
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 8, 1, 9, 0);

    @Autowired
    private CreateAppointmentUseCase crear;
    @Autowired
    private UpdateAppointmentUseCase editar;
    @Autowired
    private RescheduleAppointmentUseCase reprogramar;

    @AfterEach
    void limpiarElContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private static void autenticarEn(Long empresa, String... permisos) {
        EmployeeContext empleado = new EmployeeContext(EMPLEADO, empresa, Set.of(permisos),
                Set.of(SEDE));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(empleado, null,
                        Stream.of(permisos).map(SimpleGrantedAuthority::new).toList()));
    }

    private static CreateAppointmentCommand alta(boolean forzando) {
        return new CreateAppointmentCommand(INICIO, null, AppointmentType.CONSULTATION, EMPLEADO,
                null, null, "Walk-in", null, null, null, SEDE, EMPRESA, forzando, Set.of(SEDE));
    }

    private static UpdateAppointmentCommand edicion(boolean forzando) {
        return new UpdateAppointmentCommand(55L, INICIO, null, AppointmentType.CONSULTATION,
                EMPLEADO, null, null, "Walk-in", null, null, null, EMPRESA, forzando, Set.of(SEDE));
    }

    private static RescheduleAppointmentCommand reprogramacion(boolean forzando) {
        return new RescheduleAppointmentCommand(55L, INICIO, null, EMPLEADO, EMPRESA, forzando,
                Set.of(SEDE));
    }

    @Nested
    @DisplayName("Sin el permiso de forzado")
    class SinElPermisoDeForzado {

        @Test
        @DisplayName("agendar forzando se deniega y el caso de uso no llega a ejecutarse")
        void agendar_forzando_se_deniega() {
            autenticarEn(EMPRESA, CREAR);

            assertThatThrownBy(() -> crear.execute(alta(true)))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("editar forzando se deniega y el caso de uso no llega a ejecutarse")
        void editar_forzando_se_deniega() {
            autenticarEn(EMPRESA, EDITAR);

            // Si el gate no se evaluara, el doble habria lanzado LlegoAlCasoDeUso: que
            // salga AccessDenied prueba que se corta ANTES de tocar la agenda.
            assertThatThrownBy(() -> editar.execute(edicion(true)))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("reprogramar forzando se deniega y el caso de uso no llega a ejecutarse")
        void reprogramar_forzando_se_deniega() {
            autenticarEn(EMPRESA, EDITAR);

            assertThatThrownBy(() -> reprogramar.execute(reprogramacion(true)))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("pero editar SIN forzar sigue permitido: el gate esta atado al flag")
        void editar_sin_forzar_sigue_permitido() {
            autenticarEn(EMPRESA, EDITAR);

            // La regresion cara del otro lado: un hasAuthority(FORZAR) suelto exigiria
            // el permiso tambien para editar sin forzar y nadie podria editar una cita.
            assertThatThrownBy(() -> editar.execute(edicion(false)))
                    .isInstanceOf(LlegoAlCasoDeUso.class);
        }

        @Test
        @DisplayName("y reprogramar SIN forzar tambien")
        void reprogramar_sin_forzar_sigue_permitido() {
            autenticarEn(EMPRESA, EDITAR);

            assertThatThrownBy(() -> reprogramar.execute(reprogramacion(false)))
                    .isInstanceOf(LlegoAlCasoDeUso.class);
        }
    }

    @Nested
    @DisplayName("Con el permiso de forzado")
    class ConElPermisoDeForzado {

        @Test
        @DisplayName("editar forzando atraviesa el gate")
        void editar_forzando_atraviesa_el_gate() {
            autenticarEn(EMPRESA, EDITAR, FORZAR);

            assertThatThrownBy(() -> editar.execute(edicion(true)))
                    .isInstanceOf(LlegoAlCasoDeUso.class);
        }

        @Test
        @DisplayName("reprogramar forzando atraviesa el gate")
        void reprogramar_forzando_atraviesa_el_gate() {
            autenticarEn(EMPRESA, EDITAR, FORZAR);

            assertThatThrownBy(() -> reprogramar.execute(reprogramacion(true)))
                    .isInstanceOf(LlegoAlCasoDeUso.class);
        }

        @Test
        @DisplayName("pero no sobre una empresa ajena: el forzado no sustituye al tenant")
        void no_sirve_sobre_una_empresa_ajena() {
            autenticarEn(OTRA_EMPRESA, EDITAR, FORZAR);

            assertThatThrownBy(() -> editar.execute(edicion(true)))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    /** El doble llego a ejecutarse: el gate dejo pasar la llamada. */
    static final class LlegoAlCasoDeUso extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    /**
     * Doble de los tres puertos. No devuelve un DTO porque nada de este test mira
     * el resultado: lo unico que se observa es <em>si</em> la llamada llego hasta
     * aqui.
     */
    static class CitasDeJuguete
            implements
                CreateAppointmentUseCase,
                UpdateAppointmentUseCase,
                RescheduleAppointmentUseCase {

        @Override
        public AppointmentDto execute(CreateAppointmentCommand command) {
            throw new LlegoAlCasoDeUso();
        }

        @Override
        public AppointmentDto execute(UpdateAppointmentCommand command) {
            throw new LlegoAlCasoDeUso();
        }

        @Override
        public AppointmentDto execute(RescheduleAppointmentCommand command) {
            throw new LlegoAlCasoDeUso();
        }
    }

    /**
     * Tres beans: el interceptor de method security, el {@code Authz} real —con su
     * nombre de bean, que es lo que resuelve el {@code @authz} del SpEL— y el doble
     * de los puertos, que Spring envuelve en un proxy JDK por sus interfaces.
     *
     * <p>
     * <b>{@code @TestConfiguration}, no {@code @Configuration}.</b> Esta clase vive
     * dentro de la raiz del {@code @ComponentScan} y {@code target/test-classes}
     * esta en el classpath de failsafe, asi que una {@code @Configuration} simple
     * es candidata de escaneo: el {@code TypeExcludeFilter} de Boot solo descarta
     * lo meta-anotado con {@code @TestComponent}, que es justo lo que aporta
     * {@code @TestConfiguration}. Sin el, {@code citasDeJuguete} se registraba en
     * <em>todo</em> {@code @SpringBootTest} del repositorio y competia con
     * {@code CreateAppointmentService} al inyectar {@code AppointmentController}
     * ({@code NoUniqueBeanDefinitionException} → {@code Failed to load
     * ApplicationContext}, incluido {@code OpenApiContractIT}). Nombrada
     * explicitamente en el {@code @SpringJUnitConfig} se registra igual.
     */
    @TestConfiguration
    @EnableMethodSecurity
    static class CableadoDeMethodSecurity {

        @Bean("authz")
        Authz authz() {
            return new Authz();
        }

        @Bean
        CitasDeJuguete citasDeJuguete() {
            return new CitasDeJuguete();
        }
    }
}
