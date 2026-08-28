package com.vetsoftware.app.accountingperiod.infrastructure.web;

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

import com.vetsoftware.app.accountingperiod.application.command.LockAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.command.OpenAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.command.ReopenAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.command.SoftCloseAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import com.vetsoftware.app.accountingperiod.application.port.in.FindAccountingPeriodUseCase;
import com.vetsoftware.app.accountingperiod.application.port.in.ListAccountingPeriodsUseCase;
import com.vetsoftware.app.accountingperiod.application.port.in.LockAccountingPeriodUseCase;
import com.vetsoftware.app.accountingperiod.application.port.in.OpenAccountingPeriodUseCase;
import com.vetsoftware.app.accountingperiod.application.port.in.ReopenAccountingPeriodUseCase;
import com.vetsoftware.app.accountingperiod.application.port.in.ResolvePostingPeriodUseCase;
import com.vetsoftware.app.accountingperiod.application.port.in.SoftCloseAccountingPeriodUseCase;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodStatus;
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

/**
 * Rodaja web del calendario contable. <b>Es la unica de la feature porque no
 * hay controller de tenant</b>: el calendario es de la plataforma.
 *
 * <p>
 * Tres cosas congela esta clase y no las ve ningun test de servicio:
 *
 * <ul>
 * <li><b>Que la firma NO viaja en el cuerpo.</b> Ni el cierre ni la reapertura
 * aceptan un {@code systemUserId}: lo pone el controller desde el principal. Si
 * alguien lo anadiera al request, cerrar el mes en nombre de otro seria una
 * peticion bien formada — y la firma es justo el dato por el que un auditor
 * pregunta. Aqui se afirma sobre el command capturado, que es donde se ve.</li>
 * <li><b>Que el mes mal escrito muere en el binder.</b> El {@code @Valid} del
 * {@code @RequestBody} es lo unico que dispara el validador; sin el, el
 * {@code @Pattern} del DTO esta escrito y no se evalua nunca (#135).</li>
 * <li><b>Que {@code /posting-period} no cae en el mapeo de {@code /{id}}.</b>
 * Son dos rutas que compiten y la resolucion la decide Spring, no el orden en
 * que estan escritas: si algun dia cambiara, la resolucion del periodo de
 * imputacion contestaria un 400 de conversion de tipo.</li>
 * </ul>
 *
 * <p>
 * <b>{@code Authz} NO se sustituye aqui a proposito.</b> El doble de
 * {@link WebMvcSliceConfig} ya devuelve
 * {@link WebMvcSliceConfig#SYSTEM_USER_ID} en {@code currentSystemUserId()};
 * declarar un {@code @MockitoBean} propio lo reemplazaria por un mock sin
 * stubear, {@code currentSystemUserId()} devolveria 0L —no null, para un
 * {@code Long}— y los casos de la firma pasarian en VERDE con un usuario de
 * sistema inexistente.
 */
@WebMvcTest(SystemAccountingPeriodController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemAccountingPeriodController — contrato HTTP de plataforma")
class SystemAccountingPeriodControllerTest {

    private static final Long ID = 8800L;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private OpenAccountingPeriodUseCase openUseCase;
    @MockitoBean
    private SoftCloseAccountingPeriodUseCase softCloseUseCase;
    @MockitoBean
    private LockAccountingPeriodUseCase lockUseCase;
    @MockitoBean
    private ReopenAccountingPeriodUseCase reopenUseCase;
    @MockitoBean
    private FindAccountingPeriodUseCase findUseCase;
    @MockitoBean
    private ListAccountingPeriodsUseCase listUseCase;
    @MockitoBean
    private ResolvePostingPeriodUseCase resolvePostingPeriodUseCase;

    @Nested
    @DisplayName("Apertura del mes")
    class AperturaDelMes {

        @Test
        @DisplayName("responde 201 y traslada la clave del mes al command")
        void responde_201_y_traslada_la_clave() throws Exception {
            when(openUseCase.execute(any())).thenReturn(unMesAbierto());

            mockMvc.perform(post("/system/accounting-periods")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"periodKey": "2026-03"}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(8800))
                    .andExpect(jsonPath("$.periodKey").value("2026-03"))
                    .andExpect(jsonPath("$.status").value("OPEN"))
                    .andExpect(jsonPath("$.closedAt").doesNotExist())
                    .andExpect(jsonPath("$.reopenedReason").doesNotExist());

            ArgumentCaptor<OpenAccountingPeriodCommand> command = ArgumentCaptor
                    .forClass(OpenAccountingPeriodCommand.class);
            verify(openUseCase).execute(command.capture());
            assertThat(command.getValue().periodKey()).isEqualTo("2026-03");
        }

        @Test
        @DisplayName("un mes 13 sale 400 nombrando el campo y NO llega al caso de uso")
        void un_mes_13_sale_400() throws Exception {
            // Sin el @Valid, el @Pattern del DTO no se evalua nunca y la clave llegaria
            // al dominio para volver como un IllegalArgumentException generico: un 400
            // con otra forma, que el front no sabe pintar bajo el input.
            mockMvc.perform(post("/system/accounting-periods")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"periodKey": "2026-13"}
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[0].field").value("periodKey"));

            verifyNoInteractions(openUseCase);
        }

        @Test
        @DisplayName("un cuerpo sin la clave del mes sale 400 y no escribe")
        void un_cuerpo_sin_clave_sale_400() throws Exception {
            mockMvc.perform(post("/system/accounting-periods")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"periodKey": "   "}
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("periodKey"));

            verifyNoInteractions(openUseCase);
        }
    }

    @Nested
    @DisplayName("Transiciones de estado")
    class Transiciones {

        @Test
        @DisplayName("cerrar responde 200 y firma con el usuario del principal, no con el cuerpo")
        void cerrar_firma_con_el_usuario_del_principal() throws Exception {
            when(softCloseUseCase.execute(any())).thenReturn(unMesCerrado());

            mockMvc.perform(patch("/system/accounting-periods/8800/soft-close"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SOFT_CLOSED"))
                    .andExpect(jsonPath("$.closedAt").value("2026-04-05T17:30:15"));

            ArgumentCaptor<SoftCloseAccountingPeriodCommand> command = ArgumentCaptor
                    .forClass(SoftCloseAccountingPeriodCommand.class);
            verify(softCloseUseCase).execute(command.capture());
            assertThat(command.getValue().id()).isEqualTo(ID);
            assertThat(command.getValue().systemUserId())
                    .isEqualTo(WebMvcSliceConfig.SYSTEM_USER_ID);
        }

        @Test
        @DisplayName("declarar responde 200 y tambien firma desde el principal")
        void declarar_firma_desde_el_principal() throws Exception {
            when(lockUseCase.execute(any())).thenReturn(unMesDeclarado());

            mockMvc.perform(patch("/system/accounting-periods/8800/lock"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("LOCKED"));

            ArgumentCaptor<LockAccountingPeriodCommand> command = ArgumentCaptor
                    .forClass(LockAccountingPeriodCommand.class);
            verify(lockUseCase).execute(command.capture());
            assertThat(command.getValue().id()).isEqualTo(ID);
            assertThat(command.getValue().systemUserId())
                    .isEqualTo(WebMvcSliceConfig.SYSTEM_USER_ID);
        }

        @Test
        @DisplayName("reabrir responde 200 y lleva el motivo del cuerpo con la firma del principal")
        void reabrir_lleva_el_motivo_y_la_firma() throws Exception {
            when(reopenUseCase.execute(any())).thenReturn(unMesReabierto());

            mockMvc.perform(patch("/system/accounting-periods/8800/reopen")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"reason": "Ajuste de la conciliacion 4471, recibida fuera de plazo"}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("OPEN"))
                    // El cierre previo sigue en la respuesta: es el registro de que el
                    // mes llego a estar cerrado.
                    .andExpect(jsonPath("$.closedAt").value("2026-04-05T17:30:15"))
                    .andExpect(jsonPath("$.reopenedAt").value("2026-04-09T09:12:45"));

            ArgumentCaptor<ReopenAccountingPeriodCommand> command = ArgumentCaptor
                    .forClass(ReopenAccountingPeriodCommand.class);
            verify(reopenUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.id()).isEqualTo(ID);
                assertThat(cmd.systemUserId()).isEqualTo(WebMvcSliceConfig.SYSTEM_USER_ID);
                assertThat(cmd.reason())
                        .isEqualTo("Ajuste de la conciliacion 4471, recibida fuera de plazo");
            });
        }

        @Test
        @DisplayName("reabrir sin motivo escrito sale 400 y NO llega al caso de uso")
        void reabrir_sin_motivo_sale_400() throws Exception {
            // Reabrir sin decir por que es justo lo que la ficha existe para impedir, y
            // la constraint de la base rechazaria la fila con un error que no nombra la
            // columna.
            mockMvc.perform(patch("/system/accounting-periods/8800/reopen")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"reason": "   "}
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("reason"));

            verifyNoInteractions(reopenUseCase);
        }

        @Test
        @DisplayName("un motivo de mas de 255 caracteres sale 400")
        void un_motivo_demasiado_largo_sale_400() throws Exception {
            mockMvc.perform(patch("/system/accounting-periods/8800/reopen")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"reason": "%s"}
                            """.formatted("M".repeat(256)))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("reason"));

            verifyNoInteractions(reopenUseCase);
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("la lectura por id devuelve el mes")
        void la_lectura_por_id_devuelve_el_mes() throws Exception {
            when(findUseCase.findById(ID)).thenReturn(unMesAbierto());

            mockMvc.perform(get("/system/accounting-periods/8800")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.periodKey").value("2026-03"))
                    .andExpect(jsonPath("$.createdDate").value("2026-03-01T00:05:00"));
        }

        @Test
        @DisplayName("el listado pagina con los valores por defecto")
        void el_listado_pagina_con_los_valores_por_defecto() throws Exception {
            when(listUseCase.listAll(anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(unMesAbierto()), 0, 20, 37L));

            mockMvc.perform(get("/system/accounting-periods")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].periodKey").value("2026-03"))
                    .andExpect(jsonPath("$.totalElements").value(37));

            verify(listUseCase).listAll(0, 20);
        }

        @Test
        @DisplayName("el periodo de imputacion tiene su propia ruta y no cae en el mapeo por id")
        void el_periodo_de_imputacion_tiene_su_propia_ruta() throws Exception {
            // Si /posting-period resolviera contra /{id}, Spring intentaria convertir
            // "posting-period" a Long y esta ruta contestaria un 400 de conversion.
            when(resolvePostingPeriodUseCase.resolve(LocalDate.of(2026, 3, 18)))
                    .thenReturn(unMesAbierto());

            mockMvc.perform(get("/system/accounting-periods/posting-period").param("occurredOn",
                    "2026-03-18")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.periodKey").value("2026-03"));

            verify(resolvePostingPeriodUseCase).resolve(LocalDate.of(2026, 3, 18));
            verifyNoInteractions(findUseCase);
        }

        @Test
        @DisplayName("el periodo de imputacion sin fecha sale 400 y no llega al caso de uso")
        void el_periodo_de_imputacion_sin_fecha_sale_400() throws Exception {
            mockMvc.perform(get("/system/accounting-periods/posting-period"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(resolvePostingPeriodUseCase);
        }
    }

    private static AccountingPeriodDto unMesAbierto() {
        return new AccountingPeriodDto(ID, "2026-03", AccountingPeriodStatus.OPEN, null, null, null,
                null, null, LocalDateTime.of(2026, 3, 1, 0, 5, 0));
    }

    private static AccountingPeriodDto unMesCerrado() {
        return new AccountingPeriodDto(ID, "2026-03", AccountingPeriodStatus.SOFT_CLOSED,
                LocalDateTime.of(2026, 4, 5, 17, 30, 15), 6L, null, null, null,
                LocalDateTime.of(2026, 3, 1, 0, 5, 0));
    }

    private static AccountingPeriodDto unMesDeclarado() {
        return new AccountingPeriodDto(ID, "2026-03", AccountingPeriodStatus.LOCKED,
                LocalDateTime.of(2026, 4, 5, 17, 30, 15), 6L, null, null, null,
                LocalDateTime.of(2026, 3, 1, 0, 5, 0));
    }

    private static AccountingPeriodDto unMesReabierto() {
        return new AccountingPeriodDto(ID, "2026-03", AccountingPeriodStatus.OPEN,
                LocalDateTime.of(2026, 4, 5, 17, 30, 15), 6L,
                LocalDateTime.of(2026, 4, 9, 9, 12, 45), 11L,
                "Ajuste de la conciliacion 4471, recibida fuera de plazo",
                LocalDateTime.of(2026, 3, 1, 0, 5, 0));
    }
}
