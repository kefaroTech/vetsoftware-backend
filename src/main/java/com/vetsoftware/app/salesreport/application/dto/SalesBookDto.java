package com.vetsoftware.app.salesreport.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Libro/registro de ventas (F6): una entrada por documento + desglose de
 * impuesto por tarifa (insumo formulario 300) + recaudo por medio de pago
 * codificado DIAN + totales del periodo.
 *
 * <p>
 * <strong>Por qué los anidados llevan nombre propio en el contrato.</strong>
 * {@code PurchaseBookDto} declara sus propios {@code EntryDto} y
 * {@code TotalsDto}, y springdoc funde los esquemas <em>por nombre simple de
 * clase</em>, no por paquete ni por clase contenedora. Las formas son
 * <strong>disjuntas</strong>, así que la fusión no degradaba un campo:
 * publicaba <em>la forma de un solo libro</em> bajo los dos endpoints. Este
 * lado ganaba por orden de escaneo, y
 * {@code GET /purchase-reports/purchase-book} anunciaba estas entradas de
 * venta.
 *
 * <p>
 * <strong>Por qué también se escapa este lado, que era el que ganaba.</strong>
 * El criterio de {@code RegisterOutageAffectedCompanyRequest} y de
 * {@code IssueElectronicCreditNoteRequest} —mover el lado que <em>no</em>
 * publica hoy el nombre simple— existe para no cambiar un esquema bajo un
 * consumidor vivo, y aquí <b>no lo hay</b>: los dos {@code api.contract.ts} no
 * atan ni {@code 'EntryDto'} ni {@code 'TotalsDto'}, y no pueden, porque su
 * {@code MismatchedFields} filtra por
 * {@code type Comparable = string | number | boolean} y los objetos anidados se
 * saltan por diseño. El
 * {@code MatchesContract<SalesBookResponse, 'SalesBookDto'>} sigue verde con o
 * sin este cambio, porque nunca miró dentro. Sin atadura que proteger, dejar
 * {@code EntryDto} y {@code TotalsDto} —dos sustantivos genéricos que no
 * identifican rodaja— ocupando el nombre simple solo serviría para que la
 * colisión se rearmara con la siguiente rodaja que publique un libro.
 *
 * <p>
 * <strong>Gemelo:</strong>
 * {@code purchasereport.application.dto.PurchaseBookDto} lleva el pacto
 * simétrico. Si mueves un nombre aquí, muévelo allí.
 *
 * <p>
 * {@code TaxByRateDto} y {@code RecaudoDto} se quedan con su nombre simple a
 * propósito: hoy no tienen homónimo en el contrato, y renombrarlos movería
 * esquemas sin arreglar nada.
 *
 * <p>
 * El nombre de la clase Java <strong>no</strong> cambia: lo que colisiona es el
 * nombre publicado, no el símbolo.
 */
public record SalesBookDto(LocalDate dateFrom, LocalDate dateTo, List<EntryDto> entries,
        List<TaxByRateDto> taxByRate, List<RecaudoDto> recaudoByMeans, TotalsDto totals) {
    @Schema(name = "SalesBookEntryDto")
    public record EntryDto(Long id, String documentType, String prefix, Long consecutive,
            LocalDate issueDate, String customerDocumentId, String customerName, BigDecimal base,
            BigDecimal iva, BigDecimal inc, BigDecimal total, BigDecimal payable,
            BigDecimal reteFuente, BigDecimal reteIva, BigDecimal reteIca, String dianStatus,
            String cufe, String cude) {
    }

    public record TaxByRateDto(String taxScheme, BigDecimal taxRate, BigDecimal taxableAmount,
            BigDecimal taxAmount) {
    }

    public record RecaudoDto(String paymentMeans, String dianCode, BigDecimal amount) {
    }

    @Schema(name = "SalesBookTotalsDto")
    public record TotalsDto(long documentCount, BigDecimal base, BigDecimal iva, BigDecimal inc,
            BigDecimal total, BigDecimal payable, BigDecimal reteFuente, BigDecimal reteIva,
            BigDecimal reteIca) {
    }
}
