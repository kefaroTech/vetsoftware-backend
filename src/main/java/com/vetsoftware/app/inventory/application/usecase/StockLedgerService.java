package com.vetsoftware.app.inventory.application.usecase;

import com.vetsoftware.app.inventory.application.command.RecordAdjustmentCommand;
import com.vetsoftware.app.inventory.application.command.RecordClinicalUseCommand;
import com.vetsoftware.app.inventory.application.command.RecordPurchaseCommand;
import com.vetsoftware.app.inventory.application.command.RecordSaleCommand;
import com.vetsoftware.app.inventory.application.command.TransferStockCommand;
import com.vetsoftware.app.inventory.application.dto.StockConsumptionDto;
import com.vetsoftware.app.inventory.application.port.in.StockLedgerUseCase;
import com.vetsoftware.app.inventory.application.port.out.InventoryMetrics;
import com.vetsoftware.app.inventory.application.port.out.InventoryMetrics.Result;
import com.vetsoftware.app.inventory.application.port.out.NegativeStockPolicyPort;
import com.vetsoftware.app.inventory.application.port.out.StockBalanceRepository;
import com.vetsoftware.app.inventory.application.port.out.StockLotRepository;
import com.vetsoftware.app.inventory.application.port.out.StockMovementRepository;
import com.vetsoftware.app.inventory.domain.InsufficientStockException;
import com.vetsoftware.app.inventory.domain.StockBalance;
import com.vetsoftware.app.inventory.domain.StockLot;
import com.vetsoftware.app.inventory.domain.StockMovement;
import com.vetsoftware.app.inventory.domain.StockMovementType;
import com.vetsoftware.app.inventory.domain.StockReferenceType;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockLedgerService implements StockLedgerUseCase {

    private final StockLotRepository lotRepository;
    private final StockBalanceRepository balanceRepository;
    private final StockMovementRepository movementRepository;
    private final NegativeStockPolicyPort negativeStockPolicy;
    private final InventoryMetrics inventoryMetrics;

    public StockLedgerService(StockLotRepository lotRepository,
                              StockBalanceRepository balanceRepository,
                              StockMovementRepository movementRepository,
                              NegativeStockPolicyPort negativeStockPolicy,
                              InventoryMetrics inventoryMetrics) {
        this.lotRepository = lotRepository;
        this.balanceRepository = balanceRepository;
        this.movementRepository = movementRepository;
        this.negativeStockPolicy = negativeStockPolicy;
        this.inventoryMetrics = inventoryMetrics;
    }

    @Override
    @Observed(name = "inventory.record.purchase")
    @Transactional
    public void recordPurchase(RecordPurchaseCommand c) {
        if (c.quantity() <= 0) {
            inventoryMetrics.movement(StockMovementType.PURCHASE, Result.VALIDATION_ERROR, 0);
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        BigDecimal cost = c.unitCost() == null ? BigDecimal.ZERO : c.unitCost();
        StockLot lot = lotRepository
            .findByIdentity(c.companyId(), c.branchId(), c.productId(), c.lotNumber(), c.expireDate(), cost)
            .orElseGet(() -> StockLot.create(c.companyId(), c.branchId(), c.productId(),
                c.lotNumber(), c.expireDate(), 0, cost));
        lot.add(c.quantity());
        StockLot savedLot = lotRepository.save(lot);

        StockBalance balance = balanceForUpdate(c.companyId(), c.branchId(), c.productId());
        balance.add(c.quantity());
        balanceRepository.save(balance);

        movementRepository.save(StockMovement.of(c.companyId(), c.branchId(), c.productId(), savedLot.getId(),
            StockMovementType.PURCHASE, c.quantity(), cost, c.referenceType(), c.referenceId(), null, c.createdBy()));
        inventoryMetrics.movement(StockMovementType.PURCHASE, Result.SUCCESS, c.quantity());
    }

    @Override
    @Observed(name = "inventory.record.sale")
    @Transactional
    public List<StockConsumptionDto> recordSale(RecordSaleCommand c) {
        if (c.quantity() <= 0) {
            inventoryMetrics.movement(StockMovementType.SALE, Result.VALIDATION_ERROR, 0);
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        // Idempotencia: si ya se registró esta referencia (reintento POS), no volver a descontar.
        if (movementRepository.existsByReference(c.referenceType(), c.referenceId())) {
            inventoryMetrics.movement(StockMovementType.SALE, Result.DUPLICATE_IGNORED, 0);
            return List.of();
        }
        StockBalance balance = balanceForUpdate(c.companyId(), c.branchId(), c.productId());
        // POS fuerza negativo (c.allowNegative()); cuenta abierta respeta el flag de empresa.
        boolean allowNegative = c.allowNegative() || negativeStockPolicy.allowsNegative(c.companyId());
        if (balance.getQuantity() < c.quantity() && !allowNegative) {
            inventoryMetrics.movement(StockMovementType.SALE, Result.INSUFFICIENT_STOCK, 0);
            throw new InsufficientStockException(
                "Stock insuficiente para el producto " + c.productId() + " en la sede " + c.branchId()
                    + " (disponible " + balance.getQuantity() + ", requerido " + c.quantity() + ")");
        }
        List<StockConsumptionDto> consumptions = consumeFefo(c.companyId(), c.branchId(), c.productId(),
            c.quantity(), StockMovementType.SALE, c.referenceType(), c.referenceId(), null, c.createdBy());
        balance.subtract(c.quantity());
        balanceRepository.save(balance);
        inventoryMetrics.movement(StockMovementType.SALE, Result.SUCCESS, c.quantity());
        return consumptions;
    }

    @Override
    @Observed(name = "inventory.record.clinical.use")
    @Transactional
    public List<StockConsumptionDto> recordClinicalUse(RecordClinicalUseCommand c) {
        if (c.quantity() <= 0) {
            inventoryMetrics.movement(StockMovementType.CLINICAL_USE, Result.VALIDATION_ERROR, 0);
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        // Idempotencia por evento clínico (si viene referencia).
        if (c.referenceId() != null
            && movementRepository.existsByReference(StockReferenceType.CLINICAL_EVENT, c.referenceId())) {
            inventoryMetrics.movement(StockMovementType.CLINICAL_USE, Result.DUPLICATE_IGNORED, 0);
            return List.of();
        }
        StockBalance balance = balanceForUpdate(c.companyId(), c.branchId(), c.productId());
        if (balance.getQuantity() < c.quantity() && !negativeStockPolicy.allowsNegative(c.companyId())) {
            inventoryMetrics.movement(StockMovementType.CLINICAL_USE, Result.INSUFFICIENT_STOCK, 0);
            throw new InsufficientStockException(
                "Stock insuficiente para el consumo clínico del producto " + c.productId() + " en la sede "
                    + c.branchId() + " (disponible " + balance.getQuantity() + ", requerido " + c.quantity() + ")");
        }
        List<StockConsumptionDto> consumptions = consumeFefo(c.companyId(), c.branchId(), c.productId(),
            c.quantity(), StockMovementType.CLINICAL_USE, StockReferenceType.CLINICAL_EVENT, c.referenceId(),
            c.reason(), c.createdBy());
        balance.subtract(c.quantity());
        balanceRepository.save(balance);
        inventoryMetrics.movement(StockMovementType.CLINICAL_USE, Result.SUCCESS, c.quantity());
        return consumptions;
    }

    @Override
    @Observed(name = "inventory.record.adjustment")
    @Transactional
    public void recordAdjustment(RecordAdjustmentCommand c) {
        if (c.delta() == 0) throw new IllegalArgumentException("delta cannot be 0");
        StockMovementType movementType =
            c.delta() > 0 ? StockMovementType.ADJUSTMENT_IN : StockMovementType.ADJUSTMENT_OUT;
        if (c.reason() == null || c.reason().isBlank()) {
            inventoryMetrics.movement(movementType, Result.VALIDATION_ERROR, 0);
            throw new IllegalArgumentException("reason is required");
        }
        StockBalance balance = balanceForUpdate(c.companyId(), c.branchId(), c.productId());
        if (c.delta() > 0) {
            BigDecimal cost = c.unitCost() == null ? BigDecimal.ZERO : c.unitCost();
            StockLot lot = lotRepository
                .findByIdentity(c.companyId(), c.branchId(), c.productId(), null, null, cost)
                .orElseGet(() -> StockLot.create(c.companyId(), c.branchId(), c.productId(), null, null, 0, cost));
            lot.add(c.delta());
            StockLot savedLot = lotRepository.save(lot);
            movementRepository.save(StockMovement.of(c.companyId(), c.branchId(), c.productId(), savedLot.getId(),
                StockMovementType.ADJUSTMENT_IN, c.delta(), cost, StockReferenceType.ADJUSTMENT, c.referenceId(),
                c.reason(), c.createdBy()));
            balance.add(c.delta());
        } else {
            int out = -c.delta();
            // Los ajustes SÍ pueden dejar negativo (corrección de conteo), sin chequeo de política.
            consumeFefo(c.companyId(), c.branchId(), c.productId(), out, StockMovementType.ADJUSTMENT_OUT,
                StockReferenceType.ADJUSTMENT, c.referenceId(), c.reason(), c.createdBy());
            balance.subtract(out);
        }
        balanceRepository.save(balance);
        inventoryMetrics.movement(movementType, Result.SUCCESS, Math.abs(c.delta()));
    }

    @Override
    @Observed(name = "inventory.transfer.ledger")
    @Transactional
    public void transfer(TransferStockCommand c) {
        if (c.quantity() <= 0) {
            inventoryMetrics.movement(StockMovementType.TRANSFER_OUT, Result.VALIDATION_ERROR, 0);
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        if (c.fromBranchId().equals(c.toBranchId()))
            throw new IllegalArgumentException("origen y destino no pueden ser la misma sede");
        // Bloquea ambos saldos SIEMPRE en el mismo orden (por id de sede) para evitar deadlocks entre
        // transferencias cruzadas A→B y B→A.
        boolean fromFirst = c.fromBranchId() < c.toBranchId();
        Long firstBranch = fromFirst ? c.fromBranchId() : c.toBranchId();
        Long secondBranch = fromFirst ? c.toBranchId() : c.fromBranchId();
        StockBalance first = balanceForUpdate(c.companyId(), firstBranch, c.productId());
        StockBalance second = balanceForUpdate(c.companyId(), secondBranch, c.productId());
        StockBalance fromBal = fromFirst ? first : second;
        StockBalance toBal = fromFirst ? second : first;
        if (fromBal.getQuantity() < c.quantity()) {
            inventoryMetrics.movement(StockMovementType.TRANSFER_OUT, Result.INSUFFICIENT_STOCK, 0);
            throw new InsufficientStockException(
                "Stock insuficiente para transferir el producto " + c.productId() + " desde la sede "
                    + c.fromBranchId() + " (disponible " + fromBal.getQuantity() + ", requerido " + c.quantity() + ")");
        }

        List<StockLot> lots = lotRepository.findAvailableFefo(c.productId(), c.fromBranchId());
        int remaining = c.quantity();
        for (StockLot lot : lots) {
            if (remaining <= 0) break;
            int take = Math.min(lot.getQuantityAvailable(), remaining);
            if (take <= 0) continue;
            lot.consume(take);
            lotRepository.save(lot);
            movementRepository.save(StockMovement.of(c.companyId(), c.fromBranchId(), c.productId(), lot.getId(),
                StockMovementType.TRANSFER_OUT, take, lot.getUnitCost(), StockReferenceType.TRANSFER, null,
                c.reason(), c.createdBy()));
            // Réplica del lote en destino: misma identidad (lote/vencimiento/costo) para preservar FEFO y COGS.
            StockLot destLot = lotRepository.findByIdentity(c.companyId(), c.toBranchId(), c.productId(),
                    lot.getLotNumber(), lot.getExpireDate(), lot.getUnitCost())
                .orElseGet(() -> StockLot.create(c.companyId(), c.toBranchId(), c.productId(),
                    lot.getLotNumber(), lot.getExpireDate(), 0, lot.getUnitCost()));
            destLot.add(take);
            StockLot savedDest = lotRepository.save(destLot);
            movementRepository.save(StockMovement.of(c.companyId(), c.toBranchId(), c.productId(), savedDest.getId(),
                StockMovementType.TRANSFER_IN, take, lot.getUnitCost(), StockReferenceType.TRANSFER, null,
                c.reason(), c.createdBy()));
            remaining -= take;
        }
        if (remaining > 0) {
            inventoryMetrics.movement(StockMovementType.TRANSFER_OUT, Result.INSUFFICIENT_STOCK, 0);
            // Saldo y lotes divergen (no debería): aborta para no crear stock negativo por transferencia.
            throw new InsufficientStockException(
                "Lotes insuficientes para completar la transferencia del producto " + c.productId());
        }
        fromBal.subtract(c.quantity());
        balanceRepository.save(fromBal);
        toBal.add(c.quantity());
        balanceRepository.save(toBal);
        inventoryMetrics.movement(StockMovementType.TRANSFER_OUT, Result.SUCCESS, c.quantity());
        inventoryMetrics.movement(StockMovementType.TRANSFER_IN, Result.SUCCESS, c.quantity());
    }

    @Override
    @Observed(name = "inventory.reverse.ledger")
    @Transactional
    public void reverse(StockReferenceType referenceType, Long referenceId, Long createdBy) {
        List<StockMovement> movements = movementRepository.findByReference(referenceType, referenceId);
        // Idempotencia: si ya hay movimientos de anulación, no volver a compensar.
        boolean alreadyReversed = movements.stream()
            .anyMatch(m -> m.getType() == StockMovementType.VOID_IN || m.getType() == StockMovementType.VOID_OUT);
        if (alreadyReversed) return;

        for (StockMovement m : movements) {
            StockLot lot = lotRepository.findById(m.getLotId()).orElse(null);
            if (lot == null) continue;
            int abs = Math.abs(m.getQuantity());
            StockBalance balance = balanceForUpdate(m.getCompanyId(), m.getBranchId(), m.getProductId());
            if (m.getType().isInbound()) {
                // El original sumó stock → la reversa lo quita.
                lot.consume(abs);
                lotRepository.save(lot);
                balance.subtract(abs);
                movementRepository.save(StockMovement.of(m.getCompanyId(), m.getBranchId(), m.getProductId(),
                    lot.getId(), StockMovementType.VOID_OUT, abs, m.getUnitCost(), referenceType, referenceId,
                    "reversa", createdBy));
                inventoryMetrics.movement(StockMovementType.VOID_OUT, Result.SUCCESS, abs);
            } else {
                // El original restó stock → la reversa lo repone al mismo lote.
                lot.add(abs);
                lotRepository.save(lot);
                balance.add(abs);
                movementRepository.save(StockMovement.of(m.getCompanyId(), m.getBranchId(), m.getProductId(),
                    lot.getId(), StockMovementType.VOID_IN, abs, m.getUnitCost(), referenceType, referenceId,
                    "reversa", createdBy));
                inventoryMetrics.movement(StockMovementType.VOID_IN, Result.SUCCESS, abs);
            }
            balanceRepository.save(balance);
        }
    }

    /**
     * Descuenta {@code quantity} de los lotes de la sede en orden FEFO, creando un movimiento por lote consumido.
     * Si los lotes no alcanzan (stock negativo ya permitido por el caller), lleva el sobrante al último lote FEFO
     * (o a un lote genérico si no hay ninguno), dejándolo en negativo. No toca el saldo (lo hace el caller).
     */
    private List<StockConsumptionDto> consumeFefo(Long companyId, Long branchId, Long productId, int quantity,
                                                  StockMovementType type, StockReferenceType referenceType,
                                                  Long referenceId, String reason, Long createdBy) {
        List<StockConsumptionDto> consumptions = new ArrayList<>();
        List<StockLot> lots = lotRepository.findAvailableFefo(productId, branchId);
        int remaining = quantity;
        for (StockLot lot : lots) {
            if (remaining <= 0) break;
            int take = Math.min(lot.getQuantityAvailable(), remaining);
            if (take <= 0) continue;
            lot.consume(take);
            lotRepository.save(lot);
            movementRepository.save(StockMovement.of(companyId, branchId, productId, lot.getId(), type, take,
                lot.getUnitCost(), referenceType, referenceId, reason, createdBy));
            consumptions.add(new StockConsumptionDto(lot.getId(), take, lot.getUnitCost()));
            remaining -= take;
        }
        if (remaining > 0) {
            StockLot lot = lots.isEmpty()
                ? lotRepository.save(StockLot.create(companyId, branchId, productId, null, null, 0, BigDecimal.ZERO))
                : lots.get(lots.size() - 1);
            lot.consume(remaining);
            lotRepository.save(lot);
            movementRepository.save(StockMovement.of(companyId, branchId, productId, lot.getId(), type, remaining,
                lot.getUnitCost(), referenceType, referenceId, reason, createdBy));
            consumptions.add(new StockConsumptionDto(lot.getId(), remaining, lot.getUnitCost()));
        }
        return consumptions;
    }

    /** Saldo bloqueado (FOR UPDATE) del producto en la sede; se crea en 0 si aún no existe. */
    private StockBalance balanceForUpdate(Long companyId, Long branchId, Long productId) {
        return balanceRepository.findForUpdate(productId, branchId)
            .orElseGet(() -> balanceRepository.save(StockBalance.create(companyId, branchId, productId, 0)));
    }
}
