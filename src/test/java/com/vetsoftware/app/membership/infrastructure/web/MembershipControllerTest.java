package com.vetsoftware.app.membership.infrastructure.web;

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

import com.vetsoftware.app.membership.application.command.CreateMembershipCommand;
import com.vetsoftware.app.membership.application.command.UpdateMembershipCommand;
import com.vetsoftware.app.membership.application.dto.MembershipDto;
import com.vetsoftware.app.membership.application.port.in.CreateMembershipUseCase;
import com.vetsoftware.app.membership.application.port.in.DeleteMembershipUseCase;
import com.vetsoftware.app.membership.application.port.in.FindMembershipUseCase;
import com.vetsoftware.app.membership.application.port.in.ListMembershipsUseCase;
import com.vetsoftware.app.membership.application.port.in.ReactivateMembershipUseCase;
import com.vetsoftware.app.membership.application.port.in.UpdateMembershipUseCase;
import com.vetsoftware.app.membership.domain.MembershipHasActiveChildrenException;
import com.vetsoftware.app.membership.domain.MembershipNotFoundException;
import com.vetsoftware.app.membership.testsupport.MembershipMother;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP de {@link MembershipController}: rutas, binding, validacion del
 * request, codigos de estado y forma del JSON. Lo que hay debajo son dobles.
 */
@WebMvcTest(MembershipController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("MembershipController — contrato HTTP")
class MembershipControllerTest {

    private static final String ALTA_VALIDA = """
            {"name":"Plan Oro","status":"ACTIVE","mandatory":false}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateMembershipUseCase createUseCase;
    @MockitoBean
    private UpdateMembershipUseCase updateUseCase;
    @MockitoBean
    private FindMembershipUseCase findUseCase;
    @MockitoBean
    private ListMembershipsUseCase listUseCase;
    @MockitoBean
    private DeleteMembershipUseCase deleteUseCase;
    @MockitoBean
    private ReactivateMembershipUseCase reactivateUseCase;

    private static MembershipDto planOro() {
        return MembershipDto.from(MembershipMother.activa());
    }

    @Nested
    @DisplayName("POST /memberships")
    class Creacion {

        @Test
        @DisplayName("responde 201 con la membresia creada")
        void responde_201() throws Exception {
            when(createUseCase.execute(any())).thenReturn(planOro());

            mockMvc.perform(post("/memberships").contentType(MediaType.APPLICATION_JSON)
                    .content(ALTA_VALIDA)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(MembershipMother.MEMBERSHIP_ID))
                    .andExpect(jsonPath("$.name").value("Plan Oro"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("traduce el request al command sin inventarse campos")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(planOro());

            mockMvc.perform(post("/memberships").contentType(MediaType.APPLICATION_JSON)
                    .content(ALTA_VALIDA));

            verify(createUseCase).execute(new CreateMembershipCommand("Plan Oro", "ACTIVE", false));
        }

        @Test
        @DisplayName("sin nombre responde 400 y no crea nada")
        void sin_nombre_responde_400() throws Exception {
            mockMvc.perform(post("/memberships").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"ACTIVE\",\"mandatory\":false}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin estado responde 400 y no crea nada")
        void sin_estado_responde_400() throws Exception {
            mockMvc.perform(post("/memberships").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Plan Oro\",\"mandatory\":false}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("GET /memberships")
    class Listado {

        @Test
        @DisplayName("devuelve la lista de membresias")
        void devuelve_la_lista() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(planOro()));

            mockMvc.perform(get("/memberships")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Plan Oro"));
        }
    }

    @Nested
    @DisplayName("GET /memberships/{id}")
    class Busqueda {

        @Test
        @DisplayName("devuelve el detalle de la membresia")
        void devuelve_el_detalle() throws Exception {
            when(findUseCase.findById(MembershipMother.MEMBERSHIP_ID)).thenReturn(planOro());

            mockMvc.perform(get("/memberships/" + MembershipMother.MEMBERSHIP_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(MembershipMother.MEMBERSHIP_ID));
        }

        @Test
        @DisplayName("una membresia inexistente responde 404, no 500")
        void una_membresia_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(999L)).thenThrow(new MembershipNotFoundException(999L));

            mockMvc.perform(get("/memberships/999")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /memberships/{id}")
    class Actualizacion {

        @Test
        @DisplayName("responde 200 con la membresia actualizada")
        void responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(planOro());

            mockMvc.perform(put("/memberships/" + MembershipMother.MEMBERSHIP_ID)
                    .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Plan Oro"));
        }

        @Test
        @DisplayName("el id sale de la ruta, nunca del cuerpo")
        void el_id_sale_de_la_ruta() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(planOro());

            mockMvc.perform(put("/memberships/" + MembershipMother.MEMBERSHIP_ID)
                    .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA));

            verify(updateUseCase).execute(new UpdateMembershipCommand(
                    MembershipMother.MEMBERSHIP_ID, "Plan Oro", "ACTIVE", false));
        }

        @Test
        @DisplayName("sin nombre responde 400 y no actualiza nada")
        void sin_nombre_responde_400() throws Exception {
            mockMvc.perform(put("/memberships/" + MembershipMother.MEMBERSHIP_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"ACTIVE\",\"mandatory\":false}"))
                    .andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("actualizar una membresia inexistente responde 404")
        void actualizar_una_membresia_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new MembershipNotFoundException(999L));

            mockMvc.perform(put("/memberships/999").contentType(MediaType.APPLICATION_JSON)
                    .content(ALTA_VALIDA)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /memberships/{id}")
    class Borrado {

        @Test
        @DisplayName("responde 204 sin cuerpo")
        void responde_204() throws Exception {
            mockMvc.perform(delete("/memberships/" + MembershipMother.MEMBERSHIP_ID))
                    .andExpect(status().isNoContent());

            verify(deleteUseCase).execute(MembershipMother.MEMBERSHIP_ID);
        }

        @Test
        @DisplayName("borrar una membresia con submodulos activos responde 409, no 500")
        void borrar_con_submodulos_activos_responde_409() throws Exception {
            org.mockito.Mockito
                    .doThrow(new MembershipHasActiveChildrenException(
                            MembershipMother.MEMBERSHIP_ID, "membershipSubModule"))
                    .when(deleteUseCase).execute(MembershipMother.MEMBERSHIP_ID);

            mockMvc.perform(delete("/memberships/" + MembershipMother.MEMBERSHIP_ID))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("borrar una membresia inexistente responde 404")
        void borrar_una_membresia_inexistente_responde_404() throws Exception {
            org.mockito.Mockito.doThrow(new MembershipNotFoundException(999L)).when(deleteUseCase)
                    .execute(999L);

            mockMvc.perform(delete("/memberships/999")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /memberships/{id}/enable")
    class Reactivacion {

        @Test
        @DisplayName("responde 200 con la membresia reactivada")
        void responde_200() throws Exception {
            when(reactivateUseCase.execute(MembershipMother.MEMBERSHIP_ID)).thenReturn(planOro());

            mockMvc.perform(patch("/memberships/" + MembershipMother.MEMBERSHIP_ID + "/enable"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("reactivar una membresia inexistente responde 404")
        void reactivar_una_membresia_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(999L)).thenThrow(new MembershipNotFoundException(999L));

            mockMvc.perform(patch("/memberships/999/enable")).andExpect(status().isNotFound());
        }
    }
}
