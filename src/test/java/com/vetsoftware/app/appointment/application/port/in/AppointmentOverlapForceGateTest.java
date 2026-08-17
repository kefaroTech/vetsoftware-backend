package com.vetsoftware.app.appointment.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El forzado de solape exige permiso propio en los tres verbos de escritura
 * (BE-17).
 *
 * <p>
 * <b>Que comprueba y que no.</b> Esto lee el SpEL de la anotacion; no lo
 * evalua. Sirve para lo que de verdad se rompe en la practica —que alguien
 * quite la condicion al tocar el gate, o que un puerto nuevo se olvide de
 * ponerla— y para fijar por escrito el nombre exacto del permiso, que tiene que
 * coincidir con el que siembra Liquibase. Mismo criterio que
 * {@code GranularPermissionGateTest}.
 *
 * <p>
 * <b>La otra mitad:</b> que un caller sin {@code appointment.overlap.force}
 * reciba de verdad un 403 lo cubre {@link AppointmentOverlapForceDenialTest},
 * que levanta method security y <em>evalua</em> el SpEL. No se puede desde
 * {@code @WebMvcTest} —ahi los puertos van mockeados y el {@code @PreAuthorize}
 * no llega a evaluarse— ni desde un test de servicio, que llama a la clase
 * directamente sin pasar por el proxy de seguridad.
 */
@DisplayName("Gate de forceOverlap — permiso propio en los tres verbos de escritura")
class AppointmentOverlapForceGateTest {

    /** Debe coincidir con el permiso sembrado por el changeset 223. */
    private static final String PERMISO = "appointment.overlap.force";

    static Stream<Class<?>> puertosDeEscrituraConForzado() {
        return Stream.of(CreateAppointmentUseCase.class, UpdateAppointmentUseCase.class,
                RescheduleAppointmentUseCase.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("puertosDeEscrituraConForzado")
    @DisplayName("exige el permiso appointment.overlap.force cuando el command viene forzado")
    void exige_el_permiso_cuando_el_command_viene_forzado(Class<?> puerto) {
        Method execute = Stream.of(puerto.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("execute")).findFirst()
                .orElseThrow();
        PreAuthorize gate = execute.getAnnotation(PreAuthorize.class);

        assertThat(gate).as("el puerto debe declarar @PreAuthorize").isNotNull();
        // La condicion tiene que estar atada al flag: un `hasAuthority(PERMISO)` suelto
        // exigiria el permiso tambien para agendar sin forzar, y nadie podria agendar.
        assertThat(gate.value()).contains(PERMISO).contains("#command.forceOverlap");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("puertosDeEscrituraConForzado")
    @DisplayName("sigue acotando el tenant: el forzado no sustituye a la comprobacion de empresa")
    void sigue_acotando_el_tenant(Class<?> puerto) {
        Method execute = Stream.of(puerto.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("execute")).findFirst()
                .orElseThrow();

        assertThat(execute.getAnnotation(PreAuthorize.class).value())
                .contains("@authz.isMyCompany(#command.companyId)");
    }
}
