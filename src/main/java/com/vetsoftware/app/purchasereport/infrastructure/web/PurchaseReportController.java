package com.vetsoftware.app.purchasereport.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.purchasereport.application.dto.PurchaseBookDto;
import com.vetsoftware.app.purchasereport.application.port.in.GetPurchaseBookUseCase;
import com.vetsoftware.app.purchasereport.application.port.out.PurchaseBookPdfPort;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * F4 - libro de compras (reporte contable de solo lectura). Complementa el
 * libro de ventas (punto 16).
 *
 * <p>
 * <strong>Pacto con el libro de ventas.</strong> {@code PurchaseBookDto} sale
 * por HTTP tal cual, sin capa {@code web/response} intermedia, y sus anidados
 * {@code EntryDto} y {@code TotalsDto} tienen gemelos de nombre simple en
 * {@code SalesBookDto}. Springdoc funde por nombre simple, no por clase
 * contenedora: los dos lados llevan {@code @Schema(name = ...)}
 * ({@code PurchaseBookEntryDto} / {@code PurchaseBookTotalsDto} aqui,
 * {@code SalesBookEntryDto} / {@code SalesBookTotalsDto} alla). Si quitas uno,
 * este endpoint vuelve a publicar la forma del otro libro. El razonamiento
 * completo esta en el javadoc de {@code PurchaseBookDto}.
 */
@RestController
@RequestMapping("/purchase-reports")
public class PurchaseReportController {
    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final GetPurchaseBookUseCase purchaseBookUseCase;
    private final PurchaseBookPdfPort purchaseBookPdf;
    private final Authz authz;

    public PurchaseReportController(GetPurchaseBookUseCase purchaseBookUseCase,
            PurchaseBookPdfPort purchaseBookPdf, Authz authz) {
        this.purchaseBookUseCase = purchaseBookUseCase;
        this.purchaseBookPdf = purchaseBookPdf;
        this.authz = authz;
    }

    @GetMapping("/purchase-book")
    public PurchaseBookDto purchaseBook(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "branchId", required = false) Long branchId) {
        return purchaseBookUseCase.get(authz.currentCompanyId(), from, to,
                authz.resolveAccessibleBranch(branchId));
    }

    @GetMapping("/purchase-book/export")
    public ResponseEntity<byte[]> exportPurchaseBook(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "branchId", required = false) Long branchId,
            @RequestParam(defaultValue = "csv") String format) {
        PurchaseBookDto book = purchaseBookUseCase.get(authz.currentCompanyId(), from, to,
                authz.resolveAccessibleBranch(branchId));
        String base = "libro_compras_" + from + "_" + to;
        return "pdf".equalsIgnoreCase(format)
                ? file(purchaseBookPdf.renderPurchaseBook(book), base + ".pdf",
                        MediaType.APPLICATION_PDF)
                : file(PurchaseBookCsv.purchaseBook(book), base + ".csv", CSV);
    }

    private static ResponseEntity<byte[]> file(byte[] body, String filename, MediaType type) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok().contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded)
                .body(body);
    }
}
