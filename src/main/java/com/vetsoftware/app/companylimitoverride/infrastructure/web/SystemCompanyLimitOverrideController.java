package com.vetsoftware.app.companylimitoverride.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.companylimitoverride.application.command.GrantCompanyLimitOverrideCommand;
import com.vetsoftware.app.companylimitoverride.application.command.RevokeCompanyLimitOverrideCommand;
import com.vetsoftware.app.companylimitoverride.application.port.in.GrantCompanyLimitOverrideUseCase;
import com.vetsoftware.app.companylimitoverride.application.port.in.ListCompanyLimitOverridesUseCase;
import com.vetsoftware.app.companylimitoverride.application.port.in.ResolveEffectiveLimitUseCase;
import com.vetsoftware.app.companylimitoverride.application.port.in.RevokeCompanyLimitOverrideUseCase;
import com.vetsoftware.app.companylimitoverride.infrastructure.web.request.GrantCompanyLimitOverrideRequest;
import com.vetsoftware.app.companylimitoverride.infrastructure.web.request.RevokeCompanyLimitOverrideRequest;
import com.vetsoftware.app.companylimitoverride.infrastructure.web.response.CompanyLimitOverrideResponse;
import com.vetsoftware.app.companylimitoverride.infrastructure.web.response.EffectiveLimitResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * La consola de plataforma sobre las excepciones de techo: conceder, revocar y
 * auditar.
 *
 * <p>
 * <strong>La empresa entra por la ruta.</strong> No viaja en ningún cuerpo —lo
 * prohíbe {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}— ni se deriva del principal,
 * porque un usuario de plataforma no tiene empresa. Puede llegar por
 * {@code @PathVariable} porque el gate de los dos puertos de escritura es
 * {@code hasRole('SYSTEM')} a secas.
 *
 * <p>
 * <strong>Quién firma lo pone el servidor</strong>, con
 * {@code authz.currentSystemUserId()}, tanto al conceder como al revocar. Es el
 * mismo motivo por el que la empresa no viaja en el cuerpo: un firmante que
 * escribe el llamador no es una firma.
 *
 * <p>
 * <strong>Revocar no borra.</strong> Escribe quién la quitó, cuándo y por qué,
 * y le pone fecha de fin, así que la ruta es un {@code POST} sobre un
 * sub-recurso y no un {@code DELETE}: lo que ocurre es que nace un hecho nuevo,
 * no que desaparezca uno viejo. Por eso «¿qué techo tenía el 14 de marzo?»
 * sigue teniendo respuesta y el eje queda libre para negociar otro pacto ese
 * mismo día.
 *
 * <p>
 * La revocación se dirige por <strong>empresa y eje</strong>, no por el id de
 * la fila, y no es una elección de estilo: el puerto de salida no ofrece
 * ninguna carga «por id» suelta a propósito —una excepción es de alguien, y
 * cargarla por un id que escribe el cliente es exactamente la familia de fugas
 * que cerró BE-COV—, así que el command se identifica por el par vivo.
 */
@RestController
@RequestMapping("/system/company-limit-overrides")
public class SystemCompanyLimitOverrideController {

    private final GrantCompanyLimitOverrideUseCase grantUseCase;
    private final RevokeCompanyLimitOverrideUseCase revokeUseCase;
    private final ListCompanyLimitOverridesUseCase listUseCase;
    private final ResolveEffectiveLimitUseCase resolveUseCase;
    private final Authz authz;

    public SystemCompanyLimitOverrideController(GrantCompanyLimitOverrideUseCase grantUseCase,
            RevokeCompanyLimitOverrideUseCase revokeUseCase,
            ListCompanyLimitOverridesUseCase listUseCase,
            ResolveEffectiveLimitUseCase resolveUseCase, Authz authz) {
        this.grantUseCase = grantUseCase;
        this.revokeUseCase = revokeUseCase;
        this.listUseCase = listUseCase;
        this.resolveUseCase = resolveUseCase;
        this.authz = authz;
    }

    @PostMapping("/companies/{companyId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyLimitOverrideResponse grant(@PathVariable Long companyId,
            @Valid @RequestBody GrantCompanyLimitOverrideRequest request) {
        return CompanyLimitOverrideResponse
                .from(grantUseCase.execute(new GrantCompanyLimitOverrideCommand(companyId,
                        request.limitDimensionId(), request.limitQuantity(), request.validFrom(),
                        request.reasonCode(), request.reason(), authz.currentSystemUserId())));
    }

    @PostMapping("/companies/{companyId}/dimensions/{limitDimensionId}/revocations")
    public CompanyLimitOverrideResponse revoke(@PathVariable Long companyId,
            @PathVariable Long limitDimensionId,
            @Valid @RequestBody RevokeCompanyLimitOverrideRequest request) {
        return CompanyLimitOverrideResponse
                .from(revokeUseCase.execute(new RevokeCompanyLimitOverrideCommand(companyId,
                        limitDimensionId, authz.currentSystemUserId(), request.revokedReasonCode(),
                        request.revokedReason())));
    }

    /**
     * La historia completa de una clínica, revocadas incluidas. Es el mismo puerto
     * que sirve a {@link CompanyLimitOverrideController}: su {@code @PreAuthorize}
     * admite {@code hasRole('SYSTEM')} <em>o</em> la propia empresa, y aquí entra
     * por la primera mitad.
     */
    @GetMapping("/companies/{companyId}")
    public List<CompanyLimitOverrideResponse> listByCompany(@PathVariable Long companyId) {
        return listUseCase.listByCompanyId(companyId).stream()
                .map(CompanyLimitOverrideResponse::from).toList();
    }

    /**
     * El techo que rige de verdad sobre un eje para esa clínica, con su
     * procedencia. Es el mismo puerto que sirve a
     * {@link CompanyLimitOverrideController} —admite {@code hasRole('SYSTEM')}
     * <em>o</em> la propia empresa— y aquí entra por la primera mitad.
     *
     * <p>
     * Es lo que soporte necesita antes de negociar: sin el origen, subir un techo
     * que ya venía de una excepción viva abre una segunda sobre el mismo eje y el
     * índice único la rechaza a mitad de la llamada comercial.
     */
    @GetMapping("/companies/{companyId}/effective-limits/{limitDimensionId}")
    public EffectiveLimitResponse effectiveLimit(@PathVariable Long companyId,
            @PathVariable Long limitDimensionId) {
        return EffectiveLimitResponse.from(resolveUseCase.resolve(companyId, limitDimensionId));
    }
}
