package com.vetsoftware.app.laboratorytestfile.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import org.springframework.dao.DataIntegrityViolationException;

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
        when(laboratoryTestQueryPort.findByIdAndCompanyId(
                LaboratoryTestFileMother.LABORATORY_TEST_ID, LaboratoryTestFileMother.COMPANY_ID))
                .thenReturn(Optional.of(LaboratoryTestFileMother.EXAMEN));
        when(employeeQueryPort.findById(LaboratoryTestFileMother.EMPLOYEE_ID))
                .thenReturn(Optional.of(LaboratoryTestFileMother.VETERINARIO));
        when(laboratoryTestQueryPort.findStoragePath(LaboratoryTestFileMother.LABORATORY_TEST_ID,
                LaboratoryTestFileMother.COMPANY_ID))
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
            when(laboratoryTestQueryPort.findByIdAndCompanyId(
                    LaboratoryTestFileMother.LABORATORY_TEST_ID,
                    LaboratoryTestFileMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(LaboratoryTestFileMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("LaboratoryTest not found: "
                            + LaboratoryTestFileMother.LABORATORY_TEST_ID);

            verify(laboratoryTestQueryPort, never()).findStoragePath(any(), any());
            verifyNoInteractions(employeeQueryPort, fileStoragePort, repository);
        }

        @Test
        @DisplayName("empleado inexistente: no calcula la ruta de almacenamiento ni sube ni persiste nada")
        void empleado_inexistente() {
            when(laboratoryTestQueryPort.findByIdAndCompanyId(
                    LaboratoryTestFileMother.LABORATORY_TEST_ID,
                    LaboratoryTestFileMother.COMPANY_ID))
                    .thenReturn(Optional.of(LaboratoryTestFileMother.EXAMEN));
            when(employeeQueryPort.findById(LaboratoryTestFileMother.EMPLOYEE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(LaboratoryTestFileMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Employee not found: " + LaboratoryTestFileMother.EMPLOYEE_ID);

            verify(laboratoryTestQueryPort, never()).findStoragePath(any(), any());
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
            when(laboratoryTestQueryPort.findByIdAndCompanyId(
                    LaboratoryTestFileMother.LABORATORY_TEST_ID,
                    LaboratoryTestFileMother.COMPANY_ID))
                    .thenReturn(Optional.of(LaboratoryTestFileMother.EXAMEN));
            when(employeeQueryPort.findById(LaboratoryTestFileMother.EMPLOYEE_ID))
                    .thenReturn(Optional.of(LaboratoryTestFileMother.VETERINARIO));
            when(laboratoryTestQueryPort.findStoragePath(
                    LaboratoryTestFileMother.LABORATORY_TEST_ID,
                    LaboratoryTestFileMother.COMPANY_ID)).thenReturn(Optional.empty());

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

    /**
     * El objeto sube a S3 ANTES de que exista la fila que lo referencia, y eso es
     * deliberado (ver el javadoc del servicio: meter la subida en la transaccion
     * romperia SIN_IO_EXTERNO_EN_TRANSACCION y el pool). La contrapartida es la
     * compensacion: si la fila no llega a grabarse, lo que queda en el bucket no es
     * un fichero de mas, son datos clinicos de un paciente fuera de todo inventario
     * — ninguna fila los nombra, asi que ningun proceso de borrado los alcanza,
     * incluido el borrado a peticion del titular.
     */
    @Nested
    @DisplayName("compensacion cuando la fila no llega a grabarse")
    class CompensacionDeLaSubida {

        /**
         * El escenario del issue #136: borraron el examen mientras se subia el fichero.
         */
        private DataIntegrityViolationException examenBorradoMientrasSubia() {
            return new DataIntegrityViolationException(
                    "Cannot add or update a child row: a foreign key constraint fails"
                            + " (`laboratory_test_file`.`fk_lab_test_file_lab_test`)");
        }

        private void subidaCompletada() {
            referenciasResueltas();
            when(fileStoragePort.store(any(), any(), any()))
                    .thenReturn(new FileStoragePort.StoredFile(LaboratoryTestFileMother.BUCKET,
                            LaboratoryTestFileMother.STORAGE_KEY, LaboratoryTestFileMother.E_TAG));
        }

        @Test
        @DisplayName("si falla el save, borra de S3 el objeto recien subido con la clave que devolvio el store")
        void borra_de_s3_el_objeto_recien_subido() {
            subidaCompletada();
            when(repository.save(any())).thenThrow(examenBorradoMientrasSubia());

            assertThatThrownBy(() -> service.execute(LaboratoryTestFileMother.comandoCrear()))
                    .isInstanceOf(DataIntegrityViolationException.class);

            ArgumentCaptor<String> borradaCaptor = ArgumentCaptor.forClass(String.class);
            verify(fileStoragePort).delete(borradaCaptor.capture());
            assertThat(borradaCaptor.getValue()).isEqualTo(LaboratoryTestFileMother.STORAGE_KEY);
        }

        /**
         * La clave con la que se borra es la que devolvio el {@code store}, no la que
         * se le paso: el contrato del puerto permite que el almacenamiento normalice la
         * clave, y borrar la calculada dejaria vivo el objeto real.
         */
        @Test
        @DisplayName("borra la clave que devolvio el almacenamiento, no la que se calculo para subir")
        void borra_la_clave_devuelta_y_no_la_calculada() {
            subidaCompletada();
            when(repository.save(any())).thenThrow(examenBorradoMientrasSubia());

            assertThatThrownBy(() -> service.execute(LaboratoryTestFileMother.comandoCrear()))
                    .isInstanceOf(DataIntegrityViolationException.class);

            ArgumentCaptor<String> subidaCaptor = ArgumentCaptor.forClass(String.class);
            verify(fileStoragePort).store(subidaCaptor.capture(), any(), any());
            ArgumentCaptor<String> borradaCaptor = ArgumentCaptor.forClass(String.class);
            verify(fileStoragePort).delete(borradaCaptor.capture());
            assertThat(borradaCaptor.getValue()).isEqualTo(LaboratoryTestFileMother.STORAGE_KEY)
                    .isNotEqualTo(subidaCaptor.getValue());
        }

        /**
         * Sin envolver: el cliente tiene que ver el error que explica que paso (aqui,
         * el 409 que da {@code GlobalExceptionHandler} a una violacion de integridad),
         * y no un error de la compensacion.
         */
        @Test
        @DisplayName("relanza la excepcion original del save, sin envolverla y sin suprimidas")
        void relanza_la_excepcion_original_sin_envolverla() {
            subidaCompletada();
            DataIntegrityViolationException falloAlGuardar = examenBorradoMientrasSubia();
            when(repository.save(any())).thenThrow(falloAlGuardar);

            assertThatThrownBy(() -> service.execute(LaboratoryTestFileMother.comandoCrear()))
                    .isSameAs(falloAlGuardar).isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("foreign key constraint fails")
                    .hasNoSuppressedExceptions();
        }

        /**
         * El peor caso: el objeto se queda huerfano igualmente. Lo unico que no puede
         * pasar es que el fallo del borrado tape la causa real, porque entonces el
         * cliente veria un error de S3 en vez del motivo por el que fallo su operacion.
         * El fallo secundario viaja como {@code suppressed} para que quede en el
         * rastro.
         */
        @Test
        @DisplayName("si el borrado tambien falla, sale la excepcion original con el fallo del borrado como suprimido")
        void el_fallo_del_borrado_no_tapa_la_causa_real() {
            subidaCompletada();
            DataIntegrityViolationException falloAlGuardar = examenBorradoMientrasSubia();
            when(repository.save(any())).thenThrow(falloAlGuardar);
            IllegalStateException falloDelBorrado = new IllegalStateException(
                    "S3 no disponible: connection reset");
            doThrow(falloDelBorrado).when(fileStoragePort)
                    .delete(LaboratoryTestFileMother.STORAGE_KEY);

            assertThatThrownBy(() -> service.execute(LaboratoryTestFileMother.comandoCrear()))
                    .isSameAs(falloAlGuardar).isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("foreign key constraint fails")
                    .hasSuppressedException(falloDelBorrado);
        }

        /**
         * Un {@code delete} de mas borraria un adjunto que si se guardo bien: la fila
         * apuntaria a un objeto que ya no esta y el informe seria indescargable.
         */
        @Test
        @DisplayName("cuando el save va bien no se borra nada del almacenamiento")
        void el_camino_feliz_no_borra_nada() {
            subidaCompletada();
            when(repository.save(any())).thenReturn(LaboratoryTestFileMother.archivoValido());

            service.execute(LaboratoryTestFileMother.comandoCrear());

            verify(fileStoragePort).store(any(), any(), any());
            verifyNoMoreInteractions(fileStoragePort);
        }
    }

    /**
     * La otra mitad del defecto de aislamiento: no se trata de apropiarse de un
     * adjunto ajeno —la carga propia ya esta acotada— sino de colgar un adjunto
     * PROPIO del examen de otra empresa. El adjunto de laboratorio es el caso mas
     * caro de los cinco, porque lo que queda colgado del examen ajeno es un fichero
     * descargable por {@code GET /laboratory-test-files/id/download}, y porque el
     * objeto se sube a S3 <b>fuera</b> de toda transaccion: si la referencia se
     * resolviese sin acotar, el fichero estaria en el bucket bajo el prefijo de la
     * otra empresa aunque la fila fallase despues.
     */
    @Nested
    @DisplayName("aislamiento entre empresas")
    class AislamientoEntreEmpresas {

        @Test
        @DisplayName("un examen de otra empresa no se resuelve: nada se sube ni se persiste")
        void examen_de_otra_empresa_no_sube_ni_persiste() {
            when(laboratoryTestQueryPort.findByIdAndCompanyId(
                    LaboratoryTestFileMother.LABORATORY_TEST_ID,
                    LaboratoryTestFileMother.OTRA_COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(LaboratoryTestFileMother
                    .comandoCrear(LaboratoryTestFileMother.OTRA_COMPANY_ID)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("LaboratoryTest not found: "
                            + LaboratoryTestFileMother.LABORATORY_TEST_ID);

            verify(laboratoryTestQueryPort, never()).findStoragePath(any(), any());
            verifyNoInteractions(employeeQueryPort, fileStoragePort, repository);
        }

        /**
         * La ruta lleva dentro el id de empresa, el del propietario y el nombre del
         * animal: resolverla sin acotar seria una fuga por si misma —revelaria de quien
         * es un examen ajeno— y ademas construiria la clave de S3 bajo el prefijo del
         * otro tenant.
         */
        @Test
        @DisplayName("la ruta de almacenamiento tambien se resuelve acotada por la empresa del comando")
        void la_ruta_de_almacenamiento_se_resuelve_acotada() {
            referenciasResueltas();
            when(fileStoragePort.store(any(), any(), any()))
                    .thenReturn(new FileStoragePort.StoredFile(LaboratoryTestFileMother.BUCKET,
                            LaboratoryTestFileMother.STORAGE_KEY, LaboratoryTestFileMother.E_TAG));
            when(repository.save(any())).thenReturn(LaboratoryTestFileMother.archivoValido());

            service.execute(LaboratoryTestFileMother.comandoCrear());

            verify(laboratoryTestQueryPort).findByIdAndCompanyId(
                    LaboratoryTestFileMother.LABORATORY_TEST_ID,
                    LaboratoryTestFileMother.COMPANY_ID);
            verify(laboratoryTestQueryPort).findStoragePath(
                    LaboratoryTestFileMother.LABORATORY_TEST_ID,
                    LaboratoryTestFileMother.COMPANY_ID);
        }
    }
}
