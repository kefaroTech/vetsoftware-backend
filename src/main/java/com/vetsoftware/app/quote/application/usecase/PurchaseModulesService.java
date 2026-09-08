package com.vetsoftware.app.quote.application.usecase;

import com.vetsoftware.app.quote.application.command.PurchaseModulesCommand;
import com.vetsoftware.app.quote.application.command.SelfServeQuoteCommand;
import com.vetsoftware.app.quote.application.command.SelfServeQuoteLineCommand;
import com.vetsoftware.app.quote.application.dto.PurchaseModulesResultDto;
import com.vetsoftware.app.quote.application.dto.PurchasedModuleLineDto;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.dto.QuoteLineDto;
import com.vetsoftware.app.quote.application.port.in.PurchaseModulesUseCase;
import com.vetsoftware.app.quote.application.port.in.SelfServeQuoteUseCase;
import com.vetsoftware.app.quote.application.port.out.EmployeeAdminCheckPort;
import com.vetsoftware.app.quote.application.port.out.EmployeeEmailQueryPort;
import com.vetsoftware.app.quote.application.port.out.ImmediatePaidLineOpeningPort;
import com.vetsoftware.app.quote.application.port.out.ModuleLineSuccessionPort;
import com.vetsoftware.app.quote.application.port.out.ModuleLineSuccessionPort.CurrentLine;
import com.vetsoftware.app.quote.application.port.out.ModuleLineSuccessionPort.LinePriceSnapshot;
import com.vetsoftware.app.quote.application.port.out.PaymentSourceDefaultingPort;
import com.vetsoftware.app.quote.application.port.out.QuoteAuditPort;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compra módulos desde el escaparate del tenant.
 *
 * <p>
 * <strong>Genera y acepta la cotización sin duplicar precios ni
 * otrosí.</strong> El paso 1 delega en {@link SelfServeQuoteUseCase}, la misma
 * autocontratación que ya congela precio, IVA y tramos contra el catálogo. El
 * paso 2 acepta el documento con {@link Quote#accept} —el mismo método de
 * dominio que usa {@code AcceptQuoteService}—, pero <strong>sin</strong> pasar
 * por {@code SubscriptionProvisioningPort}: aquel sustituye el contrato entero
 * (cierra el vigente y firma uno nuevo), que es exactamente lo que destruiría
 * las demás líneas {@code TRIAL} de una cuenta gratuita a mitad de prueba. Lo
 * que sigue a la aceptación es una sucesión de líneas, no un contrato nuevo.
 *
 * <p>
 * <strong>Cada línea toma uno de dos caminos</strong>, y el desenlace lo decide
 * el {@code charge_mode} de su línea vigente HOY, no el catálogo:
 *
 * <ul>
 * <li><strong>Viene de una {@code TRIAL}.</strong>
 * {@link ModuleLineSuccessionPort#succeedLine} cierra la prueba el día en que
 * de todos modos iba a cerrar ({@code trialEndDate + 1}) y abre la línea
 * {@code PAID} desde ese mismo día, con {@code succeeds_item_id}. Cero cobro
 * hoy: el primer devengo lo recoge el corte de ciclo normal del contrato.</li>
 * <li>No viene de una {@code TRIAL} (sin línea, o con una
 * {@code FREE_LIMITED}/{@code EXPIRED_READ_ONLY} ya vencida): se cierra la
 * previa si la hay y se abre la de pago hoy por
 * {@link ImmediatePaidLineOpeningPort}; el primer cobro es el siguiente corte
 * de ciclo, ver ese puerto.</li>
 * </ul>
 */
@Observed(name = "quote.modules.purchase")
@Service
public class PurchaseModulesService implements PurchaseModulesUseCase {

    private static final String TRIAL = "TRIAL";
    private static final String CURRENCY = "COP";

    private final SelfServeQuoteUseCase selfServeQuoteUseCase;
    private final QuoteRepository quoteRepository;
    private final QuoteAuditPort audit;
    private final EmployeeAdminCheckPort adminCheckPort;
    private final EmployeeEmailQueryPort employeeEmailQueryPort;
    private final ModuleLineSuccessionPort successionPort;
    private final ImmediatePaidLineOpeningPort immediateOpeningPort;
    private final PaymentSourceDefaultingPort paymentSourceDefaultingPort;
    private final Clock clock;

    @SuppressWarnings("java:S107")
    public PurchaseModulesService(SelfServeQuoteUseCase selfServeQuoteUseCase,
            QuoteRepository quoteRepository, QuoteAuditPort audit,
            EmployeeAdminCheckPort adminCheckPort, EmployeeEmailQueryPort employeeEmailQueryPort,
            ModuleLineSuccessionPort successionPort,
            ImmediatePaidLineOpeningPort immediateOpeningPort,
            PaymentSourceDefaultingPort paymentSourceDefaultingPort, Clock clock) {
        this.selfServeQuoteUseCase = selfServeQuoteUseCase;
        this.quoteRepository = quoteRepository;
        this.audit = audit;
        this.adminCheckPort = adminCheckPort;
        this.employeeEmailQueryPort = employeeEmailQueryPort;
        this.successionPort = successionPort;
        this.immediateOpeningPort = immediateOpeningPort;
        this.paymentSourceDefaultingPort = paymentSourceDefaultingPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PurchaseModulesResultDto execute(PurchaseModulesCommand command) {
        if (!adminCheckPort.isCompanyAdmin(command.employeeId(), command.companyId())) {
            throw new AccessDeniedException(
                    "Only the company ADMIN role can purchase modules: " + command.employeeId());
        }
        if (command.paymentSourceId() == null)
            throw new IllegalArgumentException("paymentSourceId is required");

        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);

        QuoteDto issued = selfServeQuoteUseCase
                .execute(new SelfServeQuoteCommand(command.clientRequestId(), command.companyId(),
                        command.billingCycle(), command.catalogItemCodes().stream()
                                .map(code -> new SelfServeQuoteLineCommand(code, 1)).toList()));

        Quote quote = quoteRepository.findByIdAndCompanyId(issued.id(), command.companyId())
                .orElseThrow(() -> new QuoteNotFoundException(issued.id()));
        String acceptedByEmail = employeeEmailQueryPort
                .findEmail(command.employeeId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Employee has no email on file: " + command.employeeId()));
        quote.accept(acceptedByEmail, command.acceptedIp(), now, today);
        Quote accepted = quoteRepository.save(quote);
        audit.quoteAccepted(accepted.getId(), accepted.getQuoteNumber(), accepted.getCompanyId(),
                null, accepted.getTotalAmount(), CURRENCY, accepted.getAcceptedByEmail(),
                accepted.getAcceptedIp());

        paymentSourceDefaultingPort.markDefaultIfNeeded(command.companyId(),
                command.paymentSourceId());

        Long subscriptionId = successionPort.findCurrentSubscriptionId(command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Company has no current subscription: " + command.companyId()));

        List<PurchasedModuleLineDto> results = new ArrayList<>();
        for (QuoteLineDto line : issued.lines()) {
            results.add(succeedOrOpen(command, subscriptionId, line, today));
        }
        return new PurchaseModulesResultDto(accepted.getId(), List.copyOf(results));
    }

    private PurchasedModuleLineDto succeedOrOpen(PurchaseModulesCommand command,
            Long subscriptionId, QuoteLineDto line, LocalDate today) {
        Optional<CurrentLine> current = successionPort.findCurrentLine(command.companyId(),
                line.catalogItemId());
        if (current.isPresent() && TRIAL.equals(current.get().chargeMode())) {
            LocalDate firstChargeDate = current.get().trialEndDate().plusDays(1);
            successionPort.succeedLine(command.companyId(), subscriptionId, current.get().itemId(),
                    firstChargeDate, firstChargeDate, priceSnapshotOf(line));
            return purchasedLine(line.itemCode(), firstChargeDate, today);
        }
        current.ifPresent(previous -> successionPort.closeLine(command.companyId(),
                previous.itemId(), today));
        immediateOpeningPort.openNow(command.companyId(), subscriptionId, line.catalogItemId(),
                command.employeeId(), null, command.clientRequestId() + "-" + line.itemCode(),
                today);
        // El otrosí devenga hoy pero no cobra: el primer cobro es el siguiente corte de
        // ciclo.
        LocalDate firstChargeDate = successionPort.findNextBillingDate(command.companyId())
                .orElse(today);
        return purchasedLine(line.itemCode(), firstChargeDate, today);
    }

    private static PurchasedModuleLineDto purchasedLine(String itemCode, LocalDate firstChargeDate,
            LocalDate today) {
        return new PurchasedModuleLineDto(itemCode, firstChargeDate,
                firstChargeDate.isEqual(today));
    }

    private static LinePriceSnapshot priceSnapshotOf(QuoteLineDto line) {
        return new LinePriceSnapshot(line.catalogItemId(), line.itemCode(), line.itemName(),
                line.itemType(), line.includedQuantity(), line.taxTreatment(), line.unitAmount(),
                line.taxRate());
    }
}
