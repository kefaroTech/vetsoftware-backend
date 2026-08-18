package com.vetsoftware.app.salesreport.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.salesreport.application.dto.ReconciliationDto;
import com.vetsoftware.app.salesreport.application.dto.SalesBookDto;
import com.vetsoftware.app.salesreport.application.port.in.GetReconciliationUseCase;
import com.vetsoftware.app.salesreport.application.port.in.GetSalesBookUseCase;
import com.vetsoftware.app.salesreport.application.usecase.GetReconciliationService;
import com.vetsoftware.app.salesreport.application.usecase.GetSalesBookService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP de los reportes fiscales de venta.
 *
 * <p>
 * Son dos GET sin cuerpo, asi que todo lo que puede romperse esta en los
 * parametros: el rango de fechas se parsea en ISO (una fecha ilegible tiene que
 * salir como 400, no como 500), la empresa la pone {@code Authz} y la sede pasa
 * siempre por {@code resolveAccessibleBranch} — un libro de ventas es
 * informacion fiscal, y filtrarlo por una sede ajena seria una fuga entre
 * tenants.
 */
@WebMvcTest(SalesReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SalesReportController — contrato HTTP")
class SalesReportControllerTest {

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
    private GetSalesBookUseCase salesBookUseCase;
    @MockitoBean
    private GetReconciliationUseCase reconciliationUseCase;

    /**
     * Sin este stub {@code resolveAccessibleBranch} devolveria 0L —el default de
     * Mockito para un {@code Long}, no null— y el reporte se pediria para una sede
     * inexistente.
     */
    @BeforeEach
    void resolverLaSedeDesdeElContexto() {
        when(authz.resolveAccessibleBranch(any())).thenReturn(SEDE_RESUELTA);
    }

    private static SalesBookDto libro() {
        return new SalesBookDto(DESDE, HASTA,
                List.of(new SalesBookDto.EntryDto(1L, "FACTURA_VENTA", "SETP", 990L,
                        LocalDate.of(2026, 1, 15), "900123456", "Clinica Norte",
                        new BigDecimal("100000.00"), new BigDecimal("19000.00"), BigDecimal.ZERO,
                        new BigDecimal("119000.00"), new BigDecimal("119000.00"), BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, "VALIDADO", "cufe-1", null)),
                List.of(new SalesBookDto.TaxByRateDto("IVA", new BigDecimal("19.00"),
                        new BigDecimal("100000.00"), new BigDecimal("19000.00"))),
                List.of(new SalesBookDto.RecaudoDto("CASH", "10", new BigDecimal("119000.00"))),
                new SalesBookDto.TotalsDto(1L, new BigDecimal("100000.00"),
                        new BigDecimal("19000.00"), BigDecimal.ZERO, new BigDecimal("119000.00"),
                        new BigDecimal("119000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO));
    }

    private static ReconciliationDto conciliacion() {
        return new ReconciliationDto(DESDE, HASTA, 10L, 8L, 1L, 0L, 1L,
                List.of(new ReconciliationDto.PendingDto(2L, "FACTURA_VENTA", "SETP", 991L,
                        LocalDate.of(2026, 1, 16), "RECHAZADO", null, null)));
    }

    @Nested
    @DisplayName("GET /sales-reports/sales-book")
    class LibroDeVentas {

        @Test
        @DisplayName("devuelve el libro con sus entradas, impuestos y totales")
        void devuelve_el_libro() throws Exception {
            when(salesBookUseCase.get(COMPANY_ID, DESDE, HASTA, SEDE_RESUELTA)).thenReturn(libro());

            // El stub esta atado a (empresa del contexto, rango, sede resuelta): si el
            // controller tomara la empresa o la sede de otro sitio, el cuerpo saldria
            // vacio y las aserciones de abajo caen.
            mockMvc.perform(get("/sales-reports/sales-book").param("from", "2026-01-01")
                    .param("to", "2026-01-31").param("branchId", String.valueOf(SEDE_PEDIDA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.entries[0].consecutive").value(990))
                    .andExpect(jsonPath("$.entries[0].dianStatus").value("VALIDADO"))
                    .andExpect(jsonPath("$.taxByRate[0].taxRate").value(19.00))
                    .andExpect(jsonPath("$.recaudoByMeans[0].dianCode").value("10"))
                    .andExpect(jsonPath("$.totals.documentCount").value(1))
                    .andExpect(jsonPath("$.totals.payable").value(119000.00));
        }

        @Test
        @DisplayName("sin sede el reporte cubre las que alcanza el empleado")
        void sin_sede_resuelve_el_alcance_del_empleado() throws Exception {
            when(salesBookUseCase.get(COMPANY_ID, DESDE, HASTA, SEDE_RESUELTA)).thenReturn(libro());

            mockMvc.perform(get("/sales-reports/sales-book").param("from", "2026-01-01").param("to",
                    "2026-01-31")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.entries.length()").value(1));
        }

        @Test
        @DisplayName("sin rango de fechas responde 400 y no consulta nada")
        void sin_rango_responde_400() throws Exception {
            mockMvc.perform(get("/sales-reports/sales-book")).andExpect(status().isBadRequest());

            verify(salesBookUseCase, never()).get(anyLong(), any(), any(), any());
        }

        @Test
        @DisplayName("con una fecha en formato no ISO responde 400, no 500")
        void fecha_no_iso_responde_400() throws Exception {
            mockMvc.perform(get("/sales-reports/sales-book").param("from", "01/01/2026").param("to",
                    "2026-01-31")).andExpect(status().isBadRequest());

            verify(salesBookUseCase, never()).get(anyLong(), any(), any(), any());
        }

        /**
         * Este test ejercita la validacion <b>real</b>. Antes stubeaba un
         * {@code thenThrow} escrito a mano, asi que pasaba en verde aunque nadie
         * validase el rango — y durante un tiempo nadie lo hizo. Ahora el doble delega
         * en {@link GetSalesBookService} de produccion (con un puerto que devuelve
         * vacio), de modo que el 400 solo aparece si {@code ReportDateRange} rechaza de
         * verdad el rango invertido.
         */
        @Test
        @DisplayName("un rango invertido responde 400 con la validacion real del caso de uso")
        void rango_invertido_responde_400() throws Exception {
            GetSalesBookService real = new GetSalesBookService(
                    (empresa, from, to, sede) -> List.of());
            when(salesBookUseCase.get(anyLong(), any(), any(), any())).thenAnswer(
                    invocation -> real.get(invocation.getArgument(0), invocation.getArgument(1),
                            invocation.getArgument(2), invocation.getArgument(3)));

            mockMvc.perform(get("/sales-reports/sales-book").param("from", "2026-01-31").param("to",
                    "2026-01-01")).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("con la misma validacion real, un rango correcto sigue respondiendo 200")
        void rango_correcto_responde_200_con_la_validacion_real() throws Exception {
            GetSalesBookService real = new GetSalesBookService(
                    (empresa, from, to, sede) -> List.of());
            when(salesBookUseCase.get(anyLong(), any(), any(), any())).thenAnswer(
                    invocation -> real.get(invocation.getArgument(0), invocation.getArgument(1),
                            invocation.getArgument(2), invocation.getArgument(3)));

            mockMvc.perform(get("/sales-reports/sales-book").param("from", "2026-01-01").param("to",
                    "2026-01-31")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.totals.documentCount").value(0));
        }
    }

    @Nested
    @DisplayName("GET /sales-reports/reconciliation")
    class Conciliacion {

        @Test
        @DisplayName("devuelve los conteos por estado y el detalle que requiere atencion")
        void devuelve_la_conciliacion() throws Exception {
            when(reconciliationUseCase.get(COMPANY_ID, DESDE, HASTA, SEDE_RESUELTA))
                    .thenReturn(conciliacion());

            mockMvc.perform(get("/sales-reports/reconciliation").param("from", "2026-01-01")
                    .param("to", "2026-01-31").param("branchId", String.valueOf(SEDE_PEDIDA)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(10))
                    .andExpect(jsonPath("$.validados").value(8))
                    .andExpect(jsonPath("$.rechazados").value(1))
                    .andExpect(jsonPath("$.contingencia").value(0))
                    .andExpect(jsonPath("$.needsAttention[0].dianStatus").value("RECHAZADO"))
                    .andExpect(jsonPath("$.needsAttention[0].cufe").doesNotExist());
        }

        @Test
        @DisplayName("sin rango de fechas responde 400 y no consulta nada")
        void sin_rango_responde_400() throws Exception {
            mockMvc.perform(get("/sales-reports/reconciliation"))
                    .andExpect(status().isBadRequest());

            verify(reconciliationUseCase, never()).get(anyLong(), any(), any(), any());
        }

        /** Mismo montaje que el libro: el doble delega en el servicio real. */
        @Test
        @DisplayName("un rango invertido responde 400 con la validacion real del caso de uso")
        void rango_invertido_responde_400() throws Exception {
            GetReconciliationService real = new GetReconciliationService(
                    (empresa, from, to, sede) -> List.of());
            when(reconciliationUseCase.get(anyLong(), any(), any(), any())).thenAnswer(
                    invocation -> real.get(invocation.getArgument(0), invocation.getArgument(1),
                            invocation.getArgument(2), invocation.getArgument(3)));

            mockMvc.perform(get("/sales-reports/reconciliation").param("from", "2026-01-31")
                    .param("to", "2026-01-01")).andExpect(status().isBadRequest());
        }
    }
}
