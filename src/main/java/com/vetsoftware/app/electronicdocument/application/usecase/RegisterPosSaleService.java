package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand;
import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand.SaleLine;
import com.vetsoftware.app.electronicdocument.application.command.SaleLineKind;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.RegisterPosSaleUseCase;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.SalesMetrics;
import com.vetsoftware.app.electronicdocument.application.port.out.SalesMetrics.Channel;
import com.vetsoftware.app.electronicdocument.application.port.out.SalesMetrics.Result;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Registra y emite una venta de POS: construye el documento PENDIENTE desde el
 * payload y lo pasa por el emisor (mismo gate BILLING que el cierre de cuenta).
 * Con BILLING: numera + transmite. Sin BILLING: queda PENDIENTE (datos
 * guardados, emision a la DIAN diferida y re-emitible al habilitar el modulo).
 *
 * <p>
 * Efecto inventario (F2b): cada línea de producto descuenta el kardex de la
 * sede emisora (ref {@code POS_DOCUMENT}, idempotente por el id del documento).
 * Ocurre aunque el documento quede PENDIENTE (la venta física ya sucedió) y
 * SIEMPRE permite negativo (mostrador no se frena por stock). Las líneas de
 * servicio/general no mueven inventario.
 */
@Observed(name = "electronic.document.pos.sale")
@Service
public class RegisterPosSaleService implements RegisterPosSaleUseCase {
    private final PosSaleRegistrar saleRegistrar;
    private final ElectronicDocumentEmitter emitter;
    private final DeliverElectronicDocumentService deliverService;
    private final ElectronicDocumentRepository repository;
    private final SalesMetrics salesMetrics;

    public RegisterPosSaleService(PosSaleRegistrar saleRegistrar, ElectronicDocumentEmitter emitter,
            DeliverElectronicDocumentService deliverService,
            ElectronicDocumentRepository repository, SalesMetrics salesMetrics) {
        this.saleRegistrar = saleRegistrar;
        this.emitter = emitter;
        this.deliverService = deliverService;
        this.repository = repository;
        this.salesMetrics = salesMetrics;
    }

    /**
     * Sin {@code @Transactional} en el metodo completo, a proposito. La venta se
     * parte en tres tiempos para que el HTTP al proveedor —hasta 75 segundos— no
     * corra dentro de ninguna transaccion:
     *
     * <ol>
     * <li>Transaccion corta: documento PENDIENTE, descuento de stock y registro de
     * caja. Commitea y suelta la conexion.</li>
     * <li>Emision: numeracion y HTTP a MATIAS, sin transaccion abierta. El
     * desenlace lo guarda {@link TransmissionResultPersister} en una transaccion
     * propia y corta.</li>
     * <li>Entrega: PDF, QR y S3, tambien fuera de transaccion.</li>
     * </ol>
     *
     * <p>
     * La respuesta no cambia: sigue llevando CUFE y QR, que es lo que el cajero
     * imprime.
     *
     * <p>
     * Lo que si cambia es la atomicidad. Antes, un fallo del proveedor revertia la
     * venta entera —stock y caja incluidos—. Ahora la venta queda registrada y el
     * documento en PENDIENTE, re-emitible por el job de reconciliacion. Es la
     * semantica correcta para un mostrador: si ya se cobro, no se puede des-vender
     * porque el proveedor este lento. Es tambien la que el equipo ya eligio para el
     * cierre de cuenta.
     */
    @Override
    public ElectronicDocumentDto execute(RegisterPosSaleCommand command) {
        // Idempotencia: si el POST se reintenta con la misma key (respuesta perdida en
        // transporte), se
        // devuelve el documento ya emitido en vez de registrar y transmitir OTRA venta
        // a la DIAN. El
        // índice
        // único (company_id, client_request_id) respalda la carrera concurrente (la 2ª
        // inserción la
        // rechaza la BD).
        if (command.clientRequestId() != null && !command.clientRequestId().isBlank()) {
            Optional<ElectronicDocument> existing = repository.findByCompanyIdAndClientRequestId(
                    command.companyId(), command.clientRequestId());
            if (existing.isPresent()) {
                return ElectronicDocumentDto.from(existing.get());
            }
        }
        try {
            // Rechaza cantidades de producto no enteras antes de construir/transmitir.
            validateProductQuantities(command);

            // Tiempo 1 — transaccion corta: nada de I/O externo aca dentro.
            ElectronicDocument document = saleRegistrar.registerPending(command);
            // Tiempo 2 — sin transaccion abierta: numeracion + HTTP al proveedor.
            ElectronicDocument emitted = emitter.emit(document);
            // La venta se completo aqui: al emitirse, no al entregarse el PDF. Publicar la
            // metrica ANTES del tiempo 3 evita que un fallo de QR, render, S3 o del
            // guardado
            // del resultado DIAN salte al catch de abajo y contabilice como ERROR una venta
            // ya facturada y validada en la DIAN. La entrega tiene sus propios contadores
            // (document.delivery), que son los que deben registrar ese fallo.
            salesMetrics.completed(Channel.POS, emitted.getDocumentType(),
                    emitted.getPayableAmount(), emitted.getLines().size());
            // Tiempo 3 — sin transaccion: PDF, QR y S3.
            deliverService.deliverIfValidated(emitted);
            return ElectronicDocumentDto.from(emitted);
        } catch (IllegalArgumentException exception) {
            salesMetrics.failed(Channel.POS, command.documentType(), Result.REJECTED);
            throw exception;
        } catch (RuntimeException | Error exception) {
            salesMetrics.failed(Channel.POS, command.documentType(), Result.ERROR);
            throw exception;
        }
    }

    private void validateProductQuantities(RegisterPosSaleCommand command) {
        for (SaleLine line : command.lines()) {
            if (line.kind() == SaleLineKind.PRODUCT) {
                toUnits(line.refId(), line.quantity());
            }
        }
    }

    private static int toUnits(Long productId, BigDecimal quantity) {
        if (quantity == null || quantity.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(
                    "La cantidad de un producto debe ser entera (producto " + productId + "): "
                            + quantity);
        }
        return quantity.intValueExact();
    }
}
