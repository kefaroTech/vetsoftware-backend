package com.vetsoftware.app.inventory.application.usecase;

import com.vetsoftware.app.inventory.application.command.RecordAdjustmentCommand;
import com.vetsoftware.app.inventory.application.command.RecordCountCommand;
import com.vetsoftware.app.inventory.application.command.SearchCountsQuery;
import com.vetsoftware.app.inventory.application.dto.InventoryCountView;
import com.vetsoftware.app.inventory.application.dto.PageResult;
import com.vetsoftware.app.inventory.application.port.in.GetCountUseCase;
import com.vetsoftware.app.inventory.application.port.in.ListCountsUseCase;
import com.vetsoftware.app.inventory.application.port.in.RecordCountUseCase;
import com.vetsoftware.app.inventory.application.port.out.BranchQueryPort;
import com.vetsoftware.app.inventory.application.port.out.InventoryCountRepository;
import com.vetsoftware.app.inventory.application.port.out.StockBalanceRepository;
import com.vetsoftware.app.inventory.application.port.in.StockLedgerUseCase;
import com.vetsoftware.app.inventory.domain.InventoryCount;
import com.vetsoftware.app.inventory.domain.InventoryCountLine;
import com.vetsoftware.app.inventory.domain.InventoryCountNotFoundException;
import com.vetsoftware.app.inventory.domain.StockBalance;
import com.vetsoftware.app.inventory.domain.StockReferenceType;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Conteo físico (cíclico) de inventario. Al confirmar una sesión: lee el saldo de sistema de cada producto en la
 * sede, arma las líneas con su diferencia (contado − sistema), persiste la sesión y genera un ajuste por cada línea
 * con diferencia ≠ 0 delegando en el {@link StockLedgerUseCase} (ADJUSTMENT_IN si sobra, ADJUSTMENT_OUT si falta),
 * referenciando la sesión ({@code reference_id}) para trazabilidad en el kardex. Todo en una sola transacción.
 */
@Service
public class InventoryCountService implements RecordCountUseCase, ListCountsUseCase, GetCountUseCase {

    private final StockLedgerUseCase stockLedger;
    private final StockBalanceRepository balanceRepository;
    private final InventoryCountRepository countRepository;
    private final BranchQueryPort branchQueryPort;

    public InventoryCountService(StockLedgerUseCase stockLedger,
                                 StockBalanceRepository balanceRepository,
                                 InventoryCountRepository countRepository,
                                 BranchQueryPort branchQueryPort) {
        this.stockLedger = stockLedger;
        this.balanceRepository = balanceRepository;
        this.countRepository = countRepository;
        this.branchQueryPort = branchQueryPort;
    }

    @Override
    @Observed(name = "inventory.record.count")
    @Transactional
    public InventoryCountView record(RecordCountCommand command) {
        if (!branchQueryPort.existsActiveInCompany(command.branchId(), command.companyId())) {
            throw new IllegalArgumentException("Sede no válida o inactiva: " + command.branchId());
        }
        // Saldo de sistema por línea ANTES de conciliar. Las validaciones (líneas vacías, producto repetido,
        // cantidad negativa) las impone el agregado al construirse.
        List<InventoryCountLine> lines = command.lines().stream()
            .map(l -> InventoryCountLine.create(l.productId(), systemQuantity(l.productId(), command.branchId()),
                l.countedQuantity()))
            .toList();
        InventoryCount count = InventoryCount.create(command.companyId(), command.branchId(), command.note(),
            command.countedBy(), lines);
        InventoryCount saved = countRepository.save(count);

        // Un ajuste por cada línea con diferencia; referencia = id de la sesión para trazar en el kardex.
        for (InventoryCountLine line : saved.getLines()) {
            int delta = line.difference();
            if (delta == 0) continue;
            stockLedger.recordAdjustment(new RecordAdjustmentCommand(saved.getCompanyId(), saved.getBranchId(),
                line.getProductId(), delta, null, "Conteo físico #" + saved.getId(), saved.getId(),
                saved.getCountedBy()));
        }
        return InventoryCountView.from(saved);
    }

    @Override
    @Observed(name = "inventory.list.counts")
    @Transactional(readOnly = true)
    public PageResult<InventoryCountView> list(SearchCountsQuery query) {
        return countRepository.search(query);
    }

    @Override
    @Observed(name = "inventory.get.count")
    @Transactional(readOnly = true)
    public InventoryCountView get(Long companyId, Long id) {
        return countRepository.findDetail(companyId, id)
            .orElseThrow(() -> new InventoryCountNotFoundException(id));
    }

    private int systemQuantity(Long productId, Long branchId) {
        return balanceRepository.find(productId, branchId).map(StockBalance::getQuantity).orElse(0);
    }
}
