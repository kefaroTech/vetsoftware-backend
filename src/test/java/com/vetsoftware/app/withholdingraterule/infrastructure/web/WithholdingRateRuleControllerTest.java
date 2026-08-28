package com.vetsoftware.app.withholdingraterule.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import com.vetsoftware.app.withholdingraterule.application.port.in.FindWithholdingRateRuleUseCase;
import com.vetsoftware.app.withholdingraterule.application.port.in.ListWithholdingRateRulesUseCase;
import com.vetsoftware.app.withholdingraterule.application.port.in.ResolveWithholdingRateRuleUseCase;
import com.vetsoftware.app.withholdingraterule.domain.ServiceNature;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingType;
import com.vetsoftware.app.withholdingraterule.testsupport.WithholdingRateRuleMother;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja web del camino de <b>lectura</b> del tenant.
 *
 * <p>
 * Las dos cosas que esta clase congela y que ningun test de servicio ve:
 *
 * <ul>
 * <li><b>La empresa la pone el contexto de autorizacion, no el cliente.</b>
 * Ninguno de los tres endpoints acepta un {@code companyId} por parametro, y en
 * este slice eso importa mas de lo habitual: como el catalogo es global, un
 * {@code companyId} del cliente no cambiaria ni una fila del resultado y por
 * eso pasaria desapercibido en una revision. Lo que si cambiaria es a quien
 * autoriza el {@code @authz.isMyCompany(#companyId)} del puerto.</li>
 * <li><b>La tarifa sale por HTTP con sus seis decimales.</b> Es el campo que un
 * cambio de forma en la respuesta puede estropear en silencio.</li>
 * </ul>
 *
 * <p>
 * <b>Lo que esta clase NO cubre todavia:</b> el 404 de
 * {@code WithholdingRateRuleNotFoundException} y el de
 * {@code NoEffectiveWithholdingRateRuleException}. Las dos son excepciones de
 * dominio nuevas y {@code GlobalExceptionHandler} aun no las enumera, asi que
 * hoy saldrian como 500. Cuando se cableen, aqui van sus dos casos.
 */
@WebMvcTest(WithholdingRateRuleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("WithholdingRateRuleController — contrato HTTP del tenant")
class WithholdingRateRuleControllerTest {

    /**
     * Distinto del {@code COMPANY_ID} por defecto para que se vea de donde sale.
     */
    private static final Long EMPRESA_DEL_TOKEN = 77L;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private FindWithholdingRateRuleUseCase findUseCase;
    @MockitoBean
    private ListWithholdingRateRulesUseCase listUseCase;
    @MockitoBean
    private ResolveWithholdingRateRuleUseCase resolveUseCase;
    @MockitoBean
    private Authz authz;

    @BeforeEach
    void empresaDelContexto() {
        when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_TOKEN);
    }

    @Nested
    @DisplayName("Consulta por id")
    class ConsultaPorId {

        @Test
        @DisplayName("devuelve la tarifa con cada campo en su lugar del JSON")
        void devuelve_la_tarifa_con_cada_campo_en_su_lugar() throws Exception {
            when(findUseCase.findById(8302L, EMPRESA_DEL_TOKEN))
                    .thenReturn(WithholdingRateRuleMother.dtoIca());

            mockMvc.perform(get("/withholding-rate-rules/{id}", 8302L)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(8302))
                    .andExpect(jsonPath("$.withholdingType").value("ICA"))
                    .andExpect(jsonPath("$.serviceNature").value("CONSULTING"))
                    .andExpect(jsonPath("$.municipalityCode").value("11001"))
                    .andExpect(jsonPath("$.ratePercent").value(0.690000))
                    .andExpect(jsonPath("$.minimumBaseAmount").value(213010.00))
                    .andExpect(jsonPath("$.minimumBaseUvt").value(4.00))
                    .andExpect(jsonPath("$.legalReference").value("Acuerdo 65 de 2002"))
                    .andExpect(jsonPath("$.validFrom").value("2026-01-01"))
                    .andExpect(jsonPath("$.validTo").doesNotExist())
                    .andExpect(jsonPath("$.createdDate").value("2026-01-03T08:45:00"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("no expone la version del bloqueo optimista ni las columnas generadas")
        void no_expone_la_version_ni_las_columnas_generadas() throws Exception {
            // El dia que alguien añada version, municipalityKey o
            // currentRuleMarker a la respuesta «por comodidad», este caso se pone
            // rojo. Es toda su razon de ser: son detalle del motor y publicarlos
            // invita a construir logica sobre un centinela de base de datos.
            when(findUseCase.findById(8302L, EMPRESA_DEL_TOKEN))
                    .thenReturn(WithholdingRateRuleMother.dtoIca());

            mockMvc.perform(get("/withholding-rate-rules/{id}", 8302L)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.version").doesNotExist())
                    .andExpect(jsonPath("$.municipalityKey").doesNotExist())
                    .andExpect(jsonPath("$.currentRuleMarker").doesNotExist());
        }

        @Test
        @DisplayName("autoriza con la empresa del token y nunca con una que escriba el cliente")
        void autoriza_con_la_empresa_del_token() throws Exception {
            when(findUseCase.findById(anyLong(), anyLong()))
                    .thenReturn(WithholdingRateRuleMother.dtoIca());

            // El companyId que se cuela en la query string no debe llegar al caso
            // de uso: la ruta no lo declara y la empresa sale del token. Como el
            // catalogo es global, la fuga no cambiaria ni una fila del resultado
            // —solo a quien deja entrar—, y por eso hace falta este caso.
            mockMvc.perform(get("/withholding-rate-rules/{id}", 8302L).param("companyId", "999"))
                    .andExpect(status().isOk());

            verify(findUseCase).findById(8302L, EMPRESA_DEL_TOKEN);
        }
    }

    @Nested
    @DisplayName("Listado")
    class Listado {

        @Test
        @DisplayName("devuelve la pagina con su contenido y sus totales")
        void devuelve_la_pagina_con_su_contenido_y_sus_totales() throws Exception {
            when(listUseCase.listAvailable(EMPRESA_DEL_TOKEN, 2, 5))
                    .thenReturn(PageResult.of(List.of(WithholdingRateRuleMother.dtoIca(),
                            WithholdingRateRuleMother.dtoNacional()), 2, 5, 11L));

            mockMvc.perform(
                    get("/withholding-rate-rules").param("page", "2").param("pageSize", "5"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].ratePercent").value(0.690000))
                    .andExpect(jsonPath("$.content[1].municipalityCode").doesNotExist())
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(11))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("sin parametros pagina desde la primera y de veinte en veinte")
        void sin_parametros_pagina_desde_la_primera() throws Exception {
            when(listUseCase.listAvailable(anyLong(), anyInt(), anyInt()))
                    .thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/withholding-rate-rules")).andExpect(status().isOk());

            verify(listUseCase).listAvailable(EMPRESA_DEL_TOKEN, 0, 20);
        }
    }

    @Nested
    @DisplayName("Resolucion de la tarifa vigente")
    class ResolucionDeLaTarifaVigente {

        @Test
        @DisplayName("traslada los cuatro criterios y la empresa del token, en su orden")
        void traslada_los_cuatro_criterios_y_la_empresa() throws Exception {
            when(resolveUseCase.resolve(any(), any(), any(), any(), anyLong()))
                    .thenReturn(WithholdingRateRuleMother.dtoIca());

            mockMvc.perform(get("/withholding-rate-rules/effective").param("withholdingType", "ICA")
                    .param("serviceNature", "CONSULTING").param("municipalityCode", "11001")
                    .param("on", "2026-06-15")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.ratePercent").value(0.690000));

            // Cinco argumentos, cuatro de ellos de tipos distintos pero dos
            // enumerados y dos textos: cruzarlos compila sin una queja.
            ArgumentCaptor<String> municipio = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<LocalDate> fecha = ArgumentCaptor.forClass(LocalDate.class);
            verify(resolveUseCase).resolve(eq(WithholdingType.ICA), eq(ServiceNature.CONSULTING),
                    municipio.capture(), fecha.capture(), eq(EMPRESA_DEL_TOKEN));
            assertThat(municipio.getValue()).isEqualTo("11001");
            assertThat(fecha.getValue()).isEqualTo(LocalDate.of(2026, 6, 15));
        }

        @Test
        @DisplayName("una retencion nacional se pide sin municipio y llega como nulo")
        void una_retencion_nacional_se_pide_sin_municipio() throws Exception {
            when(resolveUseCase.resolve(any(), any(), any(), any(), anyLong()))
                    .thenReturn(WithholdingRateRuleMother.dtoNacional());

            mockMvc.perform(
                    get("/withholding-rate-rules/effective").param("withholdingType", "INCOME_TAX")
                            .param("serviceNature", "TECHNICAL_SERVICE").param("on", "2026-06-15"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.municipalityCode").doesNotExist());

            verify(resolveUseCase).resolve(WithholdingType.INCOME_TAX,
                    ServiceNature.TECHNICAL_SERVICE, null, LocalDate.of(2026, 6, 15),
                    EMPRESA_DEL_TOKEN);
        }

        @Test
        @DisplayName("sin fecha el controller pasa null y NO decide el que dia es hoy")
        void sin_fecha_el_controller_pasa_null() throws Exception {
            // El dia por defecto lo pone el caso de uso con su Clock inyectado. Un
            // LocalDate.now() aqui seria una fecha que ningun test puede fijar, y
            // RELOJ_INYECTADO_EN_VEZ_DE_NOW —regla congelada— rompe el build por
            // ello. Este caso se pone rojo el dia que alguien lo reintroduzca.
            when(resolveUseCase.resolve(any(), any(), any(), any(), anyLong()))
                    .thenReturn(WithholdingRateRuleMother.dtoNacional());

            mockMvc.perform(
                    get("/withholding-rate-rules/effective").param("withholdingType", "INCOME_TAX")
                            .param("serviceNature", "TECHNICAL_SERVICE"))
                    .andExpect(status().isOk());

            verify(resolveUseCase).resolve(WithholdingType.INCOME_TAX,
                    ServiceNature.TECHNICAL_SERVICE, null, null, EMPRESA_DEL_TOKEN);
        }

        @Test
        @DisplayName("un tipo de retencion que no existe se rechaza en el binder con un 400")
        void un_tipo_de_retencion_que_no_existe_se_rechaza() throws Exception {
            // La lista cerrada llega hasta la frontera HTTP: un WEALTH_TAX no
            // entra, y el error sale aqui y no como una consulta que no encuentra
            // nada.
            mockMvc.perform(
                    get("/withholding-rate-rules/effective").param("withholdingType", "WEALTH_TAX")
                            .param("serviceNature", "CONSULTING").param("on", "2026-06-15"))
                    .andExpect(status().isBadRequest());
        }
    }
}
