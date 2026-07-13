package com.vetsoftware.app.cashregister.application.usecase;

import com.vetsoftware.app.cashregister.application.command.RegisterCashMovementCommand;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.application.port.in.RegisterCashMovementUseCase;
import com.vetsoftware.app.cashregister.application.port.out.CashSessionRepository;
import com.vetsoftware.app.cashregister.domain.CashMovement;
import com.vetsoftware.app.cashregister.domain.CashReferenceType;
import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.domain.CashSessionNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra un movimiento manual (ingreso/retiro/gasto) en una sesión de caja abierta. Rechaza tipos no manuales
 * (venta/abono/reversa los inyecta la orquestación) y delega el guard de "solo con caja abierta" al agregado.
 */
@Service
public class RegisterCashMovementService implements RegisterCashMovementUseCase {

    private final CashSessionRepository repository;

    public RegisterCashMovementService(CashSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CashSessionView register(RegisterCashMovementCommand command) {
        if (command.type() == null || !command.type().isManual()) {
            throw new IllegalArgumentException("Solo se permiten movimientos manuales: ingreso, retiro o gasto.");
        }
        CashSession session = repository.findByIdAndCompany(command.sessionId(), command.companyId())
            .orElseThrow(() -> new CashSessionNotFoundException(command.sessionId()));
        CashMovement movement = CashMovement.create(command.type(), command.method(), command.amount(),
            CashReferenceType.MANUAL, null, command.createdByEmployeeId(), command.note());
        session.addMovement(movement);
        return CashSessionView.from(repository.save(session));
    }
}
