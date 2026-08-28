package com.vetsoftware.app.documentwithholding.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.documentwithholding.application.port.in.FindDocumentWithholdingUseCase;
import com.vetsoftware.app.documentwithholding.application.port.in.ListDocumentWithholdingsUseCase;
import com.vetsoftware.app.documentwithholding.application.port.in.ListUncertifiedDocumentWithholdingsByCompanyUseCase;
import com.vetsoftware.app.documentwithholding.testsupport.DocumentWithholdingMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja web del camino de <b>lectura</b> del tenant.
 *
 * <p>
 * Las dos cosas que esta clase congela y que ningun test de servicio ve:
 *
 * <ul>
 * <li><b>El {@code companyId} lo pone el contexto de autorizacion, no el
 * cliente.</b> Ninguno de los tres endpoints acepta una empresa por parametro;
 * si alguien anadiera un {@code @RequestParam Long companyId} «para la
 * consola», el {@code verify} con el valor exacto de
 * {@code authz.currentCompanyId()} lo caza.</li>
 * <li><b>El {@code certificateId} nulo llega al JSON como nulo y no
 * desaparece.</b> Ese nulo es el estado «sin respaldo», que es justo lo que el
 * cliente necesita ver para reclamar. Si alguien configurara la serializacion
 * para omitir nulos, el front no podria distinguir «no certificada» de «campo
 * que no vino».</li>
 * </ul>
 */
@WebMvcTest(DocumentWithholdingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("DocumentWithholdingController — contrato HTTP del tenant")
class DocumentWithholdingControllerTest {

    /**
     * Distinto del {@code COMPANY_ID} por defecto para que se vea de donde sale.
     */
    private static final Long EMPRESA_DEL_TOKEN = 77L;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private FindDocumentWithholdingUseCase findUseCase;
    @MockitoBean
    private ListDocumentWithholdingsUseCase listUseCase;
    @MockitoBean
    private ListUncertifiedDocumentWithholdingsByCompanyUseCase listUncertifiedUseCase;
    @MockitoBean
    private Authz authz;

    @BeforeEach
    void empresaDelContexto() {
        when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_TOKEN);
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("devuelve la retencion con cada campo en su lugar del JSON")
        void devuelve_la_retencion_con_cada_campo_en_su_lugar() throws Exception {
            when(findUseCase.findById(41L, EMPRESA_DEL_TOKEN))
                    .thenReturn(DocumentWithholdingMother.dto(41L, 8410L));

            mockMvc.perform(get("/document-withholdings/{id}", 41L)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(41))
                    .andExpect(jsonPath("$.companyId").value(900))
                    .andExpect(jsonPath("$.billingDocumentId").value(8400))
                    .andExpect(jsonPath("$.type").value("ICA"))
                    .andExpect(jsonPath("$.taxableBase").value(1234567.89))
                    // La tarifa sale en PORCENTAJE: 0,69 % es el 6,9 por mil de ICA.
                    // Quien pinte esto no debe multiplicar por cien.
                    .andExpect(jsonPath("$.ratePercent").value(0.690000))
                    .andExpect(jsonPath("$.amount").value(8518.52))
                    .andExpect(jsonPath("$.municipalityCode").value("05001"))
                    .andExpect(jsonPath("$.fiscalYear").value(2026))
                    .andExpect(jsonPath("$.fiscalPeriodKey").value("2026-B02"))
                    .andExpect(jsonPath("$.practicedOn").value("2026-03-05"))
                    .andExpect(jsonPath("$.certificateId").value(8410))
                    .andExpect(jsonPath("$.createdDate").value("2026-03-07T08:45:00"));
        }

        @Test
        @DisplayName("una retencion sin respaldo publica el certificado como nulo, no lo omite")
        void una_retencion_sin_respaldo_publica_el_certificado_como_nulo() throws Exception {
            when(findUseCase.findById(41L, EMPRESA_DEL_TOKEN))
                    .thenReturn(DocumentWithholdingMother.dto(41L, null));

            // La clave tiene que ESTAR y valer null. Por eso hasJsonPath y no exists:
            // el `exists()` de Spring falla sobre un valor nulo, asi que no distingue
            // «la clave vino con null» de «la clave no vino», que es justo la
            // distincion que aqui importa. Si alguien configurara NON_NULL en Jackson,
            // el front dejaria de poder separar «no certificada» —cartera que hay que
            // reclamar— de «este campo no llego», y este caso se pone rojo.
            mockMvc.perform(get("/document-withholdings/{id}", 41L)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.certificateId").hasJsonPath())
                    .andExpect(jsonPath("$.certificateId").value(org.hamcrest.Matchers.nullValue()))
                    .andExpect(jsonPath("$.municipalityCode").value("05001"));
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("el listado sin parametros pagina desde la primera y de veinte en veinte")
        void el_listado_sin_parametros_pagina_desde_la_primera() throws Exception {
            when(listUseCase.listByCompany(anyLong(), anyInt(), anyInt()))
                    .thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/document-withholdings")).andExpect(status().isOk());

            verify(listUseCase).listByCompany(EMPRESA_DEL_TOKEN, 0, 20);
        }

        @Test
        @DisplayName("la bandeja de reclamacion exige el ano y sin el sale 400")
        void la_bandeja_de_reclamacion_exige_el_ano() throws Exception {
            // El ano no tiene valor por defecto a proposito: un defecto silencioso —el
            // ano en curso— dejaria la bandeja del ejercicio anterior invisible justo
            // en enero, que es cuando hay que reclamarlo antes del plazo de marzo.
            mockMvc.perform(get("/document-withholdings/uncertified"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("la bandeja de reclamacion devuelve la pagina con sus totales")
        void la_bandeja_de_reclamacion_devuelve_la_pagina() throws Exception {
            when(listUncertifiedUseCase.listUncertifiedByCompany(anyLong(), anyInt(), anyInt(),
                    anyInt()))
                    .thenReturn(PageResult.of(List.of(DocumentWithholdingMother.dto(41L, null)), 0,
                            20, 3L));

            mockMvc.perform(get("/document-withholdings/uncertified").param("fiscalYear", "2026"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(41))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("acota la carga con la empresa del token y nunca con una del cliente")
        void acota_la_carga_con_la_empresa_del_token() throws Exception {
            when(findUseCase.findById(anyLong(), anyLong()))
                    .thenReturn(DocumentWithholdingMother.dto(41L, null));

            // El parametro companyId que se cuela en la query string no debe llegar al
            // caso de uso: la ruta no lo declara y la empresa sale del token.
            mockMvc.perform(get("/document-withholdings/{id}", 41L).param("companyId", "999"))
                    .andExpect(status().isOk());

            verify(findUseCase).findById(41L, EMPRESA_DEL_TOKEN);
        }

        @Test
        @DisplayName("la bandeja de reclamacion lleva el ano de la query y la empresa del token")
        void la_bandeja_lleva_el_ano_de_la_query_y_la_empresa_del_token() throws Exception {
            when(listUncertifiedUseCase.listUncertifiedByCompany(anyLong(), anyInt(), anyInt(),
                    anyInt())).thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/document-withholdings/uncertified").param("fiscalYear", "2025")
                    .param("companyId", "999")).andExpect(status().isOk());

            // Acotar por el ano no acota por nadie: sin la empresa del token, escribir
            // el id de la clinica vecina bastaria para ver que le deben certificar.
            ArgumentCaptor<Long> empresa = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Integer> ano = ArgumentCaptor.forClass(Integer.class);
            verify(listUncertifiedUseCase).listUncertifiedByCompany(empresa.capture(),
                    ano.capture(), eq(0), eq(20));
            assertThat(empresa.getValue()).isEqualTo(EMPRESA_DEL_TOKEN);
            assertThat(ano.getValue()).isEqualTo(2025);
        }

        @Test
        @DisplayName("el listado por empresa usa la del token y respeta la pagina pedida")
        void el_listado_por_empresa_usa_la_del_token() throws Exception {
            when(listUseCase.listByCompany(anyLong(), anyInt(), anyInt())).thenReturn(
                    PageResult.of(List.of(DocumentWithholdingMother.dto(41L, 8410L)), 2, 5, 11L));

            mockMvc.perform(get("/document-withholdings").param("page", "2").param("pageSize", "5"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(11))
                    .andExpect(jsonPath("$.totalPages").value(3));

            verify(listUseCase).listByCompany(EMPRESA_DEL_TOKEN, 2, 5);
        }
    }
}
