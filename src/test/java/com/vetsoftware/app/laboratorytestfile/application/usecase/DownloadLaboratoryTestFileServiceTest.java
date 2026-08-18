package com.vetsoftware.app.laboratorytestfile.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDownloadDto;
import com.vetsoftware.app.laboratorytestfile.application.port.out.FileStoragePort;
import com.vetsoftware.app.laboratorytestfile.application.port.out.LaboratoryTestFileRepository;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFileNotFoundException;
import com.vetsoftware.app.laboratorytestfile.testsupport.LaboratoryTestFileMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DownloadLaboratoryTestFileService")
class DownloadLaboratoryTestFileServiceTest {

    private static final Long OTRA_EMPRESA = 77L;

    @Mock
    private LaboratoryTestFileRepository repository;
    @Mock
    private FileStoragePort fileStoragePort;

    @InjectMocks
    private DownloadLaboratoryTestFileService service;

    @Nested
    @DisplayName("descarga")
    class Descarga {

        @Test
        @DisplayName("recupera el contenido de S3 con la clave del archivo y arma el DTO de descarga")
        void arma_el_dto_de_descarga() {
            when(repository.findByIdAndCompanyId(LaboratoryTestFileMother.FILE_ID,
                    LaboratoryTestFileMother.COMPANY_ID))
                    .thenReturn(Optional.of(LaboratoryTestFileMother.archivoValido()));
            byte[] contenido = "%PDF-contenido".getBytes();
            when(fileStoragePort.retrieve(LaboratoryTestFileMother.STORAGE_KEY))
                    .thenReturn(contenido);

            LaboratoryTestFileDownloadDto dto = service.download(LaboratoryTestFileMother.FILE_ID,
                    LaboratoryTestFileMother.COMPANY_ID);

            assertThat(dto.fileName()).isEqualTo(LaboratoryTestFileMother.ORIGINAL_FILE_NAME);
            assertThat(dto.contentType()).isEqualTo(LaboratoryTestFileMother.CONTENT_TYPE);
            assertThat(dto.content()).isEqualTo(contenido);
        }

        @Test
        @DisplayName("sin companyId (actor global) busca por id global")
        void sin_company_id_busca_por_id_global() {
            when(repository.findById(LaboratoryTestFileMother.FILE_ID))
                    .thenReturn(Optional.of(LaboratoryTestFileMother.archivoValido()));
            byte[] contenido = "%PDF-contenido".getBytes();
            when(fileStoragePort.retrieve(LaboratoryTestFileMother.STORAGE_KEY))
                    .thenReturn(contenido);

            LaboratoryTestFileDownloadDto dto = service.download(LaboratoryTestFileMother.FILE_ID,
                    null);

            assertThat(dto.content()).isEqualTo(contenido);
        }
    }

    @Nested
    @DisplayName("archivo inexistente")
    class ArchivoInexistente {

        @Test
        @DisplayName("no toca el almacenamiento")
        void no_toca_el_almacenamiento() {
            when(repository.findByIdAndCompanyId(99L, LaboratoryTestFileMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.download(99L, LaboratoryTestFileMother.COMPANY_ID))
                    .isInstanceOf(LaboratoryTestFileNotFoundException.class)
                    .hasMessageContaining("LaboratoryTestFile not found: 99");

            verifyNoInteractions(fileStoragePort);
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        /**
         * El caso que da valor a todo el frente: el archivo existe, pero es de otra
         * empresa. La descarga tiene que morir en la lectura, sin llegar a pedirle el
         * objeto a S3 — si llegara, el PDF de la historia clinica ajena ya habria
         * salido.
         */
        @Test
        @DisplayName("un archivo de otra empresa no se descarga y no se toca S3")
        void archivo_de_otra_empresa_no_se_descarga_y_no_toca_s3() {
            when(repository.findByIdAndCompanyId(LaboratoryTestFileMother.FILE_ID, OTRA_EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.download(LaboratoryTestFileMother.FILE_ID, OTRA_EMPRESA))
                    .isInstanceOf(LaboratoryTestFileNotFoundException.class)
                    .hasMessageContaining("LaboratoryTestFile not found: 700");

            verifyNoInteractions(fileStoragePort);
        }
    }
}
