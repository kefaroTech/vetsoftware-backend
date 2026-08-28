package com.vetsoftware.app.externalinvoicingoutage.application.port.out;

import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageCompany;
import com.vetsoftware.app.shared.pagination.PageResult;

/**
 * El reparto de una caida por clinica.
 *
 * <p>
 * <strong>No declara {@code delete}, y esa ausencia es la regla.</strong>
 * Quitar una clinica de la lista de alcanzadas destruye la prueba de que se le
 * aviso y de por que uso numeracion de contingencia. No hay endpoint, no hay
 * puerto y la entidad no lleva {@code @SQLDelete}: las tres puertas cerradas,
 * no una.
 *
 * <p>
 * <strong>Tampoco declara ninguna escritura sobre fila existente.</strong> Se
 * inserta una vez al repartir y ahi acaba, que es lo que justifica que la tabla
 * no tenga {@code version} ni {@code created_date}.
 */
public interface ExternalInvoicingOutageCompanyRepository {

    ExternalInvoicingOutageCompany save(ExternalInvoicingOutageCompany affected);

    /**
     * Si esa clinica ya esta en el reparto de esa caida.
     *
     * <p>
     * <strong>Es una comprobacion de cortesia, no la barandilla.</strong> La
     * barandilla es {@code uq_eioc_pair}: dos peticiones concurrentes pasarian las
     * dos por este {@code exists} y solo una sobreviviria al indice. Existe para
     * que el caso normal —el reintento del proceso que arma el reparto— conteste
     * con un choque legible en vez de con una violacion de integridad cruda.
     */
    boolean existsByOutageIdAndCompanyId(Long outageId, Long companyId);

    PageResult<ExternalInvoicingOutageCompany> findAllByOutageId(Long outageId, int page,
            int pageSize);
}
