package com.vetsoftware.app.cashregister.infrastructure.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.cashregister.application.command.CloseCashSessionCommand;
import com.vetsoftware.app.cashregister.application.command.OpenCashSessionCommand;
import com.vetsoftware.app.cashregister.application.command.RegisterCashMovementCommand;
import com.vetsoftware.app.cashregister.application.command.SearchCashSessionsQuery;
import com.vetsoftware.app.cashregister.application.dto.CashArqueoReport;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.application.port.in.CloseCashSessionUseCase;
import com.vetsoftware.app.cashregister.application.port.in.ExportArqueoUseCase;
import com.vetsoftware.app.cashregister.application.port.in.GetCashSessionUseCase;
import com.vetsoftware.app.cashregister.application.port.in.GetCurrentCashSessionUseCase;
import com.vetsoftware.app.cashregister.application.port.in.ListCashSessionsUseCase;
import com.vetsoftware.app.cashregister.application.port.in.ListOpenCashSessionsUseCase;
import com.vetsoftware.app.cashregister.application.port.in.OpenCashSessionUseCase;
import com.vetsoftware.app.cashregister.application.port.in.RegisterCashMovementUseCase;
import com.vetsoftware.app.cashregister.application.port.out.CashReportPdfPort;
import com.vetsoftware.app.cashregister.domain.CashMovementType;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashSessionAlreadyOpenException;
import com.vetsoftware.app.cashregister.domain.CashSessionClosedException;
import com.vetsoftware.app.cashregister.domain.CashSessionNotFoundException;
import com.vetsoftware.app.cashregister.domain.EmployeeCashSessionAlreadyOpenException;
import com.vetsoftware.app.cashregister.testsupport.CashSessionMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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
 * Rodaja HTTP de la caja: rutas, binding, validacion del request, codigos de
 * estado y forma del JSON. Lo que hay debajo son dobles.
 *
 * <p>
 * Lo que esta rodaja protege y ninguna otra capa cubre:
 *
 * <ul>
 * <li><b>El sello de tenant y de autoria.</b> Ni {@code companyId} ni
 * {@code employeeId} viajan en el cuerpo: los pone el backend desde
 * {@code Authz}. En caja el empleado no es decorativo — es el responsable del
 * dinero, y el que el dominio compara al cerrar. Cada command se verifica
 * completo para que un dia nadie lo tome del request.</li>
 * <li><b>La sede sale de {@code resolveAccessibleBranch}</b>, no del cuerpo. El
 * doble devuelve una sede DISTINTA de la pedida a proposito: si el controller
 * pasara {@code request.branchId()} directo, la asercion cae.</li>
 * <li><b>Los conflictos de caja salen como 409.</b>
 * {@code CashSessionPersistenceIT} ya prueba contra MySQL que solo puede haber
 * una sesion OPEN por terminal; aqui se comprueba que ese choque llega al
 * cliente como 409 y no como 500.</li>
 * </ul>
 */
@WebMvcTest(CashSessionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CashSessionController — contrato HTTP")
class CashSessionControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;

    /** Sede que resuelve {@code Authz}; distinta de la que manda el request. */
    private static final Long SEDE_RESUELTA = 31L;
    /** Sede que manda el cliente: nunca debe llegar tal cual al command. */
    private static final long SEDE_PEDIDA = 77L;
    private static final Long TERMINAL_ID = 100L;
    private static final Long SESSION_ID = CashSessionMother.SESSION_ID;

    private static final String APERTURA_VALIDA = """
            {"branchId":77,"terminalId":100,"openingFloat":150000.00,"note":"Turno manana"}
            """;

    private static final String MOVIMIENTO_VALIDO = """
            {"type":"WITHDRAWAL","method":"CASH","amount":20000.00,"note":"Pago del domicilio"}
            """;

    private static final String CIERRE_VALIDO = """
            {"counts":[{"method":"CASH","countedAmount":125000.00},
                       {"method":"CARD","countedAmount":30000.00}],
             "note":"Faltan 5.000"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private OpenCashSessionUseCase openUseCase;
    @MockitoBean
    private RegisterCashMovementUseCase registerMovementUseCase;
    @MockitoBean
    private CloseCashSessionUseCase closeUseCase;
    @MockitoBean
    private GetCurrentCashSessionUseCase getCurrentUseCase;
    @MockitoBean
    private GetCashSessionUseCase getUseCase;
    @MockitoBean
    private ListCashSessionsUseCase listUseCase;
    @MockitoBean
    private ListOpenCashSessionsUseCase listOpenUseCase;
    @MockitoBean
    private ExportArqueoUseCase exportArqueoUseCase;
    /**
     * El controller inyecta este puerto de salida directamente para el export en
     * PDF, asi que la rodaja tiene que doblarlo aunque no sea un {@code port/in}.
     */
    @MockitoBean
    private CashReportPdfPort cashReportPdf;

    /**
     * {@code WebMvcSliceConfig} no stubea el alcance de sedes. Sin esto,
     * {@code resolveAccessibleBranch} devolveria 0L —el default de Mockito para un
     * {@code Long}, no null— y el command llegaria firmado con una sede que no
     * existe, dejando pasar el bug que este test busca.
     */
    @BeforeEach
    void resolverLaSedeDesdeElContexto() {
        when(authz.resolveAccessibleBranch(any())).thenReturn(SEDE_RESUELTA);
        when(authz.currentBranchIds()).thenReturn(Set.of(SEDE_RESUELTA, 32L));
    }

    private static CashSessionView abierta() {
        return CashSessionView.from(CashSessionMother.sesionAbierta());
    }

    private static CashSessionView conMovimientos() {
        return CashSessionView.from(CashSessionMother.sesionConMovimientos());
    }

    private static CashSessionView cerrada() {
        return CashSessionView.from(CashSessionMother.sesionCerrada());
    }

    @Nested
    @DisplayName("POST /cash-sessions/open")
    class Apertura {

        @Test
        @DisplayName("responde 201 con la sesion abierta y sus totales por metodo")
        void responde_201() throws Exception {
            when(openUseCase.open(any())).thenReturn(abierta());

            mockMvc.perform(post("/cash-sessions/open").contentType(MediaType.APPLICATION_JSON)
                    .content(APERTURA_VALIDA)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(SESSION_ID))
                    .andExpect(jsonPath("$.status").value("OPEN"))
                    .andExpect(jsonPath("$.terminal").value("principal"))
                    .andExpect(jsonPath("$.openingFloat").value(100000))
                    .andExpect(jsonPath("$.closingTotal").doesNotExist())
                    .andExpect(jsonPath("$.totals[0].method").value("CASH"))
                    .andExpect(jsonPath("$.totals[0].expectedAmount").value(100000))
                    .andExpect(jsonPath("$.movements").isEmpty());
        }

        @Test
        @DisplayName("la empresa, el empleado y la sede los pone el backend, no el request")
        void el_command_lo_sella_el_backend() throws Exception {
            when(openUseCase.open(any())).thenReturn(abierta());

            mockMvc.perform(post("/cash-sessions/open").contentType(MediaType.APPLICATION_JSON)
                    .content(APERTURA_VALIDA));

            // SEDE_RESUELTA != SEDE_PEDIDA: si el controller pasara la sede del cuerpo
            // tal cual, un empleado podria abrir caja en una sede que no le toca.
            verify(openUseCase).open(new OpenCashSessionCommand(COMPANY_ID, SEDE_RESUELTA,
                    TERMINAL_ID, new BigDecimal("150000.00"), EMPLOYEE_ID, "Turno manana"));
        }

        @Test
        @DisplayName("una base negativa responde 400 y no abre nada")
        void base_negativa_responde_400() throws Exception {
            mockMvc.perform(
                    post("/cash-sessions/open").contentType(MediaType.APPLICATION_JSON).content("""
                            {"branchId":77,"terminalId":100,"openingFloat":-1}
                            """)).andExpect(status().isBadRequest());

            verify(openUseCase, never()).open(any());
        }

        @Test
        @DisplayName("sin terminal responde 400: la unicidad de caja se cuenta por terminal")
        void sin_terminal_responde_400() throws Exception {
            mockMvc.perform(
                    post("/cash-sessions/open").contentType(MediaType.APPLICATION_JSON).content("""
                            {"branchId":77,"openingFloat":150000.00}
                            """)).andExpect(status().isBadRequest());

            verify(openUseCase, never()).open(any());
        }

        @Test
        @DisplayName("si la terminal ya tiene caja abierta responde 409, no 500")
        void terminal_con_caja_abierta_responde_409() throws Exception {
            when(openUseCase.open(any())).thenThrow(
                    new CashSessionAlreadyOpenException("Sede Centro", "principal", "Ana Ruiz"));

            mockMvc.perform(post("/cash-sessions/open").contentType(MediaType.APPLICATION_JSON)
                    .content(APERTURA_VALIDA)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("si el empleado ya es responsable de otra caja responde 409")
        void empleado_con_otra_caja_responde_409() throws Exception {
            when(openUseCase.open(any())).thenThrow(new EmployeeCashSessionAlreadyOpenException());

            mockMvc.perform(post("/cash-sessions/open").contentType(MediaType.APPLICATION_JSON)
                    .content(APERTURA_VALIDA)).andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /cash-sessions/{id}/movements")
    class Movimientos {

        @Test
        @DisplayName("responde 201 con la sesion y el movimiento ya reflejado")
        void responde_201() throws Exception {
            when(registerMovementUseCase.register(any())).thenReturn(conMovimientos());

            mockMvc.perform(post("/cash-sessions/500/movements")
                    .contentType(MediaType.APPLICATION_JSON).content(MOVIMIENTO_VALIDO))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.movements.length()").value(3))
                    .andExpect(jsonPath("$.movements[2].type").value("WITHDRAWAL"))
                    .andExpect(jsonPath("$.totals[0].expectedAmount").value(130000));
        }

        @Test
        @DisplayName("el id de la sesion sale de la ruta y la autoria del contexto")
        void el_command_toma_el_id_de_la_ruta_y_la_autoria_del_contexto() throws Exception {
            when(registerMovementUseCase.register(any())).thenReturn(conMovimientos());

            mockMvc.perform(post("/cash-sessions/500/movements")
                    .contentType(MediaType.APPLICATION_JSON).content(MOVIMIENTO_VALIDO));

            verify(registerMovementUseCase).register(new RegisterCashMovementCommand(COMPANY_ID,
                    SESSION_ID, CashMovementType.WITHDRAWAL, CashPaymentMethod.CASH,
                    new BigDecimal("20000.00"), EMPLOYEE_ID, "Pago del domicilio"));
        }

        @Test
        @DisplayName("un monto de cero responde 400 y no toca la caja")
        void monto_cero_responde_400() throws Exception {
            mockMvc.perform(post("/cash-sessions/500/movements")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"type":"WITHDRAWAL","method":"CASH","amount":0}
                            """)).andExpect(status().isBadRequest());

            verify(registerMovementUseCase, never()).register(any());
        }

        @Test
        @DisplayName("sin metodo de pago responde 400")
        void sin_metodo_responde_400() throws Exception {
            mockMvc.perform(post("/cash-sessions/500/movements")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"type":"MANUAL_IN","amount":5000.00}
                            """)).andExpect(status().isBadRequest());

            verify(registerMovementUseCase, never()).register(any());
        }

        @Test
        @DisplayName("un tipo de movimiento inexistente responde 400")
        void tipo_desconocido_responde_400() throws Exception {
            mockMvc.perform(post("/cash-sessions/500/movements")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"type":"REGALO","method":"CASH","amount":5000.00}
                            """)).andExpect(status().isBadRequest());

            verify(registerMovementUseCase, never()).register(any());
        }

        @Test
        @DisplayName("mover dinero en una caja ya cerrada responde 409, no 500")
        void caja_cerrada_responde_409() throws Exception {
            when(registerMovementUseCase.register(any()))
                    .thenThrow(new CashSessionClosedException(SESSION_ID));

            mockMvc.perform(post("/cash-sessions/500/movements")
                    .contentType(MediaType.APPLICATION_JSON).content(MOVIMIENTO_VALIDO))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("una sesion inexistente responde 404")
        void sesion_inexistente_responde_404() throws Exception {
            when(registerMovementUseCase.register(any()))
                    .thenThrow(new CashSessionNotFoundException(999L));

            mockMvc.perform(post("/cash-sessions/999/movements")
                    .contentType(MediaType.APPLICATION_JSON).content(MOVIMIENTO_VALIDO))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /cash-sessions/{id}/close")
    class Cierre {

        @Test
        @DisplayName("responde 200 con el arqueo cerrado y su total contado")
        void responde_200() throws Exception {
            when(closeUseCase.close(any(), org.mockito.ArgumentMatchers.anyBoolean()))
                    .thenReturn(cerrada());

            mockMvc.perform(post("/cash-sessions/500/close").contentType(MediaType.APPLICATION_JSON)
                    .content(CIERRE_VALIDO)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CLOSED"))
                    .andExpect(jsonPath("$.closingTotal").value(155000))
                    .andExpect(jsonPath("$.counts[0].method").value("CASH"))
                    .andExpect(jsonPath("$.counts[0].difference").value(-5000));
        }

        @Test
        @DisplayName("traduce los conteos al command y cierra sin override de administrador")
        void traduce_los_conteos_y_no_es_override() throws Exception {
            when(closeUseCase.close(any(), org.mockito.ArgumentMatchers.anyBoolean()))
                    .thenReturn(cerrada());

            mockMvc.perform(post("/cash-sessions/500/close").contentType(MediaType.APPLICATION_JSON)
                    .content(CIERRE_VALIDO));

            // El false es el gate del @PreAuthorize: con true haria falta ROLE_SYSTEM.
            // Ningun endpoint publico debe poder pedir el cierre forzado.
            verify(closeUseCase).close(
                    new CloseCashSessionCommand(COMPANY_ID, SESSION_ID, EMPLOYEE_ID, "Faltan 5.000",
                            List.of(new CloseCashSessionCommand.Count(CashPaymentMethod.CASH,
                                    new BigDecimal("125000.00")),
                                    new CloseCashSessionCommand.Count(CashPaymentMethod.CARD,
                                            new BigDecimal("30000.00")))),
                    false);
        }

        @Test
        @DisplayName("un cierre sin conteos manda una lista vacia, nunca null")
        void cierre_sin_conteos_manda_lista_vacia() throws Exception {
            when(closeUseCase.close(any(), org.mockito.ArgumentMatchers.anyBoolean()))
                    .thenReturn(cerrada());

            mockMvc.perform(post("/cash-sessions/500/close").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"note":"Cierre sin declarar"}
                            """)).andExpect(status().isOk());

            verify(closeUseCase).close(new CloseCashSessionCommand(COMPANY_ID, SESSION_ID,
                    EMPLOYEE_ID, "Cierre sin declarar", List.of()), false);
        }

        @Test
        @DisplayName("un conteo negativo responde 400 y no cierra")
        void conteo_negativo_responde_400() throws Exception {
            mockMvc.perform(post("/cash-sessions/500/close").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"counts":[{"method":"CASH","countedAmount":-1}]}
                            """)).andExpect(status().isBadRequest());

            verify(closeUseCase, never()).close(any(), org.mockito.ArgumentMatchers.anyBoolean());
        }

        @Test
        @DisplayName("un conteo sin metodo responde 400")
        void conteo_sin_metodo_responde_400() throws Exception {
            mockMvc.perform(post("/cash-sessions/500/close").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"counts":[{"countedAmount":1000.00}]}
                            """)).andExpect(status().isBadRequest());

            verify(closeUseCase, never()).close(any(), org.mockito.ArgumentMatchers.anyBoolean());
        }

        @Test
        @DisplayName("cerrar dos veces la misma caja responde 409, no 500")
        void cerrar_dos_veces_responde_409() throws Exception {
            when(closeUseCase.close(any(), org.mockito.ArgumentMatchers.anyBoolean()))
                    .thenThrow(new CashSessionClosedException(SESSION_ID));

            mockMvc.perform(post("/cash-sessions/500/close").contentType(MediaType.APPLICATION_JSON)
                    .content(CIERRE_VALIDO)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("cerrar una sesion de otra empresa responde 404")
        void cerrar_una_sesion_ajena_responde_404() throws Exception {
            when(closeUseCase.close(any(), org.mockito.ArgumentMatchers.anyBoolean()))
                    .thenThrow(new CashSessionNotFoundException(SESSION_ID));

            mockMvc.perform(post("/cash-sessions/500/close").contentType(MediaType.APPLICATION_JSON)
                    .content(CIERRE_VALIDO)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /cash-sessions/current busca la caja del empleado del contexto")
        void get_current() throws Exception {
            when(getCurrentUseCase.current(COMPANY_ID, EMPLOYEE_ID)).thenReturn(conMovimientos());

            // El stub esta atado a (COMPANY_ID, EMPLOYEE_ID): si el controller leyera el
            // empleado de otro sitio, el doble devolveria null y el JSON quedaria vacio.
            mockMvc.perform(get("/cash-sessions/current")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SESSION_ID))
                    .andExpect(jsonPath("$.openedByEmployeeId").value(7));
        }

        @Test
        @DisplayName("GET /cash-sessions/current sin caja abierta responde 200 con cuerpo vacio")
        void get_current_sin_caja() throws Exception {
            when(getCurrentUseCase.current(COMPANY_ID, EMPLOYEE_ID)).thenReturn(null);

            mockMvc.perform(get("/cash-sessions/current")).andExpect(status().isOk())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("GET /cash-sessions/{id} devuelve el detalle con movimientos")
        void get_por_id() throws Exception {
            when(getUseCase.get(COMPANY_ID, SESSION_ID)).thenReturn(conMovimientos());

            mockMvc.perform(get("/cash-sessions/500")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SESSION_ID))
                    .andExpect(jsonPath("$.movements.length()").value(3))
                    .andExpect(jsonPath("$.movements[0].type").value("SALE_IN"))
                    .andExpect(jsonPath("$.movements[0].referenceType").value("POS_DOCUMENT"));
        }

        @Test
        @DisplayName("GET /cash-sessions/{id} inexistente responde 404, no 500")
        void get_inexistente_responde_404() throws Exception {
            when(getUseCase.get(COMPANY_ID, 999L))
                    .thenThrow(new CashSessionNotFoundException(999L));

            mockMvc.perform(get("/cash-sessions/999")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /cash-sessions/open pide solo las sedes que el empleado alcanza")
        void get_abiertas() throws Exception {
            when(listOpenUseCase.listOpen(COMPANY_ID, Set.of(SEDE_RESUELTA, 32L)))
                    .thenReturn(List.of(abierta()));

            // El scope de sedes no viaja en el request: lo arma Authz. Con un scope
            // distinto el doble no responde y el listado sale vacio.
            mockMvc.perform(get("/cash-sessions/open")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(SESSION_ID));
        }

        @Test
        @DisplayName("GET /cash-sessions devuelve la envoltura paginada")
        void get_historial_paginado() throws Exception {
            when(listUseCase.list(any()))
                    .thenReturn(new PageResult<>(List.of(cerrada()), 1, 5, 11L, 3));

            mockMvc.perform(get("/cash-sessions").param("page", "1").param("pageSize", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(SESSION_ID))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(11))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("GET /cash-sessions sin parametros consulta pagina 0, tamano 20 y sin filtros")
        void get_historial_por_defecto() throws Exception {
            when(listUseCase.list(any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

            mockMvc.perform(get("/cash-sessions")).andExpect(status().isOk());

            verify(listUseCase).list(new SearchCashSessionsQuery(COMPANY_ID, SEDE_RESUELTA, null,
                    null, null, 0, 20));
        }

        @Test
        @DisplayName("GET /cash-sessions traslada empleado y rango de fechas al query")
        void get_historial_traslada_los_filtros() throws Exception {
            when(listUseCase.list(any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

            mockMvc.perform(get("/cash-sessions").param("branchId", String.valueOf(SEDE_PEDIDA))
                    .param("employeeId", "8").param("from", "2026-01-01").param("to", "2026-01-31"))
                    .andExpect(status().isOk());

            verify(listUseCase).list(new SearchCashSessionsQuery(COMPANY_ID, SEDE_RESUELTA, 8L,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 0, 20));
        }

        @Test
        @DisplayName("GET /cash-sessions con una fecha ilegible responde 400")
        void get_historial_con_fecha_ilegible_responde_400() throws Exception {
            mockMvc.perform(get("/cash-sessions").param("from", "01/01/2026"))
                    .andExpect(status().isBadRequest());

            verify(listUseCase, never()).list(any());
        }
    }

    @Nested
    @DisplayName("GET /cash-sessions/{id}/arqueo/export")
    class ExportDelArqueo {

        private static CashArqueoReport arqueo() {
            return CashArqueoReport.from(CashSessionMother.sesionCerrada());
        }

        @Test
        @DisplayName("por defecto entrega un CSV descargable con el nombre del arqueo")
        void por_defecto_entrega_csv() throws Exception {
            when(exportArqueoUseCase.arqueo(COMPANY_ID, SESSION_ID)).thenReturn(arqueo());

            // El navegador solo guarda el archivo si vienen los dos: el tipo y el
            // Content-Disposition. Un 200 a secas abriria el CSV en pantalla.
            mockMvc.perform(get("/cash-sessions/500/arqueo/export")).andExpect(status().isOk())
                    .andExpect(content().contentType("text/csv;charset=UTF-8"))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"arqueo_caja_500.csv\";"
                                    + " filename*=UTF-8''arqueo_caja_500.csv"))
                    .andExpect(content().string(containsString("Arqueo de caja")))
                    .andExpect(content().string(containsString("Efectivo")));
        }

        @Test
        @DisplayName("con format=pdf entrega el PDF renderizado por el puerto")
        void con_format_pdf_entrega_pdf() throws Exception {
            when(exportArqueoUseCase.arqueo(COMPANY_ID, SESSION_ID)).thenReturn(arqueo());
            when(cashReportPdf.renderArqueo(any())).thenReturn("%PDF-1.4".getBytes());

            mockMvc.perform(get("/cash-sessions/500/arqueo/export").param("format", "pdf"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            containsString("arqueo_caja_500.pdf")));
        }

        @Test
        @DisplayName("un formato desconocido cae al CSV en vez de romper")
        void formato_desconocido_cae_al_csv() throws Exception {
            when(exportArqueoUseCase.arqueo(COMPANY_ID, SESSION_ID)).thenReturn(arqueo());

            mockMvc.perform(get("/cash-sessions/500/arqueo/export").param("format", "xlsx"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/csv;charset=UTF-8"));

            verify(cashReportPdf, never()).renderArqueo(any());
        }

        @Test
        @DisplayName("el arqueo de una sesion inexistente responde 404")
        void arqueo_inexistente_responde_404() throws Exception {
            when(exportArqueoUseCase.arqueo(COMPANY_ID, 999L))
                    .thenThrow(new CashSessionNotFoundException(999L));

            mockMvc.perform(get("/cash-sessions/999/arqueo/export"))
                    .andExpect(status().isNotFound());

            verify(cashReportPdf, never()).renderArqueo(any());
        }
    }
}
