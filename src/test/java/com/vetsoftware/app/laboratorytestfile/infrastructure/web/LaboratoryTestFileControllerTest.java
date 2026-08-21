package com.vetsoftware.app.laboratorytestfile.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.laboratorytestfile.application.command.CreateLaboratoryTestFileCommand;
import com.vetsoftware.app.laboratorytestfile.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDownloadDto;
import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDto;
import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestSummaryDto;
import com.vetsoftware.app.laboratorytestfile.application.port.in.CreateLaboratoryTestFileUseCase;
import com.vetsoftware.app.laboratorytestfile.application.port.in.DeleteLaboratoryTestFileUseCase;
import com.vetsoftware.app.laboratorytestfile.application.port.in.DownloadLaboratoryTestFileUseCase;
import com.vetsoftware.app.laboratorytestfile.application.port.in.FindLaboratoryTestFileUseCase;
import com.vetsoftware.app.laboratorytestfile.application.port.in.ListLaboratoryTestFilesByLaboratoryTestUseCase;
import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFileNotFoundException;
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
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP del controller: rutas, binding multipart, codigos de estado y
 * forma del JSON. Los casos de uso van mockeados — aqui no se prueba S3 ni la
 * logica de negocio.
 */
@WebMvcTest(LaboratoryTestFileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("LaboratoryTestFileController — contrato HTTP")
class LaboratoryTestFileControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateLaboratoryTestFileUseCase createUseCase;
    @MockitoBean
    private FindLaboratoryTestFileUseCase findUseCase;
    @MockitoBean
    private ListLaboratoryTestFilesByLaboratoryTestUseCase listByLaboratoryTestUseCase;
    @MockitoBean
    private DownloadLaboratoryTestFileUseCase downloadUseCase;
    @MockitoBean
    private DeleteLaboratoryTestFileUseCase deleteUseCase;

    /**
     * El controller sella el uploader con {@code authz.currentEmployeeId()},
     * distinto del {@code *OrNull()} que ya stubea {@link WebMvcSliceConfig}: sin
     * este stub Mockito devolveria null y el command llegaria sin empleado.
     */
    @BeforeEach
    void resolverElEmpleadoDelContexto() {
        when(authz.currentEmployeeId()).thenReturn(WebMvcSliceConfig.EMPLOYEE_ID);
    }

    private static LaboratoryTestFileDto informe() {
        return new LaboratoryTestFileDto(700L, "9/3/firulais-100/uuid-informe.pdf",
                "vetsoftware-lab-files", "informe.pdf", "application/pdf", 20480L, "etag-abc",
                new EmployeeSummaryDto(4L, "EMP-001", "Ana Ruiz"),
                new LaboratoryTestSummaryDto(500L, LocalDate.of(2026, 1, 15)),
                LocalDateTime.of(2026, 1, 15, 10, 30));
    }

    @Nested
    @DisplayName("POST /laboratory-test-files")
    class Upload {

        @Test
        @DisplayName("responde 201 con el recurso creado")
        void responde_201_con_el_recurso_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(informe());
            MockMultipartFile archivo = new MockMultipartFile("file", "informe.pdf",
                    "application/pdf", "contenido".getBytes());

            mockMvc.perform(multipart("/laboratory-test-files").file(archivo)
                    .param("laboratoryTestId", "500")).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(700))
                    .andExpect(jsonPath("$.originalFileName").value("informe.pdf"))
                    .andExpect(jsonPath("$.uploadedBy.name").value("Ana Ruiz"))
                    .andExpect(jsonPath("$.laboratoryTest.id").value(500));
        }

        @Test
        @DisplayName("traduce el multipart al command con el contenido, el tipo y el empleado del contexto")
        void traduce_el_multipart_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(informe());
            MockMultipartFile archivo = new MockMultipartFile("file", "informe.pdf",
                    "application/pdf", "contenido".getBytes());

            mockMvc.perform(multipart("/laboratory-test-files").file(archivo)
                    .param("laboratoryTestId", "500"));

            // El record tiene un campo byte[]: su equals() compara por referencia, asi
            // que se captura y se afirma campo por campo (el contenido, por valor).
            ArgumentCaptor<CreateLaboratoryTestFileCommand> commandCaptor = ArgumentCaptor
                    .forClass(CreateLaboratoryTestFileCommand.class);
            verify(createUseCase).execute(commandCaptor.capture());
            CreateLaboratoryTestFileCommand command = commandCaptor.getValue();
            assertThat(command.laboratoryTestId()).isEqualTo(500L);
            assertThat(command.originalFileName()).isEqualTo("informe.pdf");
            assertThat(command.contentType()).isEqualTo("application/pdf");
            assertThat(command.sizeBytes()).isEqualTo("contenido".getBytes().length);
            assertThat(command.content()).isEqualTo("contenido".getBytes());
            assertThat(command.uploadedById()).isEqualTo(WebMvcSliceConfig.EMPLOYEE_ID);
            // El companyId no es un parametro del multipart: lo pone el backend desde el
            // AuthContext, y es con el que el caso de uso acota el examen padre.
            assertThat(command.companyId()).isEqualTo(WebMvcSliceConfig.COMPANY_ID);
        }

        @Test
        @DisplayName("sin content-type declarado, cae a application/octet-stream")
        void sin_content_type_cae_a_octet_stream() throws Exception {
            when(createUseCase.execute(any())).thenReturn(informe());
            MockMultipartFile archivo = new MockMultipartFile("file", "informe.pdf", null,
                    "contenido".getBytes());

            mockMvc.perform(multipart("/laboratory-test-files").file(archivo)
                    .param("laboratoryTestId", "500"));

            ArgumentCaptor<CreateLaboratoryTestFileCommand> commandCaptor = ArgumentCaptor
                    .forClass(CreateLaboratoryTestFileCommand.class);
            verify(createUseCase).execute(commandCaptor.capture());
            assertThat(commandCaptor.getValue().contentType())
                    .isEqualTo("application/octet-stream");
        }

        @Test
        @DisplayName("archivo vacio responde 400 y no llega al caso de uso")
        void archivo_vacio_responde_400() throws Exception {
            MockMultipartFile vacio = new MockMultipartFile("file", "informe.pdf",
                    "application/pdf", new byte[0]);

            mockMvc.perform(multipart("/laboratory-test-files").file(vacio)
                    .param("laboratoryTestId", "500")).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("GET /laboratory-test-files/by-laboratory-test/{laboratoryTestId}")
    class ListarPorExamen {

        @Test
        @DisplayName("devuelve la lista de archivos del examen, acotada por la empresa del contexto")
        void devuelve_la_lista() throws Exception {
            when(listByLaboratoryTestUseCase.listByLaboratoryTest(500L,
                    WebMvcSliceConfig.COMPANY_ID)).thenReturn(List.of(informe()));

            mockMvc.perform(get("/laboratory-test-files/by-laboratory-test/500"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(700));
        }
    }

    @Nested
    @DisplayName("GET /laboratory-test-files/{id}")
    class BuscarPorId {

        @Test
        @DisplayName("responde 200 con el recurso encontrado")
        void responde_200() throws Exception {
            when(findUseCase.findById(700L, WebMvcSliceConfig.COMPANY_ID)).thenReturn(informe());

            mockMvc.perform(get("/laboratory-test-files/700")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.storageKey").value("9/3/firulais-100/uuid-informe.pdf"));
        }

        @Test
        @DisplayName("inexistente responde 404, no 500")
        void inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L, WebMvcSliceConfig.COMPANY_ID))
                    .thenThrow(new LaboratoryTestFileNotFoundException(99L));

            mockMvc.perform(get("/laboratory-test-files/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /laboratory-test-files/{id}/download")
    class Descargar {

        @Test
        @DisplayName("responde 200 con el contenido, el content-type y el nombre original como adjunto")
        void responde_200_con_el_contenido_como_adjunto() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(WebMvcSliceConfig.COMPANY_ID);
            when(downloadUseCase.download(700L, WebMvcSliceConfig.COMPANY_ID))
                    .thenReturn(new LaboratoryTestFileDownloadDto("informe.pdf", "application/pdf",
                            "contenido".getBytes()));

            mockMvc.perform(get("/laboratory-test-files/700/download")).andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition",
                            org.hamcrest.Matchers.containsString("informe.pdf")))
                    .andExpect(header().string("Content-Type", "application/pdf"));
        }

        @Test
        @DisplayName("descarga con la empresa del contexto, no con la que pida el cliente")
        void descarga_con_la_empresa_del_contexto() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(WebMvcSliceConfig.COMPANY_ID);
            when(downloadUseCase.download(700L, WebMvcSliceConfig.COMPANY_ID))
                    .thenReturn(new LaboratoryTestFileDownloadDto("informe.pdf", "application/pdf",
                            "contenido".getBytes()));

            mockMvc.perform(get("/laboratory-test-files/700/download"));

            verify(downloadUseCase).download(700L, WebMvcSliceConfig.COMPANY_ID);
        }

        @Test
        @DisplayName("inexistente responde 404, no 500")
        void inexistente_responde_404() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(WebMvcSliceConfig.COMPANY_ID);
            when(downloadUseCase.download(99L, WebMvcSliceConfig.COMPANY_ID))
                    .thenThrow(new LaboratoryTestFileNotFoundException(99L));

            mockMvc.perform(get("/laboratory-test-files/99/download"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /laboratory-test-files/{id}")
    class Borrar {

        @Test
        @DisplayName("responde 204 sin cuerpo y delega con la empresa del contexto")
        void responde_204_sin_cuerpo() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(WebMvcSliceConfig.COMPANY_ID);

            mockMvc.perform(delete("/laboratory-test-files/700")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(700L, WebMvcSliceConfig.COMPANY_ID);
        }

        @Test
        @DisplayName("inexistente responde 404, no 500")
        void inexistente_responde_404() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(WebMvcSliceConfig.COMPANY_ID);
            org.mockito.Mockito.doThrow(new LaboratoryTestFileNotFoundException(99L))
                    .when(deleteUseCase).execute(99L, WebMvcSliceConfig.COMPANY_ID);

            mockMvc.perform(delete("/laboratory-test-files/99")).andExpect(status().isNotFound());
        }
    }
}
