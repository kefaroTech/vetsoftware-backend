package com.vetsoftware.app.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.auth.application.dto.SystemContext;
import com.vetsoftware.app.auth.testsupport.AuthMother;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Eleva temporalmente el contexto de seguridad a {@code SYSTEM} para ejecutar
 * código interno (cachés, jobs) sin un empleado autenticado.
 */
class SystemAuthRunnerTest {

    private final SystemAuthRunner runner = new SystemAuthRunner();

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("call activa el contexto de sistema durante la acción y lo restaura después")
    void call_activa_el_contexto_de_sistema_y_lo_restaura() {
        Authentication previo = new UsernamePasswordAuthenticationToken(AuthMother.empleado(), null,
                java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(previo);
        Authentication[] capturado = new Authentication[1];

        String resultado = runner.call(() -> {
            capturado[0] = SecurityContextHolder.getContext().getAuthentication();
            return "ok";
        });

        assertThat(resultado).isEqualTo("ok");
        assertThat(capturado[0].getPrincipal()).isEqualTo(SystemContext.INSTANCE);
        assertThat(capturado[0].getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_SYSTEM");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(previo);
    }

    @Test
    @DisplayName("call restaura el contexto previo incluso si la acción lanza")
    void call_restaura_el_contexto_previo_si_la_accion_lanza() {
        Authentication previo = new UsernamePasswordAuthenticationToken(AuthMother.empleado(), null,
                java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(previo);

        assertThatThrownBy(() -> runner.call(() -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class).hasMessageContaining("boom");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(previo);
    }

    @Test
    @DisplayName("sin autenticación previa, el contexto queda limpio después de la acción")
    void sin_autenticacion_previa_el_contexto_queda_limpio_despues() {
        runner.run(() -> {
        });

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("run ejecuta la acción bajo el contexto de sistema")
    void run_ejecuta_la_accion_bajo_el_contexto_de_sistema() {
        Authentication[] capturado = new Authentication[1];

        runner.run(() -> capturado[0] = SecurityContextHolder.getContext().getAuthentication());

        assertThat(capturado[0].getPrincipal()).isEqualTo(SystemContext.INSTANCE);
    }
}
