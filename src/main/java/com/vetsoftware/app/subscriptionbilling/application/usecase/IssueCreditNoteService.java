package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.subscriptionbilling.application.command.IssueCreditNoteCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.IssueCreditNoteUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentSequenceRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.domain.BillingReason;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentKind;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentNumber;
import com.vetsoftware.app.subscriptionbilling.domain.EmptyBillingDocumentException;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocument;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocumentNotFoundException;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import com.vetsoftware.app.subscriptionbilling.domain.TaxBreakdown;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emite la nota crédito que corrige un documento ya registrado, encadenándola
 * por {@code correctsDocumentId}.
 *
 * <p>
 * <b>Es el único camino para corregir un documento con factura externa.</b> El
 * original no se toca: ni su importe, ni su periodo, ni su tipo. Si se tocara,
 * lo que dice Lumbre dejaría de coincidir con lo que tiene la DIAN y no habría
 * forma de saber cuál de los dos miente. Los dos documentos quedan visibles y
 * encadenados.
 *
 * <p>
 * <b>Los cargos que agrupa tienen que ser todos negativos</b>, y lo comprueba
 * {@link TaxBreakdown}: una nota crédito que mezcle los dos signos hace que el
 * {@code ABS(SUM(...))} de la conciliación R6 deje de ser su subtotal, y
 * entonces la vigilancia miente sin devolver ninguna fila.
 */
@Observed(name = "subscription.billing.document.credit.note")
@Service
public class IssueCreditNoteService implements IssueCreditNoteUseCase {

    private static final DocumentKind KIND = DocumentKind.CREDIT_NOTE;

    private final BillingDocumentRepository documentRepository;
    private final SubscriptionChargeRepository chargeRepository;
    private final BillingDocumentSequenceRepository sequenceRepository;
    private final Clock clock;

    public IssueCreditNoteService(BillingDocumentRepository documentRepository,
            SubscriptionChargeRepository chargeRepository,
            BillingDocumentSequenceRepository sequenceRepository, Clock clock) {
        this.documentRepository = documentRepository;
        this.chargeRepository = chargeRepository;
        this.sequenceRepository = sequenceRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BillingDocumentDto execute(IssueCreditNoteCommand command) {
        SubscriptionBillingDocument corregido = documentRepository
                .findByIdAndCompanyId(command.correctsDocumentId(), command.companyId())
                .orElseThrow(() -> new SubscriptionBillingDocumentNotFoundException(
                        command.correctsDocumentId()));

        List<Long> ids = command.chargeIds() == null ? List.of() : command.chargeIds();
        if (ids.isEmpty())
            throw new EmptyBillingDocumentException(corregido.getSubscriptionId());
        List<SubscriptionCharge> charges = chargeRepository.findAllByIdsAndCompanyId(ids,
                command.companyId());
        if (charges.size() != ids.size())
            throw new IllegalArgumentException(
                    "some charges do not exist in company " + command.companyId());
        charges.stream().filter(charge -> !charge.esFacturable()).findFirst().ifPresent(charge -> {
            throw new IllegalArgumentException("charge " + charge.getId() + " is "
                    + charge.getStatus() + " and cannot be credited again");
        });

        LocalDateTime ahora = LocalDateTime.now(clock);
        TaxBreakdown breakdown = TaxBreakdown.of(charges, KIND, command.companyId(), ahora);
        DocumentNumber number = sequenceRepository.nextNumber(KIND.sequencePrefix());
        SubscriptionBillingDocument nota = SubscriptionBillingDocument.issue(number,
                command.companyId(), corregido.getSubscriptionId(), KIND, BillingReason.ADJUSTMENT,
                corregido.getPeriod(), breakdown, corregido.getId(), clock);

        SubscriptionBillingDocument saved = documentRepository.save(nota);
        int sellados = chargeRepository.sealAsInvoiced(ids, command.companyId(), saved.getId());
        if (sellados != ids.size())
            throw new IllegalStateException("expected to seal " + ids.size()
                    + " charges into credit note " + saved.getId() + " but sealed " + sellados);
        return BillingDocumentDto.from(saved);
    }
}
