package com.vetsoftware.app.withholdingraterule.application.port.in;

import com.vetsoftware.app.withholdingraterule.application.dto.WithholdingRateRuleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindWithholdingRateRuleUseCase {

    /**
     * Una tarifa por su id.
     *
     * <p>
     * <strong>EL {@code companyId} NO SOBRA, AUNQUE LA TABLA NO TENGA
     * EMPRESA.</strong> Es el punto que mas facil se «limpia» en una revision
     * futura —«este catalogo es global, quita el parametro que no se usa»— y
     * quitarlo rompe la feature de dos maneras distintas:
     *
     * <ol>
     * <li><b>Se cae la mitad del gate.</b> Sin parametro no hay
     * {@code @authz.isMyCompany(#companyId)} que escribir, y el permiso
     * {@code withholdingRateRule.read} quedaria solo, alcanzable por un empleado de
     * cualquier empresa sin comprobar que el token declara la suya. El permiso dice
     * <em>que</em> puede hacer alguien, nunca <em>desde donde</em>.</li>
     * <li><b>Se abre por permiso lo que sus hermanas cierran.</b> Los puertos de
     * lectura de este bloque comparten forma a proposito; el que no recibe empresa
     * cae del lado de las reglas que obligan a {@code hasRole('SYSTEM')} a secas
     * ({@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM},
     * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}). Recibirla es lo que permite que el
     * tenant lea el catalogo sin ser plataforma.</li>
     * </ol>
     *
     * <p>
     * <strong>Y NO SE FILTRA POR EL.</strong> El puerto de salida no lo recibe
     * siquiera: la tarifa de industria y comercio de Bogota es la misma para todas
     * las clinicas, y filtrarla por empresa devolveria vacio siempre. El parametro
     * es una <em>credencial</em>, no un criterio de busqueda.
     *
     * <p>
     * El nombre del parametro tiene que seguir siendo {@code companyId}: el
     * {@code #companyId} del SpEL se resuelve por nombre y, si dejan de coincidir,
     * evalua a {@code null} <b>en silencio</b> y la comprobacion falla siempre.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('withholdingRateRule.read')"
            + " and @authz.isMyCompany(#companyId))")
    WithholdingRateRuleDto findById(Long id, Long companyId);
}
