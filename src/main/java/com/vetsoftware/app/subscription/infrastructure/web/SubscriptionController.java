package com.vetsoftware.app.subscription.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.subscription.application.command.AddSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.command.CancelSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionItemQuantityCommand;
import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionStatusCommand;
import com.vetsoftware.app.subscription.application.command.CreateRequestedSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.RemoveSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.command.RequestedSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.command.SubscriptionItemLineCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionAmendmentDto;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemDto;
import com.vetsoftware.app.subscription.application.dto.SubscriptionStatusChangeDto;
import com.vetsoftware.app.subscription.application.port.in.AddSubscriptionItemUseCase;
import com.vetsoftware.app.subscription.application.port.in.CancelSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.in.ChangeSubscriptionItemQuantityUseCase;
import com.vetsoftware.app.subscription.application.port.in.ChangeSubscriptionStatusUseCase;
import com.vetsoftware.app.subscription.application.port.in.CreateRequestedSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.in.FindCurrentSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.in.FindSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.in.ListSubscriptionAmendmentsUseCase;
import com.vetsoftware.app.subscription.application.port.in.ListSubscriptionItemsUseCase;
import com.vetsoftware.app.subscription.application.port.in.ListSubscriptionStatusHistoryUseCase;
import com.vetsoftware.app.subscription.application.port.in.ListSubscriptionsByCompanyUseCase;
import com.vetsoftware.app.subscription.application.port.in.RemoveSubscriptionItemUseCase;
import com.vetsoftware.app.subscription.infrastructure.web.request.AddSubscriptionItemRequest;
import com.vetsoftware.app.subscription.infrastructure.web.request.CancelSubscriptionRequest;
import com.vetsoftware.app.subscription.infrastructure.web.request.ChangeSubscriptionItemQuantityRequest;
import com.vetsoftware.app.subscription.infrastructure.web.request.ChangeSubscriptionStatusRequest;
import com.vetsoftware.app.subscription.infrastructure.web.request.CreateSubscriptionRequest;
import com.vetsoftware.app.subscription.infrastructure.web.request.RemoveSubscriptionItemRequest;
import com.vetsoftware.app.subscription.infrastructure.web.request.RequestedSubscriptionItemRequest;
import com.vetsoftware.app.subscription.infrastructure.web.request.SubscriptionItemLineRequest;
import com.vetsoftware.app.subscription.infrastructure.web.response.SubscriptionAmendmentResponse;
import com.vetsoftware.app.subscription.infrastructure.web.response.SubscriptionItemResponse;
import com.vetsoftware.app.subscription.infrastructure.web.response.SubscriptionResponse;
import com.vetsoftware.app.subscription.infrastructure.web.response.SubscriptionStatusChangeResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Los endpoints de UN contrato: los del tenant y los que la plataforma opera
 * sobre una clinica concreta. <strong>Ningun cuerpo lleva
 * {@code companyId}</strong>: lo inyecta este controller con
 * {@code authz.currentCompanyId()} y el puerto lo revalida con
 * {@code @authz.isMyCompany(#command.companyId)}, que es defensa en profundidad
 * contra otro caller o un bug futuro.
 *
 * <p>
 * <strong>Cuidado: no todos sus puertos tienen rama de tenant, y eso es
 * deliberado.</strong> La frase de arriba describe los que la tienen; los otros
 * responden 403 a un empleado, a proposito. El reparto, con el motivo escrito
 * en el javadoc de cada puerto:
 *
 * <ul>
 * <li><strong>Del tenant</strong> — todas las lecturas, mas
 * {@code PATCH /{id}/items/remove}, {@code POST /{id}/items/quantity} y
 * {@code PATCH /{id}/cancel}. Ninguno de los tres recibe precio en el cuerpo:
 * el cliente elige cuantas unidades o si se va, nunca a cuanto.
 * <li><strong>Solo plataforma</strong> — {@code POST /subscriptions} (fija
 * estado, prueba, compromiso y dias de gracia), {@code POST /{id}/items} (el
 * cuerpo trae {@code unitAmount}, asi que abrirlo seria un alta gratuita
 * autoservida) y {@code PATCH /{id}/status} (es la palanca de cobro).
 * </ul>
 *
 * <p>
 * La vista que cruza todas las clinicas es otra cosa y vive en
 * {@code SubscriptionAdminController}.
 *
 * <p>
 * <strong>Y ningun cuerpo lleva quien firma la enmienda</strong>, por el mismo
 * motivo. {@code requested_by_employee_id} y
 * {@code requested_by_system_user_id} salen del principal
 * —{@code authz.currentEmployeeIdOrNull()} y
 * {@code authz.currentSystemUserIdOrNull()}—, no de la peticion: una columna de
 * firma que escribe el propio firmante no prueba nada, y con ella en el cuerpo
 * cualquiera con acceso al endpoint podria atribuirle a otro haber pedido el
 * cambio. Son dos campos distintos porque la responsabilidad es distinta —el
 * cliente desde su cuenta, o alguien de la plataforma— y del mismo principal
 * sale exactamente uno de los dos: el dominio rechaza que vengan los dos o
 * ninguno.
 *
 * <p>
 * <strong>Ni los numeros citables.</strong> {@code subscription_number} y
 * {@code amendment_number} los reserva el servidor de forma serializada: un
 * numero que se cita en soporte y en cobranza no lo puede elegir quien llama.
 *
 * <p>
 * La vista de plataforma —todos los contratos de todas las clinicas y la
 * vigilancia de solapes— vive en {@code SubscriptionAdminController}, cerrada a
 * SYSTEM.
 */
@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final CreateRequestedSubscriptionUseCase createUseCase;
    private final FindSubscriptionUseCase findUseCase;
    private final FindCurrentSubscriptionUseCase findCurrentUseCase;
    private final ListSubscriptionsByCompanyUseCase listByCompanyUseCase;
    private final ListSubscriptionItemsUseCase listItemsUseCase;
    private final ListSubscriptionAmendmentsUseCase listAmendmentsUseCase;
    private final ListSubscriptionStatusHistoryUseCase listHistoryUseCase;
    private final AddSubscriptionItemUseCase addItemUseCase;
    private final RemoveSubscriptionItemUseCase removeItemUseCase;
    private final ChangeSubscriptionItemQuantityUseCase changeQuantityUseCase;
    private final ChangeSubscriptionStatusUseCase changeStatusUseCase;
    private final CancelSubscriptionUseCase cancelUseCase;
    private final Authz authz;

    public SubscriptionController(CreateRequestedSubscriptionUseCase createUseCase,
            FindSubscriptionUseCase findUseCase, FindCurrentSubscriptionUseCase findCurrentUseCase,
            ListSubscriptionsByCompanyUseCase listByCompanyUseCase,
            ListSubscriptionItemsUseCase listItemsUseCase,
            ListSubscriptionAmendmentsUseCase listAmendmentsUseCase,
            ListSubscriptionStatusHistoryUseCase listHistoryUseCase,
            AddSubscriptionItemUseCase addItemUseCase,
            RemoveSubscriptionItemUseCase removeItemUseCase,
            ChangeSubscriptionItemQuantityUseCase changeQuantityUseCase,
            ChangeSubscriptionStatusUseCase changeStatusUseCase,
            CancelSubscriptionUseCase cancelUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.findUseCase = findUseCase;
        this.findCurrentUseCase = findCurrentUseCase;
        this.listByCompanyUseCase = listByCompanyUseCase;
        this.listItemsUseCase = listItemsUseCase;
        this.listAmendmentsUseCase = listAmendmentsUseCase;
        this.listHistoryUseCase = listHistoryUseCase;
        this.addItemUseCase = addItemUseCase;
        this.removeItemUseCase = removeItemUseCase;
        this.changeQuantityUseCase = changeQuantityUseCase;
        this.changeStatusUseCase = changeStatusUseCase;
        this.cancelUseCase = cancelUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse create(@Valid @RequestBody CreateSubscriptionRequest request) {
        return toResponse(createUseCase.execute(new CreateRequestedSubscriptionCommand(
                authz.currentCompanyId(), request.quoteId(), request.priceListId(),
                request.billingCycle(), request.status(), request.startDate(),
                request.trialEndDate(), request.currentPeriodStart(), request.currentPeriodEnd(),
                request.nextBillingDate(), request.commitmentEndDate(), request.graceDays(),
                request.autoRenew(), toRequestedItems(request.items()))));
    }

    @GetMapping
    public PageResponse<SubscriptionResponse> listByCompany(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listByCompanyUseCase.listByCompany(authz.currentCompanyId(), page, pageSize),
                this::toResponse);
    }

    /** El contrato vigente de mi empresa. Como maximo hay uno. */
    @GetMapping("/current")
    public SubscriptionResponse findCurrent() {
        return toResponse(findCurrentUseCase.findCurrent(authz.currentCompanyId()));
    }

    @GetMapping("/{id}")
    public SubscriptionResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    /**
     * Lo contratado. Con {@code onDate} responde que tenia la clinica ese dia; sin
     * el, devuelve el expediente completo, con las lineas ya cerradas incluidas
     * —que siguen ahi, porque dar de baja no borra—.
     */
    @GetMapping("/{id}/items")
    public PageResponse<SubscriptionItemResponse> listItems(@PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate onDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listItemsUseCase.listAll(id, authz.currentCompanyId(), onDate, page, pageSize),
                this::toResponse);
    }

    @GetMapping("/{id}/amendments")
    public PageResponse<SubscriptionAmendmentResponse> listAmendments(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listAmendmentsUseCase.listAll(id, authz.currentCompanyId(), page, pageSize),
                this::toResponse);
    }

    @GetMapping("/{id}/status-history")
    public PageResponse<SubscriptionStatusChangeResponse> listStatusHistory(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listHistoryUseCase.listAll(id, authz.currentCompanyId(), page, pageSize),
                this::toResponse);
    }

    @PostMapping("/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionItemResponse addItem(@PathVariable Long id,
            @Valid @RequestBody AddSubscriptionItemRequest request) {
        return toResponse(
                addItemUseCase.execute(new AddSubscriptionItemCommand(id, authz.currentCompanyId(),
                        request.clientRequestId(), request.effectiveDate(), request.reason(),
                        authz.currentEmployeeIdOrNull(), authz.currentSystemUserIdOrNull(),
                        request.quoteId(), toLineCommand(request.line()))));
    }

    /**
     * Baja de linea. Es {@code PATCH} y no {@code DELETE} a proposito: no se borra
     * nada, se le escribe la fecha de fin.
     */
    @PatchMapping("/{id}/items/remove")
    public SubscriptionItemResponse removeItem(@PathVariable Long id,
            @Valid @RequestBody RemoveSubscriptionItemRequest request) {
        return toResponse(removeItemUseCase.execute(new RemoveSubscriptionItemCommand(id,
                authz.currentCompanyId(), request.subscriptionItemId(), request.clientRequestId(),
                request.effectiveDate(), request.reason(), authz.currentEmployeeIdOrNull(),
                authz.currentSystemUserIdOrNull())));
    }

    /** Devuelve la linea sucesora: la original queda cerrada, no modificada. */
    @PostMapping("/{id}/items/quantity")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionItemResponse changeItemQuantity(@PathVariable Long id,
            @Valid @RequestBody ChangeSubscriptionItemQuantityRequest request) {
        return toResponse(changeQuantityUseCase.execute(new ChangeSubscriptionItemQuantityCommand(
                id, authz.currentCompanyId(), request.subscriptionItemId(), request.newQuantity(),
                request.clientRequestId(), request.effectiveDate(), request.reason(),
                authz.currentEmployeeIdOrNull(), authz.currentSystemUserIdOrNull())));
    }

    @PatchMapping("/{id}/status")
    public SubscriptionResponse changeStatus(@PathVariable Long id,
            @Valid @RequestBody ChangeSubscriptionStatusRequest request) {
        return toResponse(changeStatusUseCase.execute(new ChangeSubscriptionStatusCommand(id,
                authz.currentCompanyId(), request.status(), request.reason(), request.actor())));
    }

    /**
     * Pide la baja. No cambia el estado: el contrato sigue vigente hasta la fecha
     * efectiva, que es lo que el cliente ya pago.
     */
    @PatchMapping("/{id}/cancel")
    public SubscriptionResponse cancel(@PathVariable Long id,
            @Valid @RequestBody CancelSubscriptionRequest request) {
        return toResponse(cancelUseCase.execute(
                new CancelSubscriptionCommand(id, authz.currentCompanyId(), request.requestedAt(),
                        request.effectiveDate(), request.reason(), request.clientRequestId(),
                        authz.currentEmployeeIdOrNull(), authz.currentSystemUserIdOrNull())));
    }

    private static List<SubscriptionItemLineCommand> toLineCommands(
            List<SubscriptionItemLineRequest> lines) {
        return lines == null
                ? List.of()
                : lines.stream().map(SubscriptionController::toLineCommand).toList();
    }

    private static List<RequestedSubscriptionItemCommand> toRequestedItems(
            List<RequestedSubscriptionItemRequest> lines) {
        if (lines == null)
            return List.of();
        return lines.stream().map(line -> new RequestedSubscriptionItemCommand(line.catalogItemId(),
                line.quantity(), line.effectiveFrom(), line.effectiveTo())).toList();
    }

    private static SubscriptionItemLineCommand toLineCommand(SubscriptionItemLineRequest line) {
        return line == null
                ? null
                : new SubscriptionItemLineCommand(line.catalogItemId(), line.itemCode(),
                        line.itemName(), line.itemType(), line.capacityUnit(),
                        line.includedQuantity(), line.taxTreatment(), line.quantity(),
                        line.unitAmount(), line.taxRate(), line.effectiveFrom(),
                        line.effectiveTo());
    }

    private SubscriptionResponse toResponse(SubscriptionDto dto) {
        return new SubscriptionResponse(dto.id(), dto.subscriptionNumber(), dto.companyId(),
                dto.quoteId(), dto.priceListId(), dto.billingCycle(), dto.status(), dto.current(),
                dto.startDate(), dto.trialEndDate(), dto.currentPeriodStart(),
                dto.currentPeriodEnd(), dto.nextBillingDate(), dto.commitmentEndDate(),
                dto.graceDays(), dto.pastDueSince(), dto.autoRenew(), dto.cancelRequestedAt(),
                dto.cancelEffectiveDate(), dto.cancelReason(), dto.createdDate(), dto.enabled());
    }

    private SubscriptionItemResponse toResponse(SubscriptionItemDto dto) {
        return new SubscriptionItemResponse(dto.id(), dto.companyId(), dto.subscriptionId(),
                dto.catalogItemId(), dto.itemCode(), dto.itemName(), dto.itemType(),
                dto.capacityUnit(), dto.includedQuantity(), dto.taxTreatment(), dto.quantity(),
                dto.billableQuantity(), dto.unitAmount(), dto.taxRate(), dto.effectiveFrom(),
                dto.effectiveTo(), dto.origin(), dto.createdAmendmentId(), dto.endedAmendmentId(),
                dto.createdDate(), dto.enabled());
    }

    private SubscriptionAmendmentResponse toResponse(SubscriptionAmendmentDto dto) {
        return new SubscriptionAmendmentResponse(dto.id(), dto.companyId(), dto.subscriptionId(),
                dto.amendmentNumber(), dto.amendmentType(), dto.effectiveDate(), dto.reason(),
                dto.requestedByEmployeeId(), dto.requestedBySystemUserId(), dto.prorationAmount(),
                dto.monthlyDeltaAmount(), dto.quoteId(), dto.clientRequestId(), dto.createdDate());
    }

    private SubscriptionStatusChangeResponse toResponse(SubscriptionStatusChangeDto dto) {
        return new SubscriptionStatusChangeResponse(dto.id(), dto.companyId(), dto.subscriptionId(),
                dto.fromStatus(), dto.toStatus(), dto.reason(), dto.occurredAt(), dto.actor(),
                dto.createdDate());
    }
}
