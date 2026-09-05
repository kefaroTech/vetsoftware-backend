package com.vetsoftware.app.aiproposal.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.aiproposal.application.command.EditProposalLinesCommand;
import com.vetsoftware.app.aiproposal.application.command.GenerateProposalCommand;
import com.vetsoftware.app.aiproposal.application.command.RefineProposalCommand;
import com.vetsoftware.app.aiproposal.application.dto.ProposalLineDto;
import com.vetsoftware.app.aiproposal.application.dto.ProposalViewDto;
import com.vetsoftware.app.aiproposal.application.port.in.EditProposalLinesUseCase;
import com.vetsoftware.app.aiproposal.application.port.in.GenerateProposalUseCase;
import com.vetsoftware.app.aiproposal.application.port.in.GetProposalUseCase;
import com.vetsoftware.app.aiproposal.application.port.in.RefineProposalUseCase;
import com.vetsoftware.app.aiproposal.domain.PackComparisonResult;
import com.vetsoftware.app.aiproposal.domain.ProposalPresentation;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La rodaja que {@code CONTROLLER_CON_RODAJA} exige, y que aqui vale por
 * bastante mas que por la regla: <strong>los cuatro endpoints son
 * anonimos</strong>, asi que el JSON que sale por el cable es la superficie de
 * exposicion entera de la feature. Lo que se fija aqui es lo que un atacante
 * <em>no</em> puede leer y por donde <em>no</em> viaja el token.
 *
 * <p>
 * Los casos de negocio -la fusion de la edicion manual, el tope de tres
 * refinamientos, la idempotencia acotada al correo- son de los servicios y
 * viven en sus propias rodajas.
 */
@WebMvcTest(AssistantController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("AssistantController — contrato HTTP de las cuatro rutas anonimas")
class AssistantControllerTest {

    private static final String TOKEN = "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-abcde";

    private static final String DESCRIPCION = "Tengo una veterinaria en Chapinero, somos dos"
            + " veterinarios y una auxiliar.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GenerateProposalUseCase generateUseCase;

    @MockitoBean
    private RefineProposalUseCase refineUseCase;

    @MockitoBean
    private EditProposalLinesUseCase editUseCase;

    @MockitoBean
    private GetProposalUseCase getUseCase;

    private static ProposalViewDto propuesta() {
        return new ProposalViewDto(TOKEN, ProposalPresentation.PROPOSAL,
                LocalDateTime.of(2026, 9, 13, 10, 0), 3L,
                List.of(new ProposalLineDto("CORE", "Clientes y mascotas",
                        "Clientes, mascotas y administracion de la propia cuenta", "MODULE", 1,
                        new BigDecimal("69000.00"), new BigDecimal("19.00"),
                        new BigDecimal("13110.00"), new BigDecimal("82110.00"), 30, "COP",
                        "Porque atiendes mascotas.")),
                List.of(new ProposalLineDto("LAB_IMAGING", "Laboratorio", "Ordenes de laboratorio",
                        "MODULE", 1, new BigDecimal("39000.00"), new BigDecimal("19.00"),
                        new BigDecimal("7410.00"), new BigDecimal("46410.00"), 14, "COP",
                        "Por si algun dia haces analitica.")),
                4, "COP", new BigDecimal("69000.00"), new BigDecimal("13110.00"),
                new BigDecimal("82110.00"), new BigDecimal("0.00"),
                new PackComparisonResult("PACK_CLINIC", "Consulta de barrio",
                        new BigDecimal("189000.00"), new BigDecimal("224000.00"),
                        new BigDecimal("35000.00"), "COP", 30, List.of("SCHEDULING")),
                3, true);
    }

    private static String cuerpoInicial(String descripcion) {
        return """
                {"email":"Laura@VetChapinero.CO","description":"%s",
                 "acceptances":[{"code":"PRIVACIDAD","documentVersion":2},
                                {"code":"TRANSFERENCIA","documentVersion":1}]}
                """.formatted(descripcion);
    }

    @Nested
    @DisplayName("POST /assistant/proposal")
    class Generacion {

        @Test
        @DisplayName("devuelve la propuesta con su token, sus totales y su divisa")
        void devuelve_la_propuesta_con_token_totales_y_divisa() throws Exception {
            when(generateUseCase.generate(any())).thenReturn(propuesta());

            mockMvc.perform(post("/assistant/proposal").contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpoInicial(DESCRIPCION))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(TOKEN))
                    .andExpect(jsonPath("$.presentation").value("PROPOSAL"))
                    .andExpect(jsonPath("$.currency").value("COP"))
                    .andExpect(jsonPath("$.total").value(82110.00))
                    .andExpect(jsonPath("$.lines[0].code").value("CORE"))
                    .andExpect(jsonPath("$.packOffer.trialDaysLost").value(30));
        }

        @Test
        @DisplayName("⛔ ni una linea rechazada, ni su veredicto ni su origen salen por HTTP")
        void no_serializa_veredictos_ni_origenes() throws Exception {
            when(generateUseCase.generate(any())).thenReturn(propuesta());

            mockMvc.perform(post("/assistant/proposal").contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpoInicial(DESCRIPCION))).andExpect(status().isOk())
                    // De lo descartado va como mucho un entero, sin codigos y sin
                    // desglose: cinco veredictos distinguibles serian un oraculo de
                    // cinco valores sobre el catalogo interno, y el texto de entrada
                    // que produce los codigos lo escribe quien pregunta.
                    .andExpect(jsonPath("$.discardedLines").value(4))
                    .andExpect(jsonPath("$.lines[0].verdict").doesNotExist())
                    .andExpect(jsonPath("$.lines[0].source").doesNotExist())
                    .andExpect(jsonPath("$.discardedByVerdict").doesNotExist());
        }

        @Test
        @DisplayName("la Idempotency-Key llega al caso de uso, y el correo normalizado con ella")
        void la_clave_de_idempotencia_llega_al_caso_de_uso() throws Exception {
            when(generateUseCase.generate(any())).thenReturn(propuesta());
            String clave = "3f2504e0-4f89-11d3-9a0c-0305e82c3301";

            mockMvc.perform(post("/assistant/proposal").header("Idempotency-Key", clave)
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpoInicial(DESCRIPCION)))
                    .andExpect(status().isOk());

            ArgumentCaptor<GenerateProposalCommand> captor = ArgumentCaptor
                    .forClass(GenerateProposalCommand.class);
            verify(generateUseCase).generate(captor.capture());
            assertThat(captor.getValue().idempotencyKey()).isEqualTo(clave);
            // Normalizado: el unico de idempotencia va sobre la columna generada
            // UNHEX(SHA2(LOWER(contact_email),256)), asi que con el correo tal cual
            // lo escribio el usuario la busqueda no encontraria la fila con la que
            // el INSERT si va a chocar.
            assertThat(captor.getValue().contactEmail()).isEqualTo("laura@vetchapinero.co");
        }

        @Test
        @DisplayName("la IP y el agente viajan hasheados, nunca en claro")
        void la_ip_y_el_agente_viajan_hasheados() throws Exception {
            when(generateUseCase.generate(any())).thenReturn(propuesta());

            mockMvc.perform(post("/assistant/proposal").header("User-Agent", "Mozilla/5.0")
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpoInicial(DESCRIPCION)))
                    .andExpect(status().isOk());

            ArgumentCaptor<GenerateProposalCommand> captor = ArgumentCaptor
                    .forClass(GenerateProposalCommand.class);
            verify(generateUseCase).generate(captor.capture());
            assertThat(captor.getValue().acceptedIpHash()).hasSize(64).doesNotContain("127.0.0.1");
            assertThat(captor.getValue().userAgentHash()).hasSize(64).doesNotContain("Mozilla");
        }

        @ParameterizedTest
        @ValueSource(strings = {"corto", "catorce carac"})
        @DisplayName("por debajo de 15 caracteres es 400 y no llega al caso de uso")
        void por_debajo_del_minimo_es_cuatrocientos(String descripcion) throws Exception {
            mockMvc.perform(post("/assistant/proposal").contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpoInicial(descripcion))).andExpect(status().isBadRequest());

            verify(generateUseCase, never()).generate(any());
        }

        @Test
        @DisplayName("sin ninguna aceptacion es 400: la casilla no es decorativa")
        void sin_aceptaciones_es_cuatrocientos() throws Exception {
            mockMvc.perform(
                    post("/assistant/proposal").contentType(MediaType.APPLICATION_JSON).content("""
                            {"email":"laura@vetchapinero.co","description":"%s","acceptances":[]}
                            """.formatted(DESCRIPCION))).andExpect(status().isBadRequest());

            verify(generateUseCase, never()).generate(any());
        }
    }

    @Nested
    @DisplayName("POST /assistant/proposal/refine")
    class Refinamiento {

        @Test
        @DisplayName("el token viaja en el CUERPO, no en la ruta ni en la cadena de consulta")
        void el_token_viaja_en_el_cuerpo() throws Exception {
            when(refineUseCase.refine(any())).thenReturn(propuesta());

            mockMvc.perform(post("/assistant/proposal/refine")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"token":"%s","text":"Tenemos dos sedes","version":3}
                            """.formatted(TOKEN))).andExpect(status().isOk());

            ArgumentCaptor<RefineProposalCommand> captor = ArgumentCaptor
                    .forClass(RefineProposalCommand.class);
            verify(refineUseCase).refine(captor.capture());
            assertThat(captor.getValue().publicToken()).isEqualTo(TOKEN);
            assertThat(captor.getValue().expectedVersion()).isEqualTo(3L);
        }

        @Test
        @DisplayName("«Tenemos dos sedes» (17) pasa: el minimo del refinamiento es 10, no 30")
        void el_relleno_rapido_mas_corto_pasa() throws Exception {
            when(refineUseCase.refine(any())).thenReturn(propuesta());

            mockMvc.perform(post("/assistant/proposal/refine")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"token":"%s","text":"Tenemos dos sedes"}
                            """.formatted(TOKEN))).andExpect(status().isOk());
        }

        @Test
        @DisplayName("con menos de 10 caracteres es 400")
        void con_menos_de_diez_es_cuatrocientos() throws Exception {
            mockMvc.perform(post("/assistant/proposal/refine")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"token":"%s","text":"peluqueo"}
                            """.formatted(TOKEN))).andExpect(status().isBadRequest());

            verify(refineUseCase, never()).refine(any());
        }
    }

    @Nested
    @DisplayName("PUT /assistant/proposal/lines")
    class EdicionManual {

        @Test
        @DisplayName("lo anadido y lo quitado llegan tal cual, con la version leida")
        void lo_anadido_y_lo_quitado_llegan_tal_cual() throws Exception {
            when(editUseCase.edit(any())).thenReturn(propuesta());

            mockMvc.perform(put("/assistant/proposal/lines").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"token":"%s","addedCodes":["GROOMING"],
                             "removedCodes":["EINVOICE"],"version":3}
                            """.formatted(TOKEN))).andExpect(status().isOk());

            ArgumentCaptor<EditProposalLinesCommand> captor = ArgumentCaptor
                    .forClass(EditProposalLinesCommand.class);
            verify(editUseCase).edit(captor.capture());
            assertThat(captor.getValue().addedCodes()).containsExactly("GROOMING");
            assertThat(captor.getValue().removedCodes()).containsExactly("EINVOICE");
        }

        /**
         * &#9940; <strong>El {@code @Size(max = 50)} de dentro del {@code List<>} acota
         * CADA ELEMENTO, no la lista.</strong> Con eso escrito —y leyendose como si
         * acotara algo— este {@code PUT} anonimo aceptaba una lista sin cota:
         * {@code ProposalCart.build} emite una linea por codigo, rechazos incluidos, y
         * {@code ProposalTurnWriter.escribirEdicion} las persiste todas en un unico
         * {@code saveLines}. Una escritura publica, sin sesion y gratis para quien la
         * hace, proporcional a lo que el cliente quiera mandar. Y el
         * {@code MAX_CODES = 40} del validador no cubria esto: aquel acota la salida
         * del modelo.
         */
        @Test
        @DisplayName("\u26d4 una lista de mas de 40 codigos es 400 y no llega al caso de uso")
        void una_lista_sin_cota_es_cuatrocientos() throws Exception {
            mockMvc.perform(put("/assistant/proposal/lines").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"token":"%s","addedCodes":[%s],"removedCodes":[]}
                            """.formatted(TOKEN, codigos(41)))).andExpect(status().isBadRequest());

            verify(editUseCase, never()).edit(any());
        }

        @Test
        @DisplayName("la cota tambien alcanza a removedCodes, que escribe una linea por codigo")
        void la_cota_alcanza_a_removed_codes() throws Exception {
            mockMvc.perform(put("/assistant/proposal/lines").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"token":"%s","addedCodes":[],"removedCodes":[%s]}
                            """.formatted(TOKEN, codigos(41)))).andExpect(status().isBadRequest());

            verify(editUseCase, never()).edit(any());
        }

        @Test
        @DisplayName("justo en el tope si pasa: la cota no rechaza una edicion legitima")
        void justo_en_el_tope_si_pasa() throws Exception {
            when(editUseCase.edit(any())).thenReturn(propuesta());

            mockMvc.perform(put("/assistant/proposal/lines").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"token":"%s","addedCodes":[%s],"removedCodes":[]}
                            """.formatted(TOKEN, codigos(40)))).andExpect(status().isOk());
        }

        private static String codigos(int cuantos) {
            return java.util.stream.IntStream.range(0, cuantos).mapToObj(i -> "\"C" + i + "\"")
                    .collect(java.util.stream.Collectors.joining(","));
        }
    }

    @Nested
    @DisplayName("GET /assistant/proposal")
    class Relectura {

        @Test
        @DisplayName("⛔ el token entra por ?token= y no por un segmento de ruta")
        void el_token_entra_por_parametro_de_consulta() throws Exception {
            when(getUseCase.get(TOKEN)).thenReturn(propuesta());

            mockMvc.perform(get("/assistant/proposal").param("token", TOKEN))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.token").value(TOKEN));

            verify(getUseCase).get(TOKEN);
        }

        @Test
        @DisplayName("sin token es 400: no hay lectura anonima sin credencial")
        void sin_token_es_cuatrocientos() throws Exception {
            mockMvc.perform(get("/assistant/proposal")).andExpect(status().isBadRequest());

            verify(getUseCase, never()).get(any());
        }
    }
}
