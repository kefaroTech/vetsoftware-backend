package com.vetsoftware.app.companyactivitymonth.application.port.out;

import com.vetsoftware.app.companyactivitymonth.domain.CompanyActivityMonth;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

/**
 * <strong>No declara ninguna escritura masiva, y no es que aun no haya hecho
 * falta.</strong> La unica operacion que toca una fila existente es el
 * recalculo del mes, y va por el ciclo leer-modificar-guardar de una entidad
 * gestionada, que es el unico camino que {@code @Version} protege. Un
 * {@code UPDATE} de conjunto pasaria de largo del bloqueo optimista y dejaria
 * la fila cambiada con su version intacta: el {@code save} concurrente que
 * llegara con la version vieja casaria igual y pisaria el recalculo, sin
 * excepcion y sin log ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}).
 *
 * <p>
 * <strong>Tampoco declara borrado.</strong> Una medicion no se retira: si un
 * mes quedo mal calculado se vuelve a calcular encima, que es justo la
 * operacion que esta tabla tiene.
 *
 * <p>
 * {@link #findById} <b>no tiene hermano acotado por empresa a proposito</b>.
 * Los cinco casos de uso de esta feature estan cerrados a
 * {@code hasRole('SYSTEM')} a secas y un principal {@code SYSTEM} no tiene
 * empresa con la que acotar, asi que declarar aqui un
 * {@code findByIdAndCompanyId} que nadie puede usar solo añadiria un camino
 * muerto.
 */
public interface CompanyActivityMonthRepository {

    /**
     * Escribe la fila, nueva o recalculada.
     *
     * <p>
     * El duplicado de {@code uq_cam_month} llega desde la base como violacion de
     * integridad; el adaptador lo traduce. Aqui no se pregunta antes.
     */
    CompanyActivityMonth save(CompanyActivityMonth month);

    Optional<CompanyActivityMonth> findById(Long id);

    /**
     * La fila de una clinica en un mes. Como mucho una: lo garantiza
     * {@code uq_cam_month}.
     */
    Optional<CompanyActivityMonth> findByCompanyIdAndPeriodKey(Long companyId, String periodKey);

    /** Toda la serie, de todas las clinicas. Barrido de plataforma. */
    PageResult<CompanyActivityMonth> findAll(int page, int pageSize);

    /** La serie de una clinica. */
    PageResult<CompanyActivityMonth> findAllByCompanyId(Long companyId, int page, int pageSize);

    /** Todas las clinicas en un mes. Barrido de plataforma. */
    PageResult<CompanyActivityMonth> findAllByPeriodKey(String periodKey, int page, int pageSize);

    /**
     * Las dormidas de un mes: {@code active_days <= threshold}.
     *
     * <p>
     * Se apoya en {@code ix_cam_dormant (period_key, active_days)} — una igualdad y
     * un rango, en ese orden, que es la forma que el indice sirve entera.
     */
    PageResult<CompanyActivityMonth> findDormant(String periodKey, int activeDaysThreshold,
            int page, int pageSize);
}
