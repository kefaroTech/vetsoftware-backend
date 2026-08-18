package com.vetsoftware.app.surgery.application.port.out;

import com.vetsoftware.app.surgery.domain.SurgeryTypeRef;
import java.util.Optional;

/**
 * El catalogo de tipos <b>mezcla filas generales</b> —visibles para todos los
 * tenants— con filas privadas de cada empresa, asi que «acotado por empresa»
 * significa aqui <b>general O mio</b>, nunca «mio» a secas: la lectura estricta
 * ({@code findByIdAndCompany_Id}) dejaria de poder asignar un tipo general, que
 * es el caso normal.
 *
 * <p>
 * De ahi el nombre —y de ahi que el adaptador consulte
 * {@code findAvailableById} y no la estricta, que el repositorio reserva a los
 * caminos de escritura sobre el propio catalogo—. Lo que si queda fuera es el
 * tipo <em>privado de otra empresa</em>, que era la fuga.
 */
public interface SurgeryTypeQueryPort {
    Optional<SurgeryTypeRef> findAvailableByIdAndCompanyId(Long surgeryTypeId, Long companyId);
}
