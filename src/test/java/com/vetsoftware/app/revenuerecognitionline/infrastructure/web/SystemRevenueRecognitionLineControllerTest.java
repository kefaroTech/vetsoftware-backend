package com.vetsoftware.app.revenuerecognitionline.infrastructure.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.revenuerecognitionline.application.dto.RevenueRecognitionLineDto;
import com.vetsoftware.app.revenuerecognitionline.application.port.in.FindRevenueRecognitionLineUseCase;
import com.vetsoftware.app.revenuerecognitionline.application.port.in.ListRevenueRecognitionLinesUseCase;
import com.vetsoftware.app.revenuerecognitionline.domain.RecognitionMethod;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
 * Rodaja web del libro de reconocimiento de ingreso.
 *
 * <p>
 * <b>El caso mas importante de esta clase es el que comprueba que NO hay
 * alta.</b> {@code revenue_recognition_lines} es un libro derivado: cada
 * renglon sale del prorrateo de un cargo, no de que alguien escriba un importe.
 * Un {@code POST} aqui permitiria inventar ingreso que ningun cargo respalda, y
 * el libro dejaria de cuadrar contra la cartera <b>sin que nada lo delate</b> —
 * ninguna constraint puede comprobar que un reconocimiento corresponde a lo
 * realmente devengado. {@link SinEscritura} es lo que se pondria rojo el dia
 * que alguien añadiera ese endpoint «para poder cargar el historico».
 *
 * <p>
 * Lo segundo es que el importe negativo —la fila que compensa— <b>cruza la
 * frontera HTTP con su signo</b>. Es el campo que un front puede estropear con
 * un formato, y sin signo la correccion se leeria como un ingreso mas.
 */
@WebMvcTest(SystemRevenueRecognitionLineController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemRevenueRecognitionLineController — contrato HTTP de plataforma")
class SystemRevenueRecognitionLineControllerTest {

    private static final RevenueRecognitionLineDto RENGLON = new RevenueRecognitionLineDto(8420L,
            900L, 8422L, "2028-03", "2028-03", new BigDecimal("125000.00"),
            RecognitionMethod.STRAIGHT_LINE_DAYS, LocalDateTime.of(2028, 4, 1, 2, 0));

    private static final RevenueRecognitionLineDto COMPENSA = new RevenueRecognitionLineDto(8421L,
            900L, 8422L, "2028-03", "2028-04", new BigDecimal("-125000.00"),
            RecognitionMethod.STRAIGHT_LINE_DAYS, LocalDateTime.of(2028, 5, 1, 2, 0));

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private FindRevenueRecognitionLineUseCase findUseCase;
    @MockitoBean
    private ListRevenueRecognitionLinesUseCase listUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Lectura")
    class Lectura {

        @Test
        @DisplayName("un renglon por id sale con sus dos meses, que no son el mismo campo")
        void un_renglon_por_id_sale_con_sus_dos_meses() throws Exception {
            when(findUseCase.findById(8420L)).thenReturn(RENGLON);

            mockMvc.perform(get("/system/revenue-recognition-lines/8420"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(8420))
                    .andExpect(jsonPath("$.companyId").value(900))
                    // El mes al que se imputa y el periodo en que se registra son dos
                    // datos distintos: cruzarlos aqui compila sin una queja.
                    .andExpect(jsonPath("$.periodKey").value("2028-03"))
                    .andExpect(jsonPath("$.postingPeriod").value("2028-03"));
        }

        @Test
        @DisplayName("la fila que compensa cruza la frontera HTTP con su signo negativo")
        void la_fila_que_compensa_cruza_con_su_signo() throws Exception {
            when(findUseCase.findById(8421L)).thenReturn(COMPENSA);

            mockMvc.perform(get("/system/revenue-recognition-lines/8421"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recognizedAmount").value(-125000.00))
                    .andExpect(jsonPath("$.postingPeriod").value("2028-04"));
        }

        @Test
        @DisplayName("el libro de una clinica exige companyId por la query string")
        void el_libro_de_una_clinica_exige_company_id() throws Exception {
            // La empresa viaja por query string y no sale de authz.currentCompanyId():
            // es el patron de las rutas de plataforma, donde un principal SYSTEM no
            // tiene empresa propia y elige de que clinica quiere el libro.
            when(listUseCase.listByCompany(900L, 0, 20))
                    .thenReturn(PageResult.of(List.of(RENGLON), 0, 20, 1L));

            mockMvc.perform(get("/system/revenue-recognition-lines").param("companyId", "900"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].companyId").value(900))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(listUseCase).listByCompany(900L, 0, 20);
        }

        @Test
        @DisplayName("el barrido del cierre va por periodo contable y no lleva empresa")
        void el_barrido_del_cierre_va_por_periodo_contable() throws Exception {
            when(listUseCase.listByPostingPeriod("2028-03", 0, 20))
                    .thenReturn(PageResult.of(List.of(RENGLON), 0, 20, 1L));

            mockMvc.perform(get("/system/revenue-recognition-lines/by-posting-period/2028-03"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));

            verify(listUseCase).listByPostingPeriod("2028-03", 0, 20);
        }
    }

    @Nested
    @DisplayName("Sin escritura por HTTP")
    class SinEscritura {

        @Test
        @DisplayName("no existe alta manual: un POST no encuentra ruta")
        void no_existe_alta_manual() throws Exception {
            // Es el caso que se pone rojo el dia que alguien añada el endpoint. La
            // escritura vive en RecordRevenueRecognitionUseCase, que no tiene ruta:
            // solo lo alcanza el proceso que factura.
            mockMvc.perform(post("/system/revenue-recognition-lines")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }
}
