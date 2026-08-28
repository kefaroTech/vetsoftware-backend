package com.vetsoftware.app.platformtaxprofile.infrastructure.web;

import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.platformtaxprofile.application.command.OpenPlatformTaxProfileCommand;
import com.vetsoftware.app.platformtaxprofile.application.command.SucceedPlatformTaxProfileCommand;
import com.vetsoftware.app.platformtaxprofile.application.port.in.FindCurrentPlatformTaxProfileUseCase;
import com.vetsoftware.app.platformtaxprofile.application.port.in.FindPlatformTaxProfileUseCase;
import com.vetsoftware.app.platformtaxprofile.application.port.in.ListPlatformTaxProfilesUseCase;
import com.vetsoftware.app.platformtaxprofile.application.port.in.OpenPlatformTaxProfileUseCase;
import com.vetsoftware.app.platformtaxprofile.application.port.in.SucceedPlatformTaxProfileUseCase;
import com.vetsoftware.app.platformtaxprofile.infrastructure.web.request.OpenPlatformTaxProfileRequest;
import com.vetsoftware.app.platformtaxprofile.infrastructure.web.request.SucceedPlatformTaxProfileRequest;
import com.vetsoftware.app.platformtaxprofile.infrastructure.web.response.PlatformTaxProfileResponse;
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
 * Quien es VetSoftware ante la DIAN: razon social, NIT, regimen y marca de
 * autorretenedor, con vigencia.
 *
 * <h2>Es una pantalla de consola de plataforma, y solo eso</h2>
 *
 * <p>
 * Los cinco endpoints van bajo {@code /system} y sus puertos estan cerrados a
 * {@code hasRole('SYSTEM')} a secas. <strong>No hay un controller de
 * tenant</strong>, y no es un hueco por rellenar: esta es la identidad de la
 * plataforma, no la de la clinica —esa vive en {@code company_tax_profiles}—.
 * El dia que el tenant necesite leer con que razon social se le factura, lo que
 * lee es su propia factura, no esta tabla.
 *
 * <h2>La tabla esta vacia hoy, a proposito</h2>
 *
 * <p>
 * El changeset 367 no la sembro: no habia razon social ni NIT reales de
 * VetSoftware y no se inventaron, porque una identidad fiscal inventada acaba
 * impresa en la factura de cada cliente. Hasta que alguien use
 * {@code POST /system/platform-tax-profiles}, {@code GET .../current} contesta
 * <strong>503</strong> con {@code NoCurrentPlatformTaxProfileException} y el
 * codigo {@code PLATFORM_TAX_PROFILE_NOT_CONFIGURED}. 503 y no 404 ni 409 por
 * lo mismo que sus dos hermanas
 * ({@code PlatformBillingConfigNotConfiguredException},
 * {@code PlatformCatalogNotConfiguredException}): no falta el recurso de
 * negocio que se pidio, falta el suelo sobre el que la emision se apoya, y un
 * 404 mandaria a buscar el registro equivocado. Eso <strong>no es un fallo del
 * despliegue</strong>: es la decision pendiente del dueño.
 *
 * <h2>No hay {@code PUT} ni {@code PATCH}, y tampoco {@code DELETE}</h2>
 *
 * <p>
 * <strong>Esto es lo unico importante de este controller.</strong> Quien venga
 * buscando «como se corrige el NIT» no va a encontrar un endpoint de
 * actualizacion, y su ausencia no es un hueco: reescribir la fila cambiaria
 * hacia atras con que identidad se emitieron las facturas anteriores. El
 * changeset 368 añadio {@code platform_tax_profile_id} a
 * {@code subscription_billing_documents} precisamente para que cada documento
 * apunte a la identidad con la que se emitio. El cambio es
 * {@code POST /system/platform-tax-profiles/succession}: cierra la vigente y
 * abre otra, en una transaccion.
 *
 * <p>
 * Y tampoco hay borrado. Una identidad se cierra con {@code valid_to} y queda a
 * la vista en el historico; la tabla ni siquiera tiene columna {@code enabled}.
 */
@RestController
@RequestMapping("/system/platform-tax-profiles")
public class SystemPlatformTaxProfileController {

    private final OpenPlatformTaxProfileUseCase openUseCase;
    private final SucceedPlatformTaxProfileUseCase succeedUseCase;
    private final FindCurrentPlatformTaxProfileUseCase findCurrentUseCase;
    private final FindPlatformTaxProfileUseCase findUseCase;
    private final ListPlatformTaxProfilesUseCase listUseCase;

    public SystemPlatformTaxProfileController(OpenPlatformTaxProfileUseCase openUseCase,
            SucceedPlatformTaxProfileUseCase succeedUseCase,
            FindCurrentPlatformTaxProfileUseCase findCurrentUseCase,
            FindPlatformTaxProfileUseCase findUseCase, ListPlatformTaxProfilesUseCase listUseCase) {
        this.openUseCase = openUseCase;
        this.succeedUseCase = succeedUseCase;
        this.findCurrentUseCase = findCurrentUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
    }

    /**
     * Abre la primera identidad fiscal. Si ya hay una vigente contesta 409 y remite
     * a la sucesion.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlatformTaxProfileResponse open(
            @Valid @RequestBody OpenPlatformTaxProfileRequest request) {
        return PlatformTaxProfileResponse
                .from(openUseCase.execute(new OpenPlatformTaxProfileCommand(request.documentType(),
                        request.documentId(), request.verificationDigit(), request.legalName(),
                        request.taxRegime(), request.fiscalEmail(), request.commercialName(),
                        request.economicActivityId(), request.selfWithholder(),
                        request.validFrom())));
    }

    /**
     * El cambio de identidad: cierra la vigente y abre la sucesora.
     *
     * <p>
     * <strong>Es un {@code POST} sobre un sub-recurso propio y devuelve
     * 201</strong>, no un {@code PUT} sobre la ficha. La operacion <em>crea</em>
     * una fila nueva —la que se devuelve— y deja la anterior intacta; un
     * {@code PUT} anunciaria a la consola que la ficha se reemplaza, que es justo
     * lo que no pasa.
     */
    @PostMapping("/succession")
    @ResponseStatus(HttpStatus.CREATED)
    public PlatformTaxProfileResponse succeed(
            @Valid @RequestBody SucceedPlatformTaxProfileRequest request) {
        return PlatformTaxProfileResponse.from(
                succeedUseCase.execute(new SucceedPlatformTaxProfileCommand(request.documentType(),
                        request.documentId(), request.verificationDigit(), request.legalName(),
                        request.taxRegime(), request.fiscalEmail(), request.commercialName(),
                        request.economicActivityId(), request.selfWithholder(),
                        request.effectiveFrom())));
    }

    /**
     * La identidad que rige hoy. Contesta <strong>503</strong> mientras nadie haya
     * sembrado la primera, que hoy es el estado real de la tabla.
     */
    @GetMapping("/current")
    public PlatformTaxProfileResponse findCurrent() {
        return PlatformTaxProfileResponse.from(findCurrentUseCase.findCurrent());
    }

    /** El historico, de la vigente a la mas antigua. */
    @GetMapping
    public PageResponse<PlatformTaxProfileResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(page, pageSize),
                PlatformTaxProfileResponse::from);
    }

    /**
     * Una identidad concreta del historico: la que una factura vieja nombra por id
     * a traves de {@code subscription_billing_documents.platform_tax_profile_id}.
     */
    @GetMapping("/{id}")
    public PlatformTaxProfileResponse findById(@PathVariable Long id) {
        return PlatformTaxProfileResponse.from(findUseCase.findById(id));
    }
}
