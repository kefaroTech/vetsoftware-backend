package com.vetsoftware.app.dunning.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.dunning.application.port.out.DunningSubscriptionPort;
import com.vetsoftware.app.dunning.domain.DunningSubscriptionStatus;
import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionStatusCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.port.in.ChangeSubscriptionStatusUseCase;
import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionJpaRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * La escalada de la cobranza no depende del principal de quien la dispara.
 *
 * <p>
 * <b>El defecto que fija este test.</b> Aplicar un pago que salda una factura
 * vencida termina reevaluando la mora, y esa reevaluacion mueve el estado del
 * contrato -una operacion cerrada a {@code hasRole('SYSTEM')} a secas-. Si ese
 * ultimo salto heredase el principal del que aplico el pago, un actor sin
 * {@code ROLE_SYSTEM} recibiria un {@code AccessDeniedException} <b>dentro de
 * la transaccion</b> y se revertiria la aplicacion del pago entera: la clinica
 * pago, el sistema le responde 403, no queda rastro del pago y sigue bloqueada
 * en solo lectura. Justo en el momento en que un moroso paga.
 *
 * <p>
 * Hoy los seis puertos de mutacion de la capa de dinero estan cerrados a
 * SYSTEM, asi que ese 403 no se puede provocar desde fuera. Pero eso es una
 * circunstancia del arbol, no una propiedad del codigo, y es exactamente lo que
 * este test convierte en mecanismo: se ejercita el puente con un principal de
 * tenant y se comprueba que la escalada ocurre igual.
 */
@SpringJUnitConfig(DunningStatusEscalationAuthorizationTest.Cableado.class)
@DisplayName("Cobranza - la escalada a SYSTEM no depende de quien la dispara")
class DunningStatusEscalationAuthorizationTest {

    private static final Long SUBSCRIPTION_ID = 11L;
    private static final Long COMPANY_ID = 42L;

    @Autowired
    private DunningSubscriptionPort subscriptionPort;
    @Autowired
    private ChangeSubscriptionStatusUseCase changeStatusUseCase;
    @Autowired
    private Registro registro;

    @BeforeEach
    void limpiarElRegistro() {
        registro.invocaciones().clear();
    }

    @AfterEach
    void limpiarElContexto() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("El gate del contrato sigue vivo")
    class GateVivo {

        /**
         * Sin esta comprobacion el resto del test seria vacuo: si el
         * {@code @PreAuthorize} de {@code ChangeSubscriptionStatusUseCase} no se
         * evaluara en este cableado, "no lanza" no probaria nada.
         */
        @Test
        @DisplayName("un ADMIN de tenant no puede mover el estado del contrato por su cuenta")
        void el_tenant_no_alcanza_el_puerto_de_estado_directamente() {
            autenticarAdminDeTenant();

            assertThatThrownBy(() -> changeStatusUseCase.execute(unCambioAActivo()))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("Escalada desde un principal de tenant")
    class EscaladaDesdeTenant {

        @Test
        @DisplayName("reactivar el contrato tras saldar la deuda no revierte con 403")
        void la_reevaluacion_no_revienta_aunque_la_dispare_un_tenant() {
            autenticarAdminDeTenant();

            assertThatCode(() -> subscriptionPort.changeStatus(SUBSCRIPTION_ID, COMPANY_ID,
                    DunningSubscriptionStatus.ACTIVE, "SYSTEM:DUNNING")).doesNotThrowAnyException();

            assertThat(registro.invocaciones()).hasSize(1);
        }

        @Test
        @DisplayName("el cambio de estado corre con ROLE_SYSTEM, no con el rol del llamador")
        void la_escalada_instala_el_principal_del_sistema() {
            autenticarAdminDeTenant();

            subscriptionPort.changeStatus(SUBSCRIPTION_ID, COMPANY_ID,
                    DunningSubscriptionStatus.READ_ONLY, "SYSTEM:DUNNING");

            assertThat(registro.invocaciones()).singleElement()
                    .satisfies(autoridades -> assertThat(autoridades).contains("ROLE_SYSTEM"));
        }

        @Test
        @DisplayName("el contexto del llamador se restaura al salir")
        void no_deja_el_rol_de_sistema_colgado() {
            autenticarAdminDeTenant();

            subscriptionPort.changeStatus(SUBSCRIPTION_ID, COMPANY_ID,
                    DunningSubscriptionStatus.ACTIVE, "SYSTEM:DUNNING");

            Authentication despues = SecurityContextHolder.getContext().getAuthentication();
            assertThat(autoridadesDe(despues)).contains("ROLE_ADMIN").doesNotContain("ROLE_SYSTEM");
        }
    }

    private static void autenticarAdminDeTenant() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                "tenant-admin", "n/a", "ROLE_ADMIN", "subscriptionPayment.read"));
    }

    private static ChangeSubscriptionStatusCommand unCambioAActivo() {
        return new ChangeSubscriptionStatusCommand(SUBSCRIPTION_ID, COMPANY_ID,
                com.vetsoftware.app.subscription.domain.SubscriptionStatus.ACTIVE,
                com.vetsoftware.app.subscription.domain.SubscriptionStatusChangeReason.PAYMENT_RECEIVED,
                "SYSTEM:DUNNING");
    }

    private static List<String> autoridadesDe(Authentication authentication) {
        return authentication == null
                ? List.of()
                : authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                        .toList();
    }

    /**
     * Bean aparte y no un campo del doble: el doble implementa un puerto anotado,
     * asi que Spring Security lo envuelve en un proxy JDK y no se puede inyectar
     * por su clase concreta. El registro si.
     */
    static final class Registro {

        private final List<List<String>> invocaciones = new ArrayList<>();

        List<List<String>> invocaciones() {
            return invocaciones;
        }
    }

    /**
     * Anota con que autoridades se le llamo. Es lo unico que hace falta observar:
     * el {@code @PreAuthorize} del puerto ya lo pone Spring por delante.
     */
    static final class EscaladaSpy implements ChangeSubscriptionStatusUseCase {

        private final Registro registro;

        EscaladaSpy(Registro registro) {
            this.registro = registro;
        }

        @Override
        public SubscriptionDto execute(ChangeSubscriptionStatusCommand command) {
            registro.invocaciones()
                    .add(autoridadesDe(SecurityContextHolder.getContext().getAuthentication()));
            return null;
        }
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class Cableado {

        @Bean
        Registro registro() {
            return new Registro();
        }

        @Bean
        EscaladaSpy escaladaSpy(Registro registro) {
            return new EscaladaSpy(registro);
        }

        @Bean
        SystemAuthRunner systemAuthRunner() {
            return new SystemAuthRunner();
        }

        /**
         * El repositorio no interviene en la escalada: {@code changeStatus} no lo toca.
         * Va mockeado para poder construir el adaptador real, que es lo que se quiere
         * ejercitar.
         */
        @Bean
        JpaDunningSubscriptionPort jpaDunningSubscriptionPort(
                ChangeSubscriptionStatusUseCase changeStatusUseCase,
                SystemAuthRunner systemAuthRunner) {
            return new JpaDunningSubscriptionPort(mock(SubscriptionJpaRepository.class),
                    changeStatusUseCase, systemAuthRunner);
        }
    }
}
