package com.vetsoftware.app.quote.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.quote.application.command.AcceptQuoteCommand;
import com.vetsoftware.app.quote.application.command.CreateQuoteCommand;
import com.vetsoftware.app.quote.application.command.QuoteAnswerCommand;
import com.vetsoftware.app.quote.application.command.QuoteLineCommand;
import com.vetsoftware.app.quote.application.command.RejectQuoteCommand;
import com.vetsoftware.app.quote.application.command.SendQuoteCommand;
import com.vetsoftware.app.quote.application.dto.CompanySummaryDto;
import com.vetsoftware.app.quote.application.dto.QuoteAnswerDto;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.dto.QuoteLineDto;
import com.vetsoftware.app.quote.application.dto.QuoteSummaryDto;
import com.vetsoftware.app.quote.application.dto.QuoteTotalsMismatchDto;
import com.vetsoftware.app.quote.application.port.in.AcceptQuoteUseCase;
import com.vetsoftware.app.quote.application.port.in.CreateQuoteUseCase;
import com.vetsoftware.app.quote.application.port.in.DeleteQuoteUseCase;
import com.vetsoftware.app.quote.application.port.in.ExpireOverdueQuotesUseCase;
import com.vetsoftware.app.quote.application.port.in.FindQuoteTotalsMismatchesUseCase;
import com.vetsoftware.app.quote.application.port.in.FindQuoteUseCase;
import com.vetsoftware.app.quote.application.port.in.ListQuotesByCompanyUseCase;
import com.vetsoftware.app.quote.application.port.in.ListQuotesUseCase;
import com.vetsoftware.app.quote.application.port.in.RejectQuoteUseCase;
import com.vetsoftware.app.quote.application.port.in.SendQuoteUseCase;
import com.vetsoftware.app.quote.infrastructure.web.request.AcceptQuoteRequest;
import com.vetsoftware.app.quote.infrastructure.web.request.CreateQuoteRequest;
import com.vetsoftware.app.quote.infrastructure.web.request.QuoteAnswerRequest;
import com.vetsoftware.app.quote.infrastructure.web.request.QuoteLineRequest;
import com.vetsoftware.app.quote.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.quote.infrastructure.web.response.QuoteAnswerResponse;
import com.vetsoftware.app.quote.infrastructure.web.response.QuoteLineResponse;
import com.vetsoftware.app.quote.infrastructure.web.response.QuoteResponse;
import com.vetsoftware.app.quote.infrastructure.web.response.QuoteSummaryResponse;
import com.vetsoftware.app.quote.infrastructure.web.response.QuoteTotalsMismatchResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de la cotizacion.
 *
 * <p>
 * <b>El companyId no entra por el cuerpo en ninguno.</b> Lo pone este
 * controller desde el principal, y el puerto lo revalida con
 * {@code @authz.isMyCompany} como defensa en profundidad.
 *
 * <p>
 * Los caminos de tenant usan {@code currentCompanyIdOrNull()} y no
 * {@code currentCompanyId()}, y eso es el caso raro de este modelo: para un
 * empleado devuelve su empresa; para un principal SYSTEM devuelve null, que es
 * exactamente lo que necesita una cotizacion a un prospecto que todavia no es
 * cliente. Es la unica feature del bloque donde el null es un valor legitimo y
 * no un fallo de contexto.
 */
@RestController
@RequestMapping("/quotes")
public class QuoteController {

    private final CreateQuoteUseCase createUseCase;
    private final FindQuoteUseCase findUseCase;
    private final ListQuotesByCompanyUseCase listByCompanyUseCase;
    private final ListQuotesUseCase listUseCase;
    private final SendQuoteUseCase sendUseCase;
    private final AcceptQuoteUseCase acceptUseCase;
    private final RejectQuoteUseCase rejectUseCase;
    private final DeleteQuoteUseCase deleteUseCase;
    private final ExpireOverdueQuotesUseCase expireOverdueUseCase;
    private final FindQuoteTotalsMismatchesUseCase totalsMismatchesUseCase;
    private final Authz authz;

    public QuoteController(CreateQuoteUseCase createUseCase, FindQuoteUseCase findUseCase,
            ListQuotesByCompanyUseCase listByCompanyUseCase, ListQuotesUseCase listUseCase,
            SendQuoteUseCase sendUseCase, AcceptQuoteUseCase acceptUseCase,
            RejectQuoteUseCase rejectUseCase, DeleteQuoteUseCase deleteUseCase,
            ExpireOverdueQuotesUseCase expireOverdueUseCase,
            FindQuoteTotalsMismatchesUseCase totalsMismatchesUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.findUseCase = findUseCase;
        this.listByCompanyUseCase = listByCompanyUseCase;
        this.listUseCase = listUseCase;
        this.sendUseCase = sendUseCase;
        this.acceptUseCase = acceptUseCase;
        this.rejectUseCase = rejectUseCase;
        this.deleteUseCase = deleteUseCase;
        this.expireOverdueUseCase = expireOverdueUseCase;
        this.totalsMismatchesUseCase = totalsMismatchesUseCase;
        this.authz = authz;
    }

    /**
     * Devuelve 201 tambien en el reintento idempotente. Es deliberado: el contrato
     * de una peticion repetida con la misma llave es "el mismo codigo de estado que
     * la primera vez", no un 200 que le haga creer al cliente que hizo otra cosa.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuoteResponse create(@Valid @RequestBody CreateQuoteRequest request) {
        return toResponse(createUseCase.execute(new CreateQuoteCommand(request.clientRequestId(),
                authz.currentCompanyIdOrNull(), request.prospectName(), request.prospectEmail(),
                request.prospectDocument(), request.prospectPhone(), request.priceListId(),
                request.billingCycle(), request.validUntil(), request.trialDays(),
                toLineCommands(request.lines()), toAnswerCommands(request.answers()))));
    }

    @GetMapping("/{id}")
    public QuoteResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyIdOrNull()));
    }

    /** Las cotizaciones de la empresa del principal. */
    @GetMapping
    public PageResponse<QuoteSummaryResponse> listMine(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listByCompanyUseCase.listByCompany(authz.currentCompanyId(), page, pageSize),
                QuoteController::toSummaryResponse);
    }

    /**
     * El embudo completo de la consola de plataforma. Sin filtro de empresa:
     * SYSTEM.
     */
    @GetMapping("/platform")
    public PageResponse<QuoteSummaryResponse> listAll(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(page, pageSize),
                QuoteController::toSummaryResponse);
    }

    @PostMapping("/{id}/send")
    public QuoteResponse send(@PathVariable Long id) {
        return toResponse(
                sendUseCase.execute(new SendQuoteCommand(id, authz.currentCompanyIdOrNull())));
    }

    /**
     * La IP sale de la peticion, nunca del cuerpo: es prueba de la aceptacion y una
     * prueba que el cliente escribe no prueba nada. {@code getRemoteAddr()} es
     * proxy-aware por configuracion del servidor, como en
     * {@code RegistrationController}.
     */
    @PostMapping("/{id}/accept")
    public QuoteResponse accept(@PathVariable Long id,
            @Valid @RequestBody AcceptQuoteRequest request, HttpServletRequest httpRequest) {
        return toResponse(
                acceptUseCase.execute(new AcceptQuoteCommand(id, authz.currentCompanyIdOrNull(),
                        request.acceptedByEmail(), httpRequest.getRemoteAddr())));
    }

    @PostMapping("/{id}/reject")
    public QuoteResponse reject(@PathVariable Long id) {
        return toResponse(
                rejectUseCase.execute(new RejectQuoteCommand(id, authz.currentCompanyIdOrNull())));
    }

    /**
     * Barrido de plataforma. Devuelve cuantas marco EXPIRED.
     *
     * <p>
     * Lo ejecuta a diario {@code QuoteExpirationJob}; esta ruta se conserva para
     * operarlo a mano cuando haga falta, no para que una pantalla ponga un boton
     * (incidencia #443).
     */
    @PostMapping("/expire-overdue")
    public int expireOverdue(@RequestParam(defaultValue = "200") int batchSize) {
        return expireOverdueUseCase.expireOverdue(batchSize);
    }

    /**
     * Vigilancia de R5: las cotizaciones cuya cabecera ya no cuadra con la suma de
     * sus lineas activas. <strong>Lista vacia = sano.</strong>
     *
     * <p>
     * La ruta literal va declarada ANTES que {@code /{id}} para que se lea de un
     * vistazo cual gana, aunque el emparejador prefiera el segmento literal sobre
     * la plantilla por si mismo. Incidencia #428.
     */
    @GetMapping("/totals-mismatches")
    public List<QuoteTotalsMismatchResponse> totalsMismatches() {
        return totalsMismatchesUseCase.findAllTotalsMismatches().stream()
                .map(QuoteController::toTotalsMismatchResponse).toList();
    }

    private static QuoteTotalsMismatchResponse toTotalsMismatchResponse(
            QuoteTotalsMismatchDto dto) {
        return new QuoteTotalsMismatchResponse(dto.quoteId(), dto.quoteNumber(), dto.companyId(),
                dto.headerDiscountAmount(), dto.linesDiscountAmount(), dto.headerTaxAmount(),
                dto.linesTaxAmount(), dto.headerTotalAmount(), dto.linesTotalAmount());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyIdOrNull());
    }

    private static List<QuoteLineCommand> toLineCommands(List<QuoteLineRequest> lines) {
        return lines == null
                ? List.of()
                : lines.stream().map(l -> new QuoteLineCommand(l.catalogItemId(), l.quantity(),
                        l.discountPercent())).toList();
    }

    private static List<QuoteAnswerCommand> toAnswerCommands(List<QuoteAnswerRequest> answers) {
        return answers == null
                ? List.of()
                : answers.stream().map(
                        a -> new QuoteAnswerCommand(a.questionId(), a.optionId(), a.answerValue()))
                        .toList();
    }

    private static QuoteResponse toResponse(QuoteDto dto) {
        return new QuoteResponse(dto.id(), dto.quoteNumber(), toCompanySummary(dto.company()),
                dto.prospectName(), dto.prospectEmail(), dto.prospectDocument(),
                dto.prospectPhone(), dto.priceListId(), dto.billingCycle(), dto.subtotalAmount(),
                dto.discountAmount(), dto.taxAmount(), dto.totalAmount(), dto.status(),
                dto.validUntil(), dto.trialDays(), dto.acceptedAt(), dto.acceptedByEmail(),
                dto.acceptedIp(), dto.clientRequestId(),
                dto.lines().stream().map(QuoteController::toLineResponse).toList(),
                dto.answers().stream().map(QuoteController::toAnswerResponse).toList(),
                dto.createdDate(), dto.enabled());
    }

    private static QuoteSummaryResponse toSummaryResponse(QuoteSummaryDto dto) {
        return new QuoteSummaryResponse(dto.id(), dto.quoteNumber(),
                toCompanySummary(dto.company()), dto.prospectName(), dto.prospectEmail(),
                dto.priceListId(), dto.billingCycle(), dto.subtotalAmount(), dto.discountAmount(),
                dto.taxAmount(), dto.totalAmount(), dto.status(), dto.validUntil(), dto.trialDays(),
                dto.acceptedAt(), dto.createdDate(), dto.enabled());
    }

    private static CompanySummary toCompanySummary(CompanySummaryDto dto) {
        return dto == null ? null : new CompanySummary(dto.id(), dto.name(), dto.identifier());
    }

    private static QuoteLineResponse toLineResponse(QuoteLineDto dto) {
        return new QuoteLineResponse(dto.id(), dto.lineNumber(), dto.catalogItemId(),
                dto.itemCode(), dto.itemName(), dto.itemType(), dto.contractedQuantity(),
                dto.includedQuantity(), dto.quantity(), dto.unitAmount(), dto.grossAmount(),
                dto.discountPercent(), dto.discountAmount(), dto.taxRate(), dto.taxTreatment(),
                dto.taxAmount(), dto.lineTotal(), dto.enabled());
    }

    private static QuoteAnswerResponse toAnswerResponse(QuoteAnswerDto dto) {
        return new QuoteAnswerResponse(dto.id(), dto.questionId(), dto.optionId(),
                dto.questionCode(), dto.answerValue(), dto.enabled());
    }
}
