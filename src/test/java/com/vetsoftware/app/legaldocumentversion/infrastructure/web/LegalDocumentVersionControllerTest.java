package com.vetsoftware.app.legaldocumentversion.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.legaldocumentversion.application.command.PublishLegalDocumentVersionCommand;
import com.vetsoftware.app.legaldocumentversion.application.dto.LegalDocumentVersionDto;
import com.vetsoftware.app.legaldocumentversion.application.port.in.FindAcceptedLegalDocumentUseCase;
import com.vetsoftware.app.legaldocumentversion.application.port.in.FindCurrentLegalDocumentUseCase;
import com.vetsoftware.app.legaldocumentversion.application.port.in.ListLegalDocumentVersionsUseCase;
import com.vetsoftware.app.legaldocumentversion.application.port.in.PublishLegalDocumentVersionUseCase;
import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentKind;
import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentVersion;
import com.vetsoftware.app.shared.pagination.PageResult;
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

@WebMvcTest(LegalDocumentVersionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("LegalDocumentVersionController — contrato HTTP del texto inmutable")
class LegalDocumentVersionControllerTest {

    private static final String CODE = "TERMS_OF_SERVICE";
    private static final String TEXTO = "Version 2 de los terminos.";
    private static final String HUELLA = LegalDocumentVersion.hashOf(TEXTO);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublishLegalDocumentVersionUseCase publishUseCase;

    @MockitoBean
    private FindCurrentLegalDocumentUseCase findCurrentUseCase;

    @MockitoBean
    private FindAcceptedLegalDocumentUseCase findAcceptedUseCase;

    @MockitoBean
    private ListLegalDocumentVersionsUseCase listUseCase;

    private static LegalDocumentVersionDto version2() {
        return new LegalDocumentVersionDto(7500L, CODE, 2, LegalDocumentKind.TERMS,
                "Terminos del servicio", TEXTO, HUELLA, LocalDateTime.of(2026, 9, 1, 12, 0, 0),
                WebMvcSliceConfig.SYSTEM_USER_ID, LocalDate.of(2026, 9, 15), null, true,
                LocalDateTime.of(2026, 9, 1, 12, 0, 0));
    }

    @Nested
    @DisplayName("Publicacion")
    class Publicacion {

        @Test
        @DisplayName("responde 201 y el autor lo pone el principal, no el cuerpo")
        void responde_201_y_el_autor_lo_pone_el_principal() throws Exception {
            when(publishUseCase.execute(any())).thenReturn(version2());

            mockMvc.perform(
                    post("/legal-documents").contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {"code":"TERMS_OF_SERVICE","kind":"TERMS","title":"Terminos del servicio",
                                     "content":"Version 2 de los terminos.","effectiveFrom":"2026-09-15",
                                     "documentVersion":99,"publishedBySystemUserId":1}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.documentVersion").value(2))
                    .andExpect(jsonPath("$.contentHash").value(HUELLA))
                    .andExpect(jsonPath("$.current").value(true));

            ArgumentCaptor<PublishLegalDocumentVersionCommand> command = ArgumentCaptor
                    .forClass(PublishLegalDocumentVersionCommand.class);
            verify(publishUseCase).execute(command.capture());
            // El documentVersion 99 y el publishedBySystemUserId 1 del cuerpo se ignoran:
            // el request no los declara. El numero lo asigna el servidor y el autor sale
            // del principal.
            assertThat(command.getValue().publishedBySystemUserId())
                    .isEqualTo(WebMvcSliceConfig.SYSTEM_USER_ID);
            assertThat(command.getValue().content()).isEqualTo(TEXTO);
        }

        @Test
        @DisplayName("un contenido vacio sale 400 y NO llega al caso de uso")
        void un_contenido_vacio_sale_400() throws Exception {
            mockMvc.perform(
                    post("/legal-documents").contentType(MediaType.APPLICATION_JSON).content("""
                            {"code":"TERMS_OF_SERVICE","kind":"TERMS","title":"Terminos",
                             "content":"   ","effectiveFrom":"2026-09-15"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(publishUseCase);
        }
    }

    @Nested
    @DisplayName("No hay forma de editar un texto publicado")
    class NoSePuedeEditar {

        @Test
        @DisplayName("PUT sobre una version no existe: el disparador la rechazaria de todos modos")
        void no_hay_put() throws Exception {
            mockMvc.perform(
                    put("/legal-documents").contentType(MediaType.APPLICATION_JSON).content("""
                            {"content":"otro texto"}
                            """)).andExpect(status().isMethodNotAllowed());

            verifyNoInteractions(publishUseCase);
        }

        @Test
        @DisplayName("PATCH tampoco: un texto legal se sucede, no se edita")
        void no_hay_patch() throws Exception {
            mockMvc.perform(
                    patch("/legal-documents").contentType(MediaType.APPLICATION_JSON).content("""
                            {"content":"otro texto"}
                            """)).andExpect(status().isMethodNotAllowed());

            verifyNoInteractions(publishUseCase);
        }
    }

    @Nested
    @DisplayName("Lecturas")
    class Lecturas {

        @Test
        @DisplayName("el vigente sale con su texto completo y su huella")
        void el_vigente_sale_con_texto_y_huella() throws Exception {
            when(findCurrentUseCase.findCurrentByCode(anyString(), any())).thenReturn(version2());

            mockMvc.perform(get("/legal-documents/TERMS_OF_SERVICE/current"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content").value(TEXTO))
                    .andExpect(jsonPath("$.contentHash").value(HUELLA));
        }

        @Test
        @DisplayName("la huella devuelve el texto aceptado, que es la prueba del cliente")
        void la_huella_devuelve_el_texto_aceptado() throws Exception {
            when(findAcceptedUseCase.findByCodeAndHash(anyString(), anyString(), any()))
                    .thenReturn(version2());

            mockMvc.perform(get("/legal-documents/TERMS_OF_SERVICE/by-hash/" + HUELLA))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content").value(TEXTO));

            ArgumentCaptor<String> huella = ArgumentCaptor.forClass(String.class);
            verify(findAcceptedUseCase).findByCodeAndHash(anyString(), huella.capture(), any());
            assertThat(huella.getValue()).isEqualTo(HUELLA);
        }

        @Test
        @DisplayName("el historial paginado usa el contrato unico de pagina")
        void el_historial_usa_el_contrato_unico() throws Exception {
            when(listUseCase.listByCode(anyString(), any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(version2()), 0, 20, 1));

            mockMvc.perform(get("/legal-documents/TERMS_OF_SERVICE/versions"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }
}
