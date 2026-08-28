package com.vetsoftware.app.accountmapping.application.port.out;

import com.vetsoftware.app.accountmapping.domain.AccountMapping;
import com.vetsoftware.app.accountmapping.domain.MappingKind;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.time.LocalDate;
import java.util.Optional;

/**
 * <strong>Ningun metodo recibe {@code companyId}</strong>, y esa ausencia es
 * una afirmacion sobre el modelo: {@code account_mappings} no tiene columna de
 * empresa.
 */
public interface AccountMappingRepository {

    AccountMapping save(AccountMapping mapping);

    Optional<AccountMapping> findById(Long id);

    PageResult<AccountMapping> findAllEnabled(int page, int pageSize);

    /**
     * <strong>LA consulta del negocio</strong>: que cuentas mueve este supuesto en
     * esta fecha. De ella sale el asiento entero.
     *
     * <p>
     * <b>Los tres afinados nulos no se comparan con {@code null}.</b> En SQL,
     * {@code catalog_item_id = NULL} no es cierto ni para las filas que lo tienen
     * nulo, asi que la comparacion se hace contra las claves con centinela —los
     * mismos valores que la base guarda en {@code catalog_item_key},
     * {@code charge_type_key} y {@code tax_treatment_key}—. Sin eso, los nueve
     * tipos de mapeo que no llevan articulo serian <b>inencontrables</b> y no
     * habria ningun error que lo delatara: simplemente no se generaria el asiento.
     *
     * <p>
     * Devuelve <b>como mucho uno</b>. Para las vigencias abiertas lo garantiza
     * {@code uq_account_mappings_current}; entre las cerradas, que pueden solaparse
     * si alguien cargo mal el historico, el adaptador desempata por la mas reciente
     * para que la respuesta sea determinista y no «el primero que llegue».
     */
    Optional<AccountMapping> findEffective(MappingKind mappingKind, String mappingKey,
            Long catalogItemId, String chargeType, String taxTreatment, LocalDate on);
}
