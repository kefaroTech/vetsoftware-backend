package com.vetsoftware.app.revenuerecognitionline.application.port.in;

import com.vetsoftware.app.revenuerecognitionline.application.dto.RevenueRecognitionLineDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListRevenueRecognitionLinesUseCase {

    /**
     * El libro de una clinica.
     *
     * <p>
     * Transporta {@code companyId} y va cerrado a {@code hasRole('SYSTEM')} a
     * secas, que es una de las dos salidas que admite
     * {@code TENANT_DEFENSA_EN_PROFUNDIDAD}: a un principal SYSTEM no se le puede
     * exigir {@code @authz.isMyCompany}, porque es cross-tenant por diseño y no
     * tiene empresa propia. Aqui el {@code companyId} <b>si</b> filtra de verdad —
     * el adaptador lo lleva al {@code WHERE}—, al reves que en los catalogos
     * globales del bloque, donde solo es una credencial.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<RevenueRecognitionLineDto> listByCompany(Long companyId, int page, int pageSize);

    /**
     * <strong>El barrido del cierre mensual</strong>: todo lo registrado en un
     * periodo contable, de todas las clinicas.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas, y no hay alternativa por
     * permiso.</strong> El puerto no transporta ningun {@code companyId} y sirve un
     * listado que devuelve filas de todas las empresas: es literalmente el caso que
     * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} (regla dura, BE-29) cierra a
     * plataforma. Y no es un descuido del indice: {@code ix_rrl_period} se creo sin
     * la empresa delante <em>a proposito</em>, porque ponersela lo haria inutil
     * para el cierre. La propia especificacion lo advierte —«declararlo aqui no
     * exime: la regla recorre los casos de uso, no los documentos»— y este gate es
     * el que lo paga.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<RevenueRecognitionLineDto> listByPostingPeriod(String postingPeriod, int page,
            int pageSize);
}
