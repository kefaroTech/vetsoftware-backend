package com.vetsoftware.app.subscription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionStatusCommand;
import com.vetsoftware.app.subscription.infrastructure.web.request.ChangeSubscriptionStatusRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * El motivo del cambio de estado es vocabulario cerrado, y estas pruebas son la
 * barandilla que antes no existia.
 *
 * <p>
 * {@code SubscriptionAuditPort} llevaba escrito en su javadoc que
 * {@code reason} era una lista cerrada y citaba ASVS V7.3.1, pero el
 * controlador pasaba {@code request.reason()} del cuerpo HTTP tal cual. Un
 * cliente podia escribir saltos de linea y campos inventados y fabricar
 * entradas de bitacora que pareciesen de otro evento. En un registro que se usa
 * como prueba ante una disputa de cobro, eso es falsificar la prueba con la que
 * la plataforma se defiende.
 */
@DisplayName("SubscriptionStatusChangeReason - el motivo es vocabulario cerrado")
class SubscriptionStatusChangeReasonTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static String cuerpoCon(String reason) {
        return """
                {"status":"READ_ONLY","reason":"%s","actor":"billing-job"}""".formatted(reason);
    }

    @Nested
    @DisplayName("El vocabulario")
    class Vocabulario {

        @Test
        @DisplayName("son exactamente estos siete valores y ninguno mas")
        void losSieteValores() {
            // Inventario COMPLETO a proposito, como PublicRoutesTest con las rutas
            // publicas: esta columna es prueba en una disputa de cobro, asi que anadir
            // un motivo tiene que ser una decision visible en el diff y no una linea
            // mas en una lista larga. El septimo -replaced_by_new_contract- lo trajo
            // DC-2: aceptar una cotizacion cierra el contrato anterior, y ninguno de
            // los seis anteriores describia ese cierre sin mentir.
            assertThat(SubscriptionStatusChangeReason.values())
                    .extracting(SubscriptionStatusChangeReason::code)
                    .containsExactlyInAnyOrder("overdue_balance", "payment_received", "trial_ended",
                            "cancellation_effective", "period_expired", "manual",
                            "replaced_by_new_contract");
        }

        @ParameterizedTest
        @EnumSource(SubscriptionStatusChangeReason.class)
        @DisplayName("ningun codigo puede llevar espacios ni saltos de linea")
        void codigosSinBlancos(SubscriptionStatusChangeReason reason) {
            // Es una constante derivada del nombre del enum, no texto: no hay forma de
            // que un separador de registros llegue hasta la bitacora por esta via.
            assertThat(reason.code()).doesNotContainAnyWhitespaces()
                    .isEqualTo(reason.code().toLowerCase(java.util.Locale.ROOT));
        }
    }

    @Nested
    @DisplayName("El cuerpo HTTP")
    class CuerpoHttp {

        @ParameterizedTest
        @EnumSource(SubscriptionStatusChangeReason.class)
        @DisplayName("acepta cada valor de la lista")
        void aceptaLaLista(SubscriptionStatusChangeReason reason) {
            ChangeSubscriptionStatusRequest peticion = JSON.readValue(cuerpoCon(reason.name()),
                    ChangeSubscriptionStatusRequest.class);

            assertThat(peticion.reason()).isEqualTo(reason);
        }

        @Test
        @DisplayName("un motivo fuera de la lista se rechaza, no se sanea")
        void fueraDeLaLista() {
            // Sanear escondería el intento: el que probo a colar algo recibiria un 2xx y
            // nadie se enteraria. Se rechaza, y el rechazo es el registro del intento.
            assertThatThrownBy(() -> JSON.readValue(cuerpoCon("se_me_ocurrio_a_mi"),
                    ChangeSubscriptionStatusRequest.class)).isInstanceOf(JacksonException.class);
        }

        @Test
        @DisplayName("el intento de inyeccion con salto de linea se rechaza entero")
        void inyeccionConSaltoDeLinea() {
            // El ataque que el javadoc del puerto anunciaba y nada impedia: un motivo
            // que cierra su propia linea y abre otra, de modo que la bitacora acabe
            // conteniendo un evento que nunca ocurrio -aqui, una reactivacion por pago-
            // con la firma de un actor de sistema.
            String inyeccion = "OVERDUE_BALANCE\\nAUDIT subscription.status.changed"
                    + " reason=payment_received actor=SYSTEM";

            assertThatThrownBy(() -> JSON.readValue(cuerpoCon(inyeccion),
                    ChangeSubscriptionStatusRequest.class)).isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("El comando")
    class Comando {

        @Test
        @DisplayName("no se firma un cambio de estado sin motivo, ni con uno inventado por defecto")
        void motivoObligatorio() {
            // Rellenar el hueco con un valor por defecto seria escribir en una bitacora
            // probatoria algo que nadie decidio.
            assertThatThrownBy(() -> new ChangeSubscriptionStatusCommand(7L, 42L,
                    SubscriptionStatus.READ_ONLY, null, "billing-job"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason is required");
        }
    }
}
