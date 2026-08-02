package com.vetsoftware.app.inventory.application.usecase;

import com.vetsoftware.app.inventory.application.command.RecordAdjustmentCommand;
import com.vetsoftware.app.inventory.application.command.RecordClinicalUseCommand;
import com.vetsoftware.app.inventory.application.command.RecordPurchaseCommand;
import com.vetsoftware.app.inventory.application.command.SetMinStockCommand;
import com.vetsoftware.app.inventory.application.command.TransferStockCommand;
import com.vetsoftware.app.inventory.application.port.in.AdjustStockUseCase;
import com.vetsoftware.app.inventory.application.port.in.ConsumeStockUseCase;
import com.vetsoftware.app.inventory.application.port.in.ReceiveStockUseCase;
import com.vetsoftware.app.inventory.application.port.in.SetMinStockUseCase;
import com.vetsoftware.app.inventory.application.port.in.StockLedgerUseCase;
import com.vetsoftware.app.inventory.application.port.in.TransferStockUseCase;
import com.vetsoftware.app.inventory.application.port.out.BranchQueryPort;
import com.vetsoftware.app.inventory.application.port.out.StockBalanceRepository;
import com.vetsoftware.app.inventory.domain.StockBalance;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operaciones administrativas de inventario (entrada, ajuste, transferencia, mínimo). El gate por
 * permiso vive en los @PreAuthorize de los puertos de entrada; aquí se valida que la sede
 * pertenezca a la empresa y esté activa, y se delega el movimiento al {@link StockLedgerUseCase}
 * (único punto que afecta el kardex).
 */
@Service
public class InventoryAdminService
    implements AdjustStockUseCase,
        ReceiveStockUseCase,
        TransferStockUseCase,
        SetMinStockUseCase,
        ConsumeStockUseCase {

  private final StockLedgerUseCase stockLedger;
  private final StockBalanceRepository balanceRepository;
  private final BranchQueryPort branchQueryPort;

  public InventoryAdminService(
      StockLedgerUseCase stockLedger,
      StockBalanceRepository balanceRepository,
      BranchQueryPort branchQueryPort) {
    this.stockLedger = stockLedger;
    this.balanceRepository = balanceRepository;
    this.branchQueryPort = branchQueryPort;
  }

  @Override
  @Observed(name = "inventory.adjust")
  public void adjust(RecordAdjustmentCommand command) {
    requireBranch(command.companyId(), command.branchId());
    stockLedger.recordAdjustment(command);
  }

  @Override
  @Observed(name = "inventory.receive")
  public void receive(RecordPurchaseCommand command) {
    requireBranch(command.companyId(), command.branchId());
    stockLedger.recordPurchase(command);
  }

  @Override
  @Observed(name = "inventory.transfer")
  public void transfer(TransferStockCommand command) {
    requireBranch(command.companyId(), command.fromBranchId());
    requireBranch(command.companyId(), command.toBranchId());
    stockLedger.transfer(command);
  }

  @Override
  @Observed(name = "inventory.consume")
  public void consume(RecordClinicalUseCommand command) {
    requireBranch(command.companyId(), command.branchId());
    stockLedger.recordClinicalUse(command);
  }

  @Override
  @Observed(name = "inventory.set.min.stock")
  @Transactional
  public void setMinStock(SetMinStockCommand command) {
    if (command.minStock() < 0) throw new IllegalArgumentException("minStock cannot be negative");
    requireBranch(command.companyId(), command.branchId());
    StockBalance balance =
        balanceRepository
            .findForUpdate(command.productId(), command.branchId())
            .orElseGet(
                () ->
                    StockBalance.create(
                        command.companyId(),
                        command.branchId(),
                        command.productId(),
                        command.minStock()));
    balance.setMinStock(command.minStock());
    balanceRepository.save(balance);
  }

  private void requireBranch(Long companyId, Long branchId) {
    if (!branchQueryPort.existsActiveInCompany(branchId, companyId)) {
      throw new IllegalArgumentException("Sede no válida o inactiva: " + branchId);
    }
  }
}
