package com.vetsoftware.app.billingdocumentstatushistory.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.billingdocumentstatushistory.application.command.RecordBillingDocumentStatusChangeCommand;
import com.vetsoftware.app.billingdocumentstatushistory.application.dto.BillingDocumentStatusHistoryDto;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.in.ListAllBillingDocumentStatusHistoryUseCase;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.in.RecordBillingDocumentStatusChangeUseCase;
import com.vetsoftware.app.billingdocumentstatushistory.testsupport.BillingDocumentStatusHistoryMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
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
 * Rodaja web del camino de plataforma, que es <b>el unico que escribe</b>.
 *
 * <p>
 * Lo que congela y no se ve en ningun otro sitio:
 *
 * <ul>
 * <li><b>La empresa viaja como {@code @RequestParam} y no en el cuerpo.</b> El
 * request no tiene {@code companyId} —lo prohibe
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}— y quien escribe es tesoreria, que no
 * tiene empresa propia y la elige. El {@code ArgumentCaptor} afirma que lo que
 * llega al command es el parametro y no otra cosa.</li>
 * <li><b>El {@code occurredAt} no viaja en el cuerpo.</b> Si alguien lo
 * anadiera al request «para poder cargar historia», permitiria antedatar un
 * movimiento y con ello reescribir cuantos documentos esperaban factura externa
 * a una fecha.</li>
 * <li><b>Las restricciones del request se evaluan de verdad.</b> Sin
 * {@code @Valid} delante del {@code @RequestBody}, el binder no dispara el
 * validador y el {@code @NotBlank} del motivo estaria escrito sin ejecutarse
 * nunca (#135).</li>
 * </ul>
 *
 * <p>
 * La rodaja mockea el caso de uso, asi que el {@code hasRole('SYSTEM')} a secas
 * del puerto <b>no se ejercita aqui</b>: quien lo comprueba es
 * {@code GranularPermissionGateTest}.
 */
@WebMvcTest(SystemBillingDocumentStatusHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemBillingDocumentStatusHistoryController — contrato HTTP de plataforma")
class SystemBillingDocumentStatusHistoryControllerTest {

    private static final String CUERPO_VALIDO = """
            {"billingDocumentId":8500,"fromStatus":"DRAFT","toStatus":"AWAITING_EXTERNAL",
             "actor":"Laura Restrepo","reason":"Factura externa FE-1043 registrada"}
            """;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RecordBillingDocumentStatusChangeUseCase recordUseCase;
    @MockitoBean
    private ListAllBillingDocumentStatusHistoryUseCase listAllUseCase;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("registrar un cambio responde 201 con el fotograma en el JSON")
        void registrar_un_cambio_responde_201() throws Exception {
            when(recordUseCase.execute(any()))
                    .thenReturn(BillingDocumentStatusHistoryMother.dto(41L));

            mockMvc.perform(
                    post("/system/billing-document-status-history").param("companyId", "900")
                            .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(41))
                    .andExpect(jsonPath("$.billingDocumentId").value(8500))
                    .andExpect(jsonPath("$.fromStatus").value("DRAFT"))
                    .andExpect(jsonPath("$.toStatus").value("AWAITING_EXTERNAL"))
                    .andExpect(jsonPath("$.actor").value("Laura Restrepo"))
                    .andExpect(jsonPath("$.reason").value("Factura externa FE-1043 registrada"))
                    .andExpect(jsonPath("$.occurredAt").value("2026-03-05T09:30:00"))
                    .andExpect(jsonPath("$.createdDate").value("2026-03-05T14:15:00"));
        }

        @Test
        @DisplayName("el barrido sin empresa pasa null al caso de uso y devuelve la pagina")
        void el_barrido_sin_empresa_pasa_null() throws Exception {
            when(listAllUseCase.listAll(null, 0, 20)).thenReturn(pagina());

            mockMvc.perform(get("/system/billing-document-status-history"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(41))
                    .andExpect(jsonPath("$.content[0].toStatus").value("AWAITING_EXTERNAL"))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(listAllUseCase).listAll(null, 0, 20);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un motivo en blanco lo rechaza el validador antes de llegar al caso de uso")
        void un_motivo_en_blanco_lo_rechaza_el_validador() throws Exception {
            // Sin @Valid delante del @RequestBody el binder no dispara el validador y
            // este cuerpo llegaria intacto al servicio (#135). El verifyNoInteractions
            // es lo que distingue «lo paro la validacion» de «lo paro el dominio».
            mockMvc.perform(post("/system/billing-document-status-history")
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON).content("""
                            {"billingDocumentId":8500,"fromStatus":"DRAFT",
                             "toStatus":"AWAITING_EXTERNAL","actor":"Laura Restrepo","reason":"  "}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(recordUseCase);
        }

        @Test
        @DisplayName("un actor de mas de 120 caracteres lo rechaza el validador")
        void un_actor_demasiado_largo_lo_rechaza_el_validador() throws Exception {
            mockMvc.perform(post("/system/billing-document-status-history")
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"billingDocumentId\":8500,\"fromStatus\":\"DRAFT\","
                            + "\"toStatus\":\"AWAITING_EXTERNAL\",\"actor\":\"" + "a".repeat(121)
                            + "\",\"reason\":\"motivo\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(recordUseCase);
        }

        @Test
        @DisplayName("sin documento de cobro el cuerpo no pasa")
        void sin_documento_de_cobro_el_cuerpo_no_pasa() throws Exception {
            mockMvc.perform(post("/system/billing-document-status-history")
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON).content("""
                            {"fromStatus":"DRAFT","toStatus":"AWAITING_EXTERNAL",
                             "actor":"Laura Restrepo","reason":"motivo"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(recordUseCase);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la empresa del command sale del parametro y no del cuerpo, que ni la trae")
        void la_empresa_del_command_sale_del_parametro() throws Exception {
            // El cuerpo declara un companyId que el record no tiene: Jackson lo ignora,
            // y lo que llega al command sigue siendo el del parametro. Si alguien
            // anadiera el campo al request, este caso se pone rojo.
            when(recordUseCase.execute(any()))
                    .thenReturn(BillingDocumentStatusHistoryMother.dto(41L));

            mockMvc.perform(post("/system/billing-document-status-history")
                    .param("companyId", "900").contentType(MediaType.APPLICATION_JSON).content("""
                            {"companyId":123456,"billingDocumentId":8500,"fromStatus":"DRAFT",
                             "toStatus":"AWAITING_EXTERNAL","actor":"Laura Restrepo",
                             "reason":"motivo"}
                            """)).andExpect(status().isCreated());

            ArgumentCaptor<RecordBillingDocumentStatusChangeCommand> capturado = ArgumentCaptor
                    .forClass(RecordBillingDocumentStatusChangeCommand.class);
            verify(recordUseCase).execute(capturado.capture());
            assertThat(capturado.getValue().companyId()).isEqualTo(900L);
        }

        @Test
        @DisplayName("el filtro de empresa viaja como parametro y llega tal cual al caso de uso")
        void el_filtro_de_empresa_viaja_como_parametro() throws Exception {
            // Aqui la empresa la elige quien pregunta y no el token: la proteccion no
            // es que el servidor la inyecte —no puede— sino que el caso de uso esta
            // cerrado a hasRole('SYSTEM') a secas.
            when(listAllUseCase.listAll(BillingDocumentStatusHistoryMother.OTRA_EMPRESA, 0, 20))
                    .thenReturn(pagina());

            mockMvc.perform(get("/system/billing-document-status-history").param("companyId",
                    String.valueOf(BillingDocumentStatusHistoryMother.OTRA_EMPRESA)))
                    .andExpect(status().isOk());

            verify(listAllUseCase).listAll(BillingDocumentStatusHistoryMother.OTRA_EMPRESA, 0, 20);
        }

        @Test
        @DisplayName("la paginacion pedida llega al caso de uso sin recalcularse")
        void la_paginacion_pedida_llega_al_caso_de_uso() throws Exception {
            when(listAllUseCase.listAll(null, 2, 50)).thenReturn(pagina());

            mockMvc.perform(get("/system/billing-document-status-history").param("page", "2")
                    .param("pageSize", "50")).andExpect(status().isOk());

            verify(listAllUseCase).listAll(null, 2, 50);
        }
    }

    private static PageResult<BillingDocumentStatusHistoryDto> pagina() {
        return new PageResult<>(List.of(BillingDocumentStatusHistoryMother.dto(41L)), 0, 20, 1L, 1);
    }
}
