package com.vetsoftware.app.appointment.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.appointment.application.command.CancelAppointmentCommand;
import com.vetsoftware.app.appointment.application.command.ChangeAppointmentStatusCommand;
import com.vetsoftware.app.appointment.application.command.CreateAppointmentCommand;
import com.vetsoftware.app.appointment.application.command.RescheduleAppointmentCommand;
import com.vetsoftware.app.appointment.application.command.UpdateAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.dto.BranchSummaryDto;
import com.vetsoftware.app.appointment.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.appointment.application.dto.OwnerSummaryDto;
import com.vetsoftware.app.appointment.application.port.in.CancelAppointmentUseCase;
import com.vetsoftware.app.appointment.application.port.in.ChangeAppointmentStatusUseCase;
import com.vetsoftware.app.appointment.application.port.in.CreateAppointmentUseCase;
import com.vetsoftware.app.appointment.application.port.in.DeleteAppointmentUseCase;
import com.vetsoftware.app.appointment.application.port.in.GetAppointmentUseCase;
import com.vetsoftware.app.appointment.application.port.in.ListAppointmentsUseCase;
import com.vetsoftware.app.appointment.application.port.in.RescheduleAppointmentUseCase;
import com.vetsoftware.app.appointment.application.port.in.UpdateAppointmentUseCase;
import com.vetsoftware.app.appointment.application.query.ListAppointmentsQuery;
import com.vetsoftware.app.appointment.domain.AppointmentNotFoundException;
import com.vetsoftware.app.appointment.domain.AppointmentOverlapException;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import com.vetsoftware.app.appointment.domain.InvalidAppointmentTransitionException;
import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
 * Rodaja HTTP del controller de citas (BE-10): rutas, binding, validacion del
 * request, codigos de estado y forma del JSON. Lo que hay debajo son dobles.
 *
 * <p>
 * <b>Lo que aqui NO se prueba.</b> La autorizacion vive en el
 * {@code @PreAuthorize} de cada puerto de entrada y en {@code @WebMvcTest} los
 * puertos estan mockeados, asi que el gate no se ejercita: ni el de lectura ni
 * el que exige permiso propio para forzar un solape. Esa red la ponen ArchUnit
 * y la auditoria de autorizacion.
 *
 * <p>
 * <b>Del 409 solo se afirma lo estable</b> —el estado y el codigo
 * {@code APPOINTMENT_OVERLAP}—, no el texto del {@code detail} ni los ids que
 * lo acompañan: ese contenido esta acotandose por alcance de sede. La forma
 * general del {@code ProblemDetail} ya la cubre
 * {@code GlobalExceptionHandlerTest}.
 */
@WebMvcTest(AppointmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("AppointmentController — contrato HTTP")
class AppointmentControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long APPOINTMENT_ID = 55L;

    /**
     * Sede que resuelve {@code Authz}. Distinta a proposito del
     * {@code branchId: 77} que mandan los cuerpos: asi se ve que al command llega
     * la resuelta y no la que eligio el cliente.
     */
    private static final Long SEDE_RESUELTA = 31L;

    /** Alcance de sedes del caller; viaja en el command para acotar el 409. */
    private static final java.util.Set<Long> SEDES_VISIBLES = java.util.Set.of(31L, 32L);

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 8, 1, 9, 0);
    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 7, 20, 8, 15);

    /*
     * El "forceOverlap":false explicito de estos cuerpos ya NO es obligatorio: el
     * 
     * @JsonSetter(nulls = AS_EMPTY) de los tres request records hace que la
     * ausencia caiga a false. Se conserva porque estos son los cuerpos "completos"
     * y dejan el valor a la vista; la omision tiene sus propios casos, uno por
     * verbo, en omitir_force_overlap_*.
     */
    private static final String CREAR_JSON = """
            {"startAt":"2026-08-01T09:00:00","durationMinutes":45,"type":"CONSULTATION",
             "employeeId":4,"animalId":100,"ownerId":3,"notes":"Control anual","branchId":77,
             "forceOverlap":false}
            """;

    private static final String EDITAR_JSON = """
            {"startAt":"2026-08-01T09:00:00","durationMinutes":45,"type":"CONSULTATION",
             "employeeId":4,"animalId":100,"ownerId":3,"notes":"Control anual",
             "forceOverlap":false}
            """;

    private static final String REPROGRAMAR_JSON = """
            {"startAt":"2026-08-01T09:00:00","durationMinutes":60,"employeeId":5,
             "forceOverlap":false}
            """;

    /**
     * Motivo de cancelacion justo en el limite del {@code @Size(max = 300)} de
     * {@code CancelAppointmentRequest}. Se fija a proposito el valor exacto y no
     * uno holgado: el limite es inclusivo y un {@code <} donde va un {@code <=} es
     * el error clasico que ninguna otra asercion de este fichero atraparia.
     */
    private static final String MOTIVO_EN_EL_LIMITE = "M".repeat(300);

    /** Motivo pasado de largo: 350 caracteres, 50 por encima del maximo. */
    private static final String MOTIVO_PASADO_DE_LARGO = "M".repeat(350);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateAppointmentUseCase createUseCase;
    @MockitoBean
    private ListAppointmentsUseCase listUseCase;
    @MockitoBean
    private GetAppointmentUseCase getUseCase;
    @MockitoBean
    private UpdateAppointmentUseCase updateUseCase;
    @MockitoBean
    private RescheduleAppointmentUseCase rescheduleUseCase;
    @MockitoBean
    private ChangeAppointmentStatusUseCase changeStatusUseCase;
    @MockitoBean
    private CancelAppointmentUseCase cancelUseCase;
    @MockitoBean
    private DeleteAppointmentUseCase deleteUseCase;

    /**
     * Sin este stub {@code resolveAccessibleBranch} devolveria null y la cita se
     * agendaria en una sede sin resolver sin que el test lo notara.
     */
    @BeforeEach
    void resolverLaSedeDesdeElContexto() {
        when(authz.resolveAccessibleBranch(any())).thenReturn(SEDE_RESUELTA);
        // El alcance de sedes acota que citas cruzadas puede ver el caller en el 409.
        // Sin stub, el doble devolveria null y el command viajaria sin alcance.
        org.mockito.Mockito.lenient().when(authz.currentBranchIdsOrEmpty())
                .thenReturn(SEDES_VISIBLES); // solo lo usan los verbos de escritura
    }

    private static AppointmentDto cita() {
        return cita(45, List.of());
    }

    private static AppointmentDto cita(Integer durationMinutes, List<Long> solapes) {
        return new AppointmentDto(APPOINTMENT_ID, INICIO, durationMinutes,
                AppointmentType.CONSULTATION, AppointmentStatus.REQUESTED, "Control anual", null,
                new AnimalSummaryDto(100L, "Firulais", "A-001"),
                new OwnerSummaryDto(3L, "Ana Ruiz"), null, null, null,
                new EmployeeSummaryDto(4L, "Dra. Vet"),
                new BranchSummaryDto(SEDE_RESUELTA, "Principal", "PRINCIPAL"), 3L, true, CREADA,
                solapes);
    }

    /** Cuerpo del PATCH /cancel con el motivo dado. */
    private static String cancelarJson(String motivo) {
        return "{\"reason\":\"" + motivo + "\"}";
    }

    private static AppointmentOverlapException solapeCon(List<Long> ids) {
        return new AppointmentOverlapException(4L, "Dra. Vet", INICIO, INICIO.plusMinutes(45), ids,
                ids.size());
    }

    @Nested
    @DisplayName("POST /appointments")
    class Crear {

        @Test
        @DisplayName("responde 201 con el recurso creado y sus sumarios anidados")
        void responde_201_con_el_recurso_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cita());

            mockMvc.perform(post("/appointments").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(55))
                    .andExpect(jsonPath("$.startAt").value("2026-08-01T09:00:00"))
                    .andExpect(jsonPath("$.durationMinutes").value(45))
                    .andExpect(jsonPath("$.type").value("CONSULTATION"))
                    .andExpect(jsonPath("$.status").value("REQUESTED"))
                    .andExpect(jsonPath("$.animal.code").value("A-001"))
                    .andExpect(jsonPath("$.owner.name").value("Ana Ruiz"))
                    .andExpect(jsonPath("$.employee.name").value("Dra. Vet"))
                    .andExpect(jsonPath("$.branch.code").value("PRINCIPAL"))
                    .andExpect(jsonPath("$.overlappingAppointmentIds").isArray());
        }

        @Test
        @DisplayName("traduce el request al command con la company y la sede del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cita());

            mockMvc.perform(post("/appointments").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON)).andExpect(status().isCreated());

            // Ni el companyId ni la sede efectiva viajan crudos desde el cliente: el
            // primero sale del AuthContext y la segunda pasa por resolveAccessibleBranch
            // — el 77 que mando el request no llega al command, llega la sede resuelta.
            //
            // No se verifica sobre el doble de Authz: lo crea WebMvcSliceConfig como
            // @Bean, asi que es un singleton del contexto cacheado y sus invocaciones se
            // acumulan entre tests de la clase. La asercion sobre el command ya lo dice.
            verify(createUseCase).execute(new CreateAppointmentCommand(INICIO, 45,
                    AppointmentType.CONSULTATION, 4L, 100L, 3L, null, null, null, "Control anual",
                    SEDE_RESUELTA, COMPANY_ID, false, SEDES_VISIBLES));
        }

        @Test
        @DisplayName("sin durationMinutes el command viaja con null: la cita hereda el default de la empresa")
        void sin_duracion_el_command_viaja_con_null() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cita(null, List.of()));

            mockMvc.perform(
                    post("/appointments").contentType(MediaType.APPLICATION_JSON).content("""
                            {"startAt":"2026-08-01T09:00:00","type":"CONSULTATION",
                             "employeeId":4,"clientName":"Walk-in","forceOverlap":false}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.durationMinutes").doesNotExist());

            verify(createUseCase).execute(new CreateAppointmentCommand(INICIO, null,
                    AppointmentType.CONSULTATION, 4L, null, null, "Walk-in", null, null, null,
                    SEDE_RESUELTA, COMPANY_ID, false, SEDES_VISIBLES));
        }

        @Test
        @DisplayName("forceOverlap enviado como false no salta el bloqueo")
        void force_overlap_false_no_salta_el_bloqueo() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cita());

            mockMvc.perform(post("/appointments").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON)).andExpect(status().isCreated());

            // El flag NO puede llegar como true sin que nadie lo haya pedido: seria
            // saltarse el bloqueo por accidente.
            verify(createUseCase).execute(
                    org.mockito.ArgumentMatchers.argThat(command -> !command.forceOverlap()));
        }

        /**
         * <b>Regresion de compatibilidad de BE-17, ya cerrada.</b> {@code forceOverlap}
         * es {@code boolean} primitivo en los tres request records y este proyecto
         * corre Jackson 3 (tools.jackson 3.1.4), donde
         * {@code FAIL_ON_NULL_FOR_PRIMITIVES} viene ACTIVADO por defecto —al reves que
         * en Jackson 2—: omitir el campo respondia <b>400</b> con «Cannot map `null`
         * into type `boolean`» y tumbaba todo el trafico de los dos fronts anterior a
         * BE-17, que nunca envia el flag.
         *
         * <p>
         * Lo arregla el {@code @JsonSetter(nulls = Nulls.AS_EMPTY)} del record, que
         * devuelve la ausencia a {@code false} sin cambiar el tipo ni tocar la
         * configuracion global de Jackson. Este test es la red que lo sujeta: quitar la
         * anotacion lo pone rojo. Los verbos hermanos tienen el suyo
         * —{@code Actualizar} y {@code Reprogramar}—, porque los tres records llevan su
         * propia anotacion y se puede perder una sola.
         */
        @Test
        @DisplayName("omitir forceOverlap agenda con el flag en false, no responde 400")
        void omitir_force_overlap_agenda_con_el_flag_en_false() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cita());

            mockMvc.perform(
                    post("/appointments").contentType(MediaType.APPLICATION_JSON).content("""
                            {"startAt":"2026-08-01T09:00:00","durationMinutes":45,
                             "type":"CONSULTATION","employeeId":4,"clientName":"Walk-in"}
                            """)).andExpect(status().isCreated());

            verify(createUseCase).execute(
                    org.mockito.ArgumentMatchers.argThat(command -> !command.forceOverlap()));
        }

        @Test
        @DisplayName("con durationMinutes fuera de rango responde 400 y no llega al caso de uso")
        void duracion_fuera_de_rango_responde_400() throws Exception {
            mockMvc.perform(
                    post("/appointments").contentType(MediaType.APPLICATION_JSON).content("""
                            {"startAt":"2026-08-01T09:00:00","durationMinutes":721,
                             "type":"CONSULTATION","employeeId":4,"clientName":"Walk-in",
                             "forceOverlap":false}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("con durationMinutes cero responde 400")
        void duracion_cero_responde_400() throws Exception {
            mockMvc.perform(
                    post("/appointments").contentType(MediaType.APPLICATION_JSON).content("""
                            {"startAt":"2026-08-01T09:00:00","durationMinutes":0,
                             "type":"CONSULTATION","employeeId":4,"clientName":"Walk-in",
                             "forceOverlap":false}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("sin startAt responde 400")
        void sin_start_at_responde_400() throws Exception {
            mockMvc.perform(
                    post("/appointments").contentType(MediaType.APPLICATION_JSON).content("""
                            {"type":"CONSULTATION","employeeId":4,"clientName":"Walk-in",
                             "forceOverlap":false}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("sin employeeId responde 400")
        void sin_employee_id_responde_400() throws Exception {
            mockMvc.perform(
                    post("/appointments").contentType(MediaType.APPLICATION_JSON).content("""
                            {"startAt":"2026-08-01T09:00:00","type":"CONSULTATION",
                             "clientName":"Walk-in","forceOverlap":false}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("una invariante de dominio rota sale como 400, no como 500")
        void invariante_de_dominio_sale_como_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Employee not found: 4"));

            mockMvc.perform(post("/appointments").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON)).andExpect(status().isBadRequest());
        }
    }

    /**
     * El nucleo de BE-17 visto desde HTTP: el cruce ya no es un aviso en el cuerpo
     * de un 201, es un 409 con codigo propio que el front reconoce para ofrecer el
     * reintento forzado.
     */
    @Nested
    @DisplayName("409 APPOINTMENT_OVERLAP")
    class Solape {

        @Test
        @DisplayName("POST con cruce responde 409 con el codigo APPOINTMENT_OVERLAP")
        void post_con_cruce_responde_409() throws Exception {
            when(createUseCase.execute(any())).thenThrow(solapeCon(List.of(70L, 71L)));

            mockMvc.perform(post("/appointments").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("APPOINTMENT_OVERLAP"));
        }

        @Test
        @DisplayName("PUT con cruce responde 409 con el mismo codigo")
        void put_con_cruce_responde_409() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(solapeCon(List.of(70L)));

            mockMvc.perform(put("/appointments/55").contentType(MediaType.APPLICATION_JSON)
                    .content(EDITAR_JSON)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("APPOINTMENT_OVERLAP"));
        }

        @Test
        @DisplayName("PATCH /reschedule con cruce responde 409 con el mismo codigo")
        void reschedule_con_cruce_responde_409() throws Exception {
            when(rescheduleUseCase.execute(any())).thenThrow(solapeCon(List.of(70L)));

            mockMvc.perform(patch("/appointments/55/reschedule")
                    .contentType(MediaType.APPLICATION_JSON).content(REPROGRAMAR_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("APPOINTMENT_OVERLAP"));
        }

        @Test
        @DisplayName("con forceOverlap:true el flag llega al command y la cita se crea")
        void con_force_overlap_true_el_flag_llega_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cita(45, List.of(70L)));

            mockMvc.perform(
                    post("/appointments").contentType(MediaType.APPLICATION_JSON).content("""
                            {"startAt":"2026-08-01T09:00:00","durationMinutes":45,
                             "type":"CONSULTATION","employeeId":4,"clientName":"Walk-in",
                             "forceOverlap":true}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(55));

            verify(createUseCase).execute(
                    org.mockito.ArgumentMatchers.argThat(CreateAppointmentCommand::forceOverlap));
        }

        @Test
        @DisplayName("forceOverlap:true tambien viaja en el PUT y en el reschedule")
        void force_overlap_viaja_en_el_put_y_en_el_reschedule() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(cita());
            when(rescheduleUseCase.execute(any())).thenReturn(cita());

            mockMvc.perform(
                    put("/appointments/55").contentType(MediaType.APPLICATION_JSON).content("""
                            {"startAt":"2026-08-01T09:00:00","type":"CONSULTATION",
                             "employeeId":4,"clientName":"Walk-in","forceOverlap":true}
                            """)).andExpect(status().isOk());
            mockMvc.perform(patch("/appointments/55/reschedule")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"startAt":"2026-08-01T09:00:00","employeeId":5,
                             "forceOverlap":true}
                            """)).andExpect(status().isOk());

            verify(updateUseCase).execute(
                    org.mockito.ArgumentMatchers.argThat(UpdateAppointmentCommand::forceOverlap));
            verify(rescheduleUseCase).execute(org.mockito.ArgumentMatchers
                    .argThat(RescheduleAppointmentCommand::forceOverlap));
        }
    }

    @Nested
    @DisplayName("GET /appointments")
    class Listar {

        @Test
        @DisplayName("arma la query con la company y la sede del contexto")
        void arma_la_query_con_la_company_y_la_sede_del_contexto() throws Exception {
            when(listUseCase.execute(any())).thenReturn(List.of(cita()));

            mockMvc.perform(get("/appointments").param("from", "2026-08-01")
                    .param("to", "2026-08-31").param("employeeId", "4").param("status", "REQUESTED")
                    .param("branchId", "77")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(55));

            verify(listUseCase).execute(new ListAppointmentsQuery(COMPANY_ID,
                    LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), 4L,
                    AppointmentStatus.REQUESTED, SEDE_RESUELTA));
        }

        @Test
        @DisplayName("sin filtros la query viaja con todo a null salvo la company y la sede")
        void sin_filtros_la_query_viaja_con_nulls() throws Exception {
            when(listUseCase.execute(any())).thenReturn(List.of());

            mockMvc.perform(get("/appointments")).andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$").isEmpty());

            verify(listUseCase).execute(
                    new ListAppointmentsQuery(COMPANY_ID, null, null, null, null, SEDE_RESUELTA));
        }

        @Test
        @DisplayName("un estado inexistente en el enum responde 400")
        void un_estado_inexistente_responde_400() throws Exception {
            mockMvc.perform(get("/appointments").param("status", "INVENTADO"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(listUseCase);
        }
    }

    @Nested
    @DisplayName("GET /appointments/{id}")
    class Buscar {

        @Test
        @DisplayName("acota la busqueda a la company del contexto")
        void acota_la_busqueda_a_la_company_del_contexto() throws Exception {
            when(getUseCase.findById(APPOINTMENT_ID, COMPANY_ID)).thenReturn(cita());

            mockMvc.perform(get("/appointments/55")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(55))
                    .andExpect(jsonPath("$.durationMinutes").value(45));

            verify(getUseCase).findById(APPOINTMENT_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("inexistente responde 404, no 500")
        void inexistente_responde_404() throws Exception {
            when(getUseCase.findById(99L, COMPANY_ID))
                    .thenThrow(new AppointmentNotFoundException(99L));

            mockMvc.perform(get("/appointments/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /appointments/{id}")
    class Actualizar {

        @Test
        @DisplayName("responde 200 y arma el command con el id de la ruta")
        void responde_200_y_arma_el_command_con_el_id_de_la_ruta() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(cita());

            mockMvc.perform(put("/appointments/55").contentType(MediaType.APPLICATION_JSON)
                    .content(EDITAR_JSON)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(55));

            verify(updateUseCase).execute(new UpdateAppointmentCommand(APPOINTMENT_ID, INICIO, 45,
                    AppointmentType.CONSULTATION, 4L, 100L, 3L, null, null, null, "Control anual",
                    COMPANY_ID, false, SEDES_VISIBLES));
        }

        @Test
        @DisplayName("omitir durationMinutes manda null: el PUT devuelve la cita al default")
        void omitir_duracion_manda_null() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(cita(null, List.of()));

            mockMvc.perform(
                    put("/appointments/55").contentType(MediaType.APPLICATION_JSON).content("""
                            {"startAt":"2026-08-01T09:00:00","type":"CONSULTATION",
                             "employeeId":4,"clientName":"Walk-in","forceOverlap":false}
                            """)).andExpect(status().isOk());

            // El PUT es un reemplazo completo: el null que llega aqui significa "vuelve
            // al valor de la empresa", y esa semantica empieza en el binding del request.
            verify(updateUseCase).execute(
                    org.mockito.ArgumentMatchers.argThat(c -> c.durationMinutes() == null));
        }

        /**
         * Mismo arreglo que en el POST, anotacion distinta: la de
         * {@code UpdateAppointmentRequest}. Perder solo esta dejaria el PUT
         * respondiendo 400 a todo cliente anterior a BE-17 con el POST verde.
         */
        @Test
        @DisplayName("omitir forceOverlap edita con el flag en false, no responde 400")
        void omitir_force_overlap_edita_con_el_flag_en_false() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(cita());

            mockMvc.perform(
                    put("/appointments/55").contentType(MediaType.APPLICATION_JSON).content("""
                            {"startAt":"2026-08-01T09:00:00","durationMinutes":45,
                             "type":"CONSULTATION","employeeId":4,"clientName":"Walk-in"}
                            """)).andExpect(status().isOk());

            verify(updateUseCase).execute(
                    org.mockito.ArgumentMatchers.argThat(command -> !command.forceOverlap()));
        }

        @Test
        @DisplayName("con request invalido responde 400 y no llega al caso de uso")
        void request_invalido_responde_400() throws Exception {
            mockMvc.perform(
                    put("/appointments/55").contentType(MediaType.APPLICATION_JSON).content("""
                            {"type":"CONSULTATION","employeeId":4,"clientName":"Walk-in",
                             "forceOverlap":false}
                            """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("cita inexistente responde 404")
        void cita_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new AppointmentNotFoundException(APPOINTMENT_ID));

            mockMvc.perform(put("/appointments/55").contentType(MediaType.APPLICATION_JSON)
                    .content(EDITAR_JSON)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /appointments/{id}/reschedule")
    class Reprogramar {

        @Test
        @DisplayName("responde 200 y arma el command con la duracion enviada")
        void responde_200_y_arma_el_command() throws Exception {
            when(rescheduleUseCase.execute(any())).thenReturn(cita());

            mockMvc.perform(patch("/appointments/55/reschedule")
                    .contentType(MediaType.APPLICATION_JSON).content(REPROGRAMAR_JSON))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(55));

            verify(rescheduleUseCase).execute(new RescheduleAppointmentCommand(APPOINTMENT_ID,
                    INICIO, 60, 5L, COMPANY_ID, false, SEDES_VISIBLES));
        }

        @Test
        @DisplayName("omitir durationMinutes manda null: el PATCH conserva la duracion actual")
        void omitir_duracion_manda_null() throws Exception {
            when(rescheduleUseCase.execute(any())).thenReturn(cita());

            mockMvc.perform(patch("/appointments/55/reschedule")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"startAt":"2026-08-01T09:00:00","employeeId":5,
                             "forceOverlap":false}
                            """)).andExpect(status().isOk());

            verify(rescheduleUseCase).execute(new RescheduleAppointmentCommand(APPOINTMENT_ID,
                    INICIO, null, 5L, COMPANY_ID, false, SEDES_VISIBLES));
        }

        /**
         * Tercera copia del mismo arreglo, en {@code RescheduleAppointmentRequest}. Es
         * el verbo que mas usa el front al arrastrar una cita en el calendario y el que
         * menos manda el flag.
         */
        @Test
        @DisplayName("omitir forceOverlap reprograma con el flag en false, no responde 400")
        void omitir_force_overlap_reprograma_con_el_flag_en_false() throws Exception {
            when(rescheduleUseCase.execute(any())).thenReturn(cita());

            mockMvc.perform(patch("/appointments/55/reschedule")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"startAt":"2026-08-01T09:00:00","durationMinutes":60,"employeeId":5}
                            """)).andExpect(status().isOk());

            verify(rescheduleUseCase).execute(new RescheduleAppointmentCommand(APPOINTMENT_ID,
                    INICIO, 60, 5L, COMPANY_ID, false, SEDES_VISIBLES));
        }

        @Test
        @DisplayName("sin employeeId responde 400")
        void sin_employee_id_responde_400() throws Exception {
            mockMvc.perform(patch("/appointments/55/reschedule")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"startAt":"2026-08-01T09:00:00","forceOverlap":false}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(rescheduleUseCase);
        }
    }

    @Nested
    @DisplayName("PATCH /appointments/{id}/status")
    class CambiarEstado {

        @Test
        @DisplayName("status responde 200 y arma el command con la company del contexto")
        void status_responde_200() throws Exception {
            when(changeStatusUseCase.execute(any())).thenReturn(cita());

            mockMvc.perform(patch("/appointments/55/status").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"status":"CONFIRMED"}
                            """)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(55));

            verify(changeStatusUseCase).execute(new ChangeAppointmentStatusCommand(APPOINTMENT_ID,
                    AppointmentStatus.CONFIRMED, COMPANY_ID));
        }

        @Test
        @DisplayName("una transicion invalida sale como 409, no como 500")
        void una_transicion_invalida_sale_como_409() throws Exception {
            when(changeStatusUseCase.execute(any())).thenThrow(
                    new InvalidAppointmentTransitionException(AppointmentStatus.REQUESTED,
                            AppointmentStatus.COMPLETED));

            mockMvc.perform(patch("/appointments/55/status").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"status":"COMPLETED"}
                            """)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_APPOINTMENT_TRANSITION"));
        }

        @Test
        @DisplayName("sin estado responde 400")
        void sin_estado_responde_400() throws Exception {
            mockMvc.perform(patch("/appointments/55/status").contentType(MediaType.APPLICATION_JSON)
                    .content("{}")).andExpect(status().isBadRequest());

            verifyNoInteractions(changeStatusUseCase);
        }

    }

    /**
     * Cancelacion (issue #135). El {@code @Size(max = 300)} de
     * {@code CancelAppointmentRequest} estaba escrito y no se evaluaba: al
     * parametro le faltaba el {@code @Valid}. El daño no era basura en la base
     * —{@code Appointment.cancel} revalida y la columna es {@code length = 300}—
     * sino <b>la forma del error</b>: un motivo pasado de largo salia como
     * excepcion de dominio, con otro {@code errorCode} y otra forma, en vez de como
     * error de campo sobre {@code reason}, que es lo que el contrato promete y lo
     * unico que el front sabe pintar bajo el textarea.
     *
     * <p>
     * Por eso el caso del motivo largo no se conforma con el 400: afirma tambien
     * que el caso de uso <b>no se invoca</b>. Esa segunda mitad es la que prueba
     * que quien corta es la capa web y no el dominio un piso mas abajo, que es
     * exactamente lo que distinguia el antes del despues.
     */
    @Nested
    @DisplayName("PATCH /appointments/{id}/cancel")
    class Cancelar {

        @Test
        @DisplayName("con motivo lo pasa al command")
        void cancel_con_motivo_lo_pasa_al_command() throws Exception {
            when(cancelUseCase.execute(any())).thenReturn(cita());

            mockMvc.perform(patch("/appointments/55/cancel").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"reason":"El dueno aviso"}
                            """)).andExpect(status().isOk());

            verify(cancelUseCase).execute(
                    new CancelAppointmentCommand(APPOINTMENT_ID, "El dueno aviso", COMPANY_ID));
        }

        @Test
        @DisplayName("sin cuerpo es valido y el motivo viaja como null")
        void cancel_sin_cuerpo_es_valido() throws Exception {
            when(cancelUseCase.execute(any())).thenReturn(cita());

            mockMvc.perform(patch("/appointments/55/cancel")).andExpect(status().isOk());

            verify(cancelUseCase)
                    .execute(new CancelAppointmentCommand(APPOINTMENT_ID, null, COMPANY_ID));
        }

        @Test
        @DisplayName("con el motivo explicitamente nulo es valido: @Size no aplica a null")
        void cancel_con_motivo_nulo_explicito_es_valido() throws Exception {
            when(cancelUseCase.execute(any())).thenReturn(cita());

            mockMvc.perform(patch("/appointments/55/cancel").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"reason":null}
                            """)).andExpect(status().isOk());

            verify(cancelUseCase)
                    .execute(new CancelAppointmentCommand(APPOINTMENT_ID, null, COMPANY_ID));
        }

        @Test
        @DisplayName("un motivo de exactamente 300 caracteres se acepta: el limite es inclusivo")
        void motivo_en_el_limite_se_acepta() throws Exception {
            when(cancelUseCase.execute(any())).thenReturn(cita());

            mockMvc.perform(patch("/appointments/55/cancel").contentType(MediaType.APPLICATION_JSON)
                    .content(cancelarJson(MOTIVO_EN_EL_LIMITE))).andExpect(status().isOk());

            verify(cancelUseCase).execute(
                    new CancelAppointmentCommand(APPOINTMENT_ID, MOTIVO_EN_EL_LIMITE, COMPANY_ID));
        }

        @Test
        @DisplayName("un motivo de 350 caracteres responde 400 con error de campo sobre reason y no llega al caso de uso")
        void motivo_pasado_de_largo_responde_400_con_error_de_campo() throws Exception {
            mockMvc.perform(patch("/appointments/55/cancel").contentType(MediaType.APPLICATION_JSON)
                    .content(cancelarJson(MOTIVO_PASADO_DE_LARGO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[0].field").value("reason"));

            // La mitad que distingue el arreglo: sin @Valid la peticion llegaba al
            // dominio y el 400 lo producia Appointment.cancel, con otra forma.
            verifyNoInteractions(cancelUseCase);
        }
    }

    @Nested
    @DisplayName("DELETE /appointments/{id}")
    class Borrar {

        @Test
        @DisplayName("responde 204 sin cuerpo y acota el borrado a la company del contexto")
        void responde_204_sin_cuerpo() throws Exception {
            mockMvc.perform(delete("/appointments/55")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(APPOINTMENT_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("de una cita inexistente responde 404")
        void de_una_cita_inexistente_responde_404() throws Exception {
            org.mockito.Mockito.doThrow(new AppointmentNotFoundException(99L)).when(deleteUseCase)
                    .execute(99L, COMPANY_ID);

            mockMvc.perform(delete("/appointments/99")).andExpect(status().isNotFound());
        }
    }
}
