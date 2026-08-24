package com.vetsoftware.app.subscriptionpayment.application.usecase;

import com.vetsoftware.app.subscriptionpayment.application.command.ReverseBillingDocumentApplicationCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.BillingDocumentApplicationDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ReverseBillingDocumentApplicationUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentApplicationRepository;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentQueryPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentSettlementPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.DunningReevaluationPort;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentApplication;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentApplicationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deshace una aplicacion equivocada <strong>sin borrar ni editar nada</strong>:
 * crea otra que la contra-aplica, con el importe negado y un puntero a la
 * original, y las dos quedan.
 *
 * <p>
 * Es la unica forma de corregir en este modelo, y no es una preferencia
 * estilistica: si una aplicacion pudiera borrarse, el saldo de la factura seria
 * irreconstruible y la conciliacion del mes no tendria contra que cuadrar.
 *
 * <p>
 * <strong>Idempotente por busqueda previa</strong>, dentro del bloqueo: una
 * aplicacion se revierte una sola vez ({@code uq_bda_reversal}), y el segundo
 * intento devuelve la reversa que ya existe en vez de estrellarse contra el
 * indice unico.
 */
@Observed(name = "subscription.payment.application.reverse")
@Service
public class ReverseBillingDocumentApplicationService
        implements
            ReverseBillingDocumentApplicationUseCase {

    private final BillingDocumentApplicationRepository repository;
    private final BillingDocumentQueryPort billingDocumentQueryPort;
    private final BillingDocumentSettlementPort settlementPort;
    private final DunningReevaluationPort dunningReevaluationPort;
    private final Clock clock;

    public ReverseBillingDocumentApplicationService(BillingDocumentApplicationRepository repository,
            BillingDocumentQueryPort billingDocumentQueryPort,
            BillingDocumentSettlementPort settlementPort,
            DunningReevaluationPort dunningReevaluationPort, Clock clock) {
        this.repository = repository;
        this.billingDocumentQueryPort = billingDocumentQueryPort;
        this.settlementPort = settlementPort;
        this.dunningReevaluationPort = dunningReevaluationPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BillingDocumentApplicationDto execute(ReverseBillingDocumentApplicationCommand command) {
        BillingDocumentApplication original = repository
                .findByIdAndCompanyId(command.applicationId(), command.companyId())
                .orElseThrow(() -> new BillingDocumentApplicationNotFoundException(
                        command.applicationId()));

        // El lock sobre el documento destino va ANTES de la busqueda de la reversa:
        // es lo que hace que dos peticiones simultaneas se serialicen y que la
        // segunda lea, ya dentro del lock, la fila que la primera acaba de escribir.
        billingDocumentQueryPort.lockByIdAndCompanyId(original.getTargetDocument().id(),
                command.companyId());

        Optional<BillingDocumentApplication> existing = repository
                .findByReversalOfIdAndCompanyId(original.getId(), command.companyId());
        if (existing.isPresent())
            return BillingDocumentApplicationDto.from(existing.get());

        BillingDocumentApplication reversal = BillingDocumentApplication.reversalOf(original,
                LocalDateTime.now(clock));
        BillingDocumentApplicationDto dto = BillingDocumentApplicationDto
                .from(repository.save(reversal));
        settlementPort.recalculateSettledAmount(original.getTargetDocument().id(),
                command.companyId());
        dunningReevaluationPort.reevaluate(original.getTargetDocument().id(), command.companyId());
        return dto;
    }
}
