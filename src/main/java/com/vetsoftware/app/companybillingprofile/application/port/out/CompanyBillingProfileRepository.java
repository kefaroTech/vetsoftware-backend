package com.vetsoftware.app.companybillingprofile.application.port.out;

import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfile;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

/**
 * <strong>Toda consulta va acotada por empresa, y no hay una sola variante
 * ancha que alguien pueda llamar por error.</strong> No existe
 * {@code findById(Long)} ni {@code findAll(int, int)}: la ficha de facturacion
 * dice a quien se le cobra y con que documento, asi que una lectura sin
 * {@code company_id} es la lista de clientes de la plataforma con su NIT y su
 * direccion. Al no declarar la ancha, {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA}
 * no tiene nada que echar de menos y nadie puede escribir la fuga sin añadir
 * antes el metodo aqui, que se ve en el diff.
 *
 * <p>
 * <strong>Y no hay borrado, ni logico ni fisico.</strong> Ni {@code delete}, ni
 * {@code disable}, ni reactivacion. Una ficha se cierra con {@code valid_to} y
 * queda: es lo unico que hace que una factura de hace un año siga diciendo a
 * quien se emitio.
 */
public interface CompanyBillingProfileRepository {

    /**
     * Guarda la ficha y <strong>vacia el buffer de escritura antes de
     * devolver</strong>.
     *
     * <h2>Por que el flush es parte del contrato y no un detalle del adaptador</h2>
     *
     * <p>
     * La sucesion hace dos escrituras en la misma transaccion: el {@code UPDATE}
     * que cierra la ficha vigente y el {@code INSERT} de la sucesora. Hibernate
     * <strong>no las manda en el orden en que se llamo a {@code save}</strong>: su
     * cola de acciones ejecuta todos los {@code INSERT} antes que los
     * {@code UPDATE}. Sin flush intermedio, la sucesora entraria mientras la
     * anterior sigue con {@code valid_to} nulo, las dos calcularian el mismo
     * {@code current_profile_marker} y {@code uq_company_billing_profiles_current}
     * pararia la operacion — con un {@code Duplicate entry} sobre una columna
     * generada que nadie escribio y que no aparece en ningun sitio del codigo Java.
     *
     * <p>
     * Es un defecto que no se ve leyendo el caso de uso, porque alli las dos lineas
     * estan en el orden correcto. Por eso el contrato lo fija el puerto y no la
     * implementacion.
     */
    CompanyBillingProfile save(CompanyBillingProfile profile);

    /** La ficha de la empresa por su id. No hay variante sin {@code companyId}. */
    Optional<CompanyBillingProfile> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * La ficha que rige hoy: la unica de la empresa con {@code valid_to} nulo. La
     * unicidad de que sea «la unica» no la sostiene esta consulta sino
     * {@code uq_company_billing_profiles_current} sobre la columna generada.
     */
    Optional<CompanyBillingProfile> findCurrentByCompanyId(Long companyId);

    /**
     * El historico completo de la empresa, de la vigente a la mas antigua. Es lo
     * que permite abrir una factura vieja y ver a quien se emitio.
     */
    PageResult<CompanyBillingProfile> findAllByCompanyId(Long companyId, int page, int pageSize);
}
