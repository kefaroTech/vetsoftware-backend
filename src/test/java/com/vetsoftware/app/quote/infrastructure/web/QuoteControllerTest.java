package com.vetsoftware.app.quote.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.quote.application.command.AcceptQuoteCommand;
import com.vetsoftware.app.quote.application.command.CreateQuoteCommand;
import com.vetsoftware.app.quote.application.command.QuoteAnswerCommand;
import com.vetsoftware.app.quote.application.command.QuoteLineCommand;
import com.vetsoftware.app.quote.application.command.RejectQuoteCommand;
import com.vetsoftware.app.quote.application.command.SendQuoteCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.dto.QuoteSummaryDto;
import com.vetsoftware.app.quote.application.port.in.AcceptQuoteUseCase;
import com.vetsoftware.app.quote.application.port.in.CreateQuoteUseCase;
import com.vetsoftware.app.quote.application.port.in.DeleteQuoteUseCase;
import com.vetsoftware.app.quote.application.port.in.ExpireOverdueQuotesUseCase;
import com.vetsoftware.app.quote.application.port.in.FindQuoteTotalsMismatchesUseCase;
import com.vetsoftware.app.quote.application.port.in.FindQuoteUseCase;
import com.vetsoftware.app.quote.application.port.in.ListQuotesByCompanyUseCase;
import com.vetsoftware.app.quote.application.port.in.ListQuotesUseCase;
import com.vetsoftware.app.quote.application.port.in.RejectQuoteUseCase;
import com.vetsoftware.app.quote.application.port.in.SelfServeQuoteUseCase;
import com.vetsoftware.app.quote.application.port.in.SendQuoteUseCase;
import com.vetsoftware.app.quote.domain.InvalidQuoteStatusTransitionException;
import com.vetsoftware.app.quote.domain.QuoteExpiredException;
import com.vetsoftware.app.quote.domain.QuoteNotFoundException;
import com.vetsoftware.app.quote.domain.QuoteStatus;
import com.vetsoftware.app.quote.testsupport.QuoteMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
 * Rodaja HTTP del controller de cotizaciones (BE-10): rutas, binding,
 * validacion del request, codigos de estado y forma del JSON. Lo que hay debajo
 * son dobles.
 *
 * <p>
 * <b>Lo que aqui se prueba y no se puede probar en otro sitio.</b> Tres
 * decisiones de este controller solo existen en la frontera HTTP y ningun test
 * de servicio las ve:
 *
 * <ul>
 * <li><b>El {@code companyId} no entra por el cuerpo en ningun endpoint.</b> Lo
 * pone el controller desde el principal. Se comprueba mandando un
 * {@code companyId} en el JSON y afirmando sobre el command capturado que llego
 * el del contexto y no el del cliente.
 * <li><b>La IP de la aceptacion sale de la peticion</b>
 * ({@code getRemoteAddr()}), nunca del cuerpo: una prueba que el cliente
 * escribe no prueba nada. {@code AcceptQuoteRequest} ni siquiera tiene campo de
 * IP, asi que la unica forma de verificarlo es capturar el command y comparar
 * contra la IP de la peticion.
 * <li><b>La asimetria {@code currentCompanyId()} /
 * {@code currentCompanyIdOrNull()}</b>. El listado del tenant usa la primera;
 * todos los caminos de un documento concreto usan la segunda, porque una
 * cotizacion a prospecto tiene {@code company_id} nulo y es el unico modelo del
 * sistema donde ese null es legitimo.
 * </ul>
 *
 * <p>
 * <b>Lo que aqui NO se prueba.</b> La autorizacion vive en el
 * {@code @PreAuthorize} de cada puerto de entrada y en {@code @WebMvcTest} los
 * puertos estan mockeados, asi que el gate no se ejercita. Esa red la ponen
 * ArchUnit y la auditoria de autorizacion.
 *
 * <p>
 * <b>No se verifica sobre el doble de {@code Authz}</b>: lo crea
 * {@link WebMvcSliceConfig} como {@code @Bean}, asi que es un singleton del
 * contexto cacheado y sus invocaciones se acumulan entre tests de la clase. La
 * asercion va siempre sobre el command.
 */
@WebMvcTest(QuoteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("QuoteController — contrato HTTP")
class QuoteControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long QUOTE_ID = 55L;

    /**
     * Empresa que el cliente mete en el cuerpo. Distinta a proposito de
     * {@link #COMPANY_ID}: asi la asercion enseña cual de las dos llego al command.
     */
    private static final long COMPANY_ID_DEL_CUERPO = 999L;

    /** IP de la peticion. Nunca viaja en el cuerpo; la pone el servidor. */
    private static final String IP_DE_LA_PETICION = "203.0.113.7";

    private static final String CREAR_JSON = """
            {"clientRequestId":"req-0001","prospectName":"Veterinaria del Sur",
             "prospectEmail":"ana@ejemplo.com","prospectDocument":"12345678",
             "prospectPhone":"3001112233","priceListId":7,"billingCycle":"MONTHLY",
             "validUntil":"2026-09-30","trialDays":15,
             "lines":[{"catalogItemId":1,"quantity":3,"discountPercent":"10.00"}],
             "answers":[{"questionId":11,"optionId":99,"answerValue":"SI"}]}
            """;

    /**
     * El mismo cuerpo con un {@code companyId} colado. {@code CreateQuoteRequest}
     * no declara ese componente y Jackson lo ignora en silencio (Boot deja
     * {@code FAIL_ON_UNKNOWN_PROPERTIES} en false), asi que la comprobacion util no
     * es el 400 sino que el command siga llevando la empresa del contexto.
     */
    private static final String CREAR_JSON_CON_EMPRESA_COLADA = """
            {"clientRequestId":"req-0001","companyId":999,"priceListId":7,
             "billingCycle":"MONTHLY","validUntil":"2026-09-30","trialDays":0,
             "lines":[{"catalogItemId":1,"quantity":1,"discountPercent":"0.00"}]}
            """;

    /** Sin {@code lines}: el {@code @NotEmpty} del request tiene que rechazarlo. */
    private static final String CREAR_JSON_SIN_LINEAS = """
            {"clientRequestId":"req-0001","priceListId":7,"billingCycle":"MONTHLY",
             "validUntil":"2026-09-30","trialDays":0,"lines":[]}
            """;

    /** Sin llave de idempotencia: {@code @NotBlank}. */
    private static final String CREAR_JSON_SIN_LLAVE = """
            {"clientRequestId":"  ","priceListId":7,"billingCycle":"MONTHLY",
             "validUntil":"2026-09-30","trialDays":0,
             "lines":[{"catalogItemId":1,"quantity":1,"discountPercent":"0.00"}]}
            """;

    private static final String ACEPTAR_JSON = """
            {"acceptedByEmail":"ana@ejemplo.com"}
            """;

    private static final String ACEPTAR_JSON_CORREO_INVALIDO = """
            {"acceptedByEmail":"esto-no-es-un-correo"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateQuoteUseCase createUseCase;
    @MockitoBean
    private FindQuoteUseCase findUseCase;
    @MockitoBean
    private ListQuotesByCompanyUseCase listByCompanyUseCase;
    @MockitoBean
    private ListQuotesUseCase listUseCase;
    @MockitoBean
    private SendQuoteUseCase sendUseCase;
    @MockitoBean
    private SelfServeQuoteUseCase selfServeUseCase;
    @MockitoBean
    private AcceptQuoteUseCase acceptUseCase;
    @MockitoBean
    private RejectQuoteUseCase rejectUseCase;
    @MockitoBean
    private DeleteQuoteUseCase deleteUseCase;
    @MockitoBean
    private ExpireOverdueQuotesUseCase expireOverdueUseCase;
    @MockitoBean
    private FindQuoteTotalsMismatchesUseCase totalsMismatchesUseCase;

    /**
     * {@link WebMvcSliceConfig} stubea {@code currentCompanyId()} pero NO
     * {@code currentCompanyIdOrNull()}, que es el que usan siete de los nueve
     * endpoints de este controller. Sin este stub el doble devolveria null —el
     * camino de prospecto— y todos los casos del tenant estarian probando la rama
     * equivocada en verde.
     */
    @BeforeEach
    void resolverLaEmpresaDelContexto() {
        when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
    }

    private static QuoteDto cotizacion() {
        return QuoteDto.from(QuoteMother.persistidaConRespuestas(QUOTE_ID));
    }

    private static QuoteDto cotizacionDeProspecto() {
        return QuoteDto.from(QuoteMother.persistidaDeProspecto(QUOTE_ID));
    }

    private static QuoteDto cotizacionEn(QuoteStatus estado) {
        return QuoteDto.from(QuoteMother.persistida(QUOTE_ID, estado));
    }

    private static PageResult<QuoteSummaryDto> pagina(QuoteSummaryDto... filas) {
        return PageResult.of(List.of(filas), 0, 20, filas.length);
    }

    @Nested
    @DisplayName("POST /quotes")
    class Crear {

        @Test
        @DisplayName("responde 201 con el documento completo, sus lineas y sus respuestas")
        void responde_201_con_el_documento_completo() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cotizacion());

            mockMvc.perform(
                    post("/quotes").contentType(MediaType.APPLICATION_JSON).content(CREAR_JSON))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(55))
                    .andExpect(jsonPath("$.quoteNumber").value("COT-2026-00184"))
                    .andExpect(jsonPath("$.company.id").value(42))
                    .andExpect(jsonPath("$.company.name").value("Clinica Norte"))
                    .andExpect(jsonPath("$.company.identifier").value("900123456"))
                    .andExpect(jsonPath("$.priceListId").value(7))
                    .andExpect(jsonPath("$.billingCycle").value("MONTHLY"))
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andExpect(jsonPath("$.validUntil").value("2026-09-30"))
                    .andExpect(jsonPath("$.clientRequestId").value("req-0001"))
                    .andExpect(jsonPath("$.subtotalAmount").value(100000.00))
                    .andExpect(jsonPath("$.discountAmount").value(0.00))
                    .andExpect(jsonPath("$.taxAmount").value(19000.00))
                    .andExpect(jsonPath("$.totalAmount").value(119000.00))
                    .andExpect(jsonPath("$.lines.length()").value(1))
                    .andExpect(jsonPath("$.lines[0].lineNumber").value(1))
                    .andExpect(jsonPath("$.lines[0].itemCode").value("CLINICAL_HISTORY"))
                    .andExpect(jsonPath("$.lines[0].itemType").value("MODULE"))
                    .andExpect(jsonPath("$.lines[0].contractedQuantity").value(1))
                    .andExpect(jsonPath("$.lines[0].includedQuantity").value(0))
                    .andExpect(jsonPath("$.lines[0].quantity").value(1))
                    .andExpect(jsonPath("$.lines[0].unitAmount").value(100000.00))
                    .andExpect(jsonPath("$.lines[0].grossAmount").value(100000.00))
                    .andExpect(jsonPath("$.lines[0].taxRate").value(19.00))
                    .andExpect(jsonPath("$.lines[0].taxTreatment").value("TAXED"))
                    .andExpect(jsonPath("$.lines[0].lineTotal").value(119000.00))
                    .andExpect(jsonPath("$.answers.length()").value(1))
                    .andExpect(jsonPath("$.answers[0].questionCode").value("SELLS_PRODUCTS"))
                    .andExpect(jsonPath("$.answers[0].answerValue").value("SI"));
        }

        @Test
        @DisplayName("traduce el request al command con la empresa del principal, no la del cuerpo")
        void traduce_el_request_al_command_con_la_empresa_del_principal() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cotizacion());

            mockMvc.perform(
                    post("/quotes").contentType(MediaType.APPLICATION_JSON).content(CREAR_JSON))
                    .andExpect(status().isCreated());

            ArgumentCaptor<CreateQuoteCommand> comando = ArgumentCaptor
                    .forClass(CreateQuoteCommand.class);
            verify(createUseCase).execute(comando.capture());
            assertThat(comando.getValue()).isEqualTo(new CreateQuoteCommand("req-0001", COMPANY_ID,
                    "Veterinaria del Sur", "ana@ejemplo.com", "12345678", "3001112233", 7L,
                    "MONTHLY", LocalDate.of(2026, 9, 30), 15,
                    List.of(new QuoteLineCommand(1L, 3, new BigDecimal("10.00"))),
                    List.of(new QuoteAnswerCommand(11L, 99L, "SI"))));
        }

        @Test
        @DisplayName("un companyId colado en el cuerpo no llega al command: lo pone el servidor")
        void un_company_id_colado_en_el_cuerpo_no_llega_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cotizacion());

            mockMvc.perform(post("/quotes").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON_CON_EMPRESA_COLADA)).andExpect(status().isCreated());

            ArgumentCaptor<CreateQuoteCommand> comando = ArgumentCaptor
                    .forClass(CreateQuoteCommand.class);
            verify(createUseCase).execute(comando.capture());
            assertThat(comando.getValue().companyId()).isEqualTo(COMPANY_ID)
                    .isNotEqualTo(COMPANY_ID_DEL_CUERPO);
        }

        @Test
        @DisplayName("el reintento idempotente tambien responde 201, no 200")
        void el_reintento_idempotente_tambien_responde_201() throws Exception {
            // El use case devuelve la que ya nacio la primera vez; el contrato dice que
            // el codigo de estado es el mismo que entonces. Un 200 aqui le haria creer
            // al cliente que hizo algo distinto de lo que hizo.
            when(createUseCase.execute(any())).thenReturn(cotizacion());

            mockMvc.perform(
                    post("/quotes").contentType(MediaType.APPLICATION_JSON).content(CREAR_JSON))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(55));
            mockMvc.perform(
                    post("/quotes").contentType(MediaType.APPLICATION_JSON).content(CREAR_JSON))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(55));
        }

        @Test
        @DisplayName("sin empresa en el contexto cotiza a un prospecto y la empresa sale nula")
        void sin_empresa_en_el_contexto_cotiza_a_un_prospecto() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(null);
            when(createUseCase.execute(any())).thenReturn(cotizacionDeProspecto());

            mockMvc.perform(
                    post("/quotes").contentType(MediaType.APPLICATION_JSON).content(CREAR_JSON))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.company").doesNotExist())
                    .andExpect(jsonPath("$.prospectName").value("Veterinaria del Sur"))
                    .andExpect(jsonPath("$.trialDays").value(15));

            ArgumentCaptor<CreateQuoteCommand> comando = ArgumentCaptor
                    .forClass(CreateQuoteCommand.class);
            verify(createUseCase).execute(comando.capture());
            assertThat(comando.getValue().companyId()).isNull();
        }

        @Test
        @DisplayName("sin lineas no es una oferta: 400 y el caso de uso no se llama")
        void sin_lineas_no_es_una_oferta() throws Exception {
            mockMvc.perform(post("/quotes").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON_SIN_LINEAS)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("sin llave de idempotencia no hay proteccion contra el doble clic: 400")
        void sin_llave_de_idempotencia_400() throws Exception {
            mockMvc.perform(post("/quotes").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON_SIN_LLAVE)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }
    }

    @Nested
    @DisplayName("GET /quotes/{id}")
    class Consultar {

        @Test
        @DisplayName("lee acotado por la empresa del principal")
        void lee_acotado_por_la_empresa_del_principal() throws Exception {
            when(findUseCase.findById(QUOTE_ID, COMPANY_ID)).thenReturn(cotizacion());

            mockMvc.perform(get("/quotes/{id}", QUOTE_ID)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(55))
                    .andExpect(jsonPath("$.company.id").value(42));

            verify(findUseCase).findById(QUOTE_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("sin empresa en el contexto lee ancho: el camino a una oferta de prospecto")
        void sin_empresa_en_el_contexto_lee_ancho() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(null);
            when(findUseCase.findById(QUOTE_ID, null)).thenReturn(cotizacionDeProspecto());

            mockMvc.perform(get("/quotes/{id}", QUOTE_ID)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.company").doesNotExist());

            verify(findUseCase).findById(QUOTE_ID, null);
        }

        @Test
        @DisplayName("una cotizacion que no existe para este tenant es un 404")
        void una_cotizacion_que_no_existe_es_un_404() throws Exception {
            when(findUseCase.findById(QUOTE_ID, COMPANY_ID))
                    .thenThrow(new QuoteNotFoundException(QUOTE_ID));

            mockMvc.perform(get("/quotes/{id}", QUOTE_ID)).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("QUOTE_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("GET /quotes y GET /quotes/platform")
    class Listados {

        @Test
        @DisplayName("el listado del tenant usa currentCompanyId(), que para SYSTEM no vale")
        void el_listado_del_tenant_usa_current_company_id() throws Exception {
            // Asimetria deliberada: los caminos de un documento concreto usan
            // currentCompanyIdOrNull() -un prospecto no tiene empresa-, pero "mis
            // cotizaciones" no significa nada sin empresa, asi que este exige la estricta.
            when(listByCompanyUseCase.listByCompany(COMPANY_ID, 0, 20)).thenReturn(
                    pagina(QuoteSummaryDto.from(QuoteMother.resumen(QUOTE_ID, QuoteStatus.SENT))));

            mockMvc.perform(get("/quotes")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(55))
                    .andExpect(jsonPath("$.content[0].status").value("SENT"))
                    .andExpect(jsonPath("$.content[0].totalAmount").value(119000.00))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.pageSize").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));

            verify(listByCompanyUseCase).listByCompany(COMPANY_ID, 0, 20);
        }

        @Test
        @DisplayName("respeta page y pageSize del cliente y no los reinventa")
        void respeta_page_y_page_size_del_cliente() throws Exception {
            when(listByCompanyUseCase.listByCompany(COMPANY_ID, 3, 5))
                    .thenReturn(PageResult.of(List.of(), 3, 5, 42));

            mockMvc.perform(get("/quotes").param("page", "3").param("pageSize", "5"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(3))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(42))
                    .andExpect(jsonPath("$.totalPages").value(9));

            verify(listByCompanyUseCase).listByCompany(COMPANY_ID, 3, 5);
        }

        @Test
        @DisplayName("el embudo de plataforma no filtra por empresa y admite filas sin empresa")
        void el_embudo_de_plataforma_no_filtra_por_empresa() throws Exception {
            when(listUseCase.listAll(0, 20)).thenReturn(
                    pagina(QuoteSummaryDto.from(QuoteMother.resumen(QUOTE_ID, QuoteStatus.SENT)),
                            QuoteSummaryDto.from(QuoteMother.resumenDeProspecto(56L))));

            mockMvc.perform(get("/quotes/platform")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].company.id").value(42))
                    .andExpect(jsonPath("$.content[1].company").doesNotExist())
                    .andExpect(jsonPath("$.content[1].prospectName").value("Veterinaria del Sur"));

            verify(listUseCase).listAll(0, 20);
            verifyNoInteractions(listByCompanyUseCase);
        }
    }

    @Nested
    @DisplayName("Transiciones")
    class Transiciones {

        @Test
        @DisplayName("send construye el command con la empresa del contexto")
        void send_construye_el_command_con_la_empresa_del_contexto() throws Exception {
            when(sendUseCase.execute(any())).thenReturn(cotizacionEn(QuoteStatus.SENT));

            mockMvc.perform(post("/quotes/{id}/send", QUOTE_ID)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SENT"));

            verify(sendUseCase).execute(new SendQuoteCommand(QUOTE_ID, COMPANY_ID));
        }

        @Test
        @DisplayName("enviar una oferta ya vencida es un 409, no un 500")
        void enviar_una_oferta_ya_vencida_es_un_409() throws Exception {
            when(sendUseCase.execute(any()))
                    .thenThrow(new QuoteExpiredException(QUOTE_ID, LocalDate.of(2026, 1, 1)));

            mockMvc.perform(post("/quotes/{id}/send", QUOTE_ID)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("QUOTE_EXPIRED"));
        }

        @Test
        @DisplayName("la IP de la aceptacion sale de la peticion, nunca del cuerpo")
        void la_ip_de_la_aceptacion_sale_de_la_peticion() throws Exception {
            when(acceptUseCase.execute(any())).thenReturn(cotizacionEn(QuoteStatus.ACCEPTED));

            mockMvc.perform(post("/quotes/{id}/accept", QUOTE_ID)
                    .contentType(MediaType.APPLICATION_JSON).content(ACEPTAR_JSON).with(req -> {
                        req.setRemoteAddr(IP_DE_LA_PETICION);
                        return req;
                    })).andExpect(status().isOk());

            ArgumentCaptor<AcceptQuoteCommand> comando = ArgumentCaptor
                    .forClass(AcceptQuoteCommand.class);
            verify(acceptUseCase).execute(comando.capture());
            assertThat(comando.getValue()).isEqualTo(new AcceptQuoteCommand(QUOTE_ID, COMPANY_ID,
                    "ana@ejemplo.com", IP_DE_LA_PETICION));
        }

        @Test
        @DisplayName("un correo de aceptacion invalido es 400 y no llega al caso de uso")
        void un_correo_de_aceptacion_invalido_es_400() throws Exception {
            mockMvc.perform(post("/quotes/{id}/accept", QUOTE_ID)
                    .contentType(MediaType.APPLICATION_JSON).content(ACEPTAR_JSON_CORREO_INVALIDO))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(acceptUseCase);
        }

        @Test
        @DisplayName("aceptar sin cuerpo es 400: la firma es obligatoria")
        void aceptar_sin_cuerpo_es_400() throws Exception {
            mockMvc.perform(post("/quotes/{id}/accept", QUOTE_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(acceptUseCase);
        }

        @Test
        @DisplayName("reject construye el command con la empresa del contexto")
        void reject_construye_el_command_con_la_empresa_del_contexto() throws Exception {
            when(rejectUseCase.execute(any())).thenReturn(cotizacionEn(QuoteStatus.REJECTED));

            mockMvc.perform(post("/quotes/{id}/reject", QUOTE_ID)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));

            verify(rejectUseCase).execute(new RejectQuoteCommand(QUOTE_ID, COMPANY_ID));
        }

        @Test
        @DisplayName("rechazar un borrador que nunca se envio es un 409")
        void rechazar_un_borrador_que_nunca_se_envio_es_un_409() throws Exception {
            when(rejectUseCase.execute(any())).thenThrow(new InvalidQuoteStatusTransitionException(
                    QuoteStatus.DRAFT, QuoteStatus.REJECTED));

            mockMvc.perform(post("/quotes/{id}/reject", QUOTE_ID)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_QUOTE_STATUS_TRANSITION"));
        }
    }

    @Nested
    @DisplayName("DELETE /quotes/{id} y POST /quotes/expire-overdue")
    class BajaYBarrido {

        @Test
        @DisplayName("la baja logica responde 204 sin cuerpo y acotada por empresa")
        void la_baja_logica_responde_204_sin_cuerpo() throws Exception {
            mockMvc.perform(delete("/quotes/{id}", QUOTE_ID)).andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(deleteUseCase).execute(QUOTE_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("sin empresa en el contexto la baja va por la sobrecarga ancha")
        void sin_empresa_en_el_contexto_la_baja_va_ancha() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(null);

            mockMvc.perform(delete("/quotes/{id}", QUOTE_ID)).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(QUOTE_ID, null);
        }

        @Test
        @DisplayName("dar de baja una enviada es un 409: borraria la prueba de lo ofrecido")
        void dar_de_baja_una_enviada_es_un_409() throws Exception {
            org.mockito.Mockito.doThrow(
                    new InvalidQuoteStatusTransitionException(QuoteStatus.SENT, QuoteStatus.DRAFT))
                    .when(deleteUseCase).execute(eq(QUOTE_ID), any());

            mockMvc.perform(delete("/quotes/{id}", QUOTE_ID)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_QUOTE_STATUS_TRANSITION"));
        }

        @Test
        @DisplayName("devuelve el número real de cotizaciones vencidas por el barrido")
        void devuelve_el_numero_real_de_cotizaciones_vencidas_por_el_barrido() throws Exception {
            when(expireOverdueUseCase.expireOverdue(25)).thenReturn(4);

            mockMvc.perform(post("/quotes/expire-overdue").param("batchSize", "25"))
                    .andExpect(status().isOk()).andExpect(content().string("4"));

            verify(expireOverdueUseCase).expireOverdue(25);
        }

        @Test
        @DisplayName("sin batchSize el barrido usa el lote por defecto de 200")
        void sin_batch_size_el_barrido_usa_el_lote_por_defecto() throws Exception {
            when(expireOverdueUseCase.expireOverdue(200)).thenReturn(0);

            mockMvc.perform(post("/quotes/expire-overdue")).andExpect(status().isOk())
                    .andExpect(content().string("0"));

            verify(expireOverdueUseCase).expireOverdue(200);
        }
    }
}
