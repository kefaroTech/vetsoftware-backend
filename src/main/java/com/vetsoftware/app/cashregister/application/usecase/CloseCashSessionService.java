package com.vetsoftware.app.cashregister.application.usecase;

import com.vetsoftware.app.cashregister.application.command.CloseCashSessionCommand;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.application.port.in.CloseCashSessionUseCase;
import com.vetsoftware.app.cashregister.application.port.out.CashSessionRepository;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.domain.CashSessionNotFoundException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierra la caja: arma el conteo declarado por método y delega en el agregado el cálculo esperado vs contado →
 * diferencia y la materialización de los {@code CashSessionCount}. El guard de "solo si está abierta" vive en el dominio.
 */
@Service
public class CloseCashSessionService implements CloseCashSessionUseCase {

    private final CashSessionRepository repository;

    public CloseCashSessionService(CashSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CashSessionView close(CloseCashSessionCommand command) {
        CashSession session = repository.findByIdAndCompany(command.sessionId(), command.companyId())
            .orElseThrow(() -> new CashSessionNotFoundException(command.sessionId()));
        Map<CashPaymentMethod, BigDecimal> counted = new LinkedHashMap<>();
        if (command.counts() != null) {
            for (CloseCashSessionCommand.Count c : command.counts()) {
                if (c.method() != null && c.countedAmount() != null) counted.put(c.method(), c.countedAmount());
            }
        }
        session.close(command.closedByEmployeeId(), counted, command.note());
        return CashSessionView.from(repository.save(session));
    }
}
