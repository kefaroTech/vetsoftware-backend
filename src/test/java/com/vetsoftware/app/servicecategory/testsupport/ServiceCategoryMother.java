package com.vetsoftware.app.servicecategory.testsupport;

import com.vetsoftware.app.servicecategory.application.command.CreateServiceCategoryCommand;
import com.vetsoftware.app.servicecategory.application.command.UpdateServiceCategoryCommand;
import com.vetsoftware.app.servicecategory.domain.CompanyRef;
import com.vetsoftware.app.servicecategory.domain.ServiceCategory;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo servicecategory.
 *
 * <p>
 * Las categorias se construyen con el constructor publico y no con
 * {@code ServiceCategory.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 */
public final class ServiceCategoryMother {

    public static final Long CATEGORY_ID = 70L;
    public static final Long COMPANY_ID = 9L;
    public static final Long OTRA_COMPANY_ID = 99L;
    public static final Long EMPLOYEE_ID = 4L;

    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");
    public static final CompanyRef OTRA_CLINICA = new CompanyRef(OTRA_COMPANY_ID, "Clinica Sur",
            "NIT-990");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private ServiceCategoryMother() {
    }

    /** Categoria activa, habilitada. El caso por defecto. */
    public static ServiceCategory activa() {
        return activa(CATEGORY_ID);
    }

    public static ServiceCategory activa(Long id) {
        return new ServiceCategory(id, "Consultas", "Categoria de consultas", CLINICA, CREADO, null,
                null, 0L, true);
    }

    public static ServiceCategory deshabilitada() {
        return new ServiceCategory(CATEGORY_ID, "Consultas", "Categoria de consultas", CLINICA,
                CREADO, null, null, 0L, false);
    }

    /** Comando de creacion coherente con las refs de arriba. */
    public static CreateServiceCategoryCommand comandoCrear() {
        return new CreateServiceCategoryCommand("Consultas", "Categoria de consultas", COMPANY_ID);
    }

    /** Comando de actualizacion que cambia nombre y descripcion. */
    public static UpdateServiceCategoryCommand comandoActualizar() {
        return new UpdateServiceCategoryCommand(CATEGORY_ID, "Cirugias", "Categoria de cirugias",
                COMPANY_ID, EMPLOYEE_ID, 3L);
    }
}
