package com.vetsoftware.app.externalinvoicingoutage.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.externalinvoicingoutage.application.command.EndExternalInvoicingOutageCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.command.NotifyAffectedCompaniesCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.command.OpenExternalInvoicingOutageCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.command.RegisterAffectedCompanyCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import com.vetsoftware.app.externalinvoicingoutage.application.dto.OutageAffectedCompanyDto;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.EndExternalInvoicingOutageUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.FindExternalInvoicingOutageUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.ListExternalInvoicingOutagesUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.ListOpenExternalInvoicingOutagesUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.ListOutageAffectedCompaniesUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.NotifyAffectedCompaniesUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.OpenExternalInvoicingOutageUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.RegisterAffectedCompanyUseCase;
import com.vetsoftware.app.externalinvoicingoutage.domain.CauseParty;
import com.vetsoftware.app.externalinvoicingoutage.domain.OutageResolution;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.hamcrest.Matchers;
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
 * Rodaja web de las caidas de la emision fiscal, que son solo de plataforma:
 * una caida es un hecho de VetSoftware, no de una clinica.
 *
 * <p>
 * Lo que congela esta clase y no ve ningun test de servicio:
 *
 * <ul>
 * <li><b>Los cinco campos del cuerpo de apertura llegan al command en su
 * posicion.</b> Dos son instantes y uno es un enumerado; cruzar
 * {@code startedAt} con cualquier otra fecha compila sin una queja. Por eso el
 * caso feliz captura el command y compara componente a componente con valores
 * todos distintos.</li>
 * <li><b>Los dos identificadores del reparto salen de la ruta, no del
 * cuerpo.</b> {@code POST /{id}/companies/{companyId}} lleva <em>dos</em>
 * {@code @PathVariable} del mismo tipo, uno detras de otro: invertirlos
 * compilaria, respondería 201 y colgaria el reparto de la caida equivocada. Es
 * el unico defecto de esta rodaja que ninguna capa de abajo puede cazar, porque
 * abajo los dos son {@code Long}.</li>
 * <li><b>La empresa viaja en la ruta y no en el cuerpo</b>
 * ({@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}, regla dura): el
 * {@code RegisterAffectedCompanyRequest} no tiene campo de empresa, y este caso
 * se pone rojo el dia que alguien se lo anada.</li>
 * </ul>
 *
 * <p>
 * <b>Lo que esta clase NO cubre, y por que.</b> No hay caso de 403:
 * {@code @AutoConfigureMockMvc(addFilters = false)} apaga la cadena de
 * seguridad, y el gate vive en el {@code @PreAuthorize} de los puertos de
 * entrada, que aqui son {@code @MockitoBean}. Un caso que esperara 403 pasaria
 * por el motivo equivocado —o no pasaria nunca— y seguiria verde el dia que
 * alguien borrara la anotacion. Quien vigila esos gates es
 * {@code HexagonalArchitectureTest}. Tampoco hay casos de 404 ni de 409: esas
 * excepciones se acaban de cablear en {@code GlobalExceptionHandler} y sus
 * casos van cuando la rodaja de servicio las ejercite.
 */
@WebMvcTest(SystemExternalInvoicingOutageController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemExternalInvoicingOutageController — contrato HTTP de plataforma")
class SystemExternalInvoicingOutageControllerTest {

    private static final Long CAIDA_ID = 4410L;
    private static final Long EMPRESA_ID = 77L;

    private static final LocalDateTime EMPEZO = LocalDateTime.of(2026, 3, 10, 8, 15, 0);
    private static final LocalDateTime TERMINO = LocalDateTime.of(2026, 3, 10, 14, 42, 30);
    private static final LocalDateTime AVISADO = LocalDateTime.of(2026, 3, 10, 9, 0, 0);
    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 3, 10, 8, 16, 0);

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private OpenExternalInvoicingOutageUseCase openUseCase;
    @MockitoBean
    private EndExternalInvoicingOutageUseCase endUseCase;
    @MockitoBean
    private NotifyAffectedCompaniesUseCase notifyUseCase;
    @MockitoBean
    private FindExternalInvoicingOutageUseCase findUseCase;
    @MockitoBean
    private ListExternalInvoicingOutagesUseCase listUseCase;
    @MockitoBean
    private ListOpenExternalInvoicingOutagesUseCase listOpenUseCase;
    @MockitoBean
    private RegisterAffectedCompanyUseCase registerAffectedUseCase;
    @MockitoBean
    private ListOutageAffectedCompaniesUseCase listAffectedUseCase;

    @Nested
    @DisplayName("Apertura")
    class Apertura {

        @Test
        @DisplayName("responde 201 y traslada los cinco campos del cuerpo al command sin cruzarlos")
        void responde_201_y_traslada_los_cinco_campos_sin_cruzarlos() throws Exception {
            when(openUseCase.execute(any())).thenReturn(caidaViva());

            mockMvc.perform(post("/system/external-invoicing-outages")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "startedAt": "2026-03-10T08:15:00",
                              "causeParty": "EXTERNAL_ISSUER",
                              "summary": "El proveedor no responde",
                              "affectedCompanyCount": 40,
                              "externalIncidentRef": "INC-2026-0310"
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(CAIDA_ID))
                    .andExpect(jsonPath("$.causeParty").value("EXTERNAL_ISSUER"))
                    .andExpect(jsonPath("$.open").value(true))
                    .andExpect(jsonPath("$.endedAt").doesNotExist());

            ArgumentCaptor<OpenExternalInvoicingOutageCommand> command = ArgumentCaptor
                    .forClass(OpenExternalInvoicingOutageCommand.class);
            verify(openUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.startedAt()).isEqualTo(EMPEZO);
                assertThat(cmd.causeParty()).isEqualTo(CauseParty.EXTERNAL_ISSUER);
                assertThat(cmd.summary()).isEqualTo("El proveedor no responde");
                assertThat(cmd.affectedCompanyCount()).isEqualTo(40);
                assertThat(cmd.externalIncidentRef()).isEqualTo("INC-2026-0310");
            });
        }

        @Test
        @DisplayName("una caida sin radicado del proveedor entra igual: puede no haberlo todavia")
        void una_caida_sin_radicado_entra_igual() throws Exception {
            // El radicado es lo que traslada la responsabilidad con nombre y numero,
            // pero en caliente todavia no se tiene: exigirlo obligaria a esperar al
            // proveedor para abrir la ficha, que es justo cuando mas falta hace.
            when(openUseCase.execute(any())).thenReturn(caidaViva());

            mockMvc.perform(post("/system/external-invoicing-outages")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "startedAt": "2026-03-10T08:15:00",
                              "causeParty": "NETWORK",
                              "summary": "Corte de transporte",
                              "affectedCompanyCount": 0
                            }
                            """)).andExpect(status().isCreated());

            ArgumentCaptor<OpenExternalInvoicingOutageCommand> command = ArgumentCaptor
                    .forClass(OpenExternalInvoicingOutageCommand.class);
            verify(openUseCase).execute(command.capture());
            assertThat(command.getValue().externalIncidentRef()).isNull();
        }

        @Test
        @DisplayName("un cuerpo sin inicio, sin causante y sin resumen sale 400 nombrando los tres")
        void un_cuerpo_sin_los_obligatorios_sale_400_nombrandolos() throws Exception {
            // El @Valid del @RequestBody es lo unico que dispara el validador; sin el,
            // los @NotNull y el @NotBlank del record estan escritos y no se evaluan
            // nunca (#135). Este caso se pone rojo el dia que alguien lo quite.
            mockMvc.perform(post("/system/external-invoicing-outages")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "affectedCompanyCount": 3
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[*].field",
                            Matchers.containsInAnyOrder("startedAt", "causeParty", "summary")));

            verifyNoInteractions(openUseCase);
        }

        @Test
        @DisplayName("un contador de alcanzadas negativo sale 400")
        void un_contador_negativo_sale_400() throws Exception {
            mockMvc.perform(post("/system/external-invoicing-outages")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "startedAt": "2026-03-10T08:15:00",
                              "causeParty": "NETWORK",
                              "summary": "Corte de transporte",
                              "affectedCompanyCount": -1
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("affectedCompanyCount"));

            verifyNoInteractions(openUseCase);
        }

        @Test
        @DisplayName("un causante que no existe se rechaza en el binder")
        void un_causante_que_no_existe_se_rechaza() throws Exception {
            // cause_party es la columna que separa un incidente de un incumplimiento
            // propio: un valor fuera de los cuatro no puede entrar por HTTP y quedarse
            // sin clasificar ante la autoridad.
            mockMvc.perform(post("/system/external-invoicing-outages")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "startedAt": "2026-03-10T08:15:00",
                              "causeParty": "PROVIDER",
                              "summary": "Corte de transporte",
                              "affectedCompanyCount": 0
                            }
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(openUseCase);
        }
    }

    @Nested
    @DisplayName("Cierre")
    class Cierre {

        @Test
        @DisplayName("PATCH y no DELETE: el id sale de la ruta y la hora de vuelta del cuerpo")
        void patch_con_el_id_en_la_ruta_y_la_hora_en_el_cuerpo() throws Exception {
            when(endUseCase.execute(any())).thenReturn(caidaCerrada());

            mockMvc.perform(patch("/system/external-invoicing-outages/{id}/end", CAIDA_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "endedAt": "2026-03-10T14:42:30"
                            }
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(CAIDA_ID))
                    .andExpect(jsonPath("$.open").value(false));

            ArgumentCaptor<EndExternalInvoicingOutageCommand> command = ArgumentCaptor
                    .forClass(EndExternalInvoicingOutageCommand.class);
            verify(endUseCase).execute(command.capture());
            assertThat(command.getValue().id()).isEqualTo(CAIDA_ID);
            assertThat(command.getValue().endedAt()).isEqualTo(TERMINO);
        }

        @Test
        @DisplayName("cerrar sin hora sale 400: cerrar es escribir una hora, no borrar la ficha")
        void cerrar_sin_hora_sale_400() throws Exception {
            mockMvc.perform(patch("/system/external-invoicing-outages/{id}/end", CAIDA_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "endedAt": null
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[0].field").value("endedAt"));

            verifyNoInteractions(endUseCase);
        }
    }

    @Nested
    @DisplayName("Aviso")
    class Aviso {

        @Test
        @DisplayName("el aviso traslada la hora y el contador corregido")
        void el_aviso_traslada_la_hora_y_el_contador() throws Exception {
            when(notifyUseCase.execute(any())).thenReturn(caidaViva());

            mockMvc.perform(patch("/system/external-invoicing-outages/{id}/notify", CAIDA_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "notifiedAt": "2026-03-10T09:00:00",
                              "affectedCompanyCount": 52
                            }
                            """)).andExpect(status().isOk());

            ArgumentCaptor<NotifyAffectedCompaniesCommand> command = ArgumentCaptor
                    .forClass(NotifyAffectedCompaniesCommand.class);
            verify(notifyUseCase).execute(command.capture());
            assertThat(command.getValue().id()).isEqualTo(CAIDA_ID);
            assertThat(command.getValue().notifiedAt()).isEqualTo(AVISADO);
            assertThat(command.getValue().affectedCompanyCount()).isEqualTo(52);
        }

        @Test
        @DisplayName("avisar sin hora sale 400")
        void avisar_sin_hora_sale_400() throws Exception {
            mockMvc.perform(patch("/system/external-invoicing-outages/{id}/notify", CAIDA_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "affectedCompanyCount": 52
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("notifiedAt"));

            verifyNoInteractions(notifyUseCase);
        }
    }

    @Nested
    @DisplayName("Afectados")
    class Afectados {

        @Test
        @DisplayName("los DOS identificadores salen de la ruta, y en su orden")
        void los_dos_identificadores_salen_de_la_ruta_en_su_orden() throws Exception {
            // El defecto que este caso existe para cazar: los dos @PathVariable son
            // Long y van seguidos. Invertirlos compila, responde 201 y cuelga el
            // reparto de la caida equivocada; ninguna capa de abajo puede verlo.
            // Por eso los dos valores son deliberadamente distintos.
            when(registerAffectedUseCase.execute(any())).thenReturn(afectada());

            mockMvc.perform(post("/system/external-invoicing-outages/{id}/companies/{companyId}",
                    CAIDA_ID, EMPRESA_ID).contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "failedDocumentCount": 17,
                              "resolvedBy": "CONTINGENCY_NUMBERING"
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.outageId").value(CAIDA_ID))
                    .andExpect(jsonPath("$.companyId").value(EMPRESA_ID))
                    .andExpect(jsonPath("$.contingencyNumbering").value(true));

            ArgumentCaptor<RegisterAffectedCompanyCommand> command = ArgumentCaptor
                    .forClass(RegisterAffectedCompanyCommand.class);
            verify(registerAffectedUseCase).execute(command.capture());
            assertThat(command.getValue().outageId()).isEqualTo(CAIDA_ID);
            assertThat(command.getValue().companyId()).isEqualTo(EMPRESA_ID);
            assertThat(command.getValue().failedDocumentCount()).isEqualTo(17);
            assertThat(command.getValue().resolvedBy())
                    .isEqualTo(OutageResolution.CONTINGENCY_NUMBERING);
        }

        @Test
        @DisplayName("registrar sin decir como se resolvio sale 400")
        void registrar_sin_resolucion_sale_400() throws Exception {
            mockMvc.perform(post("/system/external-invoicing-outages/{id}/companies/{companyId}",
                    CAIDA_ID, EMPRESA_ID).contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "failedDocumentCount": 17
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("resolvedBy"));

            verifyNoInteractions(registerAffectedUseCase);
        }

        @Test
        @DisplayName("un numero de documentos fallidos negativo sale 400")
        void un_numero_de_documentos_negativo_sale_400() throws Exception {
            mockMvc.perform(post("/system/external-invoicing-outages/{id}/companies/{companyId}",
                    CAIDA_ID, EMPRESA_ID).contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "failedDocumentCount": -1,
                              "resolvedBy": "RETRIED"
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("failedDocumentCount"));

            verifyNoInteractions(registerAffectedUseCase);
        }

        @Test
        @DisplayName("el listado de afectados sale paginado y acotado a su caida")
        void el_listado_de_afectados_sale_paginado() throws Exception {
            when(listAffectedUseCase.listByOutage(eq(CAIDA_ID), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(afectada()), 0, 20, 1L));

            mockMvc.perform(get("/system/external-invoicing-outages/{id}/companies", CAIDA_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].companyId").value(EMPRESA_ID))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(listAffectedUseCase).listByOutage(CAIDA_ID, 0, 20);
        }
    }

    @Nested
    @DisplayName("Listados")
    class Listados {

        @Test
        @DisplayName("el historico sale paginado con los cinco campos de la pagina")
        void el_historico_sale_paginado() throws Exception {
            when(listUseCase.listAll(anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(caidaViva()), 0, 20, 1L));

            mockMvc.perform(get("/system/external-invoicing-outages")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(CAIDA_ID))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.pageSize").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));

            // El defecto por defecto: si el controller dejara de declarar los
            // defaultValue, page y pageSize llegarian nulos y reventaria el binder.
            verify(listUseCase).listAll(0, 20);
        }

        @Test
        @DisplayName("las abiertas salen como lista pelada, sin envoltorio de pagina")
        void las_abiertas_salen_como_lista_pelada() throws Exception {
            // Es la bandeja de incidencias vivas: son pocas por construccion
            // —uq_eio_open admite como mucho una por causante— y paginarlas seria
            // envolver cuatro filas en cinco campos de metadatos.
            when(listOpenUseCase.listOpen()).thenReturn(List.of(caidaViva()));

            mockMvc.perform(get("/system/external-invoicing-outages/open"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(CAIDA_ID))
                    .andExpect(jsonPath("$[0].open").value(true));
        }

        @Test
        @DisplayName("la ficha de una caida sale por su id")
        void la_ficha_de_una_caida_sale_por_su_id() throws Exception {
            when(findUseCase.execute(CAIDA_ID)).thenReturn(caidaCerrada());

            mockMvc.perform(get("/system/external-invoicing-outages/{id}", CAIDA_ID))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(CAIDA_ID))
                    .andExpect(jsonPath("$.summary").value("El proveedor no responde"));

            verify(findUseCase).execute(CAIDA_ID);
        }
    }

    private static ExternalInvoicingOutageDto caidaViva() {
        return new ExternalInvoicingOutageDto(CAIDA_ID, EMPEZO, null, CauseParty.EXTERNAL_ISSUER,
                "El proveedor no responde", 40, null, "INC-2026-0310", true, CREADO_EL);
    }

    private static ExternalInvoicingOutageDto caidaCerrada() {
        return new ExternalInvoicingOutageDto(CAIDA_ID, EMPEZO, TERMINO, CauseParty.EXTERNAL_ISSUER,
                "El proveedor no responde", 40, AVISADO, "INC-2026-0310", false, CREADO_EL);
    }

    private static OutageAffectedCompanyDto afectada() {
        return new OutageAffectedCompanyDto(9001L, CAIDA_ID, EMPRESA_ID, 17,
                OutageResolution.CONTINGENCY_NUMBERING, true);
    }
}
