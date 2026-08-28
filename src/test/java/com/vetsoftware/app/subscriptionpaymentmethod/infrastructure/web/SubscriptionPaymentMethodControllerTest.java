package com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpaymentmethod.application.command.RegisterSubscriptionPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.command.RevokeSubscriptionPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.command.SetDefaultPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.FindSubscriptionPaymentMethodUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.ListSubscriptionPaymentMethodsUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.RegisterSubscriptionPaymentMethodUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.RevokeSubscriptionPaymentMethodUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.SetDefaultPaymentMethodUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.MandateAlreadyRevokedException;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.MandateStatus;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.PaymentMethodKind;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.PaymentMethodTokenAlreadyRegisteredException;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.SubscriptionPaymentMethodNotFoundException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja web del lado del tenant.
 *
 * <p>
 * <strong>Dos cosas que solo se ven aqui.</strong> La primera, que el
 * {@code companyId} que llega al caso de uso sale del contexto de autorizacion
 * y no del cuerpo: el request ni siquiera declara el campo, asi que el unico
 * sitio donde puede colarse una empresa ajena es este mapeo. La segunda, que la
 * response <strong>no lleva el testigo de la pasarela</strong>: es la
 * credencial con la que se cobra, y el dia que alguien lo anada «por comodidad»
 * estos casos se ponen rojos.
 */
@WebMvcTest(SubscriptionPaymentMethodController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SubscriptionPaymentMethodController — contrato HTTP")
class SubscriptionPaymentMethodControllerTest {

    /** Distinto del de {@link WebMvcSliceConfig} para que se vea de donde sale. */
    private static final Long EMPRESA_DEL_CONTEXTO = 77L;

    private static final String ALTA_VALIDA = """
            {
              "methodKind": "CARD",
              "gateway": "wompi",
              "token": "tok_test_7f3a",
              "brand": "VISA",
              "lastFour": "4242",
              "expiresOn": "2027-09-30",
              "mandateEvidence": "acta-mandato-2026-0447",
              "authorizedAt": "2026-03-04T08:15:30"
            }
            """;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RegisterSubscriptionPaymentMethodUseCase registerUseCase;
    @MockitoBean
    private RevokeSubscriptionPaymentMethodUseCase revokeUseCase;
    @MockitoBean
    private SetDefaultPaymentMethodUseCase setDefaultUseCase;
    @MockitoBean
    private FindSubscriptionPaymentMethodUseCase findUseCase;
    @MockitoBean
    private ListSubscriptionPaymentMethodsUseCase listUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Registro")
    class Registro {

        @Test
        @DisplayName("registra el medio con la empresa del contexto y no con una del cliente")
        void registra_el_medio_con_la_empresa_del_contexto() throws Exception {
            when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_CONTEXTO);
            when(registerUseCase.execute(any())).thenReturn(activa());

            mockMvc.perform(post("/subscription-payment-methods")
                    .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(31))
                    .andExpect(jsonPath("$.methodKind").value("CARD"))
                    .andExpect(jsonPath("$.gateway").value("wompi"))
                    .andExpect(jsonPath("$.brand").value("VISA"))
                    .andExpect(jsonPath("$.lastFour").value("4242"))
                    .andExpect(jsonPath("$.mandateStatus").value("ACTIVE"))
                    .andExpect(jsonPath("$.defaultMethod").value(true));

            ArgumentCaptor<RegisterSubscriptionPaymentMethodCommand> comando = ArgumentCaptor
                    .forClass(RegisterSubscriptionPaymentMethodCommand.class);
            verify(registerUseCase).execute(comando.capture());
            assertThat(comando.getValue().companyId()).isEqualTo(EMPRESA_DEL_CONTEXTO);
            assertThat(comando.getValue().methodKind()).isEqualTo(PaymentMethodKind.CARD);
            assertThat(comando.getValue().gateway()).isEqualTo("wompi");
            assertThat(comando.getValue().token()).isEqualTo("tok_test_7f3a");
            assertThat(comando.getValue().lastFour()).isEqualTo("4242");
            assertThat(comando.getValue().expiresOn()).isEqualTo(LocalDate.of(2027, 9, 30));
            assertThat(comando.getValue().authorizedAt())
                    .isEqualTo(LocalDateTime.of(2026, 3, 4, 8, 15, 30));
        }

        @Test
        @DisplayName("la respuesta nunca lleva el testigo de la pasarela ni nada parecido a un"
                + " numero de tarjeta")
        void la_respuesta_nunca_lleva_el_testigo_de_la_pasarela() throws Exception {
            when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_CONTEXTO);
            when(registerUseCase.execute(any())).thenReturn(activa());

            mockMvc.perform(post("/subscription-payment-methods")
                    .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.token").doesNotExist())
                    .andExpect(jsonPath("$.cardNumber").doesNotExist())
                    .andExpect(jsonPath("$.pan").doesNotExist())
                    // Lo que si sale es lo justo para reconocer cual es.
                    .andExpect(jsonPath("$.brand").value("VISA"))
                    .andExpect(jsonPath("$.lastFour").value("4242"));
        }

        @Test
        @DisplayName("rechaza el alta sin la constancia del mandato")
        void rechaza_el_alta_sin_la_constancia_del_mandato() throws Exception {
            String sinConstancia = """
                    {
                      "methodKind": "CARD",
                      "gateway": "wompi",
                      "token": "tok_test_7f3a",
                      "brand": "VISA",
                      "lastFour": "4242",
                      "expiresOn": "2027-09-30",
                      "mandateEvidence": "  ",
                      "authorizedAt": "2026-03-04T08:15:30"
                    }
                    """;

            mockMvc.perform(post("/subscription-payment-methods")
                    .contentType(MediaType.APPLICATION_JSON).content(sinConstancia))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[0].field").value("mandateEvidence"));
        }

        @Test
        @DisplayName("responde 409 cuando el testigo ya estaba registrado")
        void responde_409_cuando_el_testigo_ya_estaba_registrado() throws Exception {
            when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_CONTEXTO);
            when(registerUseCase.execute(any()))
                    .thenThrow(new PaymentMethodTokenAlreadyRegisteredException("wompi"));

            mockMvc.perform(post("/subscription-payment-methods")
                    .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PAYMENT_METHOD_TOKEN_ALREADY_REGISTERED"));
        }
    }

    @Nested
    @DisplayName("Revocacion")
    class Revocacion {

        @Test
        @DisplayName("revoca con el motivo del cuerpo y la empresa del contexto")
        void revoca_con_el_motivo_del_cuerpo_y_la_empresa_del_contexto() throws Exception {
            when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_CONTEXTO);
            when(revokeUseCase.execute(any())).thenReturn(revocada());

            mockMvc.perform(patch("/subscription-payment-methods/{id}/revocation", 31)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reason\":\"El cliente cambio de banco\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mandateStatus").value("REVOKED"))
                    .andExpect(jsonPath("$.revokedReason").value("El cliente cambio de banco"))
                    .andExpect(jsonPath("$.token").doesNotExist());

            ArgumentCaptor<RevokeSubscriptionPaymentMethodCommand> comando = ArgumentCaptor
                    .forClass(RevokeSubscriptionPaymentMethodCommand.class);
            verify(revokeUseCase).execute(comando.capture());
            assertThat(comando.getValue().id()).isEqualTo(31L);
            assertThat(comando.getValue().companyId()).isEqualTo(EMPRESA_DEL_CONTEXTO);
            assertThat(comando.getValue().reason()).isEqualTo("El cliente cambio de banco");
        }

        @Test
        @DisplayName("responde 409 cuando el mandato ya estaba revocado")
        void responde_409_cuando_el_mandato_ya_estaba_revocado() throws Exception {
            when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_CONTEXTO);
            when(revokeUseCase.execute(any())).thenThrow(new MandateAlreadyRevokedException(31L));

            mockMvc.perform(patch("/subscription-payment-methods/{id}/revocation", 31)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reason\":\"Otro motivo\"}")).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("MANDATE_ALREADY_REVOKED"));
        }

        @Test
        @DisplayName("rechaza la revocacion sin motivo")
        void rechaza_la_revocacion_sin_motivo() throws Exception {
            mockMvc.perform(patch("/subscription-payment-methods/{id}/revocation", 31)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[0].field").value("reason"));
        }
    }

    @Nested
    @DisplayName("Predeterminado")
    class Predeterminado {

        @Test
        @DisplayName("marca el predeterminado sin cuerpo y con la empresa del contexto")
        void marca_el_predeterminado_sin_cuerpo_y_con_la_empresa_del_contexto() throws Exception {
            when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_CONTEXTO);
            when(setDefaultUseCase.execute(any())).thenReturn(activa());

            mockMvc.perform(patch("/subscription-payment-methods/{id}/default", 31))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.defaultMethod").value(true))
                    .andExpect(jsonPath("$.token").doesNotExist());

            ArgumentCaptor<SetDefaultPaymentMethodCommand> comando = ArgumentCaptor
                    .forClass(SetDefaultPaymentMethodCommand.class);
            verify(setDefaultUseCase).execute(comando.capture());
            assertThat(comando.getValue().id()).isEqualTo(31L);
            assertThat(comando.getValue().companyId()).isEqualTo(EMPRESA_DEL_CONTEXTO);
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("carga el medio acotado por la empresa del contexto")
        void carga_el_medio_acotado_por_la_empresa_del_contexto() throws Exception {
            when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_CONTEXTO);
            when(findUseCase.findById(31L, EMPRESA_DEL_CONTEXTO)).thenReturn(activa());

            mockMvc.perform(get("/subscription-payment-methods/{id}", 31))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(31))
                    .andExpect(jsonPath("$.companyId").value(77))
                    .andExpect(jsonPath("$.expiresOn").value("2027-09-30"))
                    .andExpect(jsonPath("$.mandateEvidence").value("acta-mandato-2026-0447"))
                    .andExpect(jsonPath("$.token").doesNotExist());

            verify(findUseCase).findById(31L, EMPRESA_DEL_CONTEXTO);
        }

        @Test
        @DisplayName("responde 404 cuando el medio no existe o no es de la empresa")
        void responde_404_cuando_el_medio_no_es_de_la_empresa() throws Exception {
            when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_CONTEXTO);
            when(findUseCase.findById(31L, EMPRESA_DEL_CONTEXTO))
                    .thenThrow(new SubscriptionPaymentMethodNotFoundException(31L));

            mockMvc.perform(get("/subscription-payment-methods/{id}", 31))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("SUBSCRIPTION_PAYMENT_METHOD_NOT_FOUND"));
        }

        @Test
        @DisplayName("lista solo los medios de la empresa del contexto")
        void lista_solo_los_medios_de_la_empresa_del_contexto() throws Exception {
            when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_CONTEXTO);
            when(listUseCase.listByCompany(EMPRESA_DEL_CONTEXTO, 2, 5))
                    .thenReturn(PageResult.of(List.of(activa()), 2, 5, 11L));

            mockMvc.perform(
                    get("/subscription-payment-methods").param("page", "2").param("pageSize", "5"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(31))
                    .andExpect(jsonPath("$.content[0].lastFour").value("4242"))
                    .andExpect(jsonPath("$.content[0].token").doesNotExist())
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(11));

            verify(listUseCase).listByCompany(EMPRESA_DEL_CONTEXTO, 2, 5);
        }
    }

    private static SubscriptionPaymentMethodDto activa() {
        return new SubscriptionPaymentMethodDto(31L, EMPRESA_DEL_CONTEXTO, PaymentMethodKind.CARD,
                "wompi", "VISA", "4242", LocalDate.of(2027, 9, 30), MandateStatus.ACTIVE,
                "acta-mandato-2026-0447", LocalDateTime.of(2026, 3, 4, 8, 15, 30), null, null, true,
                LocalDateTime.of(2026, 4, 5, 13, 20, 0), 0L);
    }

    private static SubscriptionPaymentMethodDto revocada() {
        return new SubscriptionPaymentMethodDto(31L, EMPRESA_DEL_CONTEXTO, PaymentMethodKind.CARD,
                "wompi", "VISA", "4242", LocalDate.of(2027, 9, 30), MandateStatus.REVOKED,
                "acta-mandato-2026-0447", LocalDateTime.of(2026, 3, 4, 8, 15, 30),
                LocalDateTime.of(2026, 5, 6, 21, 45, 10), "El cliente cambio de banco", false,
                LocalDateTime.of(2026, 4, 5, 13, 20, 0), 1L);
    }
}
