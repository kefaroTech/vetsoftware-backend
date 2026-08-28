package com.vetsoftware.app.companylimitoverride.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.companylimitoverride.application.port.in.ListCompanyLimitOverridesUseCase;
import com.vetsoftware.app.companylimitoverride.application.port.in.ResolveEffectiveLimitUseCase;
import com.vetsoftware.app.companylimitoverride.infrastructure.web.response.CompanyLimitOverrideResponse;
import com.vetsoftware.app.companylimitoverride.infrastructure.web.response.EffectiveLimitResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lo que la clínica ve de las excepciones negociadas a su favor: <strong>solo
 * lectura</strong>.
 *
 * <p>
 * Que el cliente lea esto es lo que dice la ficha de construcción —bloque
 * mixto: escribe plataforma, leen ambos— y es la segunda mitad de «el cliente
 * ve su techo y de dónde sale». Sin este listado, un cupo distinto del de
 * catálogo es un número inexplicable.
 *
 * <p>
 * <strong>Ninguna operación de escritura vive aquí, y no es una funcionalidad
 * pendiente.</strong> Si el gate de conceder admitiera al empleado de la
 * clínica, la administradora se subiría el techo cada vez que topa y el cupo
 * dejaría de ser un cupo: subir un techo es una decisión comercial de
 * plataforma, con firma. Está en {@link SystemCompanyLimitOverrideController}.
 *
 * <p>
 * La empresa la pone el backend con {@code authz.currentCompanyId()} y el
 * puerto la revalida con {@code @authz.isMyCompany(#companyId)}.
 */
@RestController
@RequestMapping("/company-limit-overrides")
public class CompanyLimitOverrideController {

    private final ListCompanyLimitOverridesUseCase listUseCase;
    private final ResolveEffectiveLimitUseCase resolveUseCase;
    private final Authz authz;

    public CompanyLimitOverrideController(ListCompanyLimitOverridesUseCase listUseCase,
            ResolveEffectiveLimitUseCase resolveUseCase, Authz authz) {
        this.listUseCase = listUseCase;
        this.resolveUseCase = resolveUseCase;
        this.authz = authz;
    }

    @GetMapping
    public List<CompanyLimitOverrideResponse> listMine() {
        return listUseCase.listByCompanyId(authz.currentCompanyId()).stream()
                .map(CompanyLimitOverrideResponse::from).toList();
    }

    /**
     * <strong>El techo que rige de verdad sobre un eje, y de dónde sale.</strong>
     *
     * <p>
     * Es la mitad que le faltaba al cliente. Con el listado de arriba lee sus
     * excepciones, y en {@code /subscription-item-limits} sus techos congelados;
     * ninguno de los dos le dice cuál manda. Aquí se aplica la precedencia
     * {@code COMPANY_OVERRIDE > SUBSCRIPTION > CATALOG_DEFAULT > NONE} en un solo
     * sitio y sale el número <em>con su procedencia dentro</em>, que es la línea
     * que la pantalla de cupos necesita para explicarlo.
     *
     * <p>
     * Cuelga de esta rodaja porque la excepción negociada es la cima de la cadena y
     * es donde vive {@code EffectiveLimitResolver}. La empresa la sigue poniendo el
     * servidor: en la ruta solo viaja el eje.
     */
    @GetMapping("/effective-limits/{limitDimensionId}")
    public EffectiveLimitResponse effectiveLimit(@PathVariable Long limitDimensionId) {
        return EffectiveLimitResponse
                .from(resolveUseCase.resolve(authz.currentCompanyId(), limitDimensionId));
    }
}
