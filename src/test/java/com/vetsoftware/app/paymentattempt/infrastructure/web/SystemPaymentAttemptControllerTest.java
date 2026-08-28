package com.vetsoftware.app.paymentattempt.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.paymentattempt.application.command.RecordPaymentAttemptCommand;
import com.vetsoftware.app.paymentattempt.application.command.ReschedulePaymentAttemptCommand;
import com.vetsoftware.app.paymentattempt.application.dto.PaymentAttemptDto;
import com.vetsoftware.app.paymentattempt.application.port.in.ListAllPaymentAttemptsUseCase;
import com.vetsoftware.app.paymentattempt.application.port.in.ListDuePaymentAttemptsUseCase;
import com.vetsoftware.app.paymentattempt.application.port.in.RecordPaymentAttemptUseCase;
import com.vetsoftware.app.paymentattempt.application.port.in.ReschedulePaymentAttemptUseCase;
import com.vetsoftware.app.paymentattempt.domain.DeclineKind;
import com.vetsoftware.app.paymentattempt.domain.HardDeclineCannotBeRetriedException;
import com.vetsoftware.app.paymentattempt.domain.RetryBudgetExhaustedException;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja web de la consola, donde <b>si</b> se ve el codigo crudo de la
 * pasarela —es lo que permite revisar despues la traduccion— y donde vive la
 * escritura.
 *
 * <p>
 * Dos cosas que esta clase congela y que ningun test de servicio ve:
 *
 * <ul>
 * <li><b>El corte por defecto de la cola de reintentos sale del {@code Clock}
 * inyectado</b>, no de {@code LocalDateTime.now()} pelado. Es lo que hace que
 * la cola se pueda probar y adelantar en el tiempo; el {@code Clock} fijo de
 * este archivo lo deja afirmable al segundo.</li>
 * <li><b>El presupuesto agotado y el rechazo duro salen 409</b>, no 400 ni 500:
 * el cuerpo es valido y lo que falla es el estado del cobro.</li>
 * </ul>
 */
@WebMvcTest(SystemPaymentAttemptController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcSliceConfig.class, SystemPaymentAttemptControllerTest.RelojFijo.class})
@DisplayName("SystemPaymentAttemptController — contrato HTTP de plataforma")
class SystemPaymentAttemptControllerTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 20, 12, 0, 0);

    /**
     * {@code @TestConfiguration} y no un {@code @MockitoBean Clock}: un reloj es un
     * valor, no un puerto, y ya viene con una implementacion fija que no hay que
     * enseñarle a mentir. Ademas {@code @TestConfiguration} esta meta-anotado con
     * {@code @TestComponent}, asi que no puede colarse en el escaneo de produccion
     * ({@code DOBLE_DE_TEST_NO_ESCANEABLE}).
     *
     * <p>
     * <b>Y va nombrada en el {@code @Import}, que no es redundante.</b> Spring Boot
     * descubre solo las {@code @TestConfiguration} anidadas mirando
     * {@code getDeclaredClasses()} de <em>la clase que se esta ejecutando</em>.
     * Aqui todos los {@code @Test} viven dentro de clases {@code @Nested}, y los
     * {@code getDeclaredClasses()} de {@code RegistroDelIntento} no contienen a
     * {@code RelojFijo}: sin nombrarla, el contexto muere con
     * {@code NoSuchBeanDefinitionException} sobre {@code java.time.Clock}. Medido:
     * son los 11 errores que dio esta clase en la primera pasada.
     */
    @TestConfiguration
    static class RelojFijo {

        @Bean
        Clock clock() {
            return Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RecordPaymentAttemptUseCase recordUseCase;
    @MockitoBean
    private ReschedulePaymentAttemptUseCase rescheduleUseCase;
    @MockitoBean
    private ListDuePaymentAttemptsUseCase listDueUseCase;
    @MockitoBean
    private ListAllPaymentAttemptsUseCase listAllUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Registro del intento")
    class RegistroDelIntento {

        @Test
        @DisplayName("responde 201, enseña el codigo crudo y traslada los nueve campos al command")
        void responde_201_y_traslada_los_nueve_campos() throws Exception {
            when(recordUseCase.execute(any())).thenReturn(unIntento(DeclineKind.SOFT));

            mockMvc.perform(post("/system/payment-attempts").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "billingDocumentId": 8400,
                              "paymentMethodId": 8410,
                              "gateway": "wompi",
                              "requestedAmount": 119000.00,
                              "gatewayDeclineCode": "insufficient_funds",
                              "declineKind": "SOFT",
                              "attemptedAt": "2026-03-05T14:30:15",
                              "nextAttemptAt": "2026-03-08T06:00:00"
                            }
                            """)).andExpect(status().isCreated())
                    // La consola SI ve el codigo: es la otra mitad de la frontera que
                    // el controller del tenant cierra.
                    .andExpect(jsonPath("$.gatewayDeclineCode").value("insufficient_funds"))
                    .andExpect(jsonPath("$.declineKind").value("SOFT"))
                    .andExpect(jsonPath("$.attemptNumber").value(2));

            ArgumentCaptor<RecordPaymentAttemptCommand> command = ArgumentCaptor
                    .forClass(RecordPaymentAttemptCommand.class);
            verify(recordUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.companyId()).isEqualTo(900L);
                assertThat(cmd.billingDocumentId()).isEqualTo(8400L);
                assertThat(cmd.paymentMethodId()).isEqualTo(8410L);
                assertThat(cmd.gateway()).isEqualTo("wompi");
                assertThat(cmd.requestedAmount()).isEqualByComparingTo("119000.00");
                assertThat(cmd.gatewayDeclineCode()).isEqualTo("insufficient_funds");
                assertThat(cmd.declineKind()).isEqualTo(DeclineKind.SOFT);
                // Los dos instantes, distintos, en su hueco: cruzarlos programaria el
                // reintento en el pasado y lo pararia un CHECK de la base.
                assertThat(cmd.attemptedAt()).isEqualTo(LocalDateTime.of(2026, 3, 5, 14, 30, 15));
                assertThat(cmd.nextAttemptAt()).isEqualTo(LocalDateTime.of(2026, 3, 8, 6, 0, 0));
            });
        }

        @Test
        @DisplayName("un fallo propio se acepta sin medio de pago y sin codigo de la pasarela")
        void un_fallo_propio_se_acepta_sin_medio_y_sin_codigo() throws Exception {
            when(recordUseCase.execute(any())).thenReturn(unFalloPropio());

            mockMvc.perform(post("/system/payment-attempts").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "billingDocumentId": 8400,
                              "gateway": "wompi",
                              "requestedAmount": 119000.00,
                              "declineKind": "CONFIGURATION",
                              "attemptedAt": "2026-03-05T14:30:15"
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paymentMethodId").doesNotExist())
                    .andExpect(jsonPath("$.gatewayDeclineCode").doesNotExist())
                    .andExpect(jsonPath("$.declineKind").value("CONFIGURATION"));

            ArgumentCaptor<RecordPaymentAttemptCommand> command = ArgumentCaptor
                    .forClass(RecordPaymentAttemptCommand.class);
            verify(recordUseCase).execute(command.capture());
            // Exigir el medio de pago obligaria a inventarse uno para el caso en que
            // el cobro rebota antes de llegar a usarlo.
            assertThat(command.getValue().paymentMethodId()).isNull();
            assertThat(command.getValue().gatewayDeclineCode()).isNull();
        }

        @Test
        @DisplayName("un cuerpo sin la clase del rechazo sale 400 y no escribe")
        void un_cuerpo_sin_la_clase_del_rechazo_sale_400() throws Exception {
            mockMvc.perform(post("/system/payment-attempts").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "billingDocumentId": 8400,
                              "gateway": "wompi",
                              "requestedAmount": 119000.00,
                              "attemptedAt": "2026-03-05T14:30:15"
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[0].field").value("declineKind"));

            verifyNoInteractions(recordUseCase);
        }

        @Test
        @DisplayName("agotar el presupuesto de reintentos sale 409 con su codigo")
        void agotar_el_presupuesto_sale_409() throws Exception {
            when(recordUseCase.execute(any())).thenThrow(new RetryBudgetExhaustedException(8400L,
                    com.vetsoftware.app.paymentattempt.domain.PaymentAttempt.MAX_SOFT_ATTEMPTS));

            mockMvc.perform(post("/system/payment-attempts").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "billingDocumentId": 8400,
                              "paymentMethodId": 8410,
                              "gateway": "wompi",
                              "requestedAmount": 119000.00,
                              "gatewayDeclineCode": "insufficient_funds",
                              "declineKind": "SOFT",
                              "attemptedAt": "2026-03-05T14:30:15"
                            }
                            """)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("RETRY_BUDGET_EXHAUSTED"))
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers
                            .containsString("4 chargeable attempts already spent")));
        }
    }

    @Nested
    @DisplayName("Reprogramacion")
    class Reprogramacion {

        @Test
        @DisplayName("traslada el id de la ruta, la empresa del parametro y la fecha del cuerpo")
        void traslada_id_empresa_y_fecha() throws Exception {
            when(rescheduleUseCase.execute(any())).thenReturn(unIntento(DeclineKind.SOFT));

            mockMvc.perform(patch("/system/payment-attempts/{id}/schedule", 31L)
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nextAttemptAt\": \"2026-03-08T06:00:00\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nextAttemptAt").value("2026-03-08T06:00:00"));

            ArgumentCaptor<ReschedulePaymentAttemptCommand> command = ArgumentCaptor
                    .forClass(ReschedulePaymentAttemptCommand.class);
            verify(rescheduleUseCase).execute(command.capture());
            // Los dos Long del command son distintos entre si a proposito: si el
            // controller los cruzara, reprogramaria el intento 900 de la empresa 31.
            assertThat(command.getValue()).isEqualTo(new ReschedulePaymentAttemptCommand(31L, 900L,
                    LocalDateTime.of(2026, 3, 8, 6, 0, 0)));
        }

        @Test
        @DisplayName("reprogramar sin fecha sale 400 y no toca el intento")
        void reprogramar_sin_fecha_sale_400() throws Exception {
            mockMvc.perform(
                    patch("/system/payment-attempts/{id}/schedule", 31L).param("companyId", "900")
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("nextAttemptAt"));

            verifyNoInteractions(rescheduleUseCase);
        }

        @Test
        @DisplayName("reprogramar un rechazo duro sale 409: se pide medio de pago nuevo")
        void reprogramar_un_rechazo_duro_sale_409() throws Exception {
            when(rescheduleUseCase.execute(any()))
                    .thenThrow(new HardDeclineCannotBeRetriedException(31L));

            mockMvc.perform(patch("/system/payment-attempts/{id}/schedule", 31L)
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nextAttemptAt\": \"2026-03-08T06:00:00\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("HARD_DECLINE_CANNOT_BE_RETRIED"))
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers
                            .containsString("a new payment method is required")));
        }
    }

    @Nested
    @DisplayName("Cola de reintentos")
    class ColaDeReintentos {

        @Test
        @DisplayName("sin fecha de corte usa el instante del reloj inyectado")
        void sin_fecha_de_corte_usa_el_reloj_inyectado() throws Exception {
            when(listDueUseCase.listDue(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(unIntento(DeclineKind.SOFT)), 0, 20, 1L));

            mockMvc.perform(get("/system/payment-attempts/due")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(31));

            // Con un now() pelado este valor seria irreproducible y el caso tendria
            // que aflojarse a un any(). El Clock inyectado lo deja al segundo.
            verify(listDueUseCase).listDue(AHORA, 0, 20);
        }

        @Test
        @DisplayName("con fecha de corte usa la que llega y no la del reloj")
        void con_fecha_de_corte_usa_la_que_llega() throws Exception {
            when(listDueUseCase.listDue(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.empty(1, 3));

            mockMvc.perform(
                    get("/system/payment-attempts/due").param("dueBefore", "2026-04-01T00:00:00")
                            .param("page", "1").param("pageSize", "3"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.pageSize").value(3));

            verify(listDueUseCase).listDue(LocalDateTime.of(2026, 4, 1, 0, 0, 0), 1, 3);
        }
    }

    @Nested
    @DisplayName("Barrido de plataforma")
    class BarridoDePlataforma {

        @Test
        @DisplayName("sin companyId barre todas las empresas pasando null")
        void sin_company_id_barre_todas_las_empresas() throws Exception {
            when(listAllUseCase.listAll(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(unIntento(DeclineKind.HARD)), 0, 20, 1L));

            mockMvc.perform(get("/system/payment-attempts")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].declineKind").value("HARD"))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(listAllUseCase).listAll(null, 0, 20);
        }

        @Test
        @DisplayName("con companyId acota el barrido a esa empresa")
        void con_company_id_acota_el_barrido() throws Exception {
            when(listAllUseCase.listAll(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/system/payment-attempts").param("companyId", "901"))
                    .andExpect(status().isOk());

            verify(listAllUseCase).listAll(901L, 0, 20);
        }
    }

    private static PaymentAttemptDto unIntento(DeclineKind clase) {
        return new PaymentAttemptDto(31L, 900L, 8400L, 8410L, 2, "wompi",
                new BigDecimal("119000.00"), "insufficient_funds", clase,
                LocalDateTime.of(2026, 3, 5, 14, 30, 15), LocalDateTime.of(2026, 3, 8, 6, 0, 0),
                LocalDateTime.of(2026, 3, 5, 14, 30, 20), 3L);
    }

    private static PaymentAttemptDto unFalloPropio() {
        return new PaymentAttemptDto(32L, 900L, 8400L, null, 1, "wompi",
                new BigDecimal("119000.00"), null, DeclineKind.CONFIGURATION,
                LocalDateTime.of(2026, 3, 5, 14, 30, 15), null,
                LocalDateTime.of(2026, 3, 5, 14, 30, 20), 0L);
    }
}
