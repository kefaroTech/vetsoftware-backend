package com.vetsoftware.app.catalogitemaihint.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.catalogitemaihint.application.command.PublishCatalogItemAiHintCommand;
import com.vetsoftware.app.catalogitemaihint.application.command.RetireCatalogItemAiHintCommand;
import com.vetsoftware.app.catalogitemaihint.application.command.ReviseCatalogItemAiHintCommand;
import com.vetsoftware.app.catalogitemaihint.application.dto.CatalogItemAiHintDto;
import com.vetsoftware.app.catalogitemaihint.application.port.in.FindCurrentCatalogItemAiHintUseCase;
import com.vetsoftware.app.catalogitemaihint.application.port.in.ListCatalogItemAiHintRevisionsUseCase;
import com.vetsoftware.app.catalogitemaihint.application.port.in.ListCurrentCatalogItemAiHintsUseCase;
import com.vetsoftware.app.catalogitemaihint.application.port.in.PublishCatalogItemAiHintUseCase;
import com.vetsoftware.app.catalogitemaihint.application.port.in.RetireCatalogItemAiHintUseCase;
import com.vetsoftware.app.catalogitemaihint.application.port.in.ReviseCatalogItemAiHintUseCase;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
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

@WebMvcTest(CatalogItemAiHintController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CatalogItemAiHintController — contrato HTTP de las pistas del asistente")
class CatalogItemAiHintControllerTest {

    private static final Long ARTICULO = 8801L;

    private static final String TEXTO = """
            Se necesita cuando el negocio ofrece bano y peluqueria.

            Senales en el texto: "peluqueria", "bano", "estetica".

            NO se necesita si el negocio es solo clinico.""";

    /** El id de sistema que un atacante intentaria colar en el cuerpo. */
    private static final long FIRMANTE_FALSO = 1L;

    /**
     * Quien retiro una revision ya historica. Distinto de
     * {@code WebMvcSliceConfig.SYSTEM_USER_ID} —que es quien la publico— para que
     * una respuesta que sirviera el firmante equivocado no pase en verde.
     */
    private static final long RETIRADOR = 9977L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListCurrentCatalogItemAiHintsUseCase listCurrentUseCase;

    @MockitoBean
    private FindCurrentCatalogItemAiHintUseCase findCurrentUseCase;

    @MockitoBean
    private ListCatalogItemAiHintRevisionsUseCase listRevisionsUseCase;

    @MockitoBean
    private PublishCatalogItemAiHintUseCase publishUseCase;

    @MockitoBean
    private ReviseCatalogItemAiHintUseCase reviseUseCase;

    @MockitoBean
    private RetireCatalogItemAiHintUseCase retireUseCase;

    private static CatalogItemAiHintDto revision(int numero, boolean vigente) {
        return new CatalogItemAiHintDto(7700L + numero, ARTICULO, "GROOMING",
                "Estetica y bienestar", numero, TEXTO, LocalDateTime.of(2026, 9, 1, 12, 0, 0),
                WebMvcSliceConfig.SYSTEM_USER_ID,
                vigente ? null : LocalDateTime.of(2026, 10, 1, 9, 0), vigente ? null : RETIRADOR,
                vigente, LocalDateTime.of(2026, 9, 1, 12, 0, 0));
    }

    @Nested
    @DisplayName("El firmante sale de la sesion, nunca del cuerpo")
    class Firmante {

        /**
         * &#9940; <b>El caso que este controller existe para no fallar.</b> El cuerpo
         * trae un {@code publishedBySystemUserId} distinto del principal, y tiene que
         * ser ignorado: {@code PublishCatalogItemAiHintRequest} no declara ese campo, y
         * el controller lo pone desde {@code authz.currentSystemUserId()}. Si alguien
         * lo anadiera al request «para cuadrar con lo que el front ya manda», este caso
         * se pone rojo — que es lo unico que separa un rastro de auditoria de un
         * formulario.
         */
        @Test
        @DisplayName("al publicar, el publishedBySystemUserId del cuerpo se ignora")
        void al_publicar_el_firmante_del_cuerpo_se_ignora() throws Exception {
            when(publishUseCase.execute(any())).thenReturn(revision(1, true));

            mockMvc.perform(post("/catalog-item-ai-hints").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"catalogItemId":8801,"hintText":%s,
                             "publishedBySystemUserId":%d,"hintRevision":99}
                            """.formatted(json(TEXTO), FIRMANTE_FALSO)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.hintRevision").value(1))
                    .andExpect(jsonPath("$.current").value(true))
                    .andExpect(jsonPath("$.publishedBySystemUserId")
                            .value(WebMvcSliceConfig.SYSTEM_USER_ID));

            ArgumentCaptor<PublishCatalogItemAiHintCommand> command = ArgumentCaptor
                    .forClass(PublishCatalogItemAiHintCommand.class);
            verify(publishUseCase).execute(command.capture());
            assertThat(command.getValue().publishedBySystemUserId())
                    .as("el firmante es el del principal, no el 1 que venia en el JSON")
                    .isEqualTo(WebMvcSliceConfig.SYSTEM_USER_ID).isNotEqualTo(FIRMANTE_FALSO);
            assertThat(command.getValue().catalogItemId()).isEqualTo(ARTICULO);
        }

        /**
         * Lo mismo al corregir, y ademas el articulo sale de la ruta: el
         * {@code catalogItemId} del cuerpo tampoco existe, asi que no hay dos fuentes
         * que puedan discrepar.
         */
        @Test
        @DisplayName("al corregir, el firmante sale de la sesion y el articulo de la ruta")
        void al_corregir_el_firmante_sale_de_la_sesion() throws Exception {
            when(reviseUseCase.execute(any())).thenReturn(revision(2, true));

            mockMvc.perform(put("/catalog-item-ai-hints/8801")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"hintText":%s,"catalogItemId":9999,"revisedBySystemUserId":%d}
                            """.formatted(json(TEXTO), FIRMANTE_FALSO))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.hintRevision").value(2));

            ArgumentCaptor<ReviseCatalogItemAiHintCommand> command = ArgumentCaptor
                    .forClass(ReviseCatalogItemAiHintCommand.class);
            verify(reviseUseCase).execute(command.capture());
            assertThat(command.getValue().revisedBySystemUserId())
                    .isEqualTo(WebMvcSliceConfig.SYSTEM_USER_ID).isNotEqualTo(FIRMANTE_FALSO);
            assertThat(command.getValue().catalogItemId())
                    .as("el articulo lo manda la ruta, no el 9999 del cuerpo").isEqualTo(ARTICULO);
        }
    }

    @Nested
    @DisplayName("Validacion del cuerpo")
    class Validaciones {

        @Test
        @DisplayName("un texto en blanco sale 400 y NO llega al caso de uso")
        void un_texto_en_blanco_sale_400() throws Exception {
            mockMvc.perform(post("/catalog-item-ai-hints").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"catalogItemId":8801,"hintText":"   "}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(publishUseCase);
        }

        /**
         * <b>Este caso es el que prueba que el {@code @Valid} esta puesto.</b> Sin el,
         * el binder no dispara el validador, el {@code @Size(max = 1000)} no se evalua
         * nunca —aunque el contrato OpenAPI lo anuncie—, la peticion entra entera y el
         * error acaba saliendo del dominio con otra forma y otro codigo. Aqui se manda
         * un texto de 1001 caracteres <em>bien formado en lo demas</em>, asi que lo
         * unico que puede pararlo es la restriccion de longitud.
         */
        @Test
        @DisplayName("un texto de mas de 1000 caracteres sale 400 sin tocar el caso de uso")
        void un_texto_demasiado_largo_sale_400() throws Exception {
            String largo = "a".repeat(1001);

            mockMvc.perform(post("/catalog-item-ai-hints").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"catalogItemId":8801,"hintText":"%s"}
                            """.formatted(largo))).andExpect(status().isBadRequest());

            verifyNoInteractions(publishUseCase);
        }

        @Test
        @DisplayName("publicar sin catalogItemId sale 400")
        void publicar_sin_articulo_sale_400() throws Exception {
            mockMvc.perform(post("/catalog-item-ai-hints").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"hintText":%s}
                            """.formatted(json(TEXTO)))).andExpect(status().isBadRequest());

            verifyNoInteractions(publishUseCase);
        }

        @Test
        @DisplayName("corregir con el cuerpo vacio sale 400 y NO retira la pista")
        void corregir_sin_texto_sale_400() throws Exception {
            mockMvc.perform(put("/catalog-item-ai-hints/8801")
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(reviseUseCase);
            verifyNoInteractions(retireUseCase);
        }
    }

    @Nested
    @DisplayName("Lecturas y retirada")
    class Lecturas {

        @Test
        @DisplayName("el listado de vigentes usa el contrato unico de pagina")
        void el_listado_usa_el_contrato_unico() throws Exception {
            when(listCurrentUseCase.listCurrent(anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(revision(1, true)), 0, 20, 1));

            mockMvc.perform(get("/catalog-item-ai-hints")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].catalogItemCode").value("GROOMING"))
                    .andExpect(jsonPath("$.content[0].hintText").value(TEXTO));
        }

        /**
         * El historial devuelve las reemplazadas con su {@code supersededAt} y
         * {@code current: false}: es lo que hace visible que corregir no borro nada.
         */
        @Test
        @DisplayName("el historial sirve tambien las revisiones reemplazadas")
        void el_historial_sirve_las_reemplazadas() throws Exception {
            when(listRevisionsUseCase.listByCatalogItemId(anyLong(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(revision(2, true), revision(1, false)), 0, 20,
                            2));

            mockMvc.perform(get("/catalog-item-ai-hints/8801/revisions")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[1].current").value(false))
                    .andExpect(jsonPath("$.content[1].supersededAt").isNotEmpty())
                    .andExpect(jsonPath("$.content[1].hintText").value(TEXTO));
        }

        /**
         * &#9940; <b>La evidencia tiene que poder ENSENARSE, no solo guardarse.</b> El
         * changeset 393 anadio {@code superseded_by_system_user_id} porque esta tabla
         * existe para ser auditable; una columna con escritor y sin lector deja la
         * firma escrita donde nadie puede consultarla, que es exactamente el hueco que
         * venia a tapar. Este caso fija que el dato sale por HTTP y con el actor
         * correcto: si alguien sirviera {@code publishedBySystemUserId} en su lugar —el
         * valor que esta a mano en la misma fila— el {@code isNotEqualTo} implicito de
         * comparar contra {@code RETIRADOR} lo caza.
         *
         * <p>
         * &#9888; <b>Las dos filas se afirman en la MISMA respuesta a proposito.</b>
         * {@code doesNotExist()} de MockMvc no distingue «el campo no esta» de «el
         * campo vale null», asi que por si solo pasaria igual si alguien borrara el
         * campo del esquema. Comprobar en la misma pagina que la reemplazada SI lo trae
         * cierra ese agujero: el campo existe, y su ausencia en la vigente es <em>«no
         * consta»</em>, que es lo que la pantalla necesita poder distinguir de «la
         * retiro fulano».
         */
        @Test
        @DisplayName("el historial dice quien retiro cada revision, y la vigente no lo trae")
        void el_historial_dice_quien_retiro() throws Exception {
            when(listRevisionsUseCase.listByCatalogItemId(anyLong(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(revision(2, true), revision(1, false)), 0, 20,
                            2));

            mockMvc.perform(get("/catalog-item-ai-hints/8801/revisions")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[1].supersededBySystemUserId").value(RETIRADOR))
                    .andExpect(jsonPath("$.content[1].publishedBySystemUserId")
                            .value(WebMvcSliceConfig.SYSTEM_USER_ID))
                    .andExpect(jsonPath("$.content[0].current").value(true))
                    .andExpect(jsonPath("$.content[0].supersededBySystemUserId").doesNotExist());
        }

        @Test
        @DisplayName("la vigente de un articulo sale con su texto completo")
        void la_vigente_sale_con_su_texto() throws Exception {
            when(findCurrentUseCase.findCurrentByCatalogItemId(anyLong()))
                    .thenReturn(revision(3, true));

            mockMvc.perform(get("/catalog-item-ai-hints/8801")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.hintText").value(TEXTO))
                    .andExpect(jsonPath("$.hintRevision").value(3));

            verify(findCurrentUseCase).findCurrentByCatalogItemId(ARTICULO);
        }

        /**
         * &#9940; <b>El caso que fija que la retirada queda firmada.</b> El
         * {@code DELETE} no lleva cuerpo, asi que el unico sitio de donde puede salir
         * el actor es la sesion: si el controller volviera a llamar al puerto solo con
         * el id —como hacia antes del changeset 393—, esto ni compilaria; si le pasara
         * el articulo donde va el firmante, el {@code isEqualTo} lo caza. Sin la firma,
         * la fila retirada seguiria mostrando a quien la <em>publico</em> y nadie
         * podria saber quien decidio que el modulo dejara de proponerse.
         */
        @Test
        @DisplayName("retirar responde 204 y firma la retirada con el usuario de la sesion")
        void retirar_responde_204() throws Exception {
            mockMvc.perform(delete("/catalog-item-ai-hints/8801"))
                    .andExpect(status().isNoContent());

            ArgumentCaptor<RetireCatalogItemAiHintCommand> command = ArgumentCaptor
                    .forClass(RetireCatalogItemAiHintCommand.class);
            verify(retireUseCase).retire(command.capture());
            assertThat(command.getValue().catalogItemId()).isEqualTo(ARTICULO);
            assertThat(command.getValue().retiredBySystemUserId())
                    .as("quien retira sale del principal, no del cuerpo: un DELETE no lo lleva")
                    .isEqualTo(WebMvcSliceConfig.SYSTEM_USER_ID).isNotEqualTo(FIRMANTE_FALSO);
            verifyNoInteractions(reviseUseCase);
            verifyNoInteractions(publishUseCase);
        }
    }

    /**
     * El texto lleva saltos de linea y comillas: hay que escaparlo para el JSON.
     */
    private static String json(String texto) {
        return "\"" + texto.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
