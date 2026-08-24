package com.vetsoftware.app.dunning.application.usecase;

import com.vetsoftware.app.dunning.application.dto.DunningBatchResult;
import com.vetsoftware.app.dunning.application.port.in.EvaluateDunningUseCase;
import com.vetsoftware.app.dunning.application.port.in.ProcessDunningBatchUseCase;
import com.vetsoftware.app.dunning.application.port.out.DunningBillingDocumentPort;
import com.vetsoftware.app.dunning.domain.DunningBillingDocumentSnapshot;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Barrido por cursor y SKIP LOCKED; no necesita lease ni tabla auxiliar.
 *
 * <p>
 * <b>Implementa {@link ProcessDunningBatchUseCase} para que su regimen de
 * autorizacion este declarado y no heredado.</b> Antes era un {@code @Service}
 * suelto: hacia una lectura cross-tenant con {@code FOR UPDATE SKIP LOCKED} y
 * su unica proteccion era que el unico llamador —{@code DunningEvaluationJob}—
 * se autenticase como SYSTEM antes de entrar. Al no implementar ningun puerto,
 * ninguna regla de arquitectura lo miraba, y bastaba inyectarlo en un
 * controller nuevo para entregar las facturas vencidas de todos los tenants.
 */
@Service
public class DunningEvaluationWorker implements ProcessDunningBatchUseCase {

    private final DunningBillingDocumentPort billingDocumentPort;
    private final EvaluateDunningUseCase evaluateDunningUseCase;
    private final Clock clock;

    public DunningEvaluationWorker(DunningBillingDocumentPort billingDocumentPort,
            EvaluateDunningUseCase evaluateDunningUseCase, Clock clock) {
        this.billingDocumentPort = billingDocumentPort;
        this.evaluateDunningUseCase = evaluateDunningUseCase;
        this.clock = clock;
    }

    @Override
    @Transactional
    public DunningBatchResult processBatchAfter(long afterId, int batchSize) {
        if (afterId < 0)
            throw new IllegalArgumentException("afterId must not be negative");
        if (batchSize <= 0)
            throw new IllegalArgumentException("batchSize must be positive");

        List<DunningBillingDocumentSnapshot> documents = billingDocumentPort
                .lockOverdueBatchAfter(LocalDate.now(clock), afterId, batchSize);
        for (DunningBillingDocumentSnapshot document : documents) {
            evaluateDunningUseCase.evaluate(document.document().id(),
                    document.document().companyId());
        }
        long lastId = documents.isEmpty()
                ? afterId
                : documents.get(documents.size() - 1).document().id();
        return new DunningBatchResult(documents.size(), lastId);
    }
}
