package com.vetsoftware.app.subscription.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.application.dto.EmployeeContext;
import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.entitlement.application.command.InitializeCompanyEntitlementsCommand;
import com.vetsoftware.app.entitlement.application.port.in.InitializeCompanyEntitlementsUseCase;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.domain.SubscriptionChangeKind;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * El lazo del modelo: todo cambio de contrato acaba recalculando permisos. Sin
 * esto se puede dar de baja un modulo y que el cliente lo siga usando, que es
 * justo el agujero que este rediseño existe para cerrar.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EntitlementRecalculationAdapter - el cambio de contrato recalcula permisos")
class EntitlementRecalculationAdapterTest {

    private static final Long EMPRESA = 42L;
    private static final Long CONTRATO = 7L;

    @Mock
    private InitializeCompanyEntitlementsUseCase initializeCompanyEntitlementsUseCase;

    /**
     * El runner va REAL y no mockeado: lo que hay que comprobar es el intercambio
     * de principal que hace, y un mock se limitaria a no invocar la lambda.
     */
    private final SystemAuthRunner systemAuthRunner = new SystemAuthRunner();

    private EntitlementRecalculationAdapter adapter;

    @BeforeEach
    void crearAdaptador() {
        adapter = new EntitlementRecalculationAdapter(initializeCompanyEntitlementsUseCase,
                systemAuthRunner);
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Los seis puntos de emision del slice, uno a uno. El enum es la lista
     * exhaustiva: si alguien anade un septimo cambio de contrato, este test le
     * obliga a decidir que tambien recalcula.
     */
    @ParameterizedTest
    @EnumSource(SubscriptionChangeKind.class)
    @DisplayName("cualquier cambio de contrato dispara el recalculo de esa empresa")
    void cualquierCambioRecalcula(SubscriptionChangeKind kind) {
        adapter.subscriptionChanged(
                new SubscriptionChangedEvent(EMPRESA, CONTRATO, kind, LocalDate.of(2026, 5, 1)));

        ArgumentCaptor<InitializeCompanyEntitlementsCommand> captor = ArgumentCaptor
                .forClass(InitializeCompanyEntitlementsCommand.class);
        verify(initializeCompanyEntitlementsUseCase).execute(captor.capture());
        assertThat(captor.getValue().companyId()).isEqualTo(EMPRESA);
    }

    @Test
    @DisplayName("si el recalculo falla, la excepcion sube y el cambio de contrato revierte")
    void siElRecalculoFallaSube() {
        when(initializeCompanyEntitlementsUseCase.execute(any()))
                .thenThrow(new IllegalStateException("company without contract"));

        // No se traga la excepcion ni la difiere: corre en la misma transaccion, asi
        // que dejarla pasar es lo que garantiza que no quede un contrato cambiado con
        // los permisos viejos.
        assertThatThrownBy(() -> adapter.subscriptionChanged(new SubscriptionChangedEvent(EMPRESA,
                CONTRATO, SubscriptionChangeKind.ITEM_REMOVED, LocalDate.of(2026, 5, 1))))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * Desde que el tenant puede cambiar cantidades, dar de baja lineas y cancelar,
     * este recalculo corre bajo SU principal y dentro de SU transaccion. Un paso
     * interno que exija SYSTEM y herede el principal del llamador lanzaria
     * {@code AccessDeniedException} y revertiria la transaccion entera: el cliente
     * hizo la operacion, recibe un 403 y no queda rastro de nada. Estos tests fijan
     * que la escalada no depende de quien disparo la operacion.
     */
    @Nested
    @DisplayName("Escalada interna")
    class Escalada {

        @Test
        @DisplayName("el recalculo corre como SYSTEM aunque lo dispare una empleada del tenant")
        void escalaAunqueLoDispareElTenant() {
            autenticarEmpleadaDeClinica();
            AtomicReference<Authentication> vistoDentro = new AtomicReference<>();
            when(initializeCompanyEntitlementsUseCase.execute(any())).thenAnswer(invocation -> {
                vistoDentro.set(SecurityContextHolder.getContext().getAuthentication());
                return null;
            });

            adapter.subscriptionChanged(new SubscriptionChangedEvent(EMPRESA, CONTRATO,
                    SubscriptionChangeKind.QUANTITY_CHANGED, LocalDate.of(2026, 5, 1)));

            assertThat(vistoDentro.get().getAuthorities())
                    .extracting(GrantedAuthority::getAuthority).contains("ROLE_SYSTEM");
        }

        /**
         * Escalar no puede dejar el hilo escalado: la transaccion del tenant sigue
         * despues de esta llamada, y si el principal se quedara en SYSTEM el resto de
         * la operacion se autorizaria sola.
         */
        @Test
        @DisplayName("al terminar devuelve el principal del tenant, no se queda escalado")
        void devuelveElPrincipalDelTenant() {
            Authentication antes = autenticarEmpleadaDeClinica();

            adapter.subscriptionChanged(new SubscriptionChangedEvent(EMPRESA, CONTRATO,
                    SubscriptionChangeKind.CANCELLATION_REQUESTED, LocalDate.of(2026, 5, 1)));

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(antes);
        }

        private Authentication autenticarEmpleadaDeClinica() {
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    new EmployeeContext(9L, EMPRESA, Set.of("subscription.update"), Set.of(1L)),
                    null, List.of(new SimpleGrantedAuthority("subscription.update")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            return auth;
        }
    }
}
