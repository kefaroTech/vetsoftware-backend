package com.vetsoftware.app.consultationtype.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.consultationtype.application.command.CreateConsultationTypeCommand;
import com.vetsoftware.app.consultationtype.application.command.UpdateConsultationTypeCommand;
import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import com.vetsoftware.app.consultationtype.application.port.in.CreateConsultationTypeUseCase;
import com.vetsoftware.app.consultationtype.application.port.in.DeleteConsultationTypeUseCase;
import com.vetsoftware.app.consultationtype.application.port.in.FindConsultationTypeUseCase;
import com.vetsoftware.app.consultationtype.application.port.in.ListConsultationTypesUseCase;
import com.vetsoftware.app.consultationtype.application.port.in.ReactivateConsultationTypeUseCase;
import com.vetsoftware.app.consultationtype.application.port.in.UpdateConsultationTypeUseCase;
import com.vetsoftware.app.consultationtype.domain.ConsultationTypeNotFoundException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP del controller: rutas, binding, validacion del request, codigos
 * de estado y forma del JSON. Lo que hay debajo son dobles — aqui no se prueba
 * el caso de uso, se prueba el contrato que ve el front.
 */
@WebMvcTest(ConsultationTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("ConsultationTypeController — contrato HTTP")
class ConsultationTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateConsultationTypeUseCase createUseCase;
    @MockitoBean
    private UpdateConsultationTypeUseCase updateUseCase;
    @MockitoBean
    private FindConsultationTypeUseCase findUseCase;
    @MockitoBean
    private ListConsultationTypesUseCase listUseCase;
    @MockitoBean
    private DeleteConsultationTypeUseCase deleteUseCase;
    @MockitoBean
    private ReactivateConsultationTypeUseCase reactivateUseCase;

    private static ConsultationTypeDto vacunacion() {
        return new ConsultationTypeDto(1L, "Vacunacion", "Aplicacion de vacunas",
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Test
    @DisplayName("POST /consultation-types responde 201 con el recurso creado")
    void post_responde_201() throws Exception {
        when(createUseCase.execute(any())).thenReturn(vacunacion());

        mockMvc.perform(post("/consultation-types").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Vacunacion\",\"description\":\"Aplicacion de vacunas\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Vacunacion"));
    }

    @Test
    @DisplayName("POST /consultation-types traduce el request al command sin inventarse campos")
    void post_traduce_el_request_al_command() throws Exception {
        when(createUseCase.execute(any())).thenReturn(vacunacion());

        mockMvc.perform(post("/consultation-types").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Vacunacion\",\"description\":\"Aplicacion de vacunas\"}"));

        verify(createUseCase)
                .execute(new CreateConsultationTypeCommand("Vacunacion", "Aplicacion de vacunas"));
    }

    @Test
    @DisplayName("POST /consultation-types con nombre vacio responde 400 y no llega al caso de uso")
    void post_con_nombre_vacio_responde_400() throws Exception {
        mockMvc.perform(post("/consultation-types").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"description\":\"Aplicacion de vacunas\"}"))
                .andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("POST /consultation-types con descripcion vacia responde 400 y no llega al caso de uso")
    void post_con_descripcion_vacia_responde_400() throws Exception {
        mockMvc.perform(post("/consultation-types").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Vacunacion\",\"description\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET /consultation-types devuelve la lista")
    void get_lista() throws Exception {
        when(listUseCase.listAll()).thenReturn(List.of(vacunacion()));

        mockMvc.perform(get("/consultation-types")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("GET /consultation-types/{id} devuelve el recurso")
    void get_por_id_devuelve_el_recurso() throws Exception {
        when(findUseCase.findById(1L)).thenReturn(vacunacion());

        mockMvc.perform(get("/consultation-types/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Vacunacion"));
    }

    @Test
    @DisplayName("GET /consultation-types/{id} inexistente responde 404, no 500")
    void get_inexistente_responde_404() throws Exception {
        when(findUseCase.findById(99L)).thenThrow(new ConsultationTypeNotFoundException(99L));

        mockMvc.perform(get("/consultation-types/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /consultation-types/{id} responde 200 con los campos actualizados")
    void put_responde_200() throws Exception {
        when(updateUseCase.execute(any())).thenReturn(new ConsultationTypeDto(1L, "Nuevo nombre",
                "Nueva descripcion", LocalDateTime.of(2026, 1, 15, 10, 30), true));

        mockMvc.perform(put("/consultation-types/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Nuevo nombre\",\"description\":\"Nueva descripcion\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Nuevo nombre"));

        verify(updateUseCase).execute(
                new UpdateConsultationTypeCommand(1L, "Nuevo nombre", "Nueva descripcion"));
    }

    @Test
    @DisplayName("PUT /consultation-types/{id} con nombre vacio responde 400 y no llega al caso de uso")
    void put_con_nombre_vacio_responde_400() throws Exception {
        mockMvc.perform(put("/consultation-types/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"description\":\"Nueva descripcion\"}"))
                .andExpect(status().isBadRequest());

        verify(updateUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("DELETE /consultation-types/{id} responde 204 sin cuerpo")
    void delete_responde_204() throws Exception {
        mockMvc.perform(delete("/consultation-types/1")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(1L);
    }

    @Test
    @DisplayName("PATCH /consultation-types/{id}/enable responde 200 con el recurso habilitado")
    void patch_enable_responde_200() throws Exception {
        when(reactivateUseCase.execute(1L)).thenReturn(vacunacion());

        mockMvc.perform(patch("/consultation-types/1/enable")).andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }
}
