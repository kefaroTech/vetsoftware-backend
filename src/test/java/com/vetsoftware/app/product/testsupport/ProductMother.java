package com.vetsoftware.app.product.testsupport;

import com.vetsoftware.app.product.application.command.CreateProductCommand;
import com.vetsoftware.app.product.application.command.SearchProductsCommand;
import com.vetsoftware.app.product.application.command.UpdateProductCommand;
import com.vetsoftware.app.product.domain.CompanyRef;
import com.vetsoftware.app.product.domain.Product;
import com.vetsoftware.app.product.domain.ProductCategoryRef;
import com.vetsoftware.app.product.domain.SupplierRef;
import com.vetsoftware.app.product.domain.TaxRef;
import com.vetsoftware.app.product.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo product.
 *
 * <p>
 * Los productos se construyen con el constructor publico y no con
 * {@code Product.create(...)}: el factory pone {@code LocalDateTime.now()} y
 * haria no deterministas las aserciones sobre {@code createdDate}.
 */
public final class ProductMother {

    public static final Long PRODUCT_ID = 55L;
    public static final Long COMPANY_ID = 9L;

    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");
    public static final ProductCategoryRef CATEGORIA = new ProductCategoryRef(3L, "Alimentos");
    public static final ProductCategoryRef OTRA_CATEGORIA = new ProductCategoryRef(33L,
            "Medicamentos");
    public static final TaxRef IVA_19 = new TaxRef(4L, "IVA 19%", new BigDecimal("19.00"));
    public static final TaxRef IVA_5 = new TaxRef(44L, "IVA 5%", new BigDecimal("5.00"));
    public static final SupplierRef PROVEEDOR = new SupplierRef(6L, "Distribuidora Sur");
    public static final SupplierRef OTRO_PROVEEDOR = new SupplierRef(66L, "Mayorista Andes");

    public static final BigDecimal PRECIO = new BigDecimal("12500.00");
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private ProductMother() {
    }

    /** Producto GRAVADO con impuesto y proveedor. El caso por defecto. */
    public static Product gravado() {
        return gravado(PRODUCT_ID);
    }

    public static Product gravado(Long id) {
        return new Product(id, "Concentrado adulto", "P-001", PRECIO, "94", "Proveedor texto",
                PROVEEDOR, TaxTreatment.GRAVADO, "Bulto de 15 kg", CATEGORIA, IVA_19, CLINICA,
                CREADO, null, null, 0L, true);
    }

    /** Producto EXCLUIDO: sin impuesto ni proveedor asociado. */
    public static Product excluido() {
        return new Product(PRODUCT_ID, "Vacuna social", "P-002", new BigDecimal("0.00"), "94", null,
                null, TaxTreatment.EXCLUIDO, null, CATEGORIA, null, CLINICA, CREADO, null, null, 0L,
                true);
    }

    public static Product deshabilitado() {
        return new Product(PRODUCT_ID, "Concentrado adulto", "P-001", PRECIO, "94", null, null,
                TaxTreatment.EXCLUIDO, null, CATEGORIA, null, CLINICA, CREADO, null, null, 0L,
                false);
    }

    /** Comando de creacion coherente con las refs de arriba (GRAVADO con IVA). */
    public static CreateProductCommand comandoCrear() {
        return new CreateProductCommand("Concentrado adulto", "P-001", PRECIO, "94",
                "Proveedor texto", PROVEEDOR.id(), "Bulto de 15 kg", TaxTreatment.GRAVADO,
                CATEGORIA.id(), IVA_19.id(), COMPANY_ID);
    }

    /** Comando de creacion sin impuesto ni proveedor (tratamiento EXCLUIDO). */
    public static CreateProductCommand comandoCrearSinImpuestoNiProveedor() {
        return new CreateProductCommand("Vacuna social", "P-002", new BigDecimal("0.00"), "94",
                null, null, null, TaxTreatment.EXCLUIDO, CATEGORIA.id(), null, COMPANY_ID);
    }

    /**
     * Comando de actualizacion que cambia nombre, categoria, impuesto y proveedor.
     */
    public static UpdateProductCommand comandoActualizar() {
        return new UpdateProductCommand(PRODUCT_ID, "Concentrado senior", "P-009",
                new BigDecimal("18000.00"), "KGM", "Otro proveedor", OTRO_PROVEEDOR.id(),
                "Bulto de 20 kg", TaxTreatment.GRAVADO, OTRA_CATEGORIA.id(), IVA_5.id(), COMPANY_ID,
                77L, 3L);
    }

    public static SearchProductsCommand comandoBuscar() {
        return new SearchProductsCommand(COMPANY_ID, "concentrado", "P-0", CATEGORIA.id(),
                IVA_19.id(), 0, 20);
    }
}
