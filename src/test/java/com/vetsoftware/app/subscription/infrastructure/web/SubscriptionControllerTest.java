package com.vetsoftware.app.subscription.infrastructure.web;

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

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscription.application.command.AddSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.command.CancelSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionItemQuantityCommand;
import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionStatusCommand;
import com.vetsoftware.app.subscription.application.command.CreateRequestedSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.RemoveSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemDto;
import com.vetsoftware.app.subscription.application.port.in.AddSubscriptionItemUseCase;
import com.vetsoftware.app.subscription.application.port.in.CancelSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.in.ChangeSubscriptionItemQuantityUseCase;
import com.vetsoftware.app.subscription.application.port.in.ChangeSubscriptionStatusUseCase;
import com.vetsoftware.app.subscription.application.port.in.CreateRequestedSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.in.FindCurrentSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.in.FindSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.in.ListSubscriptionAmendmentsUseCase;
import com.vetsoftware.app.subscription.application.port.in.ListSubscriptionItemsUseCase;
import com.vetsoftware.app.subscription.application.port.in.ListSubscriptionStatusHistoryUseCase;
import com.vetsoftware.app.subscription.application.port.in.ListSubscriptionsByCompanyUseCase;
import com.vetsoftware.app.subscription.application.port.in.RemoveSubscriptionItemUseCase;
import com.vetsoftware.app.subscription.domain.SubscriptionNotFoundException;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.testsupport.SubscriptionMother;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
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

/**
 * El contrato HTTP de un contrato.
 *
 * <p>
 * <b>No se dobla {@code Authz}.</b> Se usa el de {@link WebMvcSliceConfig},
 * cuyo {@code COMPANY_ID}, {@code EMPLOYEE_ID} y {@code SYSTEM_USER_ID} son
 * tres valores distintos a propósito: así la aserción enseña <em>de dónde</em>
 * salió cada campo del command en vez de dar por bueno un cero que Mockito
 * devolvería solo. Esto es lo que prueba las dos afirmaciones del javadoc del
 * controller — que ningún cuerpo lleva {@code companyId} y que ninguno lleva
 * quién firma.
 */
@WebMvcTest(SubscriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SubscriptionController — contrato HTTP")
class SubscriptionControllerTest {

    private static final Long CONTRATO = 55L;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CreateRequestedSubscriptionUseCase createUseCase;
    @MockitoBean
    private FindSubscriptionUseCase findUseCase;
    @MockitoBean
    private FindCurrentSubscriptionUseCase findCurrentUseCase;
    @MockitoBean
    private ListSubscriptionsByCompanyUseCase listByCompanyUseCase;
    @MockitoBean
    private ListSubscriptionItemsUseCase listItemsUseCase;
    @MockitoBean
    private ListSubscriptionAmendmentsUseCase listAmendmentsUseCase;
    @MockitoBean
    private ListSubscriptionStatusHistoryUseCase listHistoryUseCase;
    @MockitoBean
    private AddSubscriptionItemUseCase addItemUseCase;
    @MockitoBean
    private RemoveSubscriptionItemUseCase removeItemUseCase;
    @MockitoBean
    private ChangeSubscriptionItemQuantityUseCase changeQuantityUseCase;
    @MockitoBean
    private ChangeSubscriptionStatusUseCase changeStatusUseCase;
    @MockitoBean
    private CancelSubscriptionUseCase cancelUseCase;

    @Nested
    @DisplayName("Lecturas")
    class Lecturas {

        @Test
        @DisplayName("lista el historial contractual de la empresa autenticada")
        void lista_el_historial_contractual_de_la_empresa_autenticada() throws Exception {
            when(listByCompanyUseCase.listByCompany(WebMvcSliceConfig.COMPANY_ID, 2, 9))
                    .thenReturn(PageResult.empty(2, 9));

            mockMvc.perform(get("/subscriptions").param("page", "2").param("pageSize", "9"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.pageSize").value(9));

            verify(listByCompanyUseCase).listByCompany(WebMvcSliceConfig.COMPANY_ID, 2, 9);
        }

        @Test
        @DisplayName("el listado sin parámetros pagina desde el 0 con tamaño 20")
        void listado_sin_parametros() throws Exception {
            when(listByCompanyUseCase.listByCompany(WebMvcSliceConfig.COMPANY_ID, 0, 20))
                    .thenReturn(PageResult.of(List.of(SubscriptionMother.dto()), 0, 20, 1L));

            mockMvc.perform(get("/subscriptions")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @DisplayName("el contrato vigente de mi empresa sale con todos sus campos")
        void contrato_vigente_de_mi_empresa() throws Exception {
            when(findCurrentUseCase.findCurrent(WebMvcSliceConfig.COMPANY_ID))
                    .thenReturn(SubscriptionMother.dto());

            mockMvc.perform(get("/subscriptions/current")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.subscriptionNumber").value("SUS-2026-00184"))
                    .andExpect(jsonPath("$.companyId").value(42))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.current").value(true))
                    .andExpect(jsonPath("$.billingCycle").value("MONTHLY"))
                    .andExpect(jsonPath("$.startDate").value("2026-01-01"))
                    .andExpect(jsonPath("$.currentPeriodEnd").value("2026-01-31"))
                    .andExpect(jsonPath("$.graceDays").value(5))
                    .andExpect(jsonPath("$.autoRenew").value(true))
                    .andExpect(jsonPath("$.cancelRequestedAt").isEmpty())
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("un contrato que no existe responde 404, no 500")
        void contrato_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(CONTRATO, WebMvcSliceConfig.COMPANY_ID))
                    .thenThrow(new SubscriptionNotFoundException(CONTRATO));

            mockMvc.perform(get("/subscriptions/{id}", CONTRATO)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("el expediente completo se pide sin fecha; con onDate se pide un día")
        void expediente_completo_y_por_dia() throws Exception {
            when(listItemsUseCase.listAll(CONTRATO, WebMvcSliceConfig.COMPANY_ID, null, 0, 20))
                    .thenReturn(PageResult.of(List.of(SubscriptionMother.itemDto()), 0, 20, 1L));
            when(listItemsUseCase.listAll(CONTRATO, WebMvcSliceConfig.COMPANY_ID,
                    LocalDate.of(2026, 3, 15), 0, 20)).thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/subscriptions/{id}/items", CONTRATO)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].itemCode").value("EXTRA_USER"))
                    .andExpect(jsonPath("$.content[0].capacityUnit").value("USER"))
                    .andExpect(jsonPath("$.content[0].includedQuantity").value(2))
                    .andExpect(jsonPath("$.content[0].quantity").value(5))
                    .andExpect(jsonPath("$.content[0].billableQuantity").value(3))
                    .andExpect(jsonPath("$.content[0].effectiveFrom").value("2026-01-01"))
                    .andExpect(jsonPath("$.content[0].effectiveTo").isEmpty())
                    .andExpect(jsonPath("$.content[0].origin").value("ADDON"));

            mockMvc.perform(
                    get("/subscriptions/{id}/items", CONTRATO).param("onDate", "2026-03-15"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("los otrosíes salen con quién los pidió y sus dos importes")
        void otrosies_con_su_firma() throws Exception {
            when(listAmendmentsUseCase.listAll(CONTRATO, WebMvcSliceConfig.COMPANY_ID, 0, 20))
                    .thenReturn(
                            PageResult.of(List.of(SubscriptionMother.amendmentDto()), 0, 20, 1L));

            mockMvc.perform(get("/subscriptions/{id}/amendments", CONTRATO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].amendmentNumber").value("AMD-2026-00001"))
                    .andExpect(jsonPath("$.content[0].amendmentType").value("ADD_ITEM"))
                    .andExpect(jsonPath("$.content[0].requestedByEmployeeId").value(4))
                    .andExpect(jsonPath("$.content[0].requestedBySystemUserId").isEmpty())
                    .andExpect(jsonPath("$.content[0].effectiveDate").value("2026-05-01"))
                    .andExpect(jsonPath("$.content[0].clientRequestId").value("req-1"));
        }

        @Test
        @DisplayName("la bitácora de estados sale con el de dónde, el a dónde y el actor")
        void bitacora_de_estados() throws Exception {
            when(listHistoryUseCase.listAll(CONTRATO, WebMvcSliceConfig.COMPANY_ID, 0, 20))
                    .thenReturn(PageResult.of(List.of(SubscriptionMother.statusChangeDto()), 0, 20,
                            1L));

            mockMvc.perform(get("/subscriptions/{id}/status-history", CONTRATO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].fromStatus").value("ACTIVE"))
                    .andExpect(jsonPath("$.content[0].toStatus").value("PAST_DUE"))
                    .andExpect(jsonPath("$.content[0].actor").value("cobranza"))
                    .andExpect(jsonPath("$.content[0].reason").value("Cuota vencida"));
        }
    }

    @Nested
    @DisplayName("El companyId no entra por el cuerpo")
    class ElCompanyIdSaleDelPrincipal {

        @Test
        @DisplayName("el alta toma la empresa del principal y nunca del JSON")
        void el_alta_toma_la_empresa_del_principal() throws Exception {
            when(createUseCase.execute(any())).thenReturn(SubscriptionMother.dto());

            // El cuerpo lleva un companyId de otra empresa a proposito: el request no
            // declara ese campo, asi que el binder lo ignora y el command tiene que
            // salir con el del principal. Si algun dia alguien añade companyId al
            // record, este caso lo caza.
            mockMvc.perform(
                    post("/subscriptions").contentType(MediaType.APPLICATION_JSON).content("""
                            {"companyId": 999, "priceListId": 3, "billingCycle": "MONTHLY",
                             "status": "ACTIVE", "startDate": "2026-01-01",
                             "currentPeriodStart": "2026-01-01", "currentPeriodEnd": "2026-01-31",
                             "graceDays": 5, "autoRenew": true,
                             "items": [{"catalogItemId": 100, "quantity": 2,
                                        "effectiveFrom": "2026-01-01"}]}
                            """)).andExpect(status().isCreated());

            ArgumentCaptor<CreateRequestedSubscriptionCommand> comando = ArgumentCaptor
                    .forClass(CreateRequestedSubscriptionCommand.class);
            verify(createUseCase).execute(comando.capture());
            assertThat(comando.getValue().companyId()).isEqualTo(WebMvcSliceConfig.COMPANY_ID);
            assertThat(comando.getValue().priceListId()).isEqualTo(3L);
            assertThat(comando.getValue().status()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(comando.getValue().items()).singleElement().satisfies(linea -> {
                assertThat(linea.catalogItemId()).isEqualTo(100L);
                assertThat(linea.quantity()).isEqualTo(2);
                assertThat(linea.effectiveFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
                assertThat(linea.effectiveTo()).isNull();
            });
        }

        @Test
        @DisplayName("un alta sin líneas llega con la lista vacía, no con null")
        void alta_sin_lineas() throws Exception {
            when(createUseCase.execute(any())).thenReturn(SubscriptionMother.dto());

            mockMvc.perform(
                    post("/subscriptions").contentType(MediaType.APPLICATION_JSON).content("""
                            {"priceListId": 3, "billingCycle": "MONTHLY", "status": "ACTIVE",
                             "startDate": "2026-01-01", "currentPeriodStart": "2026-01-01",
                             "currentPeriodEnd": "2026-01-31"}
                            """)).andExpect(status().isCreated());

            ArgumentCaptor<CreateRequestedSubscriptionCommand> comando = ArgumentCaptor
                    .forClass(CreateRequestedSubscriptionCommand.class);
            verify(createUseCase).execute(comando.capture());
            assertThat(comando.getValue().items()).isEmpty();
        }

        @Test
        @DisplayName("un alta sin tarifa se rechaza con 400 y no llega al caso de uso")
        void alta_sin_tarifa() throws Exception {
            mockMvc.perform(
                    post("/subscriptions").contentType(MediaType.APPLICATION_JSON).content("""
                            {"billingCycle": "MONTHLY", "status": "ACTIVE",
                             "startDate": "2026-01-01", "currentPeriodStart": "2026-01-01",
                             "currentPeriodEnd": "2026-01-31"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }
    }

    @Nested
    @DisplayName("Quién firma la enmienda sale del principal, no del cuerpo")
    class QuienFirmaSaleDelPrincipal {

        @Test
        @DisplayName("el alta de línea sella los dos posibles firmantes desde el contexto")
        void el_alta_de_linea_sella_los_firmantes() throws Exception {
            when(addItemUseCase.execute(any())).thenReturn(SubscriptionMother.itemDto());

            mockMvc.perform(post("/subscriptions/{id}/items", CONTRATO)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"clientRequestId": "req-9", "effectiveDate": "2026-05-01",
                             "reason": "Amplia usuarios",
                             "requestedByEmployeeId": 777, "requestedBySystemUserId": 888,
                             "line": {"catalogItemId": 100, "itemCode": "EXTRA_USER",
                                      "itemName": "Usuario adicional", "itemType": "CAPACITY",
                                      "capacityUnit": "USER", "includedQuantity": 2,
                                      "taxTreatment": "TAXED", "quantity": 5,
                                      "unitAmount": "179000.00", "taxRate": "19.00",
                                      "effectiveFrom": "2026-05-01"}}
                            """)).andExpect(status().isCreated());

            // Los 777 y 888 del cuerpo no existen en el record y no llegan a ningun
            // sitio: una columna de firma que escribe el propio firmante no prueba
            // nada. Los dos valores del command salen del principal, y son distintos
            // entre si para que se vea cual es cual.
            ArgumentCaptor<AddSubscriptionItemCommand> comando = ArgumentCaptor
                    .forClass(AddSubscriptionItemCommand.class);
            verify(addItemUseCase).execute(comando.capture());
            assertThat(comando.getValue().requestedByEmployeeId())
                    .isEqualTo(WebMvcSliceConfig.EMPLOYEE_ID);
            assertThat(comando.getValue().requestedBySystemUserId())
                    .isEqualTo(WebMvcSliceConfig.SYSTEM_USER_ID);
            assertThat(comando.getValue().companyId()).isEqualTo(WebMvcSliceConfig.COMPANY_ID);
            assertThat(comando.getValue().id()).isEqualTo(CONTRATO);
            assertThat(comando.getValue().clientRequestId()).isEqualTo("req-9");
            assertThat(comando.getValue().line().unitAmount()).isEqualByComparingTo("179000.00");
        }

        @Test
        @DisplayName("un alta de línea sin llave antiduplicados se rechaza con 400")
        void alta_de_linea_sin_llave() throws Exception {
            mockMvc.perform(post("/subscriptions/{id}/items", CONTRATO)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"effectiveDate": "2026-05-01",
                             "line": {"catalogItemId": 100, "itemCode": "CORE",
                                      "itemName": "Nucleo", "itemType": "MODULE",
                                      "includedQuantity": 0, "taxTreatment": "TAXED",
                                      "quantity": 1, "unitAmount": "1.00", "taxRate": "0.00"}}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(addItemUseCase);
        }

        @Test
        @DisplayName("un alta de línea sin línea se rechaza con 400")
        void alta_de_linea_sin_linea() throws Exception {
            mockMvc.perform(post("/subscriptions/{id}/items", CONTRATO)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"clientRequestId": "req-9", "effectiveDate": "2026-05-01"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(addItemUseCase);
        }
    }

    @Nested
    @DisplayName("Los tres puertos reabiertos al tenant")
    class PuertosDelTenant {

        @Test
        @DisplayName("dar de baja una línea es PATCH y no DELETE: no se borra, se fecha")
        void baja_de_linea_es_patch() throws Exception {
            SubscriptionItemDto cerrada = SubscriptionItemDto.from(SubscriptionMother
                    .lineaEntre(SubscriptionMother.ENERO_1, SubscriptionMother.JUNIO_30));
            when(removeItemUseCase.execute(any())).thenReturn(cerrada);

            mockMvc.perform(patch("/subscriptions/{id}/items/remove", CONTRATO)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"subscriptionItemId": 12, "clientRequestId": "req-baja",
                             "effectiveDate": "2026-06-30", "reason": "Ya no lo usa"}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.effectiveTo").value("2026-06-30"))
                    .andExpect(jsonPath("$.enabled").value(true));

            // La respuesta sigue trayendo enabled=true: dar de baja escribe la fecha de
            // fin y no desactiva la fila. Si algun dia se cambiara a un borrado logico,
            // desapareceria la prueba de que ese cliente tuvo ese modulo.
            ArgumentCaptor<RemoveSubscriptionItemCommand> comando = ArgumentCaptor
                    .forClass(RemoveSubscriptionItemCommand.class);
            verify(removeItemUseCase).execute(comando.capture());
            assertThat(comando.getValue().companyId()).isEqualTo(WebMvcSliceConfig.COMPANY_ID);
            assertThat(comando.getValue().subscriptionItemId()).isEqualTo(12L);
            assertThat(comando.getValue().requestedByEmployeeId())
                    .isEqualTo(WebMvcSliceConfig.EMPLOYEE_ID);
            // Los importes ya no viajan en el command: los calcula el caso de uso.
        }

        @Test
        @DisplayName("el cambio de cantidad devuelve 201: nace una línea sucesora")
        void cambio_de_cantidad_devuelve_201() throws Exception {
            when(changeQuantityUseCase.execute(any())).thenReturn(SubscriptionMother.itemDto());

            // 201 y no 200 porque el recurso que se devuelve es nuevo: la original
            // queda cerrada, no modificada.
            mockMvc.perform(post("/subscriptions/{id}/items/quantity", CONTRATO)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"subscriptionItemId": 12, "newQuantity": 8,
                             "clientRequestId": "req-cantidad",
                             "effectiveDate": "2026-06-30"}
                            """)).andExpect(status().isCreated());

            ArgumentCaptor<ChangeSubscriptionItemQuantityCommand> comando = ArgumentCaptor
                    .forClass(ChangeSubscriptionItemQuantityCommand.class);
            verify(changeQuantityUseCase).execute(comando.capture());
            assertThat(comando.getValue().newQuantity()).isEqualTo(8);
            assertThat(comando.getValue().companyId()).isEqualTo(WebMvcSliceConfig.COMPANY_ID);
            assertThat(comando.getValue().requestedBySystemUserId())
                    .isEqualTo(WebMvcSliceConfig.SYSTEM_USER_ID);
        }

        @Test
        @DisplayName("un cambio a cero unidades se rechaza con 400: eso es una baja")
        void cambio_a_cero_unidades() throws Exception {
            mockMvc.perform(post("/subscriptions/{id}/items/quantity", CONTRATO)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"subscriptionItemId": 12, "newQuantity": 0,
                             "clientRequestId": "req-cantidad",
                             "effectiveDate": "2026-06-30"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(changeQuantityUseCase);
        }

        @Test
        @DisplayName("pedir la baja no cambia el estado: sigue vigente hasta la fecha efectiva")
        void pedir_la_baja_no_cambia_el_estado() throws Exception {
            when(cancelUseCase.execute(any())).thenReturn(SubscriptionMother.dto());

            mockMvc.perform(patch("/subscriptions/{id}/cancel", CONTRATO)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"requestedAt": "2026-01-10T09:30:00", "effectiveDate": "2026-01-30",
                             "reason": "Se pasa a la competencia",
                             "clientRequestId": "req-baja"}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.current").value(true));

            ArgumentCaptor<CancelSubscriptionCommand> comando = ArgumentCaptor
                    .forClass(CancelSubscriptionCommand.class);
            verify(cancelUseCase).execute(comando.capture());
            assertThat(comando.getValue().companyId()).isEqualTo(WebMvcSliceConfig.COMPANY_ID);
            assertThat(comando.getValue().effectiveDate()).isEqualTo(LocalDate.of(2026, 1, 30));
            assertThat(comando.getValue().requestedByEmployeeId())
                    .isEqualTo(WebMvcSliceConfig.EMPLOYEE_ID);
        }

        @Test
        @DisplayName("una baja sin fecha efectiva se rechaza con 400")
        void baja_sin_fecha_efectiva() throws Exception {
            mockMvc.perform(patch("/subscriptions/{id}/cancel", CONTRATO)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"requestedAt": "2026-01-10T09:30:00",
                             "clientRequestId": "req-baja"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(cancelUseCase);
        }
    }

    @Nested
    @DisplayName("La palanca de cobro")
    class PalancaDeCobro {

        @Test
        @DisplayName("el cambio de estado pasa el estado, el motivo y el actor tal cual")
        void cambio_de_estado() throws Exception {
            when(changeStatusUseCase.execute(any())).thenReturn(SubscriptionMother.dto());

            mockMvc.perform(patch("/subscriptions/{id}/status", CONTRATO)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"status": "READ_ONLY", "reason": "Mora de 45 dias",
                             "actor": "cobranza"}
                            """)).andExpect(status().isOk());

            ArgumentCaptor<ChangeSubscriptionStatusCommand> comando = ArgumentCaptor
                    .forClass(ChangeSubscriptionStatusCommand.class);
            verify(changeStatusUseCase).execute(comando.capture());
            assertThat(comando.getValue().status()).isEqualTo(SubscriptionStatus.READ_ONLY);
            assertThat(comando.getValue().reason()).isEqualTo("Mora de 45 dias");
            assertThat(comando.getValue().actor()).isEqualTo("cobranza");
            assertThat(comando.getValue().companyId()).isEqualTo(WebMvcSliceConfig.COMPANY_ID);
            assertThat(comando.getValue().id()).isEqualTo(CONTRATO);
        }

        @Test
        @DisplayName("un estado que no existe se rechaza con 400")
        void estado_inexistente() throws Exception {
            mockMvc.perform(patch("/subscriptions/{id}/status", CONTRATO)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\": \"SUSPENDIDO\"}")).andExpect(status().isBadRequest());

            verifyNoInteractions(changeStatusUseCase);
        }

        @Test
        @DisplayName("un cambio de estado sin estado se rechaza con 400")
        void cambio_de_estado_sin_estado() throws Exception {
            mockMvc.perform(patch("/subscriptions/{id}/status", CONTRATO)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reason\": \"sin estado\"}")).andExpect(status().isBadRequest());

            verifyNoInteractions(changeStatusUseCase);
        }
    }
}
