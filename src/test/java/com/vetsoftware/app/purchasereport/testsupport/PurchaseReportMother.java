package com.vetsoftware.app.purchasereport.testsupport;

import com.vetsoftware.app.purchasereport.application.dto.PurchaseBookDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Fixtures del libro de compras (F4): un DTO valido por defecto y variantes por
 * escenario, para no repetir los mismos records en cada test.
 */
public final class PurchaseReportMother {

    public static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    public static final LocalDate HASTA = LocalDate.of(2026, 1, 31);

    private PurchaseReportMother() {
    }

    public static PurchaseBookDto.EntryDto entradaCompleta() {
        return new PurchaseBookDto.EntryDto(1L, "Distribuidora Sur", "900123456", "FC-100",
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 10), new BigDecimal("100000.00"),
                new BigDecimal("19000.00"), BigDecimal.ZERO, new BigDecimal("119000.00"),
                new BigDecimal("50000.00"), new BigDecimal("69000.00"), "PARCIAL");
    }

    public static PurchaseBookDto.EntryDto entradaConProveedor(String proveedor) {
        return new PurchaseBookDto.EntryDto(1L, proveedor, "900123456", "FC-100",
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 10), new BigDecimal("100000.00"),
                new BigDecimal("19000.00"), BigDecimal.ZERO, new BigDecimal("119000.00"),
                new BigDecimal("50000.00"), new BigDecimal("69000.00"), "PARCIAL");
    }

    public static PurchaseBookDto.EntryDto entradaSinNit() {
        return new PurchaseBookDto.EntryDto(1L, "Distribuidora Sur", null, "FC-100",
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 10), new BigDecimal("100000.00"),
                new BigDecimal("19000.00"), BigDecimal.ZERO, new BigDecimal("119000.00"),
                new BigDecimal("50000.00"), new BigDecimal("69000.00"), "PARCIAL");
    }

    public static PurchaseBookDto.EntryDto entradaSinFechas() {
        return new PurchaseBookDto.EntryDto(1L, "Distribuidora Sur", "900123456", "FC-100", null,
                null, new BigDecimal("100000.00"), new BigDecimal("19000.00"), BigDecimal.ZERO,
                new BigDecimal("119000.00"), new BigDecimal("50000.00"), new BigDecimal("69000.00"),
                "PARCIAL");
    }

    public static PurchaseBookDto.EntryDto entradaConMontosNulos() {
        return new PurchaseBookDto.EntryDto(1L, "Distribuidora Sur", "900123456", "FC-100",
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 10), null, null, null, null, null,
                null, "PARCIAL");
    }

    public static PurchaseBookDto.TotalsDto totales(long cantidadFacturas) {
        return new PurchaseBookDto.TotalsDto(cantidadFacturas, new BigDecimal("100000.00"),
                new BigDecimal("19000.00"), BigDecimal.ZERO, new BigDecimal("119000.00"),
                new BigDecimal("50000.00"), new BigDecimal("69000.00"));
    }

    public static PurchaseBookDto libro() {
        return libroConEntrada(entradaCompleta());
    }

    public static PurchaseBookDto libroConEntrada(PurchaseBookDto.EntryDto entrada) {
        return new PurchaseBookDto(DESDE, HASTA, List.of(entrada), totales(1));
    }

    public static PurchaseBookDto libroSinRango() {
        return new PurchaseBookDto(null, null, List.of(entradaCompleta()), totales(1));
    }

    public static PurchaseBookDto libroSinCompras() {
        return new PurchaseBookDto(DESDE, HASTA, List.of(),
                new PurchaseBookDto.TotalsDto(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }
}
