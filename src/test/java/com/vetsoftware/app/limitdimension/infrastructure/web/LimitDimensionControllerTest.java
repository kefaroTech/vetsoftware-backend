package com.vetsoftware.app.limitdimension.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.limitdimension.application.command.CreateLimitDimensionCommand;
import com.vetsoftware.app.limitdimension.application.command.UpdateLimitDimensionCommand;
import com.vetsoftware.app.limitdimension.application.dto.LimitDimensionDto;
import com.vetsoftware.app.limitdimension.application.port.in.CreateLimitDimensionUseCase;
import com.vetsoftware.app.limitdimension.application.port.in.FindLimitDimensionUseCase;
import com.vetsoftware.app.limitdimension.application.port.in.ListLimitDimensionsUseCase;
import com.vetsoftware.app.limitdimension.application.port.in.UpdateLimitDimensionUseCase;
import com.vetsoftware.app.limitdimension.domain.LimitDimensionCodeAlreadyExistsException;
import com.vetsoftware.app.limitdimension.domain.LimitDimensionNotFoundException;
import com.vetsoftware.app.limitdimension.domain.MeasureKind;
import com.vetsoftware.app.limitdimension.domain.SubModuleRef;
import com.vetsoftware.app.limitdimension.infrastructure.web.request.CreateLimitDimensionRequest;
import com.vetsoftware.app.limitdimension.infrastructure.web.request.UpdateLimitDimensionRequest;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.lang.reflect.RecordComponent;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP del catálogo de ejes limitables: rutas, binding, validación del
 * cuerpo, códigos de estado y forma del JSON. Lo que hay debajo son dobles.
 *
 * <p>
 * <strong>Qué prueba de autorización esta rodaja y qué no.</strong> La cadena
 * de seguridad va sustituida por una permisiva ({@code addFilters = false}) y
 * los puertos son mocks, así que ningún {@code @PreAuthorize} se
 * <em>ejecuta</em> aquí: un test que enviara una petición y esperara 403
 * estaría comprobando una mentira. Lo que sí se puede comprobar —y es donde
 * vive el riesgo real de esta feature— es que el gate declarado <em>sigue
 * siendo</em> {@code hasRole('SYSTEM')} a secas. ArchUnit solo exige que la
 * anotación exista; cambiar su expresión no rompe ninguna regla dura, y en un
 * catálogo sin empresa cualquier otra expresión abre la tabla a un tenant.
 */
@WebMvcTest(LimitDimensionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("LimitDimensionController — contrato HTTP")
class LimitDimensionControllerTest {

    private static final String CUERPO_VALIDO = """
            {"code":"PETS","name":"Mascotas","measureKind":"CUMULATIVE","subModuleId":31,
             "releaseDelayDays":30,"availableFrom":"2026-01-01"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateLimitDimensionUseCase createUseCase;
    @MockitoBean
    private UpdateLimitDimensionUseCase updateUseCase;
    @MockitoBean
    private FindLimitDimensionUseCase findUseCase;
    @MockitoBean
    private ListLimitDimensionsUseCase listUseCase;

    private static LimitDimensionDto mascotas() {
        return new LimitDimensionDto(4L, "PETS", "Mascotas", MeasureKind.CUMULATIVE,
                new SubModuleRef(31L, "CLINICAL_HISTORY", "Historia clinica"), 30,
                LocalDate.of(2026, 1, 1), LocalDateTime.of(2026, 8, 27, 9, 0));
    }

    private static LimitDimensionDto citas() {
        return new LimitDimensionDto(5L, "APPOINTMENTS", "Citas", MeasureKind.FLOW, null, null,
                LocalDate.of(2026, 6, 1), LocalDateTime.of(2026, 8, 27, 9, 5));
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("POST /limit-dimensions responde 201 con el eje declarado")
        void post_responde_201() throws Exception {
            when(createUseCase.execute(any())).thenReturn(mascotas());

            mockMvc.perform(post("/limit-dimensions").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(4))
                    .andExpect(jsonPath("$.code").value("PETS"))
                    .andExpect(jsonPath("$.measureKind").value("CUMULATIVE"))
                    .andExpect(jsonPath("$.releaseDelayDays").value(30))
                    .andExpect(jsonPath("$.availableFrom").value("2026-01-01"))
                    .andExpect(jsonPath("$.subModule.code").value("CLINICAL_HISTORY"));
        }

        @Test
        @DisplayName("POST traduce el request al command sin inventarse campos")
        void post_traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(mascotas());

            mockMvc.perform(post("/limit-dimensions").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            ArgumentCaptor<CreateLimitDimensionCommand> command = ArgumentCaptor
                    .forClass(CreateLimitDimensionCommand.class);
            verify(createUseCase).execute(command.capture());
            assertThat(command.getValue().code()).isEqualTo("PETS");
            assertThat(command.getValue().measureKind()).isEqualTo(MeasureKind.CUMULATIVE);
            assertThat(command.getValue().subModuleId()).isEqualTo(31L);
            assertThat(command.getValue().releaseDelayDays()).isEqualTo(30);
            assertThat(command.getValue().availableFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
        }

        /**
         * El submódulo es opcional a propósito: hay ejes que no cuelgan de ningún
         * módulo. Que el cuerpo pueda omitirlo y que la respuesta lo devuelva nulo son
         * la misma decisión vista por sus dos lados.
         */
        @Test
        @DisplayName("POST sin submodulo pasa un null al command y la respuesta lo omite")
        void post_sin_submodulo() throws Exception {
            when(createUseCase.execute(any())).thenReturn(citas());

            mockMvc.perform(
                    post("/limit-dimensions").contentType(MediaType.APPLICATION_JSON).content("""
                            {"code":"APPOINTMENTS","name":"Citas","measureKind":"FLOW",
                             "availableFrom":"2026-06-01"}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.subModule").doesNotExist());

            ArgumentCaptor<CreateLimitDimensionCommand> command = ArgumentCaptor
                    .forClass(CreateLimitDimensionCommand.class);
            verify(createUseCase).execute(command.capture());
            assertThat(command.getValue().subModuleId()).isNull();
        }

        @Test
        @DisplayName("POST con un codigo ya declarado responde 409")
        void post_con_codigo_repetido_responde_409() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new LimitDimensionCodeAlreadyExistsException("PETS"));

            mockMvc.perform(post("/limit-dimensions").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("POST sin codigo responde 400 y no llega al caso de uso")
        void post_sin_codigo_responde_400() throws Exception {
            mockMvc.perform(
                    post("/limit-dimensions").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Mascotas","measureKind":"STOCK","availableFrom":"2026-01-01"}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        /**
         * La prueba de que el {@code @Valid} del {@code @RequestBody} está puesto
         * (#135). Sin él las restricciones del record se leerían perfectas en el diff,
         * el OpenAPI seguiría anunciando el {@code maxLength} al front, y no se
         * evaluarían nunca: el eje entraría con un código de 80 caracteres y el error
         * saldría del dominio con otra forma.
         */
        @Test
        @DisplayName("POST con un codigo de mas de 50 caracteres responde 400: la restriccion si se evalua")
        void post_con_codigo_largo_responde_400() throws Exception {
            mockMvc.perform(post("/limit-dimensions").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"" + "X".repeat(51)
                            + "\",\"name\":\"Mascotas\",\"measureKind\":\"STOCK\","
                            + "\"availableFrom\":\"2026-01-01\"}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("POST sin fecha de disponibilidad responde 400")
        void post_sin_available_from_responde_400() throws Exception {
            mockMvc.perform(post("/limit-dimensions").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"PETS\",\"name\":\"Mascotas\",\"measureKind\":\"STOCK\"}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("POST con un tipo de medida que no existe responde 400")
        void post_con_measure_kind_desconocido_responde_400() throws Exception {
            mockMvc.perform(
                    post("/limit-dimensions").contentType(MediaType.APPLICATION_JSON).content("""
                            {"code":"PETS","name":"Mascotas","measureKind":"INVENTADO",
                             "availableFrom":"2026-01-01"}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("POST con dias de enfriamiento negativos responde 400")
        void post_con_enfriamiento_negativo_responde_400() throws Exception {
            mockMvc.perform(
                    post("/limit-dimensions").contentType(MediaType.APPLICATION_JSON).content("""
                            {"code":"PETS","name":"Mascotas","measureKind":"CUMULATIVE",
                             "releaseDelayDays":-1,"availableFrom":"2026-01-01"}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("GET /limit-dimensions devuelve la lista entera, sin envoltorio de pagina")
        void get_listado() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(mascotas(), citas()));

            mockMvc.perform(get("/limit-dimensions")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].code").value("PETS"))
                    .andExpect(jsonPath("$[1].measureKind").value("FLOW"));
        }

        @Test
        @DisplayName("GET /limit-dimensions/{id} devuelve el eje")
        void get_por_id() throws Exception {
            when(findUseCase.findById(4L)).thenReturn(mascotas());

            mockMvc.perform(get("/limit-dimensions/4")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Mascotas"));
        }

        @Test
        @DisplayName("GET /limit-dimensions/{id} de un eje que no existe responde 404")
        void get_por_id_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L)).thenThrow(new LimitDimensionNotFoundException(99L));

            mockMvc.perform(get("/limit-dimensions/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Autorizacion")
    class Autorizacion {

        /**
         * El eje es catálogo global: no hay {@code companyId} que revalidar, así que el
         * único gate correcto es el de plataforma. Si alguien relajara cualquiera de
         * los tres a un {@code hasAuthority(...)}, un empleado de una clínica podría
         * declarar ejes para todas las demás y ArchUnit lo dejaría pasar —solo exige
         * que la anotación exista—.
         */
        @Test
        @DisplayName("los tres puertos exigen SYSTEM a secas, sin empresa que revalidar")
        void los_tres_puertos_exigen_system() throws Exception {
            assertThat(CreateLimitDimensionUseCase.class
                    .getMethod("execute", CreateLimitDimensionCommand.class)
                    .getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('SYSTEM')");
            assertThat(FindLimitDimensionUseCase.class.getMethod("findById", Long.class)
                    .getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('SYSTEM')");
            assertThat(ListLimitDimensionsUseCase.class.getMethod("listAll")
                    .getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('SYSTEM')");
        }

        /**
         * Espeja {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} en el sitio donde importa. Aquí
         * la regla se cumple sola porque la tabla no tiene empresa; el test existe para
         * que añadir el campo «para filtrar» sea un fallo y no una idea razonable.
         */
        @Test
        @DisplayName("el request no declara companyId: no hay empresa que el cliente pueda elegir")
        void el_request_no_declara_company_id() {
            assertThat(CreateLimitDimensionRequest.class.getRecordComponents())
                    .extracting(RecordComponent::getName).doesNotContain("companyId");
        }
    }

    @Nested
    @DisplayName("Edicion")
    class Edicion {

        /**
         * <strong>El caso de uso que faltaba.</strong> {@code LimitDimension.update}
         * existía en el dominio y no lo llamaba nadie: corregirle una errata al nombre
         * de un eje exigía una migración.
         */
        @Test
        @DisplayName("PUT /{id} toma el eje de la ruta y devuelve el eje editado")
        void put_edita_el_eje_de_la_ruta() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(mascotas());

            mockMvc.perform(
                    put("/limit-dimensions/4").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Mascotas","subModuleId":31,"releaseDelayDays":30}
                            """)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(4))
                    .andExpect(jsonPath("$.name").value("Mascotas"));

            ArgumentCaptor<UpdateLimitDimensionCommand> command = ArgumentCaptor
                    .forClass(UpdateLimitDimensionCommand.class);
            verify(updateUseCase).execute(command.capture());
            assertThat(command.getValue().id()).isEqualTo(4L);
            assertThat(command.getValue().subModuleId()).isEqualTo(31L);
            assertThat(command.getValue().releaseDelayDays()).isEqualTo(30);
        }

        @Test
        @DisplayName("PUT sobre un eje que no existe responde 404")
        void put_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new LimitDimensionNotFoundException(99L));

            mockMvc.perform(
                    put("/limit-dimensions/99").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Mascotas"}
                            """)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PUT sin nombre responde 400 y no edita nada")
        void put_sin_nombre_responde_400() throws Exception {
            mockMvc.perform(put("/limit-dimensions/4").contentType(MediaType.APPLICATION_JSON)
                    .content("{}")).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        /**
         * <strong>El cuerpo no admite código, tipo de medida ni fecha de
         * disponibilidad, y esa ausencia es la regla.</strong> El código es la clave
         * con la que la línea del contrato nombra el eje; el tipo va atado por clave
         * foránea compuesta desde los techos vendidos; y la fecha decide D-74. Los tres
         * viven copiados aguas abajo, así que moverlos aquí no sería una edición sino
         * una migración disfrazada de {@code PUT}.
         */
        @Test
        @DisplayName("el request de edicion no declara code, measureKind ni availableFrom")
        void el_request_de_edicion_no_declara_lo_inmutable() {
            assertThat(UpdateLimitDimensionRequest.class.getRecordComponents())
                    .extracting(RecordComponent::getName)
                    .doesNotContain("code", "measureKind", "availableFrom", "companyId");
        }

        @Test
        @DisplayName("editar exige SYSTEM a secas, igual que declarar y consultar")
        void editar_exige_system_a_secas() throws Exception {
            assertThat(UpdateLimitDimensionUseCase.class
                    .getMethod("execute", UpdateLimitDimensionCommand.class)
                    .getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('SYSTEM')");
        }
    }
}
