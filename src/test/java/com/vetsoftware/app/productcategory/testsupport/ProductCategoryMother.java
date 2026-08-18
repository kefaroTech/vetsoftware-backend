package com.vetsoftware.app.productcategory.testsupport;

import com.vetsoftware.app.productcategory.application.command.CreateProductCategoryCommand;
import com.vetsoftware.app.productcategory.application.command.UpdateProductCategoryCommand;
import com.vetsoftware.app.productcategory.domain.CompanyRef;
import com.vetsoftware.app.productcategory.domain.ProductCategory;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo productcategory.
 *
 * <p>
 * Las categorias se construyen con el constructor publico y no con
 * {@code ProductCategory.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 */
public final class ProductCategoryMother {

    public static final Long CATEGORY_ID = 70L;
    public static final Long COMPANY_ID = 9L;
    public static final Long OTRA_COMPANY_ID = 99L;
    public static final Long EMPLOYEE_ID = 4L;

    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");
    public static final CompanyRef OTRA_CLINICA = new CompanyRef(OTRA_COMPANY_ID, "Clinica Sur",
            "NIT-990");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private ProductCategoryMother() {
    }

    /** Categoria activa, habilitada. El caso por defecto. */
    public static ProductCategory activa() {
        return activa(CATEGORY_ID);
    }

    public static ProductCategory activa(Long id) {
        return new ProductCategory(id, "Medicamentos", "Categoria de medicamentos", CLINICA, CREADO,
                null, null, 0L, true);
    }

    public static ProductCategory deshabilitada() {
        return new ProductCategory(CATEGORY_ID, "Medicamentos", "Categoria de medicamentos",
                CLINICA, CREADO, null, null, 0L, false);
    }

    /** Comando de creacion coherente con las refs de arriba. */
    public static CreateProductCategoryCommand comandoCrear() {
        return new CreateProductCategoryCommand("Medicamentos", "Categoria de medicamentos",
                COMPANY_ID);
    }

    /** Comando de actualizacion que cambia nombre y descripcion. */
    public static UpdateProductCategoryCommand comandoActualizar() {
        return new UpdateProductCategoryCommand(CATEGORY_ID, "Insumos", "Categoria de insumos",
                COMPANY_ID, EMPLOYEE_ID, 3L);
    }
}
