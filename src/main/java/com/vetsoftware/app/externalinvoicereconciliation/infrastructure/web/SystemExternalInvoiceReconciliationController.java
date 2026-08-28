package com.vetsoftware.app.externalinvoicereconciliation.infrastructure.web;

import com.vetsoftware.app.externalinvoicereconciliation.application.command.MatchExternalInvoiceCommand;
import com.vetsoftware.app.externalinvoicereconciliation.application.command.OpenExternalInvoiceReconciliationCommand;
import com.vetsoftware.app.externalinvoicereconciliation.application.command.ResolveExternalInvoiceReconciliationCommand;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.in.FindExternalInvoiceReconciliationUseCase;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.in.ListExternalInvoiceReconciliationsUseCase;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.in.ListMissingExternalInvoicesUseCase;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.in.MatchExternalInvoiceUseCase;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.in.OpenExternalInvoiceReconciliationUseCase;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.in.ResolveExternalInvoiceReconciliationUseCase;
import com.vetsoftware.app.externalinvoicereconciliation.infrastructure.web.request.MatchExternalInvoiceRequest;
import com.vetsoftware.app.externalinvoicereconciliation.infrastructure.web.request.OpenExternalInvoiceReconciliationRequest;
import com.vetsoftware.app.externalinvoicereconciliation.infrastructure.web.request.ResolveExternalInvoiceReconciliationRequest;
import com.vetsoftware.app.externalinvoicereconciliation.infrastructure.web.response.ExternalInvoiceReconciliationResponse;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * El cuadre entre VetSoftware y su facturador externo.
 *
 * <p>
 * <strong>Este es el UNICO controller de la feature, y la ausencia de un
 * hermano de tenant es LA DECISION, no un olvido.</strong> Los demas bloques
 * del documento maestro que escribe plataforma tienen su gemelo bajo la ruta
 * del cliente porque el documento maestro los reparte como «escribe plataforma,
 * leen ambos». Este no: la conciliacion se marca <em>escribe plataforma, LEE
 * SOLO PLATAFORMA</em>, y por eso no hay ningun
 * {@code ExternalInvoiceReconciliationController} en
 * {@code /external-invoice-reconciliations} ni ningun puerto con la alternativa
 * {@code or (hasAuthority(...) and @authz.isMyCompany(...))}. Los seis casos de
 * uso van cerrados a {@code hasRole('SYSTEM')} a secas, lecturas por id
 * incluidas.
 *
 * <p>
 * <strong>El motivo, textual:</strong> la conciliacion es el cuadre entre
 * VetSoftware y su facturador externo, y ensenarsela al cliente es ensenarle el
 * margen y los datos de terceros. Lo que se publica aqui es el total propio
 * frente al del tercero, el impuesto de cada uno, el numero y el rango de una
 * resolucion de numeracion ajena, y la nota interna con la que alguien de la
 * plataforma explico un descuadre. Eso es la contabilidad de quien cobra, no el
 * expediente de quien paga.
 *
 * <p>
 * <strong>Este parrafo existe para el dia que llegue la peticion de
 * abrirla.</strong> Una clinica pedira ver «su» conciliacion; quien atienda esa
 * peticion no va a leer el changelog, va a leer esta clase y el puerto de
 * lectura. Abrirla no es anadir una ruta ni relajar un {@code @PreAuthorize}:
 * es decidir que subconjunto de estos campos puede ver quien paga la factura, y
 * eso empieza por un {@code Response} distinto —sin {@code computedTax}
 * enfrentado a {@code externalTax}, sin la resolucion del tercero y sin
 * {@code resolutionNote}—.
 *
 * <p>
 * El {@code companyId} del alta viaja como {@code @RequestParam} y no en el
 * cuerpo: lo exige la regla dura {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}, que
 * mira todo {@code @RequestBody} sin mirar la ruta ni el rol. Y no se toma de
 * {@code authz.currentCompanyId()} porque un principal SYSTEM no tiene empresa
 * propia: es la plataforma eligiendo de que clinica es el documento que
 * concilia. La proteccion no es que el servidor inyecte la empresa —no puede—
 * sino que el caso de uso esta cerrado a plataforma.
 */
@RestController
@RequestMapping("/system/external-invoice-reconciliations")
public class SystemExternalInvoiceReconciliationController {

    private final OpenExternalInvoiceReconciliationUseCase openUseCase;
    private final MatchExternalInvoiceUseCase matchUseCase;
    private final ResolveExternalInvoiceReconciliationUseCase resolveUseCase;
    private final FindExternalInvoiceReconciliationUseCase findUseCase;
    private final ListExternalInvoiceReconciliationsUseCase listUseCase;
    private final ListMissingExternalInvoicesUseCase listMissingUseCase;

    public SystemExternalInvoiceReconciliationController(
            OpenExternalInvoiceReconciliationUseCase openUseCase,
            MatchExternalInvoiceUseCase matchUseCase,
            ResolveExternalInvoiceReconciliationUseCase resolveUseCase,
            FindExternalInvoiceReconciliationUseCase findUseCase,
            ListExternalInvoiceReconciliationsUseCase listUseCase,
            ListMissingExternalInvoicesUseCase listMissingUseCase) {
        this.openUseCase = openUseCase;
        this.matchUseCase = matchUseCase;
        this.resolveUseCase = resolveUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listMissingUseCase = listMissingUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExternalInvoiceReconciliationResponse open(@RequestParam Long companyId,
            @Valid @RequestBody OpenExternalInvoiceReconciliationRequest request) {
        return ExternalInvoiceReconciliationResponse.from(openUseCase.execute(
                new OpenExternalInvoiceReconciliationCommand(companyId, request.billingDocumentId(),
                        request.computedTotal(), request.computedTax())));
    }

    /**
     * Registra la factura del tercero. Es un sub-recurso y no un {@code PATCH} de
     * la conciliacion: lo que llega es un hecho externo nuevo, no la edicion de un
     * campo, y el estado resultante no lo elige quien llama.
     */
    @PostMapping("/{id}/external-invoice")
    public ExternalInvoiceReconciliationResponse match(@PathVariable Long id,
            @Valid @RequestBody MatchExternalInvoiceRequest request) {
        return ExternalInvoiceReconciliationResponse.from(matchUseCase
                .execute(new MatchExternalInvoiceCommand(id, request.externalInvoiceId(),
                        request.externalCufe(), request.externalTotal(), request.externalTax(),
                        request.externalResolutionNumber(), request.externalRangeFrom(),
                        request.externalRangeTo(), request.resolutionValidUntil())));
    }

    @PostMapping("/{id}/resolution")
    public ExternalInvoiceReconciliationResponse resolve(@PathVariable Long id,
            @Valid @RequestBody ResolveExternalInvoiceReconciliationRequest request) {
        return ExternalInvoiceReconciliationResponse
                .from(resolveUseCase.execute(new ResolveExternalInvoiceReconciliationCommand(id,
                        request.resolvedBySystemUserId(), request.resolutionNote(),
                        request.postingPeriod())));
    }

    /**
     * <strong>La bandeja que de verdad importa</strong>, y va antes que
     * {@code /{id}} a proposito para que se lea como lo que es: un sitio al que se
     * entra, no un filtro del barrido general. Son los documentos de cobro
     * devengados que nunca recibieron factura externa —dinero que nadie facturo—, y
     * salen los mas antiguos primero.
     */
    @GetMapping("/missing-external")
    public PageResponse<ExternalInvoiceReconciliationResponse> listMissing(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listMissingUseCase.listMissing(page, pageSize),
                ExternalInvoiceReconciliationResponse::from);
    }

    @GetMapping("/{id}")
    public ExternalInvoiceReconciliationResponse findById(@PathVariable Long id) {
        return ExternalInvoiceReconciliationResponse.from(findUseCase.findById(id));
    }

    @GetMapping
    public PageResponse<ExternalInvoiceReconciliationResponse> listAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(companyId, page, pageSize),
                ExternalInvoiceReconciliationResponse::from);
    }
}
