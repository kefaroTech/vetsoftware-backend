package com.vetsoftware.app.purchasereport.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Libro de compras (F4): una entrada por factura de proveedor del periodo +
 * totales. Complementa el libro de ventas fiscal (punto 16). Excluye las
 * facturas anuladas.
 *
 * <p>
 * <strong>Por qué los anidados llevan nombre propio en el contrato.</strong>
 * {@code SalesBookDto} declara sus propios {@code EntryDto} y
 * {@code TotalsDto}, y springdoc funde los esquemas <em>por nombre simple de
 * clase</em>, no por paquete ni por clase contenedora. Las formas son
 * <strong>disjuntas</strong> —aquellas describen un documento fiscal de venta
 * ({@code base}, {@code iva}, {@code inc}, {@code cufe}); estas, una factura de
 * proveedor ({@code supplierName}, {@code invoiceNumber}, {@code subtotal},
 * {@code withholdingAmount}, {@code balance})—, así que la fusión no degradaba
 * un campo: publicaba <em>la forma del otro libro</em>. Ganaba ventas por orden
 * de escaneo, y {@code GET /purchase-reports/purchase-book} anunciaba las
 * entradas del libro de ventas. Diez campos de este libro
 * —{@code supplierName}, {@code supplierTaxId}, {@code invoiceNumber},
 * {@code dueDate}, {@code subtotal}, {@code taxAmount},
 * {@code withholdingAmount}, {@code paidAmount}, {@code balance},
 * {@code status}— y el {@code invoiceCount} de los totales no aparecían en
 * ninguna parte del {@code openapi.json}.
 *
 * <p>
 * <strong>Por qué se escapan los dos lados y no solo este.</strong> El criterio
 * de {@code RegisterOutageAffectedCompanyRequest} y de
 * {@code IssueElectronicCreditNoteRequest} —mover el lado que <em>no</em>
 * publica hoy el nombre simple, para no cambiar el esquema bajo un consumidor
 * vivo— aquí no decide nada: <b>no hay consumidor vivo de estos nombres</b>.
 * Los dos {@code api.contract.ts} no atan ni {@code 'EntryDto'} ni
 * {@code 'TotalsDto'}, y no pueden: su {@code MismatchedFields} filtra por
 * {@code type Comparable = string | number | boolean}, así que los objetos
 * anidados se saltan por diseño. El
 * {@code MatchesContract<PurchaseBook, 'PurchaseBookDto'>} pasaba en verde
 * precisamente por eso. Sin atadura que proteger, lo que decide es el nombre:
 * {@code EntryDto} y {@code TotalsDto} son sustantivos genéricos que no
 * pertenecen a esta rodaja más que a la otra, y dejar uno con el nombre simple
 * en un contrato de más de 600 esquemas <em>reabre la colisión</em> en cuanto
 * una tercera rodaja declare su propio libro. Escapados los dos, el nombre
 * simple queda libre y ninguna rodaja lo reclama.
 *
 * <p>
 * <strong>Gemelo:</strong> {@code salesreport.application.dto.SalesBookDto}
 * lleva el pacto simétrico. Si mueves un nombre aquí, muévelo allí: el fallo se
 * rearma solo en cuanto uno de los dos lados deje de estar escapado.
 *
 * <p>
 * El nombre de la clase Java <strong>no</strong> cambia: lo que colisiona es el
 * nombre publicado, no el símbolo.
 */
public record PurchaseBookDto(LocalDate dateFrom, LocalDate dateTo, List<EntryDto> entries,
        TotalsDto totals) {
    @Schema(name = "PurchaseBookEntryDto")
    public record EntryDto(Long id, String supplierName, String supplierTaxId, String invoiceNumber,
            LocalDate issueDate, LocalDate dueDate, BigDecimal subtotal, BigDecimal taxAmount,
            BigDecimal withholdingAmount, BigDecimal total, BigDecimal paidAmount,
            BigDecimal balance, String status) {
    }

    @Schema(name = "PurchaseBookTotalsDto")
    public record TotalsDto(long invoiceCount, BigDecimal subtotal, BigDecimal taxAmount,
            BigDecimal withholdingAmount, BigDecimal total, BigDecimal paidAmount,
            BigDecimal balance) {
    }
}
