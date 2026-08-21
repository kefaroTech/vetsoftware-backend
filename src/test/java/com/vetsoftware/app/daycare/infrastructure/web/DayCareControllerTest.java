package com.vetsoftware.app.daycare.infrastructure.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.daycare.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.daycare.application.dto.CompanySummaryDto;
import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import com.vetsoftware.app.daycare.application.port.in.ListDayCaresUseCase;
import com.vetsoftware.app.daycare.domain.DayCareType;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP del controller: rutas, binding, validacion del request, codigos
 * de estado y forma del JSON. Lo que hay debajo son dobles.
 */
@WebMvcTest(DayCareController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("DayCareController — contrato HTTP")
class DayCareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListDayCaresUseCase listUseCase;

    private static DayCareDto guarderia() {
        return new DayCareDto(5L, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 3), DayCareType.DAYCARE, "Correa", "Sin novedades",
                new AnimalSummaryDto(1L, "Firulais", "A-001"),
                new CompanySummaryDto(10L, "Veterinaria de prueba", "900123456"),
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Test
    @DisplayName("GET /daycares devuelve la lista global")
    void get_lista_global() throws Exception {
        when(listUseCase.listAll()).thenReturn(List.of(guarderia()));

        mockMvc.perform(get("/daycares")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }
}
