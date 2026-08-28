package com.vetsoftware.app.billingdocumentstatushistory.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.billingdocumentstatushistory.application.dto.BillingDocumentStatusHistoryDto;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.in.FindBillingDocumentStatusHistoryUseCase;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.in.ListBillingDocumentStatusChangesByStatusUseCase;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.in.ListBillingDocumentStatusHistoryUseCase;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatus;
import com.vetsoftware.app.billingdocumentstatushistory.testsupport.BillingDocumentStatusHistoryMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Rodaja web del camino de tenant, que es <b>solo de lectura</b>.
 *
 * <p>
 * Las tres cosas que esta clase congela y que ningun test de servicio ve:
 *
 * <ul>
 * <li><b>Aqui no se escribe, y la ausencia es la decision.</b> La historia de
 * estados es una de las seis tablas irreemplazables del modelo: un
 * {@code @PostMapping} de cliente dejaria al administrador de una clinica
 * apuntando sobre su propia factura la transicion que le conviene.
 * {@link Escrituras#el_controller_de_tenant_no_declara_escrituras()} lo deja
 * escrito en un test en vez de en un comentario, y se pone rojo el dia que
 * alguien reponga el endpoint.</li>
 * <li><b>El {@code companyId} lo pone el contexto de autorizacion, no el
 * cliente.</b> Ninguno de los tres endpoints lo acepta, ni en el cuerpo ni como
 * parametro.</li>
 * <li><b>El listado por documento acota igualmente por empresa.</b> La ruta
 * cuelga de una FK ajena, que no es un filtro de tenant (BE-29).</li>
 * </ul>
 *
 * <p>
 * La rodaja mockea el caso de uso, asi que el {@code @PreAuthorize} del puerto
 * <b>no se ejercita aqui</b>: quien lo comprueba es
 * {@code GranularPermissionGateTest}.
 */
@WebMvcTest(BillingDocumentStatusHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("BillingDocumentStatusHistoryController — contrato HTTP del tenant")
class BillingDocumentStatusHistoryControllerTest {

    /**
     * Distinto del {@code COMPANY_ID} por defecto para que se vea de donde sale.
     */
    private static final Long EMPRESA_DEL_TOKEN = 77L;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private FindBillingDocumentStatusHistoryUseCase findUseCase;
    @MockitoBean
    private ListBillingDocumentStatusHistoryUseCase listByDocumentUseCase;
    @MockitoBean
    private ListBillingDocumentStatusChangesByStatusUseCase listByStatusUseCase;
    @MockitoBean
    private Authz authz;

    @BeforeEach
    void empresaDelContexto() {
        when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_TOKEN);
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("devuelve el fotograma pedido con cada campo en su lugar del JSON")
        void devuelve_el_fotograma_pedido() throws Exception {
            when(findUseCase.findById(41L, EMPRESA_DEL_TOKEN))
                    .thenReturn(BillingDocumentStatusHistoryMother.dto(41L));

            mockMvc.perform(get("/billing-document-status-history/{id}", 41L))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(41))
                    .andExpect(jsonPath("$.companyId").value(900))
                    .andExpect(jsonPath("$.fromStatus").value("DRAFT"))
                    .andExpect(jsonPath("$.toStatus").value("AWAITING_EXTERNAL"));
        }

        @Test
        @DisplayName("la pelicula de un documento sale paginada con sus totales")
        void la_pelicula_de_un_documento_sale_paginada() throws Exception {
            when(listByDocumentUseCase.listByDocument(EMPRESA_DEL_TOKEN, 8500L, 0, 20))
                    .thenReturn(pagina());

            mockMvc.perform(get("/billing-document-status-history/documents/{id}", 8500L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].toStatus").value("AWAITING_EXTERNAL"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("la bandeja por estado pasa el estado pedido al caso de uso")
        void la_bandeja_por_estado_pasa_el_estado_pedido() throws Exception {
            when(listByStatusUseCase.listByCompanyAndToStatus(EMPRESA_DEL_TOKEN,
                    BillingDocumentStatus.AWAITING_EXTERNAL, 0, 20)).thenReturn(pagina());

            mockMvc.perform(get("/billing-document-status-history/by-status").param("toStatus",
                    "AWAITING_EXTERNAL")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].billingDocumentId").value(8500));
        }
    }

    @Nested
    @DisplayName("Escrituras")
    class Escrituras {

        @Test
        @DisplayName("el controller de tenant no declara ni un solo metodo de escritura")
        void el_controller_de_tenant_no_declara_escrituras() {
            // No hay POST y no debe haberlo. Quien escribe la bitacora decide que se
            // puede probar en una disputa, y una clinica escribiendo sobre su propia
            // factura no es fuga entre empresas: es una fila legitima que falsea el
            // expediente. El registro vive en la cara de plataforma, igual que el de
            // PaymentRefundController.
            //
            // Se afirma por reflexion y no con un POST contra MockMvc a proposito: sin
            // ningun mapping en la raiz del recurso, Spring responde 404 y no 405, asi
            // que un 405 esperado se caeria por el motivo equivocado y un 404 esperado
            // pasaria igual con la ruta mal escrita. La anotacion no admite esa duda.
            assertThat(BillingDocumentStatusHistoryController.class.getDeclaredMethods())
                    .allSatisfy(metodo -> assertThat(metodo.getAnnotations())
                            .noneMatch(anotacion -> anotacion instanceof PostMapping
                                    || anotacion instanceof PutMapping
                                    || anotacion instanceof PatchMapping
                                    || anotacion instanceof DeleteMapping
                                    || (anotacion instanceof RequestMapping mapeo
                                            && Arrays.stream(mapeo.method()).anyMatch(
                                                    verbo -> verbo != RequestMethod.GET))));
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la carga por id lleva la empresa del token, no una que venga en la URL")
        void la_carga_por_id_lleva_la_empresa_del_token() throws Exception {
            when(findUseCase.findById(41L, EMPRESA_DEL_TOKEN))
                    .thenReturn(BillingDocumentStatusHistoryMother.dto(41L));

            mockMvc.perform(get("/billing-document-status-history/{id}", 41L))
                    .andExpect(status().isOk());

            verify(findUseCase).findById(41L, EMPRESA_DEL_TOKEN);
        }

        @Test
        @DisplayName("el listado por documento tambien acota por la empresa del token")
        void el_listado_por_documento_acota_por_la_empresa_del_token() throws Exception {
            // La ruta acota por la FK ajena, que NO es un filtro de tenant (BE-29): el
            // documento es de alguien y quien escribe ese id en la URL es el cliente.
            // Este verify congela que la empresa viaja igualmente.
            when(listByDocumentUseCase.listByDocument(EMPRESA_DEL_TOKEN, 8500L, 0, 20))
                    .thenReturn(pagina());

            mockMvc.perform(get("/billing-document-status-history/documents/{id}", 8500L))
                    .andExpect(status().isOk());

            verify(listByDocumentUseCase).listByDocument(EMPRESA_DEL_TOKEN, 8500L, 0, 20);
        }
    }

    private static PageResult<BillingDocumentStatusHistoryDto> pagina() {
        return new PageResult<>(List.of(BillingDocumentStatusHistoryMother.dto(41L)), 0, 20, 1L, 1);
    }
}
