package com.vetsoftware.app.servicechargeopenaccount.infrastructure.web;

import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.CHARGE_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.EMPLEADO;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.cargo;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.cargoAnulado;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.servicechargeopenaccount.application.command.CreateServiceChargeOpenAccountCommand;
import com.vetsoftware.app.servicechargeopenaccount.application.command.VoidServiceChargeOpenAccountCommand;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.CreateServiceChargeOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.ListServiceChargeOpenAccountsByOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.ListServiceChargeOpenAccountsUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.VoidServiceChargeOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccountAlreadyVoidedException;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
 * Rodaja HTTP del controller de cargos por servicio (BE-10): rutas, binding,
 * codigos de estado y forma del JSON. Los casos de uso son dobles; la
 * autorizacion vive en el {@code @PreAuthorize} de cada puerto de entrada y
 * ArchUnit ya la comprueba, no aqui.
 *
 * <p>
 * El {@code COMPANY_ID} de {@link WebMvcSliceConfig} (9L) coincide a proposito
 * con el de {@code ServiceChargeOpenAccountMother}, asi que este test reusa
 * directamente sus fixtures de dominio para construir los DTO de respuesta.
 */
@WebMvcTest(ServiceChargeOpenAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("ServiceChargeOpenAccountController — contrato HTTP")
class ServiceChargeOpenAccountControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateServiceChargeOpenAccountUseCase createUseCase;
    @MockitoBean
    private ListServiceChargeOpenAccountsUseCase listUseCase;
    @MockitoBean
    private ListServiceChargeOpenAccountsByOpenAccountUseCase listByOpenAccountUseCase;
    @MockitoBean
    private VoidServiceChargeOpenAccountUseCase voidUseCase;

    /**
     * El controller sella el {@code createdBy}/{@code voidedBy} con
     * {@code authz.currentEmployeeId()}: sin este stub Mockito devolveria null y el
     * command llegaria sin empleado.
     */
    @BeforeEach
    void resolverElEmpleadoDesdeElContexto() {
        when(authz.currentEmployeeId()).thenReturn(EMPLEADO.id());
    }

    private static ServiceChargeOpenAccountDto cargoDto() {
        return ServiceChargeOpenAccountDto.from(cargo());
    }

    @Nested
    @DisplayName("POST /service-charge-open-accounts")
    class Crear {

        @Test
        @DisplayName("responde 201 con el recurso creado y sus sumarios anidados")
        void responde_201_con_el_recurso_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cargoDto());

            mockMvc.perform(post("/service-charge-open-accounts")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"animalId":1,"serviceId":2,"openAccountId":50}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(CHARGE_ID))
                    .andExpect(jsonPath("$.animal.code").value("A-001"))
                    .andExpect(jsonPath("$.service.name").value("Consulta general"))
                    .andExpect(jsonPath("$.unitPrice").value(11900))
                    .andExpect(jsonPath("$.hasTax").value(true))
                    .andExpect(jsonPath("$.baseAmount").value(10000.00))
                    .andExpect(jsonPath("$.taxAmount").value(1900.00))
                    .andExpect(jsonPath("$.totalAmount").value(11900.00))
                    .andExpect(jsonPath("$.openAccount.id").value(50))
                    .andExpect(jsonPath("$.createdBy.name").value("Ana Ruiz"))
                    .andExpect(jsonPath("$.enabled").value(true))
                    .andExpect(jsonPath("$.voided").value(false));
        }

        @Test
        @DisplayName("traduce el request al command con la company y el empleado del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cargoDto());

            mockMvc.perform(post("/service-charge-open-accounts")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"animalId":1,"serviceId":2,"openAccountId":50,
                             "clientRequestId":"req-1","expectedVersion":3}
                            """)).andExpect(status().isCreated());

            // Ni companyId ni employeeId viajan crudos desde el cliente: los dos salen del
            // AuthContext, no del cuerpo del request.
            verify(createUseCase).execute(new CreateServiceChargeOpenAccountCommand(1L, 2L, 50L,
                    COMPANY_ID, EMPLEADO.id(), "req-1", 3L));
        }

        @Test
        @DisplayName("sin animalId responde 400 y no llega al caso de uso")
        void sin_animal_id_responde_400() throws Exception {
            mockMvc.perform(post("/service-charge-open-accounts")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"serviceId":2,"openAccountId":50}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("una invariante de dominio rota sale como 400, no como 500")
        void invariante_de_dominio_sale_como_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Animal not found: 1"));

            mockMvc.perform(post("/service-charge-open-accounts")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"animalId":1,"serviceId":2,"openAccountId":50}
                            """)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /service-charge-open-accounts")
    class Listar {

        @Test
        @DisplayName("arma la pagina con la company del contexto")
        void arma_la_pagina_con_la_company_del_contexto() throws Exception {
            when(listUseCase.listAll(eq(COMPANY_ID), eq(0), eq(20)))
                    .thenReturn(new PageResult<>(List.of(cargoDto()), 0, 20, 1L, 1));

            mockMvc.perform(get("/service-charge-open-accounts")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(CHARGE_ID));

            verify(listUseCase).listAll(COMPANY_ID, 0, 20);
        }

        @Test
        @DisplayName("acepta page y pageSize por query param")
        void acepta_page_y_page_size_por_query_param() throws Exception {
            when(listUseCase.listAll(COMPANY_ID, 2, 10))
                    .thenReturn(new PageResult<>(List.of(), 2, 10, 0L, 0));

            mockMvc.perform(
                    get("/service-charge-open-accounts").param("page", "2").param("pageSize", "10"))
                    .andExpect(status().isOk());

            verify(listUseCase).listAll(COMPANY_ID, 2, 10);
        }
    }

    @Nested
    @DisplayName("GET /service-charge-open-accounts/by-open-account/{openAccountId}")
    class ListarPorCuenta {

        @Test
        @DisplayName("devuelve los cargos de la cuenta acotados a la company del contexto")
        void devuelve_los_cargos_de_la_cuenta() throws Exception {
            when(listByOpenAccountUseCase.listByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of(cargoDto()));

            mockMvc.perform(get("/service-charge-open-accounts/by-open-account/50"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(CHARGE_ID));

            verify(listByOpenAccountUseCase).listByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("PATCH /service-charge-open-accounts/{id}/void")
    class Anular {

        @Test
        @DisplayName("responde 200 y arma el command con la razon y el empleado del contexto")
        void responde_200_y_arma_el_command() throws Exception {
            when(voidUseCase.execute(any()))
                    .thenReturn(ServiceChargeOpenAccountDto.from(cargoAnulado()));

            mockMvc.perform(patch("/service-charge-open-accounts/100/void")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"reason":"Cobrado por error","expectedVersion":1}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.voided").value(true))
                    .andExpect(jsonPath("$.voidReason").value("Cobrado por error"));

            verify(voidUseCase).execute(new VoidServiceChargeOpenAccountCommand(CHARGE_ID,
                    COMPANY_ID, EMPLEADO.id(), "Cobrado por error", 1L));
        }

        @Test
        @DisplayName("sin motivo responde 400 y no llega al caso de uso")
        void sin_motivo_responde_400() throws Exception {
            mockMvc.perform(patch("/service-charge-open-accounts/100/void")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"reason":""}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(voidUseCase);
        }

        @Test
        @DisplayName("un cargo ya anulado responde 409 con el codigo estable")
        void un_cargo_ya_anulado_responde_409() throws Exception {
            when(voidUseCase.execute(any()))
                    .thenThrow(new ServiceChargeOpenAccountAlreadyVoidedException(CHARGE_ID));

            mockMvc.perform(patch("/service-charge-open-accounts/100/void")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"reason":"Otra vez"}
                            """)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CHARGE_OPEN_ACCOUNT_ALREADY_VOIDED"));
        }
    }
}
