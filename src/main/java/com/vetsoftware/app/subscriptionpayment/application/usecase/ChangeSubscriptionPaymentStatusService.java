package com.vetsoftware.app.subscriptionpayment.application.usecase;

import com.vetsoftware.app.subscriptionpayment.application.command.ChangeSubscriptionPaymentStatusCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ChangeSubscriptionPaymentStatusUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentApplicationRepository;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentQueryPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentSettlementPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.DunningReevaluationPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.OverpaymentCreditGrantPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentAuditPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentMetrics;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentRef;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentHasActiveApplicationsException;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentNotFoundException;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirma, marca como fallido o devuelve un pago.
 *
 * <p>
 * <strong>Y arrastra el recalculo de R4, que es la mitad del trabajo.</strong>
 * Solo los pagos {@code CONFIRMED} cuentan como cobro, asi que el
 * {@code settled_amount} de una factura depende del estado del pago tanto como
 * de la aplicacion. Aplicar un pago {@code PENDING} y confirmarlo despues sin
 * recalcular deja el saldo sin bajar: la clinica pago, el sistema no se entera
 * y el reloj de la mora sigue corriendo. Devolver un pago aplicado sin
 * recalcular hace lo contrario: da por saldada una factura con dinero que se
 * devolvio.
 */
@Observed(name = "subscription.payment.change.status")
@Service
public class ChangeSubscriptionPaymentStatusService
        implements
            ChangeSubscriptionPaymentStatusUseCase {

    private final SubscriptionPaymentRepository repository;
    private final BillingDocumentApplicationRepository applicationRepository;
    private final BillingDocumentQueryPort billingDocumentQueryPort;
    private final BillingDocumentSettlementPort settlementPort;
    private final DunningReevaluationPort dunningReevaluationPort;
    private final SubscriptionPaymentMetrics metrics;
    private final SubscriptionPaymentAuditPort audit;
    private final OverpaymentCreditGrantPort overpaymentCreditGrantPort;

    @SuppressWarnings("java:S107")
    public ChangeSubscriptionPaymentStatusService(SubscriptionPaymentRepository repository,
            BillingDocumentApplicationRepository applicationRepository,
            BillingDocumentQueryPort billingDocumentQueryPort,
            BillingDocumentSettlementPort settlementPort,
            DunningReevaluationPort dunningReevaluationPort, SubscriptionPaymentMetrics metrics,
            SubscriptionPaymentAuditPort audit,
            OverpaymentCreditGrantPort overpaymentCreditGrantPort) {
        this.repository = repository;
        this.applicationRepository = applicationRepository;
        this.billingDocumentQueryPort = billingDocumentQueryPort;
        this.settlementPort = settlementPort;
        this.dunningReevaluationPort = dunningReevaluationPort;
        this.metrics = metrics;
        this.audit = audit;
        this.overpaymentCreditGrantPort = overpaymentCreditGrantPort;
    }

    @Override
    @Transactional
    public SubscriptionPaymentDto execute(ChangeSubscriptionPaymentStatusCommand command) {
        SubscriptionPayment payment = repository
                .lockByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new SubscriptionPaymentNotFoundException(command.id()));
        if (payment.getGateway() != null && payment.getStatus() == command.status()) {
            // El webhook y el sondeo de la pasarela pueden llegar los dos al mismo
            // desenlace del mismo pago. GatewayOutcomeSettler ya hace un check-then-act
            // fuera del candado; esto lo cierra de verdad, bajo el lock que acaba de
            // tomarse arriba. Un pago manual en el mismo estado sigue rechazandose: ahi
            // repetir la peticion es una senal a mirar, no una carrera esperada.
            return SubscriptionPaymentDto.from(payment);
        }
        if (command.status() == SubscriptionPaymentStatus.REFUNDED) {
            var netAppliedAmount = applicationRepository.sumAppliedFromPayment(command.id(),
                    command.companyId());
            if (netAppliedAmount.signum() != 0)
                throw new SubscriptionPaymentHasActiveApplicationsException(command.id(),
                        netAppliedAmount);
        }
        SubscriptionPaymentStatus previousStatus = payment.getStatus();
        payment.changeStatus(command.status());
        SubscriptionPaymentDto dto = SubscriptionPaymentDto.from(repository.save(payment));

        // CONFIRMED -> REFUNDED es plata que sale y PENDING -> FAILED es plata que
        // nunca
        // entro y que alguien pudo dar por cobrada. Los dos eran hechos contables sin
        // contador y sin mas rastro que un http_mutation que no decia de cuanto.
        metrics.paymentStatusChanged(payment.getPaymentMethod(), payment.getStatus());
        audit.paymentStatusChanged(payment.getId(), previousStatus, payment.getStatus(),
                payment.getAmount(), payment.getCurrency(), payment.getGateway(),
                payment.getGatewayReference());
        recalculateAffectedDocuments(command);
        return dto;
    }

    /**
     * Los documentos se bloquean por id ascendente, el mismo orden que usa
     * {@code ApplyBillingDocumentService}: dos caminos que toman los mismos
     * candados en ordenes distintos es como se fabrica un interbloqueo.
     */
    private void recalculateAffectedDocuments(ChangeSubscriptionPaymentStatusCommand command) {
        List<Long> documentIds = applicationRepository
                .findTargetDocumentIdsByPaymentId(command.id(), command.companyId()).stream()
                .sorted().toList();
        for (Long documentId : documentIds) {
            billingDocumentQueryPort.lockByIdAndCompanyId(documentId, command.companyId());
            if (command.status() == SubscriptionPaymentStatus.CONFIRMED) {
                capOverpayment(command, documentId);
            }
            settlementPort.recalculateSettledAmount(documentId, command.companyId());
            dunningReevaluationPort.reevaluate(documentId, command.companyId());
        }
    }

    /**
     * R3: dos pagos confirmados sobre el mismo documento pueden sumar mas de lo que
     * el documento vale -nada lo impedia al aplicarlos, porque ninguno contaba
     * todavia-. {@code computeUncappedSettledAmount} ve esa suma sin el tope que
     * {@code recalculateSettledAmount} va a aplicar a continuacion; lo que exceda
     * el total se convierte en saldo a favor por pago en exceso en vez de perderse
     * contra el tope o reventar {@code chk_sbd_settled_cap}.
     */
    private void capOverpayment(ChangeSubscriptionPaymentStatusCommand command, Long documentId) {
        BigDecimal uncappedSettled = settlementPort.computeUncappedSettledAmount(documentId,
                command.companyId());
        BigDecimal totalAmount = billingDocumentQueryPort
                .findByIdAndCompanyId(documentId, command.companyId())
                .map(BillingDocumentRef::totalAmount).orElse(null);
        if (totalAmount == null)
            return;
        BigDecimal excess = uncappedSettled.subtract(totalAmount);
        if (excess.signum() <= 0)
            return;
        overpaymentCreditGrantPort.grantForOverpayment(command.companyId(), command.id(),
                documentId, excess);
        audit.overpaymentCredited(command.id(), documentId, excess);
    }
}
