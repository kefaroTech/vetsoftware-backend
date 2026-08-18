package com.vetsoftware.app.coderecovery.application.usecase;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.coderecovery.application.command.RecoverEmployeeCodeCommand;
import com.vetsoftware.app.coderecovery.application.port.out.CodeRecoveryEmailSender;
import com.vetsoftware.app.coderecovery.application.port.out.EmployeeAccountsByEmailPort;
import com.vetsoftware.app.coderecovery.application.port.out.EmployeeAccountsByEmailPort.EmployeeAccount;
import com.vetsoftware.app.coderecovery.testsupport.CodeRecoveryMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecoverEmployeeCodeService")
class RecoverEmployeeCodeServiceTest {

    @Mock
    private EmployeeAccountsByEmailPort accountsByEmail;
    @Mock
    private CodeRecoveryEmailSender emailSender;
    @InjectMocks
    private RecoverEmployeeCodeService service;

    @Nested
    @DisplayName("email inválido")
    class EmailInvalido {

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("correo vacío o en blanco no consulta ni envía")
        void correo_vacio_o_en_blanco_no_consulta_ni_envia(String email) {
            service.execute(new RecoverEmployeeCodeCommand(email));

            verifyNoInteractions(accountsByEmail, emailSender);
        }

        @Test
        @DisplayName("correo nulo no consulta ni envía")
        void correo_nulo_no_consulta_ni_envia() {
            service.execute(new RecoverEmployeeCodeCommand(null));

            verifyNoInteractions(accountsByEmail, emailSender);
        }
    }

    @Nested
    @DisplayName("sin cuentas — anti-enumeración")
    class SinCuentas {

        @Test
        @DisplayName("sin cuentas elegibles no envía correo")
        void sin_cuentas_elegibles_no_envia_correo() {
            when(accountsByEmail.findByEmail("a@b.com")).thenReturn(List.of());

            service.execute(new RecoverEmployeeCodeCommand("a@b.com"));

            verify(accountsByEmail).findByEmail("a@b.com");
            verifyNoInteractions(emailSender);
        }
    }

    @Nested
    @DisplayName("con cuentas")
    class ConCuentas {

        @Test
        @DisplayName("envía el correo con las cuentas encontradas y el nombre de la primera")
        void envia_el_correo_con_las_cuentas_encontradas() {
            List<EmployeeAccount> cuentas = CodeRecoveryMother.dosCuentas();
            when(accountsByEmail.findByEmail("a@b.com")).thenReturn(cuentas);

            service.execute(new RecoverEmployeeCodeCommand("a@b.com"));

            ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> nombreCaptor = ArgumentCaptor.forClass(String.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<EmployeeAccount>> cuentasCaptor = ArgumentCaptor
                    .forClass(List.class);
            verify(emailSender).send(emailCaptor.capture(), nombreCaptor.capture(),
                    cuentasCaptor.capture());

            assertThat(emailCaptor.getValue()).isEqualTo("a@b.com");
            assertThat(nombreCaptor.getValue()).isEqualTo(cuentas.get(0).name());
            assertThat(cuentasCaptor.getValue()).isEqualTo(cuentas);
        }

        @Test
        @DisplayName("recorta espacios del correo antes de buscar y de enviar")
        void recorta_espacios_del_correo_antes_de_buscar_y_de_enviar() {
            List<EmployeeAccount> cuentas = List.of(CodeRecoveryMother.cuentaVeterinariaCentral());
            when(accountsByEmail.findByEmail("a@b.com")).thenReturn(cuentas);

            service.execute(new RecoverEmployeeCodeCommand(" a@b.com "));

            verify(accountsByEmail).findByEmail("a@b.com");
            verify(emailSender).send(eq("a@b.com"), eq(cuentas.get(0).name()), eq(cuentas));
        }
    }
}
