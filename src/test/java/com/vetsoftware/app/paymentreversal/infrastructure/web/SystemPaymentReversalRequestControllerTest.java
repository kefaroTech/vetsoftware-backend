package com.vetsoftware.app.paymentreversal.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.paymentreversal.application.command.AcknowledgeReversalRequestCommand;
import com.vetsoftware.app.paymentreversal.application.command.OpenPaymentReversalRequestCommand;
import com.vetsoftware.app.paymentreversal.application.command.OpposeReversalRequestCommand;
import com.vetsoftware.app.paymentreversal.application.command.ResolveReversalRequestCommand;
import com.vetsoftware.app.paymentreversal.application.dto.PaymentReversalRequestDto;
import com.vetsoftware.app.paymentreversal.application.port.in.AcknowledgeReversalRequestUseCase;
import com.vetsoftware.app.paymentreversal.application.port.in.ListAllPaymentReversalRequestsUseCase;
import com.vetsoftware.app.paymentreversal.application.port.in.ListExpiringReversalRequestsUseCase;
import com.vetsoftware.app.paymentreversal.application.port.in.OpenPaymentReversalRequestUseCase;
import com.vetsoftware.app.paymentreversal.application.port.in.OpposeReversalRequestUseCase;
import com.vetsoftware.app.paymentreversal.application.port.in.ResolveReversalRequestUseCase;
import com.vetsoftware.app.paymentreversal.domain.ConsumerDetermination;
import com.vetsoftware.app.paymentreversal.domain.OppositionGround;
import com.vetsoftware.app.paymentreversal.domain.ReversalCausal;
import com.vetsoftware.app.paymentreversal.domain.ReversalOrigin;
import com.vetsoftware.app.paymentreversal.domain.ReversalOutcome;
import com.vetsoftware.app.paymentreversal.domain.ReversalRequestAlreadyExistsException;
import com.vetsoftware.app.paymentreversal.domain.ReversalRequestAlreadyResolvedException;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
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
 * La cara de plataforma: aqui viven <b>todas</b> las escrituras del expediente.
 *
 * <p>
 * <b>Lo que estos casos congelan es el armado del command, que es donde se
 * pierden las fechas.</b> Un {@code OpenPaymentReversalRequestCommand} lleva
 * tres {@code LocalDateTime} seguidos —conocimiento, queja y aviso al emisor— y
 * un cuarto detras; intercambiar dos en la lista de argumentos del controller
 * compila, responde 201 y solo se nota el dia que hay que alegar que la
 * reclamacion llego fuera de plazo. Por eso cada caso captura el command y
 * afirma <b>cada</b> fecha con un valor distinto y reconocible, en vez de
 * conformarse con el codigo de estado.
 *
 * <p>
 * <b>Y la empresa viaja como {@code @RequestParam}, jamas dentro del
 * cuerpo.</b> Un principal SYSTEM no tiene empresa propia, asi que aqui hay que
 * elegirla; pero un {@code companyId} escrito en el JSON convertiria
 * {@code @authz.isMyCompany(#command.companyId)} en una comparacion consigo
 * mismo. El caso
 * {@link Apertura#la_empresa_viaja_en_el_query_param_y_no_en_el_cuerpo} manda
 * un {@code companyId} dentro del cuerpo <em>a proposito</em> y comprueba que
 * el que llega al caso de uso es el del query param.
 */
@WebMvcTest(SystemPaymentReversalRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemPaymentReversalRequestController — contrato HTTP")
class SystemPaymentReversalRequestControllerTest {

    private static final Long EMPRESA = 900L;
    private static final Long OTRA_EMPRESA = 901L;

    private static final String CUERPO_APERTURA = """
            {
              "paymentId": 8001,
              "origin": "CONSUMER_CLAIM",
              "causal": "PRODUCT_NOT_RECEIVED",
              "consumerDetermination": "CONSUMER",
              "consumerBecameAwareAt": "2026-03-01T08:15:11",
              "claimReceivedAt": "2026-03-05T09:26:22",
              "issuerNotifiedAt": "2026-03-07T10:37:33",
              "claimEvidenceRef": "EV-QUEJA-0001",
              "deadlineAt": "2026-04-19T11:48:44"
            }
            """;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private OpenPaymentReversalRequestUseCase openUseCase;
    @MockitoBean
    private AcknowledgeReversalRequestUseCase acknowledgeUseCase;
    @MockitoBean
    private OpposeReversalRequestUseCase opposeUseCase;
    @MockitoBean
    private ResolveReversalRequestUseCase resolveUseCase;
    @MockitoBean
    private ListExpiringReversalRequestsUseCase listExpiringUseCase;
    @MockitoBean
    private ListAllPaymentReversalRequestsUseCase listAllUseCase;

    @Nested
    @DisplayName("Apertura")
    class Apertura {

        @Test
        @DisplayName("abre el expediente con 201 y traslada las tres fechas y el plazo a su sitio"
                + " del command, sin intercambiarlos")
        void abre_el_expediente_y_traslada_cada_fecha_a_su_sitio() throws Exception {
            when(openUseCase.execute(any())).thenReturn(expediente());

            mockMvc.perform(post("/system/payment-reversal-requests").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_APERTURA))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(77))
                    .andExpect(jsonPath("$.origin").value("CONSUMER_CLAIM"))
                    .andExpect(jsonPath("$.claimReceivedAt").value("2026-03-05T09:26:22"))
                    .andExpect(jsonPath("$.deadlineAt").value("2026-04-19T11:48:44"));

            ArgumentCaptor<OpenPaymentReversalRequestCommand> comando = ArgumentCaptor
                    .forClass(OpenPaymentReversalRequestCommand.class);
            verify(openUseCase).execute(comando.capture());
            assertThat(comando.getValue()).isEqualTo(new OpenPaymentReversalRequestCommand(EMPRESA,
                    8_001L, ReversalOrigin.CONSUMER_CLAIM, ReversalCausal.PRODUCT_NOT_RECEIVED,
                    ConsumerDetermination.CONSUMER, LocalDateTime.of(2026, 3, 1, 8, 15, 11),
                    LocalDateTime.of(2026, 3, 5, 9, 26, 22),
                    LocalDateTime.of(2026, 3, 7, 10, 37, 33), "EV-QUEJA-0001",
                    LocalDateTime.of(2026, 4, 19, 11, 48, 44)));
        }

        @Test
        @DisplayName("la empresa viaja en el query param: un companyId escrito en el cuerpo no"
                + " llega al caso de uso")
        void la_empresa_viaja_en_el_query_param_y_no_en_el_cuerpo() throws Exception {
            when(openUseCase.execute(any())).thenReturn(expediente());

            mockMvc.perform(
                    post("/system/payment-reversal-requests").param("companyId", "900")
                            .contentType(MediaType.APPLICATION_JSON).content(CUERPO_APERTURA
                                    .replaceFirst("\\{", "{\n  \"companyId\": 901,")))
                    .andExpect(status().isCreated());

            ArgumentCaptor<OpenPaymentReversalRequestCommand> comando = ArgumentCaptor
                    .forClass(OpenPaymentReversalRequestCommand.class);
            verify(openUseCase).execute(comando.capture());
            assertThat(comando.getValue().companyId()).isEqualTo(EMPRESA);
        }

        @Test
        @DisplayName("sin el pago que se reversa responde 400 y no llega al caso de uso")
        void sin_el_pago_que_se_reversa_responde_400() throws Exception {
            mockMvc.perform(post("/system/payment-reversal-requests").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_APERTURA.replace("\"paymentId\": 8001,", "")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

            verifyNoInteractions(openUseCase);
        }

        @Test
        @DisplayName("un pago que ya tiene expediente responde 409: una reversion por pago")
        void un_pago_que_ya_tiene_expediente_responde_409() throws Exception {
            when(openUseCase.execute(any()))
                    .thenThrow(new ReversalRequestAlreadyExistsException(8_001L));

            mockMvc.perform(post("/system/payment-reversal-requests").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_APERTURA))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("REVERSAL_REQUEST_ALREADY_EXISTS"));
        }
    }

    @Nested
    @DisplayName("Instruccion del expediente")
    class Instruccion {

        @Test
        @DisplayName("acusa la reclamacion trasladando id, empresa y constancia al command")
        void acusa_la_reclamacion_trasladando_la_constancia() throws Exception {
            when(acknowledgeUseCase.execute(any())).thenReturn(expediente());

            mockMvc.perform(patch("/system/payment-reversal-requests/77/acknowledgement")
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"acknowledgementRef\":\"ACU-0001\"}")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.acknowledgementRef").value("ACU-0001"));

            ArgumentCaptor<AcknowledgeReversalRequestCommand> comando = ArgumentCaptor
                    .forClass(AcknowledgeReversalRequestCommand.class);
            verify(acknowledgeUseCase).execute(comando.capture());
            assertThat(comando.getValue())
                    .isEqualTo(new AcknowledgeReversalRequestCommand(77L, EMPRESA, "ACU-0001"));
        }

        @Test
        @DisplayName("una constancia en blanco responde 400: un acuse sin referencia no se puede"
                + " exhibir")
        void una_constancia_en_blanco_responde_400() throws Exception {
            mockMvc.perform(patch("/system/payment-reversal-requests/77/acknowledgement")
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"acknowledgementRef\":\"   \"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(acknowledgeUseCase);
        }

        @Test
        @DisplayName("se opone con motivo tasado y constancia: los dos llegan al command")
        void se_opone_con_motivo_tasado_y_constancia() throws Exception {
            when(opposeUseCase.execute(any())).thenReturn(expediente());

            mockMvc.perform(patch("/system/payment-reversal-requests/77/opposition")
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON).content("""
                            {"ground":"CAUSAL_NOT_REPORTED",
                             "oppositionEvidenceRef":"EV-OPOSICION-0001"}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.oppositionGround").value("CAUSAL_NOT_REPORTED"));

            ArgumentCaptor<OpposeReversalRequestCommand> comando = ArgumentCaptor
                    .forClass(OpposeReversalRequestCommand.class);
            verify(opposeUseCase).execute(comando.capture());
            assertThat(comando.getValue()).isEqualTo(new OpposeReversalRequestCommand(77L, EMPRESA,
                    OppositionGround.CAUSAL_NOT_REPORTED, "EV-OPOSICION-0001"));
        }

        @Test
        @DisplayName("una oposicion sin constancia responde 400: no es una oposicion, es una"
                + " afirmacion propia")
        void una_oposicion_sin_constancia_responde_400() throws Exception {
            mockMvc.perform(patch("/system/payment-reversal-requests/77/opposition")
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ground\":\"CAUSAL_NOT_REPORTED\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(opposeUseCase);
        }

        @Test
        @DisplayName("resuelve con el desenlace, el importe aplicado y la devolucion enlazada")
        void resuelve_con_el_importe_aplicado_y_la_devolucion_enlazada() throws Exception {
            when(resolveUseCase.execute(any())).thenReturn(expediente());

            mockMvc.perform(patch("/system/payment-reversal-requests/77/outcome")
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON).content("""
                            {"outcome":"PARTIALLY_ACCEPTED","appliedAmount":137500.75,
                             "resultingRefundId":4400}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.appliedAmount").value(137500.75));

            ArgumentCaptor<ResolveReversalRequestCommand> comando = ArgumentCaptor
                    .forClass(ResolveReversalRequestCommand.class);
            verify(resolveUseCase).execute(comando.capture());
            assertThat(comando.getValue().id()).isEqualTo(77L);
            assertThat(comando.getValue().companyId()).isEqualTo(EMPRESA);
            assertThat(comando.getValue().outcome()).isEqualTo(ReversalOutcome.PARTIALLY_ACCEPTED);
            assertThat(comando.getValue().appliedAmount()).isEqualByComparingTo("137500.75");
            assertThat(comando.getValue().resultingRefundId()).isEqualTo(4_400L);
        }

        @Test
        @DisplayName("un importe aplicado no positivo responde 400")
        void un_importe_aplicado_no_positivo_responde_400() throws Exception {
            mockMvc.perform(patch("/system/payment-reversal-requests/77/outcome")
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"outcome\":\"ACCEPTED\",\"appliedAmount\":-1.00}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(resolveUseCase);
        }

        @Test
        @DisplayName("resolver un expediente ya resuelto responde 409 y no 400: el cuerpo esta"
                + " bien escrito, lo que choca es el estado del dinero")
        void resolver_un_expediente_ya_resuelto_responde_409() throws Exception {
            when(resolveUseCase.execute(any())).thenThrow(
                    new ReversalRequestAlreadyResolvedException(77L, ReversalOutcome.ACCEPTED));

            mockMvc.perform(patch("/system/payment-reversal-requests/77/outcome")
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"outcome\":\"REJECTED\"}")).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("REVERSAL_REQUEST_ALREADY_RESOLVED"));
        }
    }

    @Nested
    @DisplayName("Barridos de plataforma")
    class Barridos {

        @Test
        @DisplayName("el barrido de plazos recorre todas las clinicas y propaga el corte tal cual"
                + " llega")
        void el_barrido_de_plazos_recorre_todas_las_clinicas() throws Exception {
            when(listExpiringUseCase.listExpiring(any(), anyInt(), anyInt())).thenReturn(
                    PageResult.of(List.of(expediente(), expedienteDe(OTRA_EMPRESA)), 2, 5, 11L));

            mockMvc.perform(get("/system/payment-reversal-requests/expiring")
                    .param("before", "2026-04-15T00:00:00").param("page", "2")
                    .param("pageSize", "5")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].companyId").value(900))
                    .andExpect(jsonPath("$.content[1].companyId").value(901))
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(11));

            verify(listExpiringUseCase).listExpiring(LocalDateTime.of(2026, 4, 15, 0, 0, 0), 2, 5);
        }

        @Test
        @DisplayName("el listado de plataforma sin empresa las pide todas, con la paginacion por"
                + " defecto")
        void el_listado_de_plataforma_sin_empresa_las_pide_todas() throws Exception {
            when(listAllUseCase.listAll(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/system/payment-reversal-requests")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.pageSize").value(20));

            verify(listAllUseCase).listAll(isNull(), eq(0), eq(20));
        }

        @Test
        @DisplayName("el listado de plataforma con empresa filtra por esa y no por otra")
        void el_listado_de_plataforma_con_empresa_filtra_por_esa() throws Exception {
            when(listAllUseCase.listAll(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(expedienteDe(OTRA_EMPRESA)), 0, 20, 1L));

            mockMvc.perform(get("/system/payment-reversal-requests").param("companyId", "901"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].companyId").value(901));

            ArgumentCaptor<Long> companyId = ArgumentCaptor.forClass(Long.class);
            verify(listAllUseCase).listAll(companyId.capture(), anyInt(), anyInt());
            assertThat(companyId.getValue()).isEqualTo(OTRA_EMPRESA);
        }
    }

    private static PaymentReversalRequestDto expediente() {
        return expedienteDe(EMPRESA);
    }

    private static PaymentReversalRequestDto expedienteDe(Long companyId) {
        return new PaymentReversalRequestDto(77L, companyId, 8_001L, ReversalOrigin.CONSUMER_CLAIM,
                ReversalCausal.PRODUCT_NOT_RECEIVED, ConsumerDetermination.CONSUMER,
                LocalDateTime.of(2026, 3, 1, 8, 15, 11), LocalDateTime.of(2026, 3, 5, 9, 26, 22),
                LocalDateTime.of(2026, 3, 7, 10, 37, 33), "EV-QUEJA-0001", "ACU-0001",
                LocalDateTime.of(2026, 3, 6, 13, 5, 6), OppositionGround.CAUSAL_NOT_REPORTED,
                "EV-OPOSICION-0001", LocalDateTime.of(2026, 3, 8, 14, 16, 17),
                LocalDateTime.of(2026, 4, 19, 11, 48, 44), new BigDecimal("137500.75"),
                ReversalOutcome.PARTIALLY_ACCEPTED, 4_400L,
                LocalDateTime.of(2026, 3, 5, 12, 59, 55), 3L);
    }
}
