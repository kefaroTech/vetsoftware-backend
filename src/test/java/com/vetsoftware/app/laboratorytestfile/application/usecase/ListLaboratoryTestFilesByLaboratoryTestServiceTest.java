package com.vetsoftware.app.laboratorytestfile.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDto;
import com.vetsoftware.app.laboratorytestfile.application.port.out.LaboratoryTestFileRepository;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFile;
import com.vetsoftware.app.laboratorytestfile.testsupport.LaboratoryTestFileMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListLaboratoryTestFilesByLaboratoryTestService")
class ListLaboratoryTestFilesByLaboratoryTestServiceTest {

    @Mock
    private LaboratoryTestFileRepository repository;

    @InjectMocks
    private ListLaboratoryTestFilesByLaboratoryTestService service;

    @Test
    @DisplayName("devuelve los archivos del examen acotados por empresa, mapeados a DTO")
    void devuelve_los_archivos_mapeados_a_dto() {
        LaboratoryTestFile primero = LaboratoryTestFileMother
                .archivoValido(LaboratoryTestFileMother.FILE_ID);
        LaboratoryTestFile segundo = LaboratoryTestFileMother
                .archivoValido(LaboratoryTestFileMother.OTRO_FILE_ID);
        when(repository.findAllByLaboratoryTestIdAndCompanyId(
                LaboratoryTestFileMother.LABORATORY_TEST_ID, LaboratoryTestFileMother.COMPANY_ID))
                .thenReturn(List.of(primero, segundo));

        List<LaboratoryTestFileDto> resultado = service.listByLaboratoryTest(
                LaboratoryTestFileMother.LABORATORY_TEST_ID, LaboratoryTestFileMother.COMPANY_ID);

        assertThat(resultado).extracting(LaboratoryTestFileDto::id).containsExactly(
                LaboratoryTestFileMother.FILE_ID, LaboratoryTestFileMother.OTRO_FILE_ID);
    }

    @Test
    @DisplayName("un examen sin archivos devuelve una lista vacia")
    void examen_sin_archivos_devuelve_lista_vacia() {
        when(repository.findAllByLaboratoryTestIdAndCompanyId(
                LaboratoryTestFileMother.LABORATORY_TEST_ID, LaboratoryTestFileMother.COMPANY_ID))
                .thenReturn(List.of());

        List<LaboratoryTestFileDto> resultado = service.listByLaboratoryTest(
                LaboratoryTestFileMother.LABORATORY_TEST_ID, LaboratoryTestFileMother.COMPANY_ID);

        assertThat(resultado).isEmpty();
    }
}
