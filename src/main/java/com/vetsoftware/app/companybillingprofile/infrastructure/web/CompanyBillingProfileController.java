package com.vetsoftware.app.companybillingprofile.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.companybillingprofile.application.command.OpenCompanyBillingProfileCommand;
import com.vetsoftware.app.companybillingprofile.application.command.SucceedCompanyBillingProfileCommand;
import com.vetsoftware.app.companybillingprofile.application.port.in.FindCompanyBillingProfileUseCase;
import com.vetsoftware.app.companybillingprofile.application.port.in.FindCurrentCompanyBillingProfileUseCase;
import com.vetsoftware.app.companybillingprofile.application.port.in.ListCompanyBillingProfilesUseCase;
import com.vetsoftware.app.companybillingprofile.application.port.in.OpenCompanyBillingProfileUseCase;
import com.vetsoftware.app.companybillingprofile.application.port.in.SucceedCompanyBillingProfileUseCase;
import com.vetsoftware.app.companybillingprofile.infrastructure.web.request.OpenCompanyBillingProfileRequest;
import com.vetsoftware.app.companybillingprofile.infrastructure.web.request.SucceedCompanyBillingProfileRequest;
import com.vetsoftware.app.companybillingprofile.infrastructure.web.response.CompanyBillingProfileResponse;
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
 * A quien se le factura la suscripcion de esta clinica.
 *
 * <h2>No hay {@code PUT} ni {@code PATCH}, y tampoco {@code DELETE}</h2>
 *
 * <p>
 * <strong>Esto es lo unico importante de este controller.</strong> Quien venga
 * buscando «como se corrige el NIT» no va a encontrar un endpoint de
 * actualizacion, y su ausencia no es un hueco por rellenar: reescribir la fila
 * cambiaria hacia atras a quien se le emitieron las facturas anteriores.
 * {@code subscription_billing_documents} apunta a la ficha por
 * {@code (company_id, billing_profile_id)}, asi que una factura del año pasado
 * seguiria enlazada a la misma fila y esa fila diria otra cosa. El cambio de
 * datos es {@code POST /company-billing-profile/succession}: cierra la vigente
 * y abre otra, en una transaccion.
 *
 * <p>
 * Y tampoco hay borrado. Una ficha se cierra con {@code valid_to} y queda a la
 * vista en el historico; no existe la baja logica.
 *
 * <h2>El {@code companyId} no viaja en ningun cuerpo</h2>
 *
 * <p>
 * Ninguno de los dos requests lo lleva. Lo pone aqui
 * {@code authz.currentCompanyId()} desde el principal y lo revalida el
 * {@code @PreAuthorize} de cada puerto. Aceptarlo del cliente seria dejar que
 * cualquiera cambiara a nombre de quien se factura en otra clinica.
 *
 * <h2>Sin controller de plataforma</h2>
 *
 * <p>
 * No hay un {@code SystemCompanyBillingProfileController}. El dia que la
 * consola de plataforma necesite ver las fichas de todos los tenants —para
 * conciliar un cobro, por ejemplo— eso son <em>casos de uso aparte</em> con
 * {@code hasRole('SYSTEM')} a secas, nunca un parametro {@code companyId}
 * añadido a estos: mezclar los dos caminos en un mismo puerto es como se abre
 * la fuga que persigue {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}.
 */
@RestController
@RequestMapping("/company-billing-profile")
public class CompanyBillingProfileController {

    private final OpenCompanyBillingProfileUseCase openUseCase;
    private final SucceedCompanyBillingProfileUseCase succeedUseCase;
    private final FindCurrentCompanyBillingProfileUseCase findCurrentUseCase;
    private final FindCompanyBillingProfileUseCase findUseCase;
    private final ListCompanyBillingProfilesUseCase listUseCase;
    private final Authz authz;

    public CompanyBillingProfileController(OpenCompanyBillingProfileUseCase openUseCase,
            SucceedCompanyBillingProfileUseCase succeedUseCase,
            FindCurrentCompanyBillingProfileUseCase findCurrentUseCase,
            FindCompanyBillingProfileUseCase findUseCase,
            ListCompanyBillingProfilesUseCase listUseCase, Authz authz) {
        this.openUseCase = openUseCase;
        this.succeedUseCase = succeedUseCase;
        this.findCurrentUseCase = findCurrentUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    /**
     * Abre la primera ficha. Si ya hay una vigente contesta 409 y remite a la
     * sucesion.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyBillingProfileResponse open(
            @Valid @RequestBody OpenCompanyBillingProfileRequest request) {
        return CompanyBillingProfileResponse.from(openUseCase.execute(
                new OpenCompanyBillingProfileCommand(request.personKind(), request.taxIdKind(),
                        request.taxId(), request.verificationDigit(), request.legalName(),
                        request.firstName(), request.middleName(), request.lastName(),
                        request.secondLastName(), request.address(), request.cityId(),
                        request.billingEmail(), request.taxRegime(), request.withholdingAgent(),
                        request.validFrom(), authz.currentCompanyId())));
    }

    /**
     * El cambio de datos: cierra la vigente y abre la sucesora.
     *
     * <p>
     * <strong>Es un {@code POST} sobre un sub-recurso propio y devuelve
     * 201</strong>, no un {@code PUT} sobre la ficha. La operacion <em>crea</em>
     * una fila nueva —la que se devuelve— y deja la anterior intacta; un
     * {@code PUT} anunciaria al front que la ficha se reemplaza, que es justo lo
     * que no pasa.
     */
    @PostMapping("/succession")
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyBillingProfileResponse succeed(
            @Valid @RequestBody SucceedCompanyBillingProfileRequest request) {
        return CompanyBillingProfileResponse.from(succeedUseCase.execute(
                new SucceedCompanyBillingProfileCommand(request.personKind(), request.taxIdKind(),
                        request.taxId(), request.verificationDigit(), request.legalName(),
                        request.firstName(), request.middleName(), request.lastName(),
                        request.secondLastName(), request.address(), request.cityId(),
                        request.billingEmail(), request.taxRegime(), request.withholdingAgent(),
                        request.effectiveFrom(), authz.currentCompanyId())));
    }

    /** La ficha que rige hoy. 404 si la empresa todavia no abrio ninguna. */
    @GetMapping
    public CompanyBillingProfileResponse findCurrent() {
        return CompanyBillingProfileResponse
                .from(findCurrentUseCase.findCurrent(authz.currentCompanyId()));
    }

    /**
     * El historico, de la vigente a la mas antigua. Va <em>antes</em> del mapeo por
     * {@code {id}} porque {@code PathPatternParser} da preferencia al literal
     * frente a la variable, pero dejarlo escrito arriba evita que alguien tenga que
     * comprobarlo.
     */
    @GetMapping("/history")
    public PageResponse<CompanyBillingProfileResponse> listHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUseCase.listByCompany(authz.currentCompanyId(), page, pageSize),
                CompanyBillingProfileResponse::from);
    }

    /**
     * Una ficha concreta del historico: la que una factura vieja nombra por id. El
     * {@code companyId} lo pone el backend, asi que un id de otra clinica contesta
     * 404.
     */
    @GetMapping("/{id}")
    public CompanyBillingProfileResponse findById(@PathVariable Long id) {
        return CompanyBillingProfileResponse
                .from(findUseCase.findById(id, authz.currentCompanyId()));
    }
}
