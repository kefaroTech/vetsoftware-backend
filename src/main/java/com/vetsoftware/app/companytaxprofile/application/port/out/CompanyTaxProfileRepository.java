package com.vetsoftware.app.companytaxprofile.application.port.out;

import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfile;
import java.util.Optional;

/**
 * <strong>Y no hay borrado.</strong> Un perfil se cierra con {@code validTo} y
 * queda: es lo único que hace que una factura de hace un año siga diciendo con
 * qué identidad fiscal se emitió.
 */
public interface CompanyTaxProfileRepository {

    /**
     * Guarda el perfil y <strong>vacía el buffer de escritura antes de
     * devolver</strong>.
     *
     * <h2>Por qué el flush es parte del contrato y no un detalle del adaptador</h2>
     *
     * <p>
     * La sucesión hace dos escrituras en la misma transacción: el {@code UPDATE}
     * que cierra el perfil vigente y el {@code INSERT} del sucesor. Hibernate
     * <strong>no las manda en el orden en que se llamó a {@code save}</strong>: su
     * cola de acciones ejecuta todos los {@code INSERT} antes que los
     * {@code UPDATE}. Sin flush intermedio, el sucesor entraría mientras el
     * anterior sigue con {@code valid_to} nulo, los dos calcularían el mismo
     * {@code current_profile_marker} y {@code uq_company_tax_profiles_current}
     * pararía la operación — con un {@code Duplicate entry} sobre una columna
     * generada que nadie escribió y que no aparece en ningún sitio del código Java.
     *
     * <p>
     * Es un defecto que no se ve leyendo el caso de uso, porque allí las dos líneas
     * están en el orden correcto. Por eso el contrato lo fija el puerto y no la
     * implementación. Mismo criterio que
     * {@code CompanyBillingProfileRepository.save}.
     */
    CompanyTaxProfile save(CompanyTaxProfile profile);

    /**
     * Cierra la ficha vigente: escribe <strong>solo</strong> su {@code validTo}, ya
     * puesto por {@code CompanyTaxProfile.closeOn(...)}.
     *
     * <h2>Por qué el cierre no es un {@code save}</h2>
     *
     * <p>
     * Guardar el agregado entero reconstruye sus responsabilidades como filas
     * nuevas —el dominio guarda el código y no el id de la fila—, y una ficha que
     * se cierra <em>sin tocar</em> sus responsabilidades las reinsertaría contra
     * {@code uq_ctp_responsibilities_profile_code}. El cierre mueve una columna;
     * escribe una columna.
     *
     * @return filas afectadas: {@code 1} si se cerró. <strong>Cero significa que
     *         esa ficha ya no era la vigente</strong> —otra sucesión ganó la
     *         carrera— y el caso de uso tiene que abortar, no seguir e insertar una
     *         segunda vigente
     */
    int close(CompanyTaxProfile profile);

    /**
     * El perfil que rige hoy: el único de la empresa sin fecha de cierre. Desde el
     * changeset 364 la tabla guarda histórico, así que una lectura por empresa sin
     * filtro de vigencia devolvería también identidades fiscales ya sucedidas.
     */
    Optional<CompanyTaxProfile> findCurrentByCompanyId(Long companyId);

    /** Si la empresa ya tiene un perfil <strong>vigente</strong>. */
    boolean existsCurrentByCompanyId(Long companyId);
}
