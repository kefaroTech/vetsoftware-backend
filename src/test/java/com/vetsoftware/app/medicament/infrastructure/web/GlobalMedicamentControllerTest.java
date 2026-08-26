package com.vetsoftware.app.medicament.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.medicament.application.command.CreateGlobalMedicamentCommand;
import com.vetsoftware.app.medicament.application.command.UpdateGlobalMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.in.CreateGlobalMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.in.DeleteGlobalMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.in.ListDisabledGlobalMedicamentsUseCase;
import com.vetsoftware.app.medicament.application.port.in.ListGlobalMedicamentsUseCase;
import com.vetsoftware.app.medicament.application.port.in.ReactivateGlobalMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.in.UpdateGlobalMedicamentUseCase;
import com.vetsoftware.app.medicament.domain.MedicamentNameAlreadyExistsException;
import com.vetsoftware.app.medicament.domain.MedicamentNotFoundException;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
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
 * Rodaja HTTP de {@link GlobalMedicamentController}: rutas bajo
 * {@code /admin/medicaments}, binding, forma del JSON y codigos de estado.
 *
 * <p>
 * Lo que separa esta rodaja de {@code MedicamentControllerTest} es lo que NO
 * hay: ningun endpoint recibe ni deriva {@code companyId}, el controller no
 * inyecta {@code Authz} y los dos requests no tienen campo de empresa ni de
 * {@code general}. Que la empresa salga {@code null} en las respuestas no es un
 * detalle del fixture: es el contrato.
 *
 * <p>
 * Aqui no se prueba la autorizacion —la cadena de seguridad se sustituye por
 * una permisiva, ver {@link WebMvcSliceConfig}—. El {@code hasRole('SYSTEM')} a
 * secas de cada puerto lo verifica ArchUnit.
 */
@WebMvcTest(GlobalMedicamentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("GlobalMedicamentController — contrato HTTP de /admin/medicaments")
class GlobalMedicamentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateGlobalMedicamentUseCase createUseCase;
    @MockitoBean
    private UpdateGlobalMedicamentUseCase updateUseCase;
    @MockitoBean
    private ListGlobalMedicamentsUseCase listUseCase;
    @MockitoBean
    private ListDisabledGlobalMedicamentsUseCase listDisabledUseCase;
    @MockitoBean
    private DeleteGlobalMedicamentUseCase deleteUseCase;
    @MockitoBean
    private ReactivateGlobalMedicamentUseCase reactivateUseCase;

    private static MedicamentDto global() {
        return new MedicamentDto(1L, "Amoxicilina", "Antibiotico", null, true,
                LocalDateTime.of(2026, 1, 1, 0, 0), true);
    }

    private static MedicamentDto globalPausado() {
        return new MedicamentDto(1L, "Amoxicilina", "Antibiotico retirado", null, true,
                LocalDateTime.of(2026, 1, 1, 0, 0), false);
    }

    @Nested
    @DisplayName("POST /admin/medicaments")
    class Creacion {

        @Test
        @DisplayName("crea y responde 201 con la empresa nula y general=true")
        void crea_y_responde_201() throws Exception {
            when(createUseCase.execute(any())).thenReturn(global());

            mockMvc.perform(
                    post("/admin/medicaments").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Amoxicilina","description":"Antibiotico"}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Amoxicilina"))
                    .andExpect(jsonPath("$.general").value(true))
                    .andExpect(jsonPath("$.company").doesNotExist());

            ArgumentCaptor<CreateGlobalMedicamentCommand> captor = ArgumentCaptor
                    .forClass(CreateGlobalMedicamentCommand.class);
            verify(createUseCase).execute(captor.capture());
            assertThat(captor.getValue().name()).isEqualTo("Amoxicilina");
            assertThat(captor.getValue().description()).isEqualTo("Antibiotico");
        }

        /**
         * El request no tiene campo de empresa ni de {@code general}, asi que un
         * cliente que los cuele en el cuerpo no puede elegir de quien es la fila que
         * crea: se ignoran al deserializar y el command sigue llevando dos campos. Si
         * alguien anadiera esos campos al request, este caso deja de compilar o empieza
         * a fallar, que es justo lo que se quiere.
         */
        @Test
        @DisplayName("un companyId o un general colados en el cuerpo no llegan al command")
        void companyid_colado_en_el_cuerpo_no_llega_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(global());

            mockMvc.perform(
                    post("/admin/medicaments").contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {"name":"Amoxicilina","description":"Antibiotico","companyId":7,"general":false}
                                    """))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.company").doesNotExist())
                    .andExpect(jsonPath("$.general").value(true));

            ArgumentCaptor<CreateGlobalMedicamentCommand> captor = ArgumentCaptor
                    .forClass(CreateGlobalMedicamentCommand.class);
            verify(createUseCase).execute(captor.capture());
            assertThat(CreateGlobalMedicamentCommand.class.getRecordComponents())
                    .extracting(java.lang.reflect.RecordComponent::getName)
                    .containsExactly("name", "description");
            assertThat(captor.getValue().name()).isEqualTo("Amoxicilina");
        }

        @Test
        @DisplayName("rechaza un name en blanco con 400")
        void rechaza_name_en_blanco() throws Exception {
            mockMvc.perform(
                    post("/admin/medicaments").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"","description":"x"}
                            """)).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("nombre repetido responde 409 con el errorCode de negocio")
        void nombre_repetido_responde_409() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new MedicamentNameAlreadyExistsException("Amoxicilina"));

            mockMvc.perform(
                    post("/admin/medicaments").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Amoxicilina","description":"Antibiotico"}
                            """)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("MEDICAMENT_NAME_ALREADY_EXISTS"));
        }
    }

    @Nested
    @DisplayName("GET /admin/medicaments")
    class Listado {

        @Test
        @DisplayName("sin parametros pagina con los valores por defecto (0, 20)")
        void pagina_con_los_valores_por_defecto() throws Exception {
            when(listUseCase.listAll(null, 0, 20))
                    .thenReturn(PageResult.of(List.of(global()), 0, 20, 1L));

            mockMvc.perform(get("/admin/medicaments")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Amoxicilina"))
                    .andExpect(jsonPath("$.content[0].company").doesNotExist())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.pageSize").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("page y pageSize llegan tal cual al caso de uso")
        void page_y_pagesize_llegan_al_caso_de_uso() throws Exception {
            when(listUseCase.listAll(null, 2, 5)).thenReturn(PageResult.of(List.of(), 2, 5, 47L));

            mockMvc.perform(get("/admin/medicaments").param("page", "2").param("pageSize", "5"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(47))
                    .andExpect(jsonPath("$.totalPages").value(10));
        }

        /**
         * Sin {@code q} el caso de uso recibe {@code null}, no cadena vacia: es lo que
         * hace que el listado se comporte exactamente como antes de existir la
         * busqueda. Lo afirman los dos casos de arriba, cuyos stubs son
         * {@code listAll(null, ...)} — con STRICT_STUBS, si el controller mandara "" el
         * stub quedaria sin usar y el test rompe.
         */
        @Test
        @DisplayName("el parametro q llega al caso de uso tal cual, sin recortar")
        void el_parametro_q_llega_al_caso_de_uso() throws Exception {
            when(listUseCase.listAll("  clavul  ", 0, 20))
                    .thenReturn(PageResult.of(List.of(global()), 0, 20, 1L));

            mockMvc.perform(get("/admin/medicaments").param("q", "  clavul  "))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Amoxicilina"));
        }

        /**
         * El recorte y la traduccion de «en blanco» a «sin filtro» son del adaptador de
         * persistencia, no de la web: aqui la cadena vacia tiene que llegar tal cual.
         * Si el controller la normalizara, habria dos sitios decidiendo lo mismo y
         * sería cuestión de tiempo que discreparan.
         */
        @Test
        @DisplayName("un q vacio NO lo traduce el controller: llega vacio al caso de uso")
        void un_q_vacio_llega_vacio_al_caso_de_uso() throws Exception {
            when(listUseCase.listAll("", 0, 20)).thenReturn(PageResult.of(List.of(), 0, 20, 0L));

            mockMvc.perform(get("/admin/medicaments").param("q", "")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("una busqueda sin resultados es un 200 con la pagina vacia")
        void busqueda_sin_resultados_es_200_vacio() throws Exception {
            when(listUseCase.listAll("no-existe", 0, 20)).thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/admin/medicaments").param("q", "no-existe"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("/disabled devuelve los globales pausados con enabled=false")
        void disabled_devuelve_los_globales_pausados() throws Exception {
            when(listDisabledUseCase.listDisabled()).thenReturn(List.of(globalPausado()));

            mockMvc.perform(get("/admin/medicaments/disabled")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].enabled").value(false))
                    .andExpect(jsonPath("$[0].company").doesNotExist());
        }
    }

    @Nested
    @DisplayName("PUT /admin/medicaments/{id}")
    class Actualizacion {

        @Test
        @DisplayName("traduce el id del path y el cuerpo al command, sin empresa")
        void traduce_el_path_id_y_el_cuerpo_al_command() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(global());

            mockMvc.perform(
                    put("/admin/medicaments/1").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Amoxicilina trihidrato","description":"Revisado"}
                            """)).andExpect(status().isOk());

            ArgumentCaptor<UpdateGlobalMedicamentCommand> captor = ArgumentCaptor
                    .forClass(UpdateGlobalMedicamentCommand.class);
            verify(updateUseCase).execute(captor.capture());
            assertThat(captor.getValue().id()).isEqualTo(1L);
            assertThat(captor.getValue().name()).isEqualTo("Amoxicilina trihidrato");
            assertThat(UpdateGlobalMedicamentCommand.class.getRecordComponents())
                    .extracting(java.lang.reflect.RecordComponent::getName)
                    .containsExactly("id", "name", "description");
        }

        /**
         * El 404 es lo que ve un administrador que apunta al id de un medicamento
         * privado de una clinica: no se revela de quien es la fila.
         */
        @Test
        @DisplayName("un id que la rama global no alcanza responde 404")
        void id_inalcanzable_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new MedicamentNotFoundException(1L));

            mockMvc.perform(
                    put("/admin/medicaments/1").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Robado","description":"Robado"}
                            """)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("rechaza un name en blanco con 400")
        void rechaza_name_en_blanco() throws Exception {
            mockMvc.perform(
                    put("/admin/medicaments/1").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"","description":"x"}
                            """)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE y PATCH /enable")
    class BorradoYReactivacion {

        @Test
        @DisplayName("delete responde 204 y pasa solo el id, sin empresa")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/admin/medicaments/1")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(1L);
        }

        @Test
        @DisplayName("delete de un id inalcanzable responde 404, no 204")
        void delete_de_un_id_inalcanzable_responde_404() throws Exception {
            org.mockito.Mockito.doThrow(new MedicamentNotFoundException(1L)).when(deleteUseCase)
                    .execute(1L);

            mockMvc.perform(delete("/admin/medicaments/1")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("enable reactiva y devuelve el medicamento recuperado")
        void enable_reactiva_y_devuelve_el_medicamento() throws Exception {
            when(reactivateUseCase.execute(1L)).thenReturn(global());

            mockMvc.perform(patch("/admin/medicaments/1/enable")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Amoxicilina"))
                    .andExpect(jsonPath("$.enabled").value(true))
                    .andExpect(jsonPath("$.company").doesNotExist());
        }

        @Test
        @DisplayName("enable de un global que no existe responde 404")
        void enable_de_un_global_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(1L)).thenThrow(new MedicamentNotFoundException(1L));

            mockMvc.perform(patch("/admin/medicaments/1/enable")).andExpect(status().isNotFound());
        }
    }
}
