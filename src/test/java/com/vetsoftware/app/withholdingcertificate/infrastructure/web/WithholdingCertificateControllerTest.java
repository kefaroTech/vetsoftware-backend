package com.vetsoftware.app.withholdingcertificate.infrastructure.web;

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
import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import com.vetsoftware.app.withholdingcertificate.application.port.in.FindWithholdingCertificateUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.in.ListMissingWithholdingCertificatesByCompanyUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.in.ListWithholdingCertificatesUseCase;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
 * <li><b>Este controller no escribe.</b> Las tres escrituras del bloque viven
 * en {@link SystemWithholdingCertificateController}, y el certificado se sigue
 * leyendo aqui porque la retencion es plata del cliente.</li>
 * </ul>
 */
@WebMvcTest(WithholdingCertificateController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("WithholdingCertificateController — contrato HTTP del tenant")
class WithholdingCertificateControllerTest {

    /**
     * Distinto del {@code COMPANY_ID} por defecto para que se vea de donde sale.
     */
    private static final Long EMPRESA_DEL_TOKEN = 77L;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private FindWithholdingCertificateUseCase findUseCase;
    @MockitoBean
    private ListWithholdingCertificatesUseCase listUseCase;
    @MockitoBean
    private ListMissingWithholdingCertificatesByCompanyUseCase listMissingUseCase;
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
        @DisplayName("devuelve el certificado con cada campo en su lugar del JSON")
        void devuelve_el_certificado_con_cada_campo_en_su_lugar() throws Exception {
            when(findUseCase.findById(41L, EMPRESA_DEL_TOKEN)).thenReturn(unCertificado());

            mockMvc.perform(get("/withholding-certificates/{id}", 41L)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(41))
                    .andExpect(jsonPath("$.companyId").value(77))
                    .andExpect(jsonPath("$.issuedByTaxId").value("830012345"))
                    .andExpect(jsonPath("$.certificateNumber").value("CERT-2025-0001"))
                    .andExpect(jsonPath("$.withholdingType").value("ICA"))
                    .andExpect(jsonPath("$.fiscalYear").value(2025))
                    .andExpect(jsonPath("$.fiscalPeriodKey").value("2025-B03"))
                    .andExpect(jsonPath("$.certifiedAmount").value(1847320.55))
                    .andExpect(jsonPath("$.issuedOn").value("2026-02-10"))
                    .andExpect(jsonPath("$.legalDeadlineOn").value("2026-03-31"))
                    .andExpect(jsonPath("$.receivedOn").value("2026-03-18"))
                    .andExpect(jsonPath("$.fileRef").value("s3://certificados/CERT.pdf"))
                    .andExpect(jsonPath("$.supported").value(true))
                    .andExpect(jsonPath("$.createdDate").value("2026-02-12T09:15:30"));
        }

        @Test
        @DisplayName("la tarifa por mil sale por HTTP sin perder precision")
        void la_tarifa_por_mil_sale_sin_perder_precision() throws Exception {
            // OJO: por el cable NO viaja la escala, solo el valor. Jackson emite
            // 0.690000 como 0.69, asi que una asercion sobre la escala mediria el
            // serializador y no lo que importa. Lo que importa son los digitos
            // significativos: 4,14 por mil es 0,00414 y una columna de cuatro
            // decimales lo cortaria en 0,0041 -exactamente el defecto que el
            // changeset 328 evita con DECIMAL(9,6)-. Ese corte SI se ve aqui.
            when(findUseCase.findById(anyLong(), anyLong())).thenReturn(conTarifaPorMil());

            mockMvc.perform(get("/withholding-certificates/{id}", 41L)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.ratePercent").value(0.00414));
        }

        @Test
        @DisplayName("acota la carga con la empresa del token y nunca con una del cliente")
        void acota_la_carga_con_la_empresa_del_token() throws Exception {
            when(findUseCase.findById(anyLong(), anyLong())).thenReturn(unCertificado());

            // El parametro companyId que se cuela en la query string no debe llegar al
            // caso de uso: la ruta no lo declara y la empresa sale del token.
            mockMvc.perform(get("/withholding-certificates/{id}", 41L).param("companyId", "999"))
                    .andExpect(status().isOk());

            verify(findUseCase).findById(41L, EMPRESA_DEL_TOKEN);
        }
    }

    @Nested
    @DisplayName("Listados")
    class Listados {

        @Test
        @DisplayName("el listado de la empresa devuelve la pagina con su contenido y sus totales")
        void el_listado_de_la_empresa_devuelve_la_pagina() throws Exception {
            when(listUseCase.listByCompany(EMPRESA_DEL_TOKEN, 2, 5))
                    .thenReturn(PageResult.of(List.of(unCertificado()), 2, 5, 11L));

            mockMvc.perform(
                    get("/withholding-certificates").param("page", "2").param("pageSize", "5"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(41))
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(11))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("sin parametros pagina desde la primera y de veinte en veinte")
        void sin_parametros_pagina_desde_la_primera() throws Exception {
            when(listUseCase.listByCompany(anyLong(), anyInt(), anyInt()))
                    .thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/withholding-certificates")).andExpect(status().isOk());

            verify(listUseCase).listByCompany(EMPRESA_DEL_TOKEN, 0, 20);
        }

        @Test
        @DisplayName("el aviso de vencimientos lleva la fecha de la peticion y la empresa del token")
        void el_aviso_de_vencimientos_lleva_la_fecha_y_la_empresa() throws Exception {
            when(listMissingUseCase.listMissingByCompany(anyLong(), any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(unCertificado()), 0, 20, 1L));

            mockMvc.perform(
                    get("/withholding-certificates/missing").param("deadlineBefore", "2026-03-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].legalDeadlineOn").value("2026-03-31"));

            // Acotar por vencimiento no acota por tenant: la empresa del token tiene
            // que llegar igual, y en su posicion.
            ArgumentCaptor<Long> empresa = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<LocalDate> corte = ArgumentCaptor.forClass(LocalDate.class);
            verify(listMissingUseCase).listMissingByCompany(empresa.capture(), corte.capture(),
                    eq(0), eq(20));
            assertThat(empresa.getValue()).isEqualTo(EMPRESA_DEL_TOKEN);
            assertThat(corte.getValue()).isEqualTo(LocalDate.of(2026, 3, 31));
        }

        @Test
        @DisplayName("el aviso sin fecha de corte es una peticion mal formada")
        void el_aviso_sin_fecha_de_corte_es_una_peticion_mal_formada() throws Exception {
            // La fecha no tiene defecto a proposito: un corte implicito seria el reloj
            // del servidor decidiendo por quien pregunta que vencimientos se miran.
            mockMvc.perform(get("/withholding-certificates/missing"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("el listado ignora la empresa que el cliente intente colar por query")
        void el_listado_ignora_la_empresa_que_cuele_el_cliente() throws Exception {
            when(listUseCase.listByCompany(anyLong(), anyInt(), anyInt()))
                    .thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/withholding-certificates").param("companyId", "999"))
                    .andExpect(status().isOk());

            verify(listUseCase).listByCompany(EMPRESA_DEL_TOKEN, 0, 20);
        }
    }

    /** 4,14 por mil, la tarifa de ICA cuyo corte a cuatro decimales se nota. */
    private static WithholdingCertificateDto conTarifaPorMil() {
        return new WithholdingCertificateDto(41L, EMPRESA_DEL_TOKEN, "830012345", "CERT-2025-0001",
                WithholdingType.ICA, 2025, "2025-B03", new BigDecimal("0.004140"),
                new BigDecimal("1847320.55"), LocalDate.of(2026, 2, 10), LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 3, 18), "s3://certificados/CERT.pdf", null, null, true,
                LocalDateTime.of(2026, 2, 12, 9, 15, 30));
    }

    private static WithholdingCertificateDto unCertificado() {
        return new WithholdingCertificateDto(41L, EMPRESA_DEL_TOKEN, "830012345", "CERT-2025-0001",
                WithholdingType.ICA, 2025, "2025-B03", new BigDecimal("0.690000"),
                new BigDecimal("1847320.55"), LocalDate.of(2026, 2, 10), LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 3, 18), "s3://certificados/CERT.pdf", null, null, true,
                LocalDateTime.of(2026, 2, 12, 9, 15, 30));
    }
}
