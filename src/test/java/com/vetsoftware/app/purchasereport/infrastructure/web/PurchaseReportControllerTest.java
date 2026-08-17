package com.vetsoftware.app.purchasereport.infrastructure.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.purchasereport.application.dto.PurchaseBookDto;
import com.vetsoftware.app.purchasereport.application.port.in.GetPurchaseBookUseCase;
import com.vetsoftware.app.purchasereport.application.port.out.PurchaseBookPdfPort;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP del libro de compras: el JSON y, sobre todo, el export.
 *
 * <p>
 * El export es lo que no cubre ninguna otra capa: un 200 con el cuerpo correcto
 * pero sin {@code Content-Type} ni {@code Content-Disposition} abre el CSV en
 * la pantalla del contador en vez de descargarlo, y el bug no lo ve ningun test
 * de servicio. Tambien se comprueba que el selector de formato no rompa cuando
 * llega algo que no es ni csv ni pdf.
 */
@WebMvcTest(PurchaseReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("PurchaseReportController — contrato HTTP")
class PurchaseReportControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    /** Sede que resuelve {@code Authz}; distinta de la que manda el request. */
    private static final Long SEDE_RESUELTA = 31L;
    private static final long SEDE_PEDIDA = 77L;

    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 1, 31);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private GetPurchaseBookUseCase purchaseBookUseCase;
    /**
     * El controller inyecta este puerto de salida directamente para el export en
     * PDF; la rodaja tiene que doblarlo aunque no sea un {@code port/in}.
     */
    @MockitoBean
    private PurchaseBookPdfPort purchaseBookPdf;

    /**
     * Sin este stub {@code resolveAccessibleBranch} devolveria 0L —el default de
     * Mockito para un {@code Long}, no null— y el libro se pediria para una sede
     * inexistente.
     */
    @BeforeEach
    void resolverLaSedeDesdeElContexto() {
        when(authz.resolveAccessibleBranch(any())).thenReturn(SEDE_RESUELTA);
    }

    private static PurchaseBookDto libro() {
        return new PurchaseBookDto(DESDE, HASTA,
                List.of(new PurchaseBookDto.EntryDto(1L, "Distribuidora Sur", "900123456", "FC-100",
                        LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 10),
                        new BigDecimal("100000.00"), new BigDecimal("19000.00"), BigDecimal.ZERO,
                        new BigDecimal("119000.00"), new BigDecimal("50000.00"),
                        new BigDecimal("69000.00"), "PARCIAL")),
                new PurchaseBookDto.TotalsDto(1L, new BigDecimal("100000.00"),
                        new BigDecimal("19000.00"), BigDecimal.ZERO, new BigDecimal("119000.00"),
                        new BigDecimal("50000.00"), new BigDecimal("69000.00")));
    }

    @Nested
    @DisplayName("GET /purchase-reports/purchase-book")
    class LibroEnJson {

        @Test
        @DisplayName("devuelve las facturas del periodo con sus totales")
        void devuelve_el_libro() throws Exception {
            when(purchaseBookUseCase.get(COMPANY_ID, DESDE, HASTA, SEDE_RESUELTA))
                    .thenReturn(libro());

            // El stub esta atado a (empresa del contexto, rango, sede resuelta): si el
            // controller tomara la sede del query param tal cual, el cuerpo saldria vacio.
            mockMvc.perform(get("/purchase-reports/purchase-book").param("from", "2026-01-01")
                    .param("to", "2026-01-31").param("branchId", String.valueOf(SEDE_PEDIDA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.entries[0].invoiceNumber").value("FC-100"))
                    .andExpect(jsonPath("$.entries[0].balance").value(69000.00))
                    .andExpect(jsonPath("$.entries[0].status").value("PARCIAL"))
                    .andExpect(jsonPath("$.totals.invoiceCount").value(1))
                    .andExpect(jsonPath("$.totals.total").value(119000.00));
        }

        @Test
        @DisplayName("sin rango de fechas responde 400 y no consulta nada")
        void sin_rango_responde_400() throws Exception {
            mockMvc.perform(get("/purchase-reports/purchase-book"))
                    .andExpect(status().isBadRequest());

            verify(purchaseBookUseCase, never()).get(anyLong(), any(), any(), any());
        }

        @Test
        @DisplayName("con una fecha en formato no ISO responde 400, no 500")
        void fecha_no_iso_responde_400() throws Exception {
            mockMvc.perform(get("/purchase-reports/purchase-book").param("from", "10-01-2026")
                    .param("to", "2026-01-31")).andExpect(status().isBadRequest());

            verify(purchaseBookUseCase, never()).get(anyLong(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("GET /purchase-reports/purchase-book/export")
    class Export {

        @Test
        @DisplayName("por defecto entrega un CSV descargable con el rango en el nombre")
        void por_defecto_entrega_csv() throws Exception {
            when(purchaseBookUseCase.get(COMPANY_ID, DESDE, HASTA, SEDE_RESUELTA))
                    .thenReturn(libro());

            mockMvc.perform(get("/purchase-reports/purchase-book/export")
                    .param("from", "2026-01-01").param("to", "2026-01-31"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/csv;charset=UTF-8"))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"libro_compras_2026-01-01_2026-01-31.csv\";"
                                    + " filename*=UTF-8''libro_compras_2026-01-01_2026-01-31.csv"))
                    .andExpect(content().string(containsString("Libro de compras")))
                    .andExpect(content().string(containsString("FC-100")));

            verify(purchaseBookPdf, never()).renderPurchaseBook(any());
        }

        @Test
        @DisplayName("con format=pdf entrega el PDF renderizado por el puerto")
        void con_format_pdf_entrega_pdf() throws Exception {
            when(purchaseBookUseCase.get(COMPANY_ID, DESDE, HASTA, SEDE_RESUELTA))
                    .thenReturn(libro());
            when(purchaseBookPdf.renderPurchaseBook(libro())).thenReturn("%PDF-1.4".getBytes());

            mockMvc.perform(get("/purchase-reports/purchase-book/export")
                    .param("from", "2026-01-01").param("to", "2026-01-31").param("format", "PDF"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            containsString("libro_compras_2026-01-01_2026-01-31.pdf")));
        }

        @Test
        @DisplayName("un formato desconocido cae al CSV en vez de romper")
        void formato_desconocido_cae_al_csv() throws Exception {
            when(purchaseBookUseCase.get(COMPANY_ID, DESDE, HASTA, SEDE_RESUELTA))
                    .thenReturn(libro());

            mockMvc.perform(get("/purchase-reports/purchase-book/export")
                    .param("from", "2026-01-01").param("to", "2026-01-31").param("format", "xlsx"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/csv;charset=UTF-8"));

            verify(purchaseBookPdf, never()).renderPurchaseBook(any());
        }

        @Test
        @DisplayName("el export sin rango responde 400 y no renderiza nada")
        void export_sin_rango_responde_400() throws Exception {
            mockMvc.perform(get("/purchase-reports/purchase-book/export"))
                    .andExpect(status().isBadRequest());

            verify(purchaseBookUseCase, never()).get(anyLong(), any(), any(), any());
            verify(purchaseBookPdf, never()).renderPurchaseBook(any());
        }
    }
}
