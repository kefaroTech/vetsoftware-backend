package com.vetsoftware.app.cashregister.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.cashregister.application.command.CloseCashSessionCommand;
import com.vetsoftware.app.cashregister.application.command.OpenCashSessionCommand;
import com.vetsoftware.app.cashregister.application.command.RegisterCashMovementCommand;
import com.vetsoftware.app.cashregister.application.command.SearchCashSessionsQuery;
import com.vetsoftware.app.cashregister.application.dto.CashArqueoReport;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.application.dto.PageResult;
import com.vetsoftware.app.cashregister.application.port.in.CloseCashSessionUseCase;
import com.vetsoftware.app.cashregister.application.port.in.ExportArqueoUseCase;
import com.vetsoftware.app.cashregister.application.port.in.GetCashSessionUseCase;
import com.vetsoftware.app.cashregister.application.port.in.GetCurrentCashSessionUseCase;
import com.vetsoftware.app.cashregister.application.port.in.ListCashSessionsUseCase;
import com.vetsoftware.app.cashregister.application.port.in.ListOpenCashSessionsUseCase;
import com.vetsoftware.app.cashregister.application.port.in.OpenCashSessionUseCase;
import com.vetsoftware.app.cashregister.application.port.in.RegisterCashMovementUseCase;
import com.vetsoftware.app.cashregister.application.port.out.CashReportPdfPort;
import com.vetsoftware.app.cashregister.infrastructure.web.request.CloseCashSessionRequest;
import com.vetsoftware.app.cashregister.infrastructure.web.request.OpenCashSessionRequest;
import com.vetsoftware.app.cashregister.infrastructure.web.request.RegisterCashMovementRequest;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Caja / arqueo por sede (punto 5). Apertura/movimiento manual con {@code cashregister.operate};
 * cierre con {@code cashregister.close}; lecturas con {@code cashregister.read} (gate en
 * los @PreAuthorize de los puertos). La sede se resuelve por el alcance del empleado con {@link
 * Authz#resolveAccessibleBranch}. Los movimientos de venta/ abono/reversa NO entran por aquí: los
 * inyecta la orquestación (POS / cuenta abierta).
 */
@RestController
@RequestMapping("/cash-sessions")
public class CashSessionController {

  private final OpenCashSessionUseCase openUseCase;
  private final RegisterCashMovementUseCase registerMovementUseCase;
  private final CloseCashSessionUseCase closeUseCase;
  private final GetCurrentCashSessionUseCase getCurrentUseCase;
  private final GetCashSessionUseCase getUseCase;
  private final ListCashSessionsUseCase listUseCase;
  private final ListOpenCashSessionsUseCase listOpenUseCase;
  private final ExportArqueoUseCase exportArqueoUseCase;
  private final CashReportPdfPort cashReportPdf;
  private final Authz authz;

  public CashSessionController(
      OpenCashSessionUseCase openUseCase,
      RegisterCashMovementUseCase registerMovementUseCase,
      CloseCashSessionUseCase closeUseCase,
      GetCurrentCashSessionUseCase getCurrentUseCase,
      GetCashSessionUseCase getUseCase,
      ListCashSessionsUseCase listUseCase,
      ListOpenCashSessionsUseCase listOpenUseCase,
      ExportArqueoUseCase exportArqueoUseCase,
      CashReportPdfPort cashReportPdf,
      Authz authz) {
    this.openUseCase = openUseCase;
    this.registerMovementUseCase = registerMovementUseCase;
    this.closeUseCase = closeUseCase;
    this.getCurrentUseCase = getCurrentUseCase;
    this.getUseCase = getUseCase;
    this.listUseCase = listUseCase;
    this.listOpenUseCase = listOpenUseCase;
    this.exportArqueoUseCase = exportArqueoUseCase;
    this.cashReportPdf = cashReportPdf;
    this.authz = authz;
  }

  @PostMapping("/open")
  @ResponseStatus(HttpStatus.CREATED)
  public CashSessionView open(@Valid @RequestBody OpenCashSessionRequest request) {
    return openUseCase.open(
        new OpenCashSessionCommand(
            authz.currentCompanyId(),
            authz.resolveAccessibleBranch(request.branchId()),
            request.terminalId(),
            request.openingFloat(),
            authz.currentEmployeeIdOrNull(),
            request.note()));
  }

  @PostMapping("/{id}/movements")
  @ResponseStatus(HttpStatus.CREATED)
  public CashSessionView registerMovement(
      @PathVariable Long id, @Valid @RequestBody RegisterCashMovementRequest request) {
    return registerMovementUseCase.register(
        new RegisterCashMovementCommand(
            authz.currentCompanyId(),
            id,
            request.type(),
            request.method(),
            request.amount(),
            authz.currentEmployeeIdOrNull(),
            request.note()));
  }

  @PostMapping("/{id}/close")
  public CashSessionView close(
      @PathVariable Long id, @Valid @RequestBody CloseCashSessionRequest request) {
    return closeUseCase.close(
        new CloseCashSessionCommand(
            authz.currentCompanyId(),
            id,
            authz.currentEmployeeIdOrNull(),
            request.note(),
            request.counts() == null
                ? java.util.List.of()
                : request.counts().stream()
                    .map(c -> new CloseCashSessionCommand.Count(c.method(), c.countedAmount()))
                    .toList()),
        false);
  }

  @GetMapping("/current")
  public CashSessionView current() {
    return getCurrentUseCase.current(authz.currentCompanyId(), authz.currentEmployeeIdOrNull());
  }

  @GetMapping("/{id}")
  public CashSessionView get(@PathVariable Long id) {
    CashSessionView session = getUseCase.get(authz.currentCompanyId(), id);
    authz.resolveAccessibleBranch(session.branchId());
    return session;
  }

  @GetMapping("/open")
  public List<CashSessionView> openSessions() {
    Set<Long> accessibleBranchIds = authz.currentBranchIds();
    return listOpenUseCase.listOpen(authz.currentCompanyId(), accessibleBranchIds);
  }

  @GetMapping
  public PageResponse<CashSessionView> list(
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) Long employeeId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    PageResult<CashSessionView> result =
        listUseCase.list(
            new SearchCashSessionsQuery(
                authz.currentCompanyId(),
                authz.resolveAccessibleBranch(branchId),
                employeeId,
                from,
                to,
                page,
                pageSize));
    return new PageResponse<>(
        result.content(),
        result.page(),
        result.pageSize(),
        result.totalElements(),
        result.totalPages());
  }

  // ── Arqueo (export CSV/PDF) ───────────────────────────────────────────────────
  private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

  @GetMapping("/{id}/arqueo/export")
  public ResponseEntity<byte[]> exportArqueo(
      @PathVariable Long id, @RequestParam(defaultValue = "csv") String format) {
    CashArqueoReport report = exportArqueoUseCase.arqueo(authz.currentCompanyId(), id);
    String base = "arqueo_caja_" + id;
    return "pdf".equalsIgnoreCase(format)
        ? file(cashReportPdf.renderArqueo(report), base + ".pdf", MediaType.APPLICATION_PDF)
        : file(CashArqueoCsv.arqueo(report), base + ".csv", CSV);
  }

  private static ResponseEntity<byte[]> file(byte[] body, String filename, MediaType type) {
    String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
        .contentType(type)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded)
        .body(body);
  }
}
