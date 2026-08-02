package com.vetsoftware.app.cashregister.application.usecase;

import com.vetsoftware.app.cashregister.application.dto.CashArqueoReport;
import com.vetsoftware.app.cashregister.application.port.in.ExportArqueoUseCase;
import com.vetsoftware.app.cashregister.application.port.out.CashSessionRepository;
import com.vetsoftware.app.cashregister.domain.CashSessionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Construye el reporte de arqueo desde el agregado de la sesión (movimientos +
 * counts).
 */
@Observed(name = "cash.register.export.arqueo")
@Service
public class ExportArqueoService implements ExportArqueoUseCase {

    private final CashSessionRepository repository;

    public ExportArqueoService(CashSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public CashArqueoReport arqueo(Long companyId, Long sessionId) {
        return repository.findByIdAndCompany(sessionId, companyId).map(CashArqueoReport::from)
                .orElseThrow(() -> new CashSessionNotFoundException(sessionId));
    }
}
