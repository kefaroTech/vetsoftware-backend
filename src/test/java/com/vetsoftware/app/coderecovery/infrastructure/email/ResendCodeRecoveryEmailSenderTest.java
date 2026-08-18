package com.vetsoftware.app.coderecovery.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.coderecovery.application.port.out.EmployeeAccountsByEmailPort.EmployeeAccount;
import com.vetsoftware.app.coderecovery.testsupport.CodeRecoveryMother;
import com.vetsoftware.app.infrastructure.email.ResendEmailClient;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * No mockea {@code DevEmailPreview.show} (llamada estática de solo logging,
 * appender declarado únicamente en el perfil local) ni el
 * {@code ClassPathResource} de la plantilla: la plantilla real vive en
 * {@code src/main/resources/email-templates/recover-code.html} y su carga es
 * determinista.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResendCodeRecoveryEmailSender")
class ResendCodeRecoveryEmailSenderTest {

    private static final String SUBJECT = "Tu código de usuario de Vetrina";

    @Mock
    private ResendEmailClient email;

    @Nested
    @DisplayName("email deshabilitado")
    class EmailDeshabilitado {

        @Test
        @DisplayName("no envía el correo cuando el email está deshabilitado")
        void no_envia_el_correo_cuando_el_email_esta_deshabilitado() {
            when(email.isEnabled()).thenReturn(false);
            var sender = new ResendCodeRecoveryEmailSender(email, "https://app.vetrina.co/login");

            sender.send("empleado@vetrina.co", "Juan Pérez", CodeRecoveryMother.dosCuentas());

            verify(email, never()).send(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        }
    }

    @Nested
    @DisplayName("construcción del HTML")
    class ConstruccionHtml {

        @Test
        @DisplayName("envía con el asunto fijo y el HTML de una sola fila sin divisor")
        void envia_con_una_cuenta_sin_divisor() {
            when(email.isEnabled()).thenReturn(true);
            var sender = new ResendCodeRecoveryEmailSender(email, "https://app.vetrina.co/login");
            List<EmployeeAccount> unaCuenta = List
                    .of(CodeRecoveryMother.cuentaVeterinariaCentral());

            sender.send("empleado@vetrina.co", "Juan Pérez", unaCuenta);

            ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
            verify(email).send(eq("empleado@vetrina.co"), isNull(), eq(SUBJECT),
                    htmlCaptor.capture(), isNull());
            String html = htmlCaptor.getValue();
            assertThat(html).contains("Juan Pérez").contains("Veterinaria Central")
                    .contains("EMP001").contains("https://app.vetrina.co/login");
            // "acct-div" a secas aparece siempre: es el selector CSS ".acct-div" fijo del
            // <style> de la plantilla. La marca real de fila-con-divisor es la clase
            // "stack acct-div" que ROW_TEMPLATE añade al <td> cuando no es la primera fila.
            assertThat(html).doesNotContain("stack acct-div");
        }

        @Test
        @DisplayName("la segunda fila y siguientes llevan el divisor, la primera no")
        void con_dos_cuentas_solo_la_segunda_lleva_divisor() {
            when(email.isEnabled()).thenReturn(true);
            var sender = new ResendCodeRecoveryEmailSender(email, "https://app.vetrina.co/login");

            sender.send("empleado@vetrina.co", "Juan Pérez", CodeRecoveryMother.dosCuentas());

            ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
            verify(email).send(eq("empleado@vetrina.co"), isNull(), eq(SUBJECT),
                    htmlCaptor.capture(), isNull());
            String html = htmlCaptor.getValue();
            assertThat(html).contains("Veterinaria Central").contains("Veterinaria Norte");
            // El {DIV} del ROW_TEMPLATE aparece dos veces por fila (una por <td>), asi que
            // la segunda fila deja DOS ocurrencias de "stack acct-div" -> 3 fragmentos.
            assertThat(html.split("stack acct-div", -1)).hasSize(3);
        }

        @Test
        @DisplayName("un nombre de empleado nulo se trata como cadena vacía")
        void nombre_de_empleado_nulo_no_lanza() {
            when(email.isEnabled()).thenReturn(true);
            var sender = new ResendCodeRecoveryEmailSender(email, "");

            sender.send("empleado@vetrina.co", null,
                    List.of(CodeRecoveryMother.cuentaVeterinariaCentral()));

            verify(email).send(eq("empleado@vetrina.co"), isNull(), eq(SUBJECT),
                    org.mockito.ArgumentMatchers.any(), isNull());
        }

        @Test
        @DisplayName("un segundo envío en la misma instancia reusa la plantilla ya cargada")
        void segundo_envio_reusa_la_plantilla_cacheada() {
            when(email.isEnabled()).thenReturn(true);
            var sender = new ResendCodeRecoveryEmailSender(email, "https://app.vetrina.co/login");
            List<EmployeeAccount> unaCuenta = List
                    .of(CodeRecoveryMother.cuentaVeterinariaCentral());

            sender.send("empleado@vetrina.co", "Juan Pérez", unaCuenta);
            sender.send("otro@vetrina.co", "Ana Ruiz", unaCuenta);

            verify(email, org.mockito.Mockito.times(2)).send(org.mockito.ArgumentMatchers.any(),
                    isNull(), eq(SUBJECT), org.mockito.ArgumentMatchers.any(), isNull());
        }
    }

    @Nested
    @DisplayName("escapado HTML")
    class Escapado {

        @Test
        @DisplayName("escapa los caracteres especiales del nombre de la veterinaria")
        void escapa_los_caracteres_especiales_del_nombre_de_la_veterinaria() {
            when(email.isEnabled()).thenReturn(true);
            var sender = new ResendCodeRecoveryEmailSender(email, "https://app.vetrina.co/login");
            EmployeeAccount cuentaConCaracteresEspeciales = new EmployeeAccount("Juan Pérez",
                    "EMP001", "Veterinaria \"Los <Amigos> & Cía'\"");

            sender.send("empleado@vetrina.co", "Juan Pérez",
                    List.of(cuentaConCaracteresEspeciales));

            ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
            verify(email).send(eq("empleado@vetrina.co"), isNull(), eq(SUBJECT),
                    htmlCaptor.capture(), isNull());
            String html = htmlCaptor.getValue();
            assertThat(html).contains("&amp;").contains("&lt;Amigos&gt;").contains("&quot;")
                    .contains("&#39;");
            assertThat(html).doesNotContain("<Amigos>");
        }

        @Test
        @DisplayName("una veterinaria sin nombre se renderiza como cadena vacía, no como \"null\"")
        void companyName_nulo_se_trata_como_cadena_vacia() {
            when(email.isEnabled()).thenReturn(true);
            var sender = new ResendCodeRecoveryEmailSender(email, "https://app.vetrina.co/login");
            EmployeeAccount sinNombreDeCompania = new EmployeeAccount("Juan Pérez", "EMP001", null);

            sender.send("empleado@vetrina.co", "Juan Pérez", List.of(sinNombreDeCompania));

            ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
            verify(email).send(eq("empleado@vetrina.co"), isNull(), eq(SUBJECT),
                    htmlCaptor.capture(), isNull());
            assertThat(htmlCaptor.getValue()).doesNotContain("null").contains("EMP001");
        }
    }
}
