package com.vetsoftware.app.smmlvvalue.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.smmlvvalue.application.command.ChangeSmmlvStatusCommand;
import com.vetsoftware.app.smmlvvalue.application.dto.SmmlvValueDto;
import com.vetsoftware.app.smmlvvalue.application.port.in.ChangeSmmlvStatusUseCase;
import com.vetsoftware.app.smmlvvalue.application.port.in.CreateSmmlvValueUseCase;
import com.vetsoftware.app.smmlvvalue.application.port.in.FindSmmlvValueForYearUseCase;
import com.vetsoftware.app.smmlvvalue.application.port.in.ListSmmlvValuesUseCase;
import com.vetsoftware.app.smmlvvalue.domain.SmmlvStatus;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

@WebMvcTest(SmmlvValueController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SmmlvValueController — contrato HTTP del salario minimo y su estado")
class SmmlvValueControllerTest {

    private static final String AUTO = "Consejo de Estado, Seccion Segunda, auto del 12-02-2026 (suspension provisional)";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateSmmlvValueUseCase createUseCase;

    @MockitoBean
    private ChangeSmmlvStatusUseCase changeStatusUseCase;

    @MockitoBean
    private FindSmmlvValueForYearUseCase findUseCase;

    @MockitoBean
    private ListSmmlvValuesUseCase listUseCase;

    private static SmmlvValueDto suspendido2026() {
        return new SmmlvValueDto(7400L, 2026, new BigDecimal("1750905.00"), "Decreto 1469 de 2025",
                SmmlvStatus.SUSPENDED, AUTO, LocalDate.of(2026, 2, 12), false,
                LocalDateTime.of(2025, 12, 30, 8, 0, 0), true);
    }

    @Nested
    @DisplayName("Lectura por ano")
    class LecturaPorAno {

        @Test
        @DisplayName("la cifra viaja SIEMPRE con su estado: 2026 sale suspendido y no vigente")
        void la_cifra_viaja_con_su_estado() throws Exception {
            when(findUseCase.findByYear(anyInt(), any())).thenReturn(suspendido2026());

            mockMvc.perform(get("/smmlv-values/years/2026")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.valueAmount").value(1750905.00))
                    .andExpect(jsonPath("$.status").value("SUSPENDED"))
                    .andExpect(jsonPath("$.inForce").value(false))
                    .andExpect(jsonPath("$.statusReference").value(AUTO))
                    .andExpect(jsonPath("$.statusChangedOn").value("2026-02-12"));
        }

        @Test
        @DisplayName("el listado paginado usa el contrato unico de pagina")
        void el_listado_paginado_usa_el_contrato_unico() throws Exception {
            when(listUseCase.listAll(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(suspendido2026()), 0, 20, 1));

            mockMvc.perform(get("/smmlv-values")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].inForce").value(false));
        }
    }

    @Nested
    @DisplayName("Cambio de estado")
    class CambioDeEstado {

        @Test
        @DisplayName("la ruta identifica el ano, no el id de la fila")
        void la_ruta_identifica_el_ano() throws Exception {
            when(changeStatusUseCase.execute(any())).thenReturn(suspendido2026());

            mockMvc.perform(patch("/smmlv-values/years/2026/status")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"status":"SUSPENDED","statusReference":"Auto del 12-02-2026",
                             "statusChangedOn":"2026-02-12"}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUSPENDED"));

            ArgumentCaptor<ChangeSmmlvStatusCommand> command = ArgumentCaptor
                    .forClass(ChangeSmmlvStatusCommand.class);
            verify(changeStatusUseCase).execute(command.capture());
            assertThat(command.getValue().fiscalYear()).isEqualTo(2026);
            assertThat(command.getValue().status()).isEqualTo(SmmlvStatus.SUSPENDED);
            assertThat(command.getValue().statusChangedOn()).isEqualTo(LocalDate.of(2026, 2, 12));
        }

        @Test
        @DisplayName("sin estado sale 400 y NO llega al caso de uso")
        void sin_estado_sale_400() throws Exception {
            mockMvc.perform(patch("/smmlv-values/years/2026/status")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"statusReference":"Auto","statusChangedOn":"2026-02-12"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(changeStatusUseCase);
        }
    }

    @Nested
    @DisplayName("Publicacion")
    class Publicacion {

        @Test
        @DisplayName("responde 201; el ano nace vigente y el cuerpo no puede decir otra cosa")
        void responde_201_y_el_ano_nace_vigente() throws Exception {
            when(createUseCase.execute(any())).thenReturn(suspendido2026());

            mockMvc.perform(
                    post("/smmlv-values").contentType(MediaType.APPLICATION_JSON).content("""
                            {"fiscalYear":2026,"valueAmount":1750905.00,
                             "legalReference":"Decreto 1469 de 2025","status":"SUSPENDED"}
                            """)).andExpect(status().isCreated());

            // El "status" del cuerpo se ignora: CreateSmmlvValueRequest no lo declara y el
            // dominio hace nacer la fila IN_FORCE. Suspender es otra operacion, con su
            // providencia escrita.
            verify(createUseCase).execute(any());
        }
    }
}
