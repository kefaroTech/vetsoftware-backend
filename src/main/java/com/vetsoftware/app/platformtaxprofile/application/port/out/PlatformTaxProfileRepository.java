package com.vetsoftware.app.platformtaxprofile.application.port.out;

import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxProfile;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

/**
 * <strong>Ningun metodo de este puerto recibe {@code companyId}, y esa ausencia
 * es una afirmacion sobre el modelo, no un olvido.</strong>
 * {@code platform_tax_profiles} no tiene columna de empresa: es la identidad
 * fiscal de Lumbre, una sola para toda la plataforma. Añadir aqui un
 * {@code findAllByCompanyId} exigiria inventarse la columna.
 *
 * <p>
 * Esa misma ausencia es la que deja fuera de alcance a las reglas de tenancy de
 * BE-COV y BE-29, que se activan sobre el repositorio que <em>si</em> sabe
 * filtrar por empresa. El cierre a {@code hasRole('SYSTEM')} vive en los
 * puertos de entrada, que es donde puede vivir.
 *
 * <p>
 * <strong>Y no hay borrado, ni logico ni fisico.</strong> Ni {@code delete}, ni
 * {@code disable}, ni reactivacion — la tabla ni siquiera tiene columna
 * {@code enabled}. Una identidad se cierra con {@code valid_to} y queda: es lo
 * unico que hace que una factura de hace dos años siga diciendo con que razon
 * social se emitio.
 */
public interface PlatformTaxProfileRepository {

    /**
     * Guarda la identidad y <strong>vacia el buffer de escritura antes de
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
     * {@code current_profile_marker} —que aqui vale {@code 1} para cualquier fila
     * vigente, no {@code company_id}— y {@code uq_platform_tax_profiles_current}
     * pararia la operacion con un {@code Duplicate entry} sobre una columna
     * generada que nadie escribio y que no aparece en ningun sitio del codigo Java.
     *
     * <p>
     * Es un defecto que no se ve leyendo el caso de uso, porque alli las dos lineas
     * estan en el orden correcto. Por eso el contrato lo fija el puerto y no la
     * implementacion.
     */
    PlatformTaxProfile save(PlatformTaxProfile profile);

    Optional<PlatformTaxProfile> findById(Long id);

    /**
     * La identidad que rige hoy: la unica de la tabla con {@code valid_to} nulo.
     * Que sea «la unica» no lo sostiene esta consulta sino
     * {@code uq_platform_tax_profiles_current} sobre la columna generada.
     *
     * <p>
     * Devuelve vacio mientras nadie haya sembrado la primera, que hoy es el estado
     * normal. Quien lo traduce en un fallo ruidoso es el caso de uso, no este
     * puerto.
     */
    Optional<PlatformTaxProfile> findCurrent();

    /**
     * El historico completo, de la vigente a la mas antigua. Es lo que permite
     * abrir una factura vieja y ver con que razon social se emitio.
     */
    PageResult<PlatformTaxProfile> findAll(int page, int pageSize);
}
