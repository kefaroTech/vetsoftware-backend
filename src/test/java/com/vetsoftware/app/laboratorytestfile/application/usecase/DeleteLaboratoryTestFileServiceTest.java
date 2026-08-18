package com.vetsoftware.app.laboratorytestfile.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.laboratorytestfile.application.port.out.FileStoragePort;
import com.vetsoftware.app.laboratorytestfile.application.port.out.LaboratoryTestFileRepository;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFileNotFoundException;
import com.vetsoftware.app.laboratorytestfile.testsupport.LaboratoryTestFileMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteLaboratoryTestFileService")
class DeleteLaboratoryTestFileServiceTest {

    private static final Long OTRA_EMPRESA = 77L;

    @Mock
    private LaboratoryTestFileRepository repository;
    @Mock
    private FileStoragePort fileStoragePort;

    private DeleteLaboratoryTestFileService service;

    @org.junit.jupiter.api.BeforeEach
    void montar() {
        service = new DeleteLaboratoryTestFileService(repository, fileStoragePort);
    }

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("borra primero la fila y despues el objeto en S3, con la clave de la fila borrada")
        void borra_primero_la_fila_y_despues_el_objeto_en_s3() {
            when(repository.findByIdAndCompanyId(LaboratoryTestFileMother.FILE_ID,
                    LaboratoryTestFileMother.COMPANY_ID))
                    .thenReturn(Optional.of(LaboratoryTestFileMother.archivoValido()));

            service.execute(LaboratoryTestFileMother.FILE_ID, LaboratoryTestFileMother.COMPANY_ID);

            InOrder orden = Mockito.inOrder(repository, fileStoragePort);
            orden.verify(repository).delete(LaboratoryTestFileMother.FILE_ID);
            orden.verify(fileStoragePort).delete(LaboratoryTestFileMother.STORAGE_KEY);
        }

        @Test
        @DisplayName("sin companyId (actor global) busca por id global antes de borrar")
        void sin_company_id_busca_por_id_global() {
            when(repository.findById(LaboratoryTestFileMother.FILE_ID))
                    .thenReturn(Optional.of(LaboratoryTestFileMother.archivoValido()));

            service.execute(LaboratoryTestFileMother.FILE_ID, null);

            verify(repository).delete(LaboratoryTestFileMother.FILE_ID);
            verify(fileStoragePort).delete(LaboratoryTestFileMother.STORAGE_KEY);
        }
    }

    @Nested
    @DisplayName("archivo inexistente")
    class ArchivoInexistente {

        @Test
        @DisplayName("no toca el repositorio de borrado ni el almacenamiento")
        void no_toca_nada() {
            when(repository.findByIdAndCompanyId(99L, LaboratoryTestFileMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(99L, LaboratoryTestFileMother.COMPANY_ID))
                    .isInstanceOf(LaboratoryTestFileNotFoundException.class)
                    .hasMessageContaining("LaboratoryTestFile not found: 99");

            verify(repository, Mockito.never()).delete(org.mockito.ArgumentMatchers.any());
            verifyNoInteractions(fileStoragePort);
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        /**
         * El borrado arrastra el objeto en S3 y eso no lo deshace ningun rollback: la
         * fila de otra empresa tiene que ser un 404 antes de que se toque nada.
         */
        @Test
        @DisplayName("un archivo de otra empresa no se borra y no se toca S3")
        void archivo_de_otra_empresa_no_se_borra_y_no_toca_s3() {
            when(repository.findByIdAndCompanyId(LaboratoryTestFileMother.FILE_ID, OTRA_EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(LaboratoryTestFileMother.FILE_ID, OTRA_EMPRESA))
                    .isInstanceOf(LaboratoryTestFileNotFoundException.class)
                    .hasMessageContaining("LaboratoryTestFile not found: 700");

            verify(repository, Mockito.never()).delete(org.mockito.ArgumentMatchers.any());
            verifyNoInteractions(fileStoragePort);
        }
    }
}
