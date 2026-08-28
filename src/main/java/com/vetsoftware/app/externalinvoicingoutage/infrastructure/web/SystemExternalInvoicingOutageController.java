package com.vetsoftware.app.externalinvoicingoutage.infrastructure.web;

import com.vetsoftware.app.externalinvoicingoutage.application.command.EndExternalInvoicingOutageCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.command.NotifyAffectedCompaniesCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.command.OpenExternalInvoicingOutageCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.command.RegisterAffectedCompanyCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.EndExternalInvoicingOutageUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.FindExternalInvoicingOutageUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.ListExternalInvoicingOutagesUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.ListOpenExternalInvoicingOutagesUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.ListOutageAffectedCompaniesUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.NotifyAffectedCompaniesUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.OpenExternalInvoicingOutageUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.RegisterAffectedCompanyUseCase;
import com.vetsoftware.app.externalinvoicingoutage.infrastructure.web.request.EndExternalInvoicingOutageRequest;
import com.vetsoftware.app.externalinvoicingoutage.infrastructure.web.request.NotifyAffectedCompaniesRequest;
import com.vetsoftware.app.externalinvoicingoutage.infrastructure.web.request.OpenExternalInvoicingOutageRequest;
import com.vetsoftware.app.externalinvoicingoutage.infrastructure.web.request.RegisterAffectedCompanyRequest;
import com.vetsoftware.app.externalinvoicingoutage.infrastructure.web.response.ExternalInvoicingOutageResponse;
import com.vetsoftware.app.externalinvoicingoutage.infrastructure.web.response.OutageAffectedCompanyResponse;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
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
 * El expediente de las caidas de la emision fiscal, entero: abrir, cerrar,
 * anotar el aviso y repartir por clinica.
 *
 * <p>
 * <strong>Consola de plataforma y solo consola de plataforma.</strong> Los ocho
 * puertos de entrada llevan {@code hasRole('SYSTEM')} a secas, sin alternativa
 * por permiso, y esa uniformidad es lo que exige
 * {@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM}: en una feature cerrada a SYSTEM
 * una authority suelta seria un endpoint que se abre sembrando un permiso,
 * sobre un registro que habla de todos los tenants a la vez.
 *
 * <p>
 * <strong>Lo que el cliente puede ver es que hubo una caida; nunca a cuantos
 * alcanzo ni a quienes.</strong> El reparto por clinica —el
 * {@code failed_document_count} de cada una, y con el la lista de quienes
 * usaron numeracion de contingencia— no sale por ningun puerto de cliente.
 * Cuando exista la vista del tenant sera un caso de uso hermano que reciba
 * {@code companyId} y devuelva <em>su</em> fila, jamas este listado con otro
 * gate.
 *
 * <p>
 * <strong>Aqui no hay {@code companyId} salvo en la ruta del reparto, y esa
 * excepcion se explica igual que en tesoreria</strong>: un principal SYSTEM no
 * tiene empresa propia y elige a que clinica afecta. Va como
 * {@code @PathVariable} y no en el cuerpo porque
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} prohibe el cuerpo, donde la empresa
 * convierte cualquier comprobacion de propiedad en una comparacion del numero
 * consigo mismo.
 *
 * <p>
 * <strong>No hay endpoint de borrado, ni de la caida ni del reparto.</strong>
 * Una caida que termino sigue siendo la explicacion correcta del hueco en la
 * numeracion de sus clinicas; lo que se hace es <em>cerrarla</em>, que ademas
 * es lo que libera el hueco de {@code uq_eio_open} para la siguiente del mismo
 * causante. Y quitar una clinica del reparto destruiria la prueba de que se le
 * aviso.
 */
@RestController
@RequestMapping("/system/external-invoicing-outages")
public class SystemExternalInvoicingOutageController {

    private final OpenExternalInvoicingOutageUseCase openUseCase;
    private final EndExternalInvoicingOutageUseCase endUseCase;
    private final NotifyAffectedCompaniesUseCase notifyUseCase;
    private final FindExternalInvoicingOutageUseCase findUseCase;
    private final ListExternalInvoicingOutagesUseCase listUseCase;
    private final ListOpenExternalInvoicingOutagesUseCase listOpenUseCase;
    private final RegisterAffectedCompanyUseCase registerAffectedUseCase;
    private final ListOutageAffectedCompaniesUseCase listAffectedUseCase;

    public SystemExternalInvoicingOutageController(OpenExternalInvoicingOutageUseCase openUseCase,
            EndExternalInvoicingOutageUseCase endUseCase,
            NotifyAffectedCompaniesUseCase notifyUseCase,
            FindExternalInvoicingOutageUseCase findUseCase,
            ListExternalInvoicingOutagesUseCase listUseCase,
            ListOpenExternalInvoicingOutagesUseCase listOpenUseCase,
            RegisterAffectedCompanyUseCase registerAffectedUseCase,
            ListOutageAffectedCompaniesUseCase listAffectedUseCase) {
        this.openUseCase = openUseCase;
        this.endUseCase = endUseCase;
        this.notifyUseCase = notifyUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listOpenUseCase = listOpenUseCase;
        this.registerAffectedUseCase = registerAffectedUseCase;
        this.listAffectedUseCase = listAffectedUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExternalInvoicingOutageResponse open(
            @Valid @RequestBody OpenExternalInvoicingOutageRequest request) {
        return ExternalInvoicingOutageResponse.from(
                openUseCase.execute(new OpenExternalInvoicingOutageCommand(request.startedAt(),
                        request.causeParty(), request.summary(), request.affectedCompanyCount(),
                        request.externalIncidentRef())));
    }

    /**
     * {@code PATCH} y no {@code DELETE}: cerrar una caida es escribir una hora en
     * una fila que se queda, no retirarla.
     */
    @PatchMapping("/{id}/end")
    public ExternalInvoicingOutageResponse end(@PathVariable Long id,
            @Valid @RequestBody EndExternalInvoicingOutageRequest request) {
        return ExternalInvoicingOutageResponse.from(
                endUseCase.execute(new EndExternalInvoicingOutageCommand(id, request.endedAt())));
    }

    @PatchMapping("/{id}/notify")
    public ExternalInvoicingOutageResponse notifyCompanies(@PathVariable Long id,
            @Valid @RequestBody NotifyAffectedCompaniesRequest request) {
        return ExternalInvoicingOutageResponse
                .from(notifyUseCase.execute(new NotifyAffectedCompaniesCommand(id,
                        request.notifiedAt(), request.affectedCompanyCount())));
    }

    @GetMapping("/{id}")
    public ExternalInvoicingOutageResponse find(@PathVariable Long id) {
        return ExternalInvoicingOutageResponse.from(findUseCase.execute(id));
    }

    @GetMapping
    public PageResponse<ExternalInvoicingOutageResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(page, pageSize),
                ExternalInvoicingOutageResponse::from);
    }

    /**
     * Que esta caido ahora mismo. <b>Sin paginar</b>: {@code uq_eio_open} acota el
     * resultado a una fila por causante, y los causantes son cuatro.
     */
    @GetMapping("/open")
    public List<ExternalInvoicingOutageResponse> listOpen() {
        return listOpenUseCase.listOpen().stream().map(ExternalInvoicingOutageResponse::from)
                .toList();
    }

    /**
     * Mete a una clinica en el reparto.
     *
     * <p>
     * La empresa va en la ruta y no en el cuerpo
     * ({@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}).
     */
    @PostMapping("/{id}/companies/{companyId}")
    @ResponseStatus(HttpStatus.CREATED)
    public OutageAffectedCompanyResponse registerAffectedCompany(@PathVariable Long id,
            @PathVariable Long companyId,
            @Valid @RequestBody RegisterAffectedCompanyRequest request) {
        return OutageAffectedCompanyResponse
                .from(registerAffectedUseCase.execute(new RegisterAffectedCompanyCommand(id,
                        companyId, request.failedDocumentCount(), request.resolvedBy())));
    }

    @GetMapping("/{id}/companies")
    public PageResponse<OutageAffectedCompanyResponse> listAffectedCompanies(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listAffectedUseCase.listByOutage(id, page, pageSize),
                OutageAffectedCompanyResponse::from);
    }
}
