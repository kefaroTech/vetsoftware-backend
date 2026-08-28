package com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpaymentmethod.application.command.ExpireSubscriptionPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.ExpireSubscriptionPaymentMethodUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.ListAllSubscriptionPaymentMethodsUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.ListExpiringPaymentMethodsUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.MandateAlreadyRevokedException;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.MandateStatus;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.PaymentMethodKind;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja web del lado de plataforma: el barrido de tarjetas por vencer, la
 * consulta cross-tenant de la consola y la constatacion de que un mandato
 * caduco.
 *
 * <p>
 * Aqui la empresa <strong>si</strong> viaja como parametro —quien llama es la
 * plataforma y no tiene una propia—, asi que lo que hay que congelar es que
 * llega tal cual al caso de uso. Y la response sigue sin llevar el testigo de
 * la pasarela: que la consola sea interna no lo convierte en un dato de
 * presentacion.
 */
@WebMvcTest(SystemSubscriptionPaymentMethodController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemSubscriptionPaymentMethodController — contrato HTTP")
class SystemSubscriptionPaymentMethodControllerTest {

    private static final Long EMPRESA = 901L;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ListExpiringPaymentMethodsUseCase listExpiringUseCase;
    @MockitoBean
    private ListAllSubscriptionPaymentMethodsUseCase listAllUseCase;
    @MockitoBean
    private ExpireSubscriptionPaymentMethodUseCase expireUseCase;

    @Nested
    @DisplayName("Barrido de vencimientos")
    class Vencimientos {

        @Test
        @DisplayName("expone las tarjetas que caducan antes de la fecha de corte, de todas las"
                + " clinicas")
        void expone_las_tarjetas_que_caducan_antes_de_la_fecha_de_corte() throws Exception {
            when(listExpiringUseCase.listExpiring(LocalDate.of(2026, 12, 1), 2, 5))
                    .thenReturn(PageResult.of(List.of(activa()), 2, 5, 11L));

            mockMvc.perform(get("/system/subscription-payment-methods/expiring")
                    .param("before", "2026-12-01").param("page", "2").param("pageSize", "5"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(31))
                    .andExpect(jsonPath("$.content[0].companyId").value(901))
                    .andExpect(jsonPath("$.content[0].brand").value("VISA"))
                    .andExpect(jsonPath("$.content[0].lastFour").value("4242"))
                    .andExpect(jsonPath("$.content[0].expiresOn").value("2027-09-30"))
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(11))
                    .andExpect(jsonPath("$.totalPages").value(3));

            verify(listExpiringUseCase).listExpiring(LocalDate.of(2026, 12, 1), 2, 5);
        }

        @Test
        @DisplayName("el barrido tampoco expone el testigo de la pasarela")
        void el_barrido_tampoco_expone_el_testigo_de_la_pasarela() throws Exception {
            when(listExpiringUseCase.listExpiring(LocalDate.of(2026, 12, 1), 0, 20))
                    .thenReturn(PageResult.of(List.of(activa()), 0, 20, 1L));

            mockMvc.perform(get("/system/subscription-payment-methods/expiring").param("before",
                    "2026-12-01")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].token").doesNotExist())
                    .andExpect(jsonPath("$.content[0].cardNumber").doesNotExist())
                    .andExpect(jsonPath("$.content[0].pan").doesNotExist());
        }

        @Test
        @DisplayName("rechaza el barrido sin fecha de corte")
        void rechaza_el_barrido_sin_fecha_de_corte() throws Exception {
            mockMvc.perform(get("/system/subscription-payment-methods/expiring"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Consulta cross-tenant")
    class ConsultaCrossTenant {

        @Test
        @DisplayName("lista el parque completo cuando la consola no filtra por empresa")
        void lista_el_parque_completo_cuando_la_consola_no_filtra_por_empresa() throws Exception {
            when(listAllUseCase.listAll(null, 0, 20))
                    .thenReturn(PageResult.of(List.of(activa()), 0, 20, 1L));

            mockMvc.perform(get("/system/subscription-payment-methods")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(31))
                    .andExpect(jsonPath("$.content[0].mandateStatus").value("ACTIVE"))
                    .andExpect(jsonPath("$.content[0].token").doesNotExist())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.pageSize").value(20));

            verify(listAllUseCase).listAll(null, 0, 20);
        }

        @Test
        @DisplayName("pasa la empresa al caso de uso cuando la consola la indica")
        void pasa_la_empresa_al_caso_de_uso_cuando_la_consola_la_indica() throws Exception {
            when(listAllUseCase.listAll(EMPRESA, 1, 3)).thenReturn(PageResult.empty(1, 3));

            mockMvc.perform(get("/system/subscription-payment-methods").param("companyId", "901")
                    .param("page", "1").param("pageSize", "3")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.pageSize").value(3));

            verify(listAllUseCase).listAll(EMPRESA, 1, 3);
        }
    }

    @Nested
    @DisplayName("Caducidad del mandato")
    class Caducidad {

        @Test
        @DisplayName("constata la caducidad con el id de la ruta y la empresa del parametro")
        void constata_la_caducidad_con_el_id_de_la_ruta_y_la_empresa_del_parametro()
                throws Exception {
            when(expireUseCase.execute(any())).thenReturn(caducada());

            mockMvc.perform(patch("/system/subscription-payment-methods/{id}/expiration", 31)
                    .param("companyId", "901")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(31))
                    .andExpect(jsonPath("$.companyId").value(901))
                    .andExpect(jsonPath("$.mandateStatus").value("EXPIRED"))
                    // Caducar no es revocar: no hay fecha ni motivo de revocacion.
                    .andExpect(jsonPath("$.revokedAt").doesNotExist())
                    .andExpect(jsonPath("$.revokedReason").doesNotExist())
                    .andExpect(jsonPath("$.token").doesNotExist());

            ArgumentCaptor<ExpireSubscriptionPaymentMethodCommand> comando = ArgumentCaptor
                    .forClass(ExpireSubscriptionPaymentMethodCommand.class);
            verify(expireUseCase).execute(comando.capture());
            assertThat(comando.getValue().id()).isEqualTo(31L);
            assertThat(comando.getValue().companyId()).isEqualTo(EMPRESA);
        }

        @Test
        @DisplayName("responde 404 cuando el medio no existe o no es de esa empresa")
        void responde_404_cuando_el_medio_no_existe() throws Exception {
            when(expireUseCase.execute(any()))
                    .thenThrow(new SubscriptionPaymentMethodNotFoundException(31L));

            mockMvc.perform(patch("/system/subscription-payment-methods/{id}/expiration", 31)
                    .param("companyId", "901")).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("SUBSCRIPTION_PAYMENT_METHOD_NOT_FOUND"));
        }

        @Test
        @DisplayName("responde 409 cuando el mandato ya lo habia revocado el cliente")
        void responde_409_cuando_el_mandato_ya_lo_habia_revocado_el_cliente() throws Exception {
            when(expireUseCase.execute(any())).thenThrow(new MandateAlreadyRevokedException(31L));

            mockMvc.perform(patch("/system/subscription-payment-methods/{id}/expiration", 31)
                    .param("companyId", "901")).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("MANDATE_ALREADY_REVOKED"));
        }

        @Test
        @DisplayName("rechaza constatar la caducidad sin la empresa")
        void rechaza_constatar_la_caducidad_sin_la_empresa() throws Exception {
            mockMvc.perform(patch("/system/subscription-payment-methods/{id}/expiration", 31))
                    .andExpect(status().isBadRequest());
        }
    }

    private static SubscriptionPaymentMethodDto activa() {
        return new SubscriptionPaymentMethodDto(31L, EMPRESA, PaymentMethodKind.CARD, "wompi",
                "VISA", "4242", LocalDate.of(2027, 9, 30), MandateStatus.ACTIVE,
                "acta-mandato-2026-0447", LocalDateTime.of(2026, 3, 4, 8, 15, 30), null, null, true,
                LocalDateTime.of(2026, 4, 5, 13, 20, 0), 0L);
    }

    private static SubscriptionPaymentMethodDto caducada() {
        return new SubscriptionPaymentMethodDto(31L, EMPRESA, PaymentMethodKind.CARD, "wompi",
                "VISA", "4242", LocalDate.of(2026, 1, 31), MandateStatus.EXPIRED,
                "acta-mandato-2026-0447", LocalDateTime.of(2026, 3, 4, 8, 15, 30), null, null,
                false, LocalDateTime.of(2026, 4, 5, 13, 20, 0), 1L);
    }
}
