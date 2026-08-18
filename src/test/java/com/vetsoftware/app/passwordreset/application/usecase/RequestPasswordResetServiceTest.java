package com.vetsoftware.app.passwordreset.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.passwordreset.application.command.RequestPasswordResetCommand;
import com.vetsoftware.app.passwordreset.application.port.out.EmployeeAccountLookupPort;
import com.vetsoftware.app.passwordreset.application.port.out.EmployeeAccountLookupPort.EmployeeAccount;
import com.vetsoftware.app.passwordreset.application.port.out.PasswordResetEmailSender;
import com.vetsoftware.app.passwordreset.application.port.out.PasswordResetTokenRepository;
import com.vetsoftware.app.passwordreset.domain.PasswordResetToken;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Anti-enumeracion: nada de lo que este service hace hacia afuera puede
 * distinguir "codigo inexistente" de "correo sin verificar" de "no elegible por
 * cualquier otro motivo". Las tres ramas de rechazo terminan igual: sin tocar
 * el repositorio de tokens ni el emisor de correo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RequestPasswordResetService")
class RequestPasswordResetServiceTest {

    private static final Long EMPLOYEE_ID = 500L;
    private static final Long COMPANY_ID = 9L;
    private static final long TTL_HOURS = 2L;

    @Mock
    private EmployeeAccountLookupPort employeeLookup;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private PasswordResetEmailSender emailSender;

    @Captor
    private ArgumentCaptor<PasswordResetToken> tokenCaptor;
    @Captor
    private ArgumentCaptor<String> rawTokenCaptor;

    private RequestPasswordResetService service;

    @BeforeEach
    void crearServicio() {
        service = new RequestPasswordResetService(employeeLookup, tokenRepository, emailSender,
                TTL_HOURS);
    }

    private static EmployeeAccount cuentaElegible() {
        return new EmployeeAccount(EMPLOYEE_ID, COMPANY_ID, "Ana Ruiz", "ana@vetrina.co",
                "Clinica Norte", true);
    }

    @Nested
    @DisplayName("codigo en blanco: termina sin consultar ningun puerto")
    class CodigoEnBlanco {

        @ParameterizedTest(name = "[{0}]")
        @ValueSource(strings = {"", "   "})
        @DisplayName("codigo vacio o en blanco no toca nada")
        void codigo_vacio_o_en_blanco_no_toca_nada(String codigo) {
            service.execute(new RequestPasswordResetCommand(codigo));

            verifyNoInteractions(employeeLookup, tokenRepository, emailSender);
        }

        @Test
        @DisplayName("codigo nulo no toca nada")
        void codigo_nulo_no_toca_nada() {
            service.execute(new RequestPasswordResetCommand(null));

            verifyNoInteractions(employeeLookup, tokenRepository, emailSender);
        }
    }

    @Nested
    @DisplayName("anti-enumeracion: no revela si el codigo existe")
    class AntiEnumeracion {

        @Test
        @DisplayName("codigo inexistente: no crea token ni envia correo")
        void codigo_inexistente_no_crea_token_ni_envia_correo() {
            when(employeeLookup.findByCode("EMP-404")).thenReturn(Optional.empty());

            service.execute(new RequestPasswordResetCommand("EMP-404"));

            verifyNoInteractions(tokenRepository, emailSender);
        }

        @Test
        @DisplayName("cuenta sin correo verificado: no crea token ni envia correo")
        void cuenta_sin_correo_verificado_no_crea_token_ni_envia_correo() {
            EmployeeAccount noVerificada = new EmployeeAccount(EMPLOYEE_ID, COMPANY_ID, "Ana Ruiz",
                    "ana@vetrina.co", "Clinica Norte", false);
            when(employeeLookup.findByCode("EMP001")).thenReturn(Optional.of(noVerificada));

            service.execute(new RequestPasswordResetCommand("EMP001"));

            verifyNoInteractions(tokenRepository, emailSender);
        }

        @Test
        @DisplayName("recorta espacios del codigo antes de buscarlo")
        void recorta_espacios_del_codigo_antes_de_buscarlo() {
            when(employeeLookup.findByCode("EMP001")).thenReturn(Optional.empty());

            service.execute(new RequestPasswordResetCommand("  EMP001  "));

            verify(employeeLookup).findByCode("EMP001");
        }
    }

    @Nested
    @DisplayName("cuenta elegible: genera, guarda y envia")
    class CuentaElegible {

        @Test
        @DisplayName("invalida los tokens vivos del empleado antes de emitir el nuevo")
        void invalida_los_tokens_vivos_antes_de_emitir_el_nuevo() {
            when(employeeLookup.findByCode("EMP001")).thenReturn(Optional.of(cuentaElegible()));
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(new RequestPasswordResetCommand("EMP001"));

            // La empresa sale de la MISMA lectura que el id (findByCode), pero de una
            // fila distinta a la que se actualiza: el AND afirma que el token es de esa
            // empresa.
            verify(tokenRepository).consumeActiveForEmployee(eq(EMPLOYEE_ID), eq(COMPANY_ID),
                    any(LocalDateTime.class));
        }

        @Test
        @DisplayName("guarda un token cuyo hash coincide con el raw token que se envia por correo")
        void guarda_un_token_con_el_hash_del_que_se_envia() {
            when(employeeLookup.findByCode("EMP001")).thenReturn(Optional.of(cuentaElegible()));
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(new RequestPasswordResetCommand("EMP001"));

            verify(emailSender).send(eq("ana@vetrina.co"), eq("Ana Ruiz"), eq("EMP001"),
                    eq("Clinica Norte"), rawTokenCaptor.capture());
            verify(tokenRepository).save(tokenCaptor.capture());
            PasswordResetToken guardado = tokenCaptor.getValue();
            assertThat(guardado.getTokenHash())
                    .isEqualTo(PasswordResetTokens.hash(rawTokenCaptor.getValue()));
            assertThat(guardado.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(guardado.getCompanyId()).isEqualTo(COMPANY_ID);
            assertThat(guardado.getId()).isNull();
        }

        @Test
        @DisplayName("el vencimiento respeta el ttl configurado")
        void el_vencimiento_respeta_el_ttl_configurado() {
            when(employeeLookup.findByCode("EMP001")).thenReturn(Optional.of(cuentaElegible()));
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(new RequestPasswordResetCommand("EMP001"));

            verify(tokenRepository).save(tokenCaptor.capture());
            // No hay Clock inyectable (deuda anotada en CLAUDE.md): la asercion compara
            // contra una ventana alrededor de now() + ttl en vez de un instante exacto.
            assertThat(tokenCaptor.getValue().getExpiresAt()).isCloseTo(
                    LocalDateTime.now().plusHours(TTL_HOURS), within(10, ChronoUnit.SECONDS));
        }
    }
}
