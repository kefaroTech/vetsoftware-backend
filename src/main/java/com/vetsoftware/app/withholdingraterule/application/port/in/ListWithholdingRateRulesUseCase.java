package com.vetsoftware.app.withholdingraterule.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.withholdingraterule.application.dto.WithholdingRateRuleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListWithholdingRateRulesUseCase {

    /**
     * El catalogo de tarifas que el tenant puede consultar.
     *
     * <p>
     * <strong>Se llama {@code listAvailable} y no {@code listAll} por
     * convencion:</strong> {@code CLAUDE.md} reserva ese nombre para el catalogo
     * que lee el tenant. Aqui «lo disponible» resulta ser todo el catalogo
     * habilitado, porque no hay filas privadas de nadie —la tabla es global— pero
     * el nombre sigue diciendo desde donde se mira.
     *
     * <p>
     * <strong>El {@code companyId} llega y no filtra, y las dos mitades son
     * deliberadas.</strong> No filtra porque no hay por donde: la tarifa de un
     * supuesto fiscal es la misma para todas las clinicas y el puerto de salida no
     * conoce ninguna empresa. Y llega igualmente porque
     * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} —regla dura, BE-29— examina <em>si
     * el puerto transporta un companyId</em> antes de mirar nada mas: un listado
     * que no lo transporta solo lo puede servir {@code hasRole('SYSTEM')} a secas,
     * sin alternativa por permiso. Quitar el parametro no simplificaria la firma;
     * convertiria este puerto en uno cerrado a plataforma y dejaria al tenant sin
     * poder leer el catalogo.
     *
     * <p>
     * Dicho al reves, para quien lea esto dentro de un ano: <b>el parametro es la
     * credencial que autoriza la lectura, no el criterio que la acota</b>. Se
     * comprueba con {@code @authz.isMyCompany(#companyId)} —que el token declare
     * esa empresa— y despues se descarta.
     *
     * <p>
     * El nombre {@code companyId} es load-bearing: el {@code #companyId} del SpEL
     * se resuelve por nombre de parametro y, si dejan de coincidir, evalua a
     * {@code null} en silencio y la comprobacion falla siempre.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('withholdingRateRule.read')"
            + " and @authz.isMyCompany(#companyId))")
    PageResult<WithholdingRateRuleDto> listAvailable(Long companyId, int page, int pageSize);
}
