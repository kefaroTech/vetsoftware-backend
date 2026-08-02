package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand;
import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand.SaleLine;
import com.vetsoftware.app.electronicdocument.application.command.SaleLineKind;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.RegisterPosSaleUseCase;
import com.vetsoftware.app.electronicdocument.application.port.out.CashPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.InventoryLedgerPort;
import com.vetsoftware.app.electronicdocument.application.port.out.SalesMetrics;
import com.vetsoftware.app.electronicdocument.application.port.out.SalesMetrics.Channel;
import com.vetsoftware.app.electronicdocument.application.port.out.SalesMetrics.Result;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra y emite una venta de POS: construye el documento PENDIENTE desde el payload y lo pasa
 * por el emisor (mismo gate BILLING que el cierre de cuenta). Con BILLING: numera + transmite. Sin
 * BILLING: queda PENDIENTE (datos guardados, emision a la DIAN diferida y re-emitible al habilitar
 * el modulo).
 *
 * <p>Efecto inventario (F2b): cada línea de producto descuenta el kardex de la sede emisora (ref
 * {@code POS_DOCUMENT}, idempotente por el id del documento). Ocurre aunque el documento quede
 * PENDIENTE (la venta física ya sucedió) y SIEMPRE permite negativo (mostrador no se frena por
 * stock). Las líneas de servicio/general no mueven inventario.
 */
@Observed(name = "electronic.document.pos.sale")
@Service
public class RegisterPosSaleService implements RegisterPosSaleUseCase {
  private final PosSaleDocumentBuilder documentBuilder;
  private final ElectronicDocumentEmitter emitter;
  private final DeliverElectronicDocumentService deliverService;
  private final ElectronicDocumentRepository repository;
  private final InventoryLedgerPort inventoryLedger;
  private final CashPort cashPort;
  private final SalesMetrics salesMetrics;

  public RegisterPosSaleService(
      PosSaleDocumentBuilder documentBuilder,
      ElectronicDocumentEmitter emitter,
      DeliverElectronicDocumentService deliverService,
      ElectronicDocumentRepository repository,
      InventoryLedgerPort inventoryLedger,
      CashPort cashPort,
      SalesMetrics salesMetrics) {
    this.documentBuilder = documentBuilder;
    this.emitter = emitter;
    this.deliverService = deliverService;
    this.repository = repository;
    this.inventoryLedger = inventoryLedger;
    this.cashPort = cashPort;
    this.salesMetrics = salesMetrics;
  }

  @Override
  @Transactional
  public ElectronicDocumentDto execute(RegisterPosSaleCommand command) {
    // Idempotencia: si el POST se reintenta con la misma key (respuesta perdida en transporte), se
    // devuelve el documento ya emitido en vez de registrar y transmitir OTRA venta a la DIAN. El
    // índice
    // único (company_id, client_request_id) respalda la carrera concurrente (la 2ª inserción la
    // rechaza la BD).
    if (command.clientRequestId() != null && !command.clientRequestId().isBlank()) {
      Optional<ElectronicDocument> existing =
          repository.findByCompanyIdAndClientRequestId(
              command.companyId(), command.clientRequestId());
      if (existing.isPresent()) {
        return ElectronicDocumentDto.from(existing.get());
      }
    }
    try {
      // Rechaza cantidades de producto no enteras antes de construir/transmitir.
      validateProductQuantities(command);

      ElectronicDocument document = documentBuilder.build(command);
      // Si la empresa exige caja, se valida antes de transmitir.
      cashPort.requireOpenSession(
          command.companyId(), document.getBranchId(), command.issuedByEmployeeId());
      ElectronicDocument emitted = emitter.emit(document);
      discountStock(command, emitted);
      registerCash(command, emitted);
      deliverService.deliverIfValidated(emitted);
      salesMetrics.completed(
          Channel.POS,
          emitted.getDocumentType(),
          emitted.getPayableAmount(),
          emitted.getLines().size());
      return ElectronicDocumentDto.from(emitted);
    } catch (IllegalArgumentException exception) {
      salesMetrics.failed(Channel.POS, command.documentType(), Result.REJECTED);
      throw exception;
    } catch (RuntimeException | Error exception) {
      salesMetrics.failed(Channel.POS, command.documentType(), Result.ERROR);
      throw exception;
    }
  }

  private void registerCash(RegisterPosSaleCommand command, ElectronicDocument document) {
    if (document.getOpenAccountId() != null || document.getPayments().isEmpty()) return;
    List<CashPort.PaymentLine> payments =
        document.getPayments().stream()
            .map(p -> new CashPort.PaymentLine(p.getPaymentMeans(), p.getAmount()))
            .toList();
    cashPort.registerSale(
        command.companyId(),
        document.getBranchId(),
        document.getId(),
        payments,
        command.issuedByEmployeeId());
  }

  private void validateProductQuantities(RegisterPosSaleCommand command) {
    for (SaleLine line : command.lines()) {
      if (line.kind() == SaleLineKind.PRODUCT) {
        toUnits(line.refId(), line.quantity());
      }
    }
  }

  private void discountStock(RegisterPosSaleCommand command, ElectronicDocument document) {
    for (SaleLine line : command.lines()) {
      if (line.kind() != SaleLineKind.PRODUCT) continue;
      inventoryLedger.recordPosSale(
          command.companyId(),
          document.getBranchId(),
          line.refId(),
          toUnits(line.refId(), line.quantity()),
          document.getId(),
          command.issuedByEmployeeId());
    }
  }

  /**
   * Convierte la cantidad a unidades enteras; rechaza fracciones (un producto se vende por unidad).
   */
  private static int toUnits(Long productId, BigDecimal quantity) {
    if (quantity == null || quantity.stripTrailingZeros().scale() > 0) {
      throw new IllegalArgumentException(
          "La cantidad de un producto debe ser entera (producto " + productId + "): " + quantity);
    }
    return quantity.intValueExact();
  }
}
