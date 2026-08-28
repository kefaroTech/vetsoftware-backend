package com.vetsoftware.app.withholdingraterule.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.withholdingraterule.domain.ServiceNature;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingRateRule;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingType;
import java.time.LocalDate;
import java.util.Optional;

/**
 * <strong>Ningun metodo de este puerto recibe {@code companyId}, y esa ausencia
 * es una afirmacion sobre el modelo, no un olvido.</strong>
 * {@code withholding_rate_rules} no tiene columna de empresa: la tarifa depende
 * del supuesto fiscal y no del cliente. Anadir aqui un
 * {@code findAllByCompanyId} exigiria inventarse la columna.
 *
 * <p>
 * Esa misma ausencia es la que deja fuera de alcance a las reglas de tenancy de
 * BE-COV y BE-29, que se activan sobre el repositorio que <em>si</em> sabe
 * filtrar por empresa. Es tambien la razon de que el filtro tenga que vivir en
 * el puerto de entrada y no aqui: ver
 * {@code ListWithholdingRateRulesUseCase#listAvailable}.
 */
public interface WithholdingRateRuleRepository {

    WithholdingRateRule save(WithholdingRateRule rule);

    Optional<WithholdingRateRule> findById(Long id);

    /**
     * El catalogo completo, paginado. No hay hermano acotado por empresa porque no
     * hay empresa: la lista es la misma para plataforma y para cualquier tenant.
     */
    PageResult<WithholdingRateRule> findAllEnabled(int page, int pageSize);

    /**
     * <strong>LA consulta del negocio</strong>: la tarifa vigente para un supuesto
     * en una fecha. Es la que decide cuanto se espera que retenga el cliente, y por
     * tanto de cuanto va a ser el giro.
     *
     * <p>
     * <b>El {@code municipalityCode} nulo no se compara con {@code null}.</b> En
     * SQL, {@code municipality_code = NULL} no es cierto ni para las filas
     * nacionales, asi que la comparacion se hace contra la clave con centinela —el
     * mismo valor que la base guarda en la columna generada
     * {@code municipality_key}—. Sin eso, las tarifas nacionales serian
     * inencontrables y la retencion esperada saldria cero sin dar un solo error.
     *
     * <p>
     * Devuelve <b>como mucho una</b>. Para las vigencias abiertas lo garantiza
     * {@code uq_withholding_rate_rules_current}; entre las cerradas, que pueden
     * solaparse si alguien cargo mal el historico, el adaptador desempata por la
     * mas reciente para que la respuesta sea determinista y no «la primera que
     * llegue».
     */
    Optional<WithholdingRateRule> findEffective(WithholdingType withholdingType,
            ServiceNature serviceNature, String municipalityCode, LocalDate on);
}
