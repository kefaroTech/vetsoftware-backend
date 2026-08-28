package com.vetsoftware.app.publicholiday.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.publicholiday.application.command.CreatePublicHolidayCommand;
import com.vetsoftware.app.publicholiday.application.command.ResolveBusinessDayDeadlineCommand;
import com.vetsoftware.app.publicholiday.application.dto.BusinessDayDeadlineDto;
import com.vetsoftware.app.publicholiday.application.dto.PublicHolidayDto;
import com.vetsoftware.app.publicholiday.application.port.in.CreatePublicHolidayUseCase;
import com.vetsoftware.app.publicholiday.application.port.in.FindPublicHolidayUseCase;
import com.vetsoftware.app.publicholiday.application.port.in.ListPublicHolidaysUseCase;
import com.vetsoftware.app.publicholiday.application.port.in.ResolveBusinessDayDeadlineUseCase;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
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

@WebMvcTest(PublicHolidayController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("PublicHolidayController — contrato HTTP del calendario y de los plazos")
class PublicHolidayControllerTest {

    private static final Long ID = 7100L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private CreatePublicHolidayUseCase createUseCase;

    @MockitoBean
    private FindPublicHolidayUseCase findUseCase;

    @MockitoBean
    private ListPublicHolidaysUseCase listUseCase;

    @MockitoBean
    private ResolveBusinessDayDeadlineUseCase deadlineUseCase;

    private static PublicHolidayDto unFestivo() {
        return new PublicHolidayDto(ID, LocalDate.of(2026, 7, 13),
                "Nuestra Senora del Rosario de Chiquinquira", LocalDate.of(2026, 7, 9), true,
                "Ley 2578 de 2026, art. 6", LocalDateTime.of(2026, 6, 2, 10, 0, 0), true);
    }

    @Nested
    @DisplayName("Alta del festivo")
    class Alta {

        @Test
        @DisplayName("responde 201 y traslada las dos fechas y el traslado al command")
        void responde_201_y_traslada_las_dos_fechas() throws Exception {
            when(createUseCase.execute(any())).thenReturn(unFestivo());

            mockMvc.perform(
                    post("/public-holidays").contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {"holidayDate":"2026-07-13","name":"Nuestra Senora del Rosario de Chiquinquira",
                                     "nominalDate":"2026-07-09","moved":true,"legalReference":"Ley 2578 de 2026, art. 6"}
                                    """))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(7100))
                    .andExpect(jsonPath("$.holidayDate").value("2026-07-13"))
                    .andExpect(jsonPath("$.nominalDate").value("2026-07-09"))
                    .andExpect(jsonPath("$.moved").value(true));

            ArgumentCaptor<CreatePublicHolidayCommand> command = ArgumentCaptor
                    .forClass(CreatePublicHolidayCommand.class);
            verify(createUseCase).execute(command.capture());
            assertThat(command.getValue().holidayDate()).isEqualTo(LocalDate.of(2026, 7, 13));
            assertThat(command.getValue().nominalDate()).isEqualTo(LocalDate.of(2026, 7, 9));
            assertThat(command.getValue().moved()).isTrue();
        }

        @Test
        @DisplayName("un nombre en blanco sale 400 y NO llega al caso de uso")
        void un_nombre_en_blanco_sale_400() throws Exception {
            // Sin el @Valid del controller, el @NotBlank del DTO no se evaluaria nunca y
            // el nombre vacio llegaria al dominio para volver como un 400 de otra forma,
            // sin el campo que el front pinta bajo el input.
            mockMvc.perform(
                    post("/public-holidays").contentType(MediaType.APPLICATION_JSON).content("""
                            {"holidayDate":"2026-07-13","name":"  ","moved":false,
                             "legalReference":"Ley 2578 de 2026"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }
    }

    @Nested
    @DisplayName("El plazo en dias habiles")
    class Plazo {

        @Test
        @DisplayName("devuelve el vencimiento y los festivos saltados")
        void devuelve_el_vencimiento_y_los_festivos_saltados() throws Exception {
            when(deadlineUseCase.resolve(any())).thenReturn(new BusinessDayDeadlineDto(
                    LocalDate.of(2026, 7, 1), 15, LocalDate.of(2026, 7, 24), 2));

            mockMvc.perform(get("/public-holidays/deadline").param("startDate", "2026-07-01")
                    .param("businessDays", "15")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.startDate").value("2026-07-01"))
                    .andExpect(jsonPath("$.businessDays").value(15))
                    .andExpect(jsonPath("$.dueDate").value("2026-07-24"))
                    .andExpect(jsonPath("$.weekdayHolidaysSkipped").value(2));
        }

        @Test
        @DisplayName("sin startDate el controller no inventa hoy: deja el campo nulo al servicio")
        void sin_start_date_el_controller_no_inventa_hoy() throws Exception {
            when(deadlineUseCase.resolve(any())).thenReturn(new BusinessDayDeadlineDto(
                    LocalDate.of(2026, 7, 7), 1, LocalDate.of(2026, 7, 8), 0));

            mockMvc.perform(get("/public-holidays/deadline").param("businessDays", "1"))
                    .andExpect(status().isOk());

            ArgumentCaptor<ResolveBusinessDayDeadlineCommand> command = ArgumentCaptor
                    .forClass(ResolveBusinessDayDeadlineCommand.class);
            verify(deadlineUseCase).resolve(command.capture());
            // El «hoy» lo pone el reloj inyectado del servicio, con la zona del negocio.
            // Resolverlo aqui con LocalDate.now() daria el dia siguiente desde las 19:00
            // de Bogota.
            assertThat(command.getValue().startDate()).isNull();
            assertThat(command.getValue().businessDays()).isEqualTo(1);
        }

        @Test
        @DisplayName("la empresa la pone el controller desde el principal, nunca la query")
        void la_empresa_la_pone_el_controller() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(WebMvcSliceConfig.COMPANY_ID);
            when(deadlineUseCase.resolve(any())).thenReturn(new BusinessDayDeadlineDto(
                    LocalDate.of(2026, 7, 1), 15, LocalDate.of(2026, 7, 24), 2));

            mockMvc.perform(get("/public-holidays/deadline").param("startDate", "2026-07-01")
                    .param("businessDays", "15").param("companyId", "999"))
                    .andExpect(status().isOk());

            ArgumentCaptor<ResolveBusinessDayDeadlineCommand> command = ArgumentCaptor
                    .forClass(ResolveBusinessDayDeadlineCommand.class);
            verify(deadlineUseCase).resolve(command.capture());
            // El 999 de la query se ignora por completo: el controller no lo lee.
            assertThat(command.getValue().companyId()).isEqualTo(WebMvcSliceConfig.COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("Lecturas")
    class Lecturas {

        @Test
        @DisplayName("el ano completo sale sin paginar")
        void el_ano_completo_sale_sin_paginar() throws Exception {
            when(listUseCase.listByYear(anyInt(), any())).thenReturn(List.of(unFestivo()));

            mockMvc.perform(get("/public-holidays/years/2026")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].holidayDate").value("2026-07-13"));
        }

        @Test
        @DisplayName("el listado paginado usa el contrato unico de pagina")
        void el_listado_paginado_usa_el_contrato_unico() throws Exception {
            when(listUseCase.listAll(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(unFestivo()), 0, 20, 1));

            mockMvc.perform(get("/public-holidays")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("el detalle por id responde el festivo con sus dos fechas")
        void el_detalle_por_id_responde_el_festivo() throws Exception {
            when(findUseCase.findById(anyLong(), any())).thenReturn(unFestivo());

            mockMvc.perform(get("/public-holidays/7100")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.holidayDate").value("2026-07-13"))
                    .andExpect(jsonPath("$.nominalDate").value("2026-07-09"));
        }
    }
}
