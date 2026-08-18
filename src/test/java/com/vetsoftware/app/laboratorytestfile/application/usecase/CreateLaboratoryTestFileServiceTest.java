package com.vetsoftware.app.laboratorytestfile.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDto;
import com.vetsoftware.app.laboratorytestfile.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.laboratorytestfile.application.port.out.FileStoragePort;
import com.vetsoftware.app.laboratorytestfile.application.port.out.LaboratoryTestFileRepository;
import com.vetsoftware.app.laboratorytestfile.application.port.out.LaboratoryTestQueryPort;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFile;
import com.vetsoftware.app.laboratorytestfile.testsupport.LaboratoryTestFileMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateLaboratoryTestFileService")
class CreateLaboratoryTestFileServiceTest {

    @Mock
    private LaboratoryTestFileRepository repository;
    @Mock
    private LaboratoryTestQueryPort laboratoryTestQueryPort;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @Mock
    private FileStoragePort fileStoragePort;

    @InjectMocks
    private CreateLaboratoryTestFileService service;

    @Captor
    private ArgumentCaptor<LaboratoryTestFile> fileCaptor;

    private void referenciasResueltas() {
        when(laboratoryTestQueryPort.findById(LaboratoryTestFileMother.LABORATORY_TEST_ID))
                .thenReturn(Optional.of(LaboratoryTestFileMother.EXAMEN));
        when(employeeQueryPort.findById(LaboratoryTestFileMother.EMPLOYEE_ID))
                .thenReturn(Optional.of(LaboratoryTestFileMother.VETERINARIO));
        when(laboratoryTestQueryPort.findStoragePath(LaboratoryTestFileMother.LABORATORY_TEST_ID))
                .thenReturn(Optional.of(LaboratoryTestFileMother.rutaAlmacenamiento()));
    }

    @Nested
    @DisplayName("creacion valida")
    class CreacionValida {

        @Test
        @DisplayName("sube el contenido a S3 con la clave calculada a partir de la ruta de almacenamiento")
        void sube_el_contenido_con_la_clave_calculada() {
            referenciasResueltas();
            when(fileStoragePort.store(any(), any(), any()))
                    .thenReturn(new FileStoragePort.StoredFile(LaboratoryTestFileMother.BUCKET,
                            LaboratoryTestFileMother.STORAGE_KEY, LaboratoryTestFileMother.E_TAG));
            when(repository.save(any())).thenReturn(LaboratoryTestFileMother.archivoValido());

            service.execute(LaboratoryTestFileMother.comandoCrear());

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(fileStoragePort).store(keyCaptor.capture(),
                    org.mockito.ArgumentMatchers.eq("contenido-del-archivo".getBytes()),
                    org.mockito.ArgumentMatchers.eq("application/pdf"));
            assertThat(keyCaptor.getValue()).startsWith("9/3/firulais-100/")
                    .endsWith("-informe.pdf");
        }

        @Test
        @DisplayName("persiste el archivo con las referencias resueltas por los puertos, no las del comando")
        void persiste_con_las_referencias_resueltas_por_los_puertos() {
            referenciasResueltas();
            when(fileStoragePort.store(any(), any(), any()))
                    .thenReturn(new FileStoragePort.StoredFile(LaboratoryTestFileMother.BUCKET,
                            LaboratoryTestFileMother.STORAGE_KEY, LaboratoryTestFileMother.E_TAG));
            when(repository.save(any())).thenReturn(LaboratoryTestFileMother.archivoValido());

            service.execute(LaboratoryTestFileMother.comandoCrear());

            verify(repository).save(fileCaptor.capture());
            LaboratoryTestFile guardado = fileCaptor.getValue();
            assertThat(guardado.getUploadedBy()).isEqualTo(LaboratoryTestFileMother.VETERINARIO);
            assertThat(guardado.getLaboratoryTest()).isEqualTo(LaboratoryTestFileMother.EXAMEN);
            assertThat(guardado.getBucket()).isEqualTo(LaboratoryTestFileMother.BUCKET);
            assertThat(guardado.getStorageKey()).isEqualTo(LaboratoryTestFileMother.STORAGE_KEY);
            assertThat(guardado.getETag()).isEqualTo(LaboratoryTestFileMother.E_TAG);
            assertThat(guardado.getOriginalFileName())
                    .isEqualTo(LaboratoryTestFileMother.ORIGINAL_FILE_NAME);
            assertThat(guardado.getContentType()).isEqualTo(LaboratoryTestFileMother.CONTENT_TYPE);
            assertThat(guardado.getSizeBytes()).isEqualTo(LaboratoryTestFileMother.SIZE_BYTES);
            assertThat(guardado.getId()).isNull();
        }

        @Test
        @DisplayName("devuelve el DTO del archivo ya persistido")
        void devuelve_el_dto_del_archivo_ya_persistido() {
            referenciasResueltas();
            when(fileStoragePort.store(any(), any(), any()))
                    .thenReturn(new FileStoragePort.StoredFile(LaboratoryTestFileMother.BUCKET,
                            LaboratoryTestFileMother.STORAGE_KEY, LaboratoryTestFileMother.E_TAG));
            when(repository.save(any())).thenReturn(LaboratoryTestFileMother.archivoValido());

            LaboratoryTestFileDto dto = service.execute(LaboratoryTestFileMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(LaboratoryTestFileMother.FILE_ID);
            assertThat(dto.storageKey()).isEqualTo(LaboratoryTestFileMother.STORAGE_KEY);
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("examen inexistente: no consulta ni el empleado ni la ruta, ni sube ni persiste nada")
        void examen_inexistente() {
            when(laboratoryTestQueryPort.findById(LaboratoryTestFileMother.LABORATORY_TEST_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(LaboratoryTestFileMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("LaboratoryTest not found: "
                            + LaboratoryTestFileMother.LABORATORY_TEST_ID);

            verify(laboratoryTestQueryPort, never()).findStoragePath(any());
            verifyNoInteractions(employeeQueryPort, fileStoragePort, repository);
        }

        @Test
        @DisplayName("empleado inexistente: no calcula la ruta de almacenamiento ni sube ni persiste nada")
        void empleado_inexistente() {
            when(laboratoryTestQueryPort.findById(LaboratoryTestFileMother.LABORATORY_TEST_ID))
                    .thenReturn(Optional.of(LaboratoryTestFileMother.EXAMEN));
            when(employeeQueryPort.findById(LaboratoryTestFileMother.EMPLOYEE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(LaboratoryTestFileMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Employee not found: " + LaboratoryTestFileMother.EMPLOYEE_ID);

            verify(laboratoryTestQueryPort, never()).findStoragePath(any());
            verifyNoInteractions(fileStoragePort, repository);
        }

        /**
         * El mensaje tiene que hablar de la ruta, no del examen. Cuando falla
         * {@code findStoragePath} el examen YA se confirmo unas lineas antes: reusar
         * "LaboratoryTest not found" atribuia el fallo a una causa que el propio
         * servicio acababa de descartar, y mandaba a buscar donde no era.
         */
        @Test
        @DisplayName("sin ruta de almacenamiento: no sube ni persiste, y el mensaje culpa a la ruta, no al examen")
        void sin_ruta_de_almacenamiento() {
            when(laboratoryTestQueryPort.findById(LaboratoryTestFileMother.LABORATORY_TEST_ID))
                    .thenReturn(Optional.of(LaboratoryTestFileMother.EXAMEN));
            when(employeeQueryPort.findById(LaboratoryTestFileMother.EMPLOYEE_ID))
                    .thenReturn(Optional.of(LaboratoryTestFileMother.VETERINARIO));
            when(laboratoryTestQueryPort
                    .findStoragePath(LaboratoryTestFileMother.LABORATORY_TEST_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(LaboratoryTestFileMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("storage path could not be resolved")
                    .hasMessageContaining(
                            "laboratoryTestId: " + LaboratoryTestFileMother.LABORATORY_TEST_ID)
                    .hasMessageNotContaining("LaboratoryTest not found: "
                            + LaboratoryTestFileMother.LABORATORY_TEST_ID);

            verifyNoInteractions(fileStoragePort, repository);
        }
    }
}
