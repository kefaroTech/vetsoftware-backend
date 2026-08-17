package com.vetsoftware.app.supplier.testsupport;

import com.vetsoftware.app.supplier.domain.CompanyRef;
import com.vetsoftware.app.supplier.domain.Supplier;
import java.time.LocalDateTime;

/**
 * Object mother de la feature {@code supplier}: proveedores validos por
 * defecto, con las fechas fijas.
 *
 * <p>
 * Todas las variantes devuelven un proveedor <b>nuevo</b> —{@code id} y
 * {@code version} en {@code null}— porque es lo que espera el adaptador de
 * persistencia para insertar. No se usa {@link Supplier#create} a proposito:
 * ese factory llama a {@code LocalDateTime.now()} y dejaria el
 * {@code createdDate} fuera del control del test, que es justo el campo por el
 * que ordena el {@code search}.
 */
public final class SupplierMother {

    /** Fecha de creacion por defecto. Fija: el search ordena por ella. */
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private SupplierMother() {
    }

    /** Referencia a la empresa duena del proveedor. */
    public static CompanyRef empresa(Long id, String nombre, String identificador) {
        return new CompanyRef(id, nombre, identificador);
    }

    /** Proveedor nuevo con todos los campos opcionales informados. */
    public static Supplier completo(String nombre, CompanyRef empresa) {
        return completo(nombre, empresa, CREADO);
    }

    /** Igual, fijando la fecha de creacion (para ordenar y paginar). */
    public static Supplier completo(String nombre, CompanyRef empresa, LocalDateTime creado) {
        return new Supplier(null, nombre, "901555444-1", "Marta Gil", "3001234567",
                "compras@sur.test", "Calle 10 # 5-20", 30, "Entrega los martes", empresa, creado,
                null, null, null, true);
    }

    /** Proveedor nuevo con un NIT propio, para el filtro por {@code taxId}. */
    public static Supplier conNit(String nombre, String nit, CompanyRef empresa) {
        return new Supplier(null, nombre, nit, "Marta Gil", "3001234567", "compras@sur.test",
                "Calle 10 # 5-20", 30, "Entrega los martes", empresa, CREADO, null, null, null,
                true);
    }

    /** Proveedor nuevo con solo lo obligatorio: el resto de campos en null. */
    public static Supplier minimo(String nombre, CompanyRef empresa) {
        return new Supplier(null, nombre, null, null, null, null, null, null, null, empresa, CREADO,
                null, null, null, true);
    }
}
