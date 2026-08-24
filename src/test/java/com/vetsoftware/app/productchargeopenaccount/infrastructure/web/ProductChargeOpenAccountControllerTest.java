package com.vetsoftware.app.productchargeopenaccount.infrastructure.web;

import static com.vetsoftware.app.productchargeopenaccount.testsupport.ProductChargeOpenAccountMother.ANIMAL;
import static com.vetsoftware.app.productchargeopenaccount.testsupport.ProductChargeOpenAccountMother.CHARGE_ID;
import static com.vetsoftware.app.productchargeopenaccount.testsupport.ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.productchargeopenaccount.testsupport.ProductChargeOpenAccountMother.PRODUCTO;
import static com.vetsoftware.app.productchargeopenaccount.testsupport.ProductChargeOpenAccountMother.cargo;
import static com.vetsoftware.app.productchargeopenaccount.testsupport.ProductChargeOpenAccountMother.cargoAnulado;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.productchargeopenaccount.application.command.CreateProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.application.command.VoidProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.CreateProductChargeOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.ListProductChargeOpenAccountsByOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.ListProductChargeOpenAccountsUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.VoidProductChargeOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountAlreadyVoidedException;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountNotFoundException;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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

/**
 * Rodaja HTTP del controller de cargos por producto: rutas, verbos, binding, el
 * {@code @Valid} de cada cuerpo, los codigos de estado y la forma del JSON
 * campo por campo. Los casos de uso son dobles —solo los puertos
 * {@code port/in}—; el DTO de respuesta se construye de verdad desde el dominio
 * del {@code ProductChargeOpenAccountMother}.
 *
 * <p>
 * La autorizacion no se prueba aqui: vive en el {@code @PreAuthorize} de cada
 * puerto de entrada y ArchUnit ya lo verifica. Lo que si se fija es la
 * <b>autoria</b>: el controller sella {@code companyId}, {@code createdById} y
 * la sede desde el {@code Authz} del contexto, no desde el cuerpo del request,
 * y eso se afirma con {@link ArgumentCaptor} sobre el command —no con un
 * {@code verify(...any())}, que pasaria igual si el controller sellara al
 * empleado equivocado—.
 *
 * <p>
 * El {@code COMPANY_ID} de {@link WebMvcSliceConfig} (9) coincide a proposito
 * con el del mother, asi que sus fixtures de dominio sirven tal cual. El
 * empleado, en cambio, se deja distinto a proposito
 * ({@code WebMvcSliceConfig.EMPLOYEE_ID} = 4 frente al 7 del mother): asi la
 * asercion distingue el empleado del contexto del que trae el fixture.
 */
@WebMvcTest(ProductChargeOpenAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("ProductChargeOpenAccountController — contrato HTTP")
class ProductChargeOpenAccountControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLEADO_DEL_CONTEXTO = WebMvcSliceConfig.EMPLOYEE_ID;

    /**
     * Sede que devuelve {@code resolveAccessibleBranch}, distinta de la que pide el
     * cuerpo: el command tiene que llevar la resuelta, no la cruda del cliente.
     */
    private static final Long SEDE_RESUELTA = 33L;

    private static final Long SEDE_PEDIDA = 3L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateProductChargeOpenAccountUseCase createUseCase;
    @MockitoBean
    private ListProductChargeOpenAccountsUseCase listUseCase;
    @MockitoBean
    private ListProductChargeOpenAccountsByOpenAccountUseCase listByOpenAccountUseCase;
    @MockitoBean
    private VoidProductChargeOpenAccountUseCase voidUseCase;

    /**
     * {@code WebMvcSliceConfig} stubea {@code currentEmployeeIdOrNull()}, pero este
     * controller lee {@code currentEmployeeId()} y resuelve la sede accesible. Sin
     * estos dos stubs Mockito devolveria {@code 0L} para el {@code Long} —no null—
     * y el command llegaria firmado por un empleado inexistente y con la sede en
     * cero, en verde.
     */
    @BeforeEach
    void resolverElContextoDeAutoria() {
        when(authz.currentEmployeeId()).thenReturn(EMPLEADO_DEL_CONTEXTO);
        when(authz.resolveAccessibleBranch(any())).thenReturn(SEDE_RESUELTA);
    }

    private static ProductChargeOpenAccountDto cargoDto() {
        return ProductChargeOpenAccountDto.from(cargo());
    }

    private CreateProductChargeOpenAccountCommand comandoCapturado() {
        ArgumentCaptor<CreateProductChargeOpenAccountCommand> captor = ArgumentCaptor
                .forClass(CreateProductChargeOpenAccountCommand.class);
        verify(createUseCase).execute(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("POST /product-charge-open-accounts")
    class Crear {

        @Test
        @DisplayName("responde 201 con el recurso creado y sus sumarios anidados")
        void responde_201_con_el_recurso_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cargoDto());

            mockMvc.perform(post("/product-charge-open-accounts")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"animalId":1,"productId":2,"quantity":1,"openAccountId":50}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(CHARGE_ID))
                    .andExpect(jsonPath("$.animal.id").value(ANIMAL.id()))
                    .andExpect(jsonPath("$.animal.name").value("Firulais"))
                    .andExpect(jsonPath("$.animal.code").value("A-001"))
                    .andExpect(jsonPath("$.product.id").value(PRODUCTO.id()))
                    .andExpect(jsonPath("$.product.name").value("Alimento"))
                    .andExpect(jsonPath("$.product.code").value("P-001"))
                    .andExpect(jsonPath("$.product.salePrice").value(11900))
                    .andExpect(jsonPath("$.unitPrice").value(11900))
                    .andExpect(jsonPath("$.quantity").value(1))
                    .andExpect(jsonPath("$.hasTax").value(true))
                    .andExpect(jsonPath("$.taxPercentage").value(19.00))
                    .andExpect(jsonPath("$.taxName").value("IVA 19%"))
                    .andExpect(jsonPath("$.baseAmount").value(10000.00))
                    .andExpect(jsonPath("$.taxAmount").value(1900.00))
                    .andExpect(jsonPath("$.totalAmount").value(11900.00))
                    .andExpect(jsonPath("$.openAccount.id").value(OPEN_ACCOUNT_ID))
                    .andExpect(jsonPath("$.openAccount.companyId").value(COMPANY_ID))
                    .andExpect(jsonPath("$.createdBy.name").value("Ana Ruiz"))
                    .andExpect(jsonPath("$.createdDate").value("2026-01-15T10:30:00"))
                    .andExpect(jsonPath("$.enabled").value(true))
                    .andExpect(jsonPath("$.voided").value(false));
        }

        @Test
        @DisplayName("sella empresa, empleado y sede desde el contexto, no desde el cuerpo")
        void sella_empresa_empleado_y_sede_desde_el_contexto() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cargoDto());

            mockMvc.perform(post("/product-charge-open-accounts")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"animalId":1,"productId":2,"quantity":4,"openAccountId":50,
                             "branchId":3,"clientRequestId":"req-1","expectedVersion":7}
                            """)).andExpect(status().isCreated());

            // Ni companyId ni employeeId viajan crudos desde el cliente, y la sede pasa
            // por el resolutor de alcance: un verify(...any()) no distinguiria eso.
            assertThat(comandoCapturado())
                    .isEqualTo(new CreateProductChargeOpenAccountCommand(1L, 2L, 4, OPEN_ACCOUNT_ID,
                            COMPANY_ID, EMPLEADO_DEL_CONTEXTO, SEDE_RESUELTA, "req-1", 7L));
        }

        @Test
        @DisplayName("la sede que llega al command es la resuelta, no la que pidio el cliente")
        void la_sede_del_command_es_la_resuelta() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cargoDto());

            mockMvc.perform(post("/product-charge-open-accounts")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"animalId":1,"productId":2,"openAccountId":50,"branchId":3}
                            """)).andExpect(status().isCreated());

            assertThat(comandoCapturado().branchId()).isEqualTo(SEDE_RESUELTA)
                    .isNotEqualTo(SEDE_PEDIDA);
        }

        @Test
        @DisplayName("sin quantity el controller manda una unidad")
        void sin_quantity_el_controller_manda_una_unidad() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cargoDto());

            mockMvc.perform(post("/product-charge-open-accounts")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"animalId":1,"productId":2,"openAccountId":50}
                            """)).andExpect(status().isCreated());

            // quantity es Integer y opcional por compatibilidad: el default vive en el
            // controller, no en el record ni en el service.
            assertThat(comandoCapturado().quantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("sin animalId responde 400 con la forma de error del handler real")
        void sin_animal_id_responde_400() throws Exception {
            mockMvc.perform(post("/product-charge-open-accounts")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"productId":2,"openAccountId":50}
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[0].field").value("animalId"));

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("sin openAccountId responde 400 y no llega al caso de uso")
        void sin_open_account_id_responde_400() throws Exception {
            mockMvc.perform(post("/product-charge-open-accounts")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"animalId":1,"productId":2}
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("openAccountId"));

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("una cantidad de cero viola el minimo y responde 400")
        void una_cantidad_de_cero_responde_400() throws Exception {
            mockMvc.perform(post("/product-charge-open-accounts")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"animalId":1,"productId":2,"quantity":0,"openAccountId":50}
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("quantity"));

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("un clientRequestId de mas de 36 caracteres responde 400")
        void un_client_request_id_demasiado_largo_responde_400() throws Exception {
            mockMvc.perform(post("/product-charge-open-accounts")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"animalId":1,"productId":2,"openAccountId":50,
                             "clientRequestId":"0123456789012345678901234567890123456789"}
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("clientRequestId"));

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("una invariante de dominio rota sale como 400, no como 500")
        void una_invariante_de_dominio_rota_sale_como_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Animal not found: 1"));

            mockMvc.perform(post("/product-charge-open-accounts")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"animalId":1,"productId":2,"openAccountId":50}
                            """)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /product-charge-open-accounts")
    class Listar {

        @Test
        @DisplayName("arma la pagina con la empresa del contexto y el tamano por defecto")
        void arma_la_pagina_con_la_empresa_del_contexto() throws Exception {
            when(listUseCase.listAll(COMPANY_ID, 0, 20))
                    .thenReturn(new PageResult<>(List.of(cargoDto()), 0, 20, 1L, 1));

            mockMvc.perform(get("/product-charge-open-accounts")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(CHARGE_ID))
                    .andExpect(jsonPath("$.content[0].product.code").value("P-001"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.pageSize").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));

            verify(listUseCase).listAll(COMPANY_ID, 0, 20);
        }

        @Test
        @DisplayName("acepta page y pageSize por query param")
        void acepta_page_y_page_size_por_query_param() throws Exception {
            when(listUseCase.listAll(COMPANY_ID, 2, 10))
                    .thenReturn(new PageResult<>(List.of(), 2, 10, 0L, 0));

            mockMvc.perform(
                    get("/product-charge-open-accounts").param("page", "2").param("pageSize", "10"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());

            verify(listUseCase).listAll(COMPANY_ID, 2, 10);
        }
    }

    @Nested
    @DisplayName("GET /product-charge-open-accounts/by-open-account/{openAccountId}")
    class ListarPorCuenta {

        @Test
        @DisplayName("devuelve los cargos de la cuenta acotados a la empresa del contexto")
        void devuelve_los_cargos_de_la_cuenta() throws Exception {
            when(listByOpenAccountUseCase.listByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of(cargoDto()));

            mockMvc.perform(get("/product-charge-open-accounts/by-open-account/50"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(CHARGE_ID))
                    .andExpect(jsonPath("$[0].totalAmount").value(11900.00));

            verify(listByOpenAccountUseCase).listByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("una cuenta sin cargos devuelve 200 con lista vacia, no 404")
        void una_cuenta_sin_cargos_devuelve_lista_vacia() throws Exception {
            when(listByOpenAccountUseCase.listByOpenAccount(77L, COMPANY_ID)).thenReturn(List.of());

            mockMvc.perform(get("/product-charge-open-accounts/by-open-account/77"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("un openAccountId no numerico responde 400, no 500")
        void un_open_account_id_no_numerico_responde_400() throws Exception {
            mockMvc.perform(get("/product-charge-open-accounts/by-open-account/abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(listByOpenAccountUseCase);
        }
    }

    @Nested
    @DisplayName("PATCH /product-charge-open-accounts/{id}/void")
    class Anular {

        @Test
        @DisplayName("responde 200 y sella el command con la razon y el empleado del contexto")
        void responde_200_y_sella_el_command() throws Exception {
            when(voidUseCase.execute(any()))
                    .thenReturn(ProductChargeOpenAccountDto.from(cargoAnulado()));

            mockMvc.perform(patch("/product-charge-open-accounts/100/void")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"reason":"Cobrado por error","expectedVersion":1}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.voided").value(true))
                    .andExpect(jsonPath("$.voidedBy.name").value("Luis Paz"))
                    .andExpect(jsonPath("$.voidReason").value("Cobrado por error"))
                    .andExpect(jsonPath("$.enabled").value(true));

            verify(voidUseCase).execute(new VoidProductChargeOpenAccountCommand(CHARGE_ID,
                    COMPANY_ID, EMPLEADO_DEL_CONTEXTO, "Cobrado por error", 1L));
        }

        @Test
        @DisplayName("sin expectedVersion el command viaja sin chequeo optimista")
        void sin_expected_version_el_command_viaja_sin_chequeo() throws Exception {
            when(voidUseCase.execute(any()))
                    .thenReturn(ProductChargeOpenAccountDto.from(cargoAnulado()));

            mockMvc.perform(patch("/product-charge-open-accounts/100/void")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"reason":"Cobrado por error"}
                            """)).andExpect(status().isOk());

            verify(voidUseCase).execute(new VoidProductChargeOpenAccountCommand(CHARGE_ID,
                    COMPANY_ID, EMPLEADO_DEL_CONTEXTO, "Cobrado por error", null));
        }

        @Test
        @DisplayName("con el motivo en blanco responde 400 y no llega al caso de uso")
        void con_el_motivo_en_blanco_responde_400() throws Exception {
            mockMvc.perform(patch("/product-charge-open-accounts/100/void")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"reason":"   "}
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[0].field").value("reason"));

            verifyNoInteractions(voidUseCase);
        }

        @Test
        @DisplayName("un cargo ya anulado responde 409 con el codigo estable")
        void un_cargo_ya_anulado_responde_409() throws Exception {
            when(voidUseCase.execute(any()))
                    .thenThrow(new ProductChargeOpenAccountAlreadyVoidedException(CHARGE_ID));

            mockMvc.perform(patch("/product-charge-open-accounts/100/void")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"reason":"Otra vez"}
                            """)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CHARGE_OPEN_ACCOUNT_ALREADY_VOIDED"));
        }

        @Test
        @DisplayName("un cargo inexistente responde 404, no 500")
        void un_cargo_inexistente_responde_404() throws Exception {
            when(voidUseCase.execute(any()))
                    .thenThrow(new ProductChargeOpenAccountNotFoundException(CHARGE_ID));

            mockMvc.perform(patch("/product-charge-open-accounts/100/void")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"reason":"No existe"}
                            """)).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PRODUCT_CHARGE_OPEN_ACCOUNT_NOT_FOUND"));
        }
    }
}
