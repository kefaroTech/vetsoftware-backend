package com.vetsoftware.app.laboratorytestfile.testsupport;

import com.vetsoftware.app.laboratorytestfile.application.command.CreateLaboratoryTestFileCommand;
import com.vetsoftware.app.laboratorytestfile.domain.EmployeeRef;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFile;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestRef;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestStoragePathRef;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures de la feature laboratorytestfile.
 *
 * <p>
 * Se construyen con el constructor publico de {@link LaboratoryTestFile} y no
 * con {@code LaboratoryTestFile.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 */
public final class LaboratoryTestFileMother {

    public static final Long FILE_ID = 700L;
    public static final Long OTRO_FILE_ID = 701L;
    public static final Long COMPANY_ID = 9L;
    /** Empresa ajena: el tenant contra el que se prueba el aislamiento. */
    public static final Long OTRA_COMPANY_ID = 77L;
    public static final Long LABORATORY_TEST_ID = 500L;
    public static final Long EMPLOYEE_ID = 4L;
    public static final Long OWNER_ID = 3L;
    public static final Long ANIMAL_ID = 100L;

    public static final EmployeeRef VETERINARIO = new EmployeeRef(EMPLOYEE_ID, "EMP-001",
            "Ana Ruiz");
    public static final EmployeeRef OTRO_VETERINARIO = new EmployeeRef(5L, "EMP-002", "Luis Paz");
    public static final LaboratoryTestRef EXAMEN = new LaboratoryTestRef(LABORATORY_TEST_ID,
            LocalDate.of(2026, 1, 15));

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    public static final String STORAGE_KEY = "9/3/firulais-100/11111111-1111-1111-1111-111111111111-informe.pdf";
    public static final String BUCKET = "vetsoftware-lab-files";
    public static final String ORIGINAL_FILE_NAME = "informe.pdf";
    public static final String CONTENT_TYPE = "application/pdf";
    public static final Long SIZE_BYTES = 20480L;
    public static final String E_TAG = "etag-abc123";

    private LaboratoryTestFileMother() {
    }

    /** Archivo valido, tal como quedaria persistido. El caso por defecto. */
    public static LaboratoryTestFile archivoValido() {
        return archivoValido(FILE_ID);
    }

    public static LaboratoryTestFile archivoValido(Long id) {
        return new LaboratoryTestFile(id, STORAGE_KEY, BUCKET, ORIGINAL_FILE_NAME, CONTENT_TYPE,
                SIZE_BYTES, E_TAG, VETERINARIO, EXAMEN, CREADO);
    }

    public static LaboratoryTestStoragePathRef rutaAlmacenamiento() {
        return new LaboratoryTestStoragePathRef(COMPANY_ID, OWNER_ID, ANIMAL_ID, "Firulais");
    }

    public static CreateLaboratoryTestFileCommand comandoCrear() {
        return comandoCrear(ORIGINAL_FILE_NAME, CONTENT_TYPE);
    }

    public static CreateLaboratoryTestFileCommand comandoCrear(String originalFileName,
            String contentType) {
        return comandoCrear(originalFileName, contentType, COMPANY_ID);
    }

    /** El mismo comando dirigido a otra empresa: el caso de fuga entre tenants. */
    public static CreateLaboratoryTestFileCommand comandoCrear(Long companyId) {
        return comandoCrear(ORIGINAL_FILE_NAME, CONTENT_TYPE, companyId);
    }

    public static CreateLaboratoryTestFileCommand comandoCrear(String originalFileName,
            String contentType, Long companyId) {
        return new CreateLaboratoryTestFileCommand(LABORATORY_TEST_ID, originalFileName,
                contentType, SIZE_BYTES, "contenido-del-archivo".getBytes(), EMPLOYEE_ID,
                companyId);
    }
}
