package com.vetsoftware.app.laboratorytestfile.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFile;
import com.vetsoftware.app.laboratorytestfile.testsupport.LaboratoryTestFileMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LaboratoryTestFileDto — from()")
class LaboratoryTestFileDtoTest {

    @Test
    @DisplayName("copia cada campo del agregado, incluidas las referencias companion")
    void copia_cada_campo_del_agregado() {
        LaboratoryTestFile file = LaboratoryTestFileMother.archivoValido();

        LaboratoryTestFileDto dto = LaboratoryTestFileDto.from(file);

        assertThat(dto.id()).isEqualTo(file.getId());
        assertThat(dto.storageKey()).isEqualTo(file.getStorageKey());
        assertThat(dto.bucket()).isEqualTo(file.getBucket());
        assertThat(dto.originalFileName()).isEqualTo(file.getOriginalFileName());
        assertThat(dto.contentType()).isEqualTo(file.getContentType());
        assertThat(dto.sizeBytes()).isEqualTo(file.getSizeBytes());
        assertThat(dto.eTag()).isEqualTo(file.getETag());
        assertThat(dto.createdDate()).isEqualTo(file.getCreatedDate());
        assertThat(dto.uploadedBy())
                .isEqualTo(EmployeeSummaryDto.from(LaboratoryTestFileMother.VETERINARIO));
        assertThat(dto.laboratoryTest())
                .isEqualTo(LaboratoryTestSummaryDto.from(LaboratoryTestFileMother.EXAMEN));
    }
}
