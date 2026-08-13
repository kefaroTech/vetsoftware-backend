package com.vetsoftware.app.openaccount.application.command;

import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import java.util.List;

/**
 * Criterios del listado de cuentas.
 *
 * <p>
 * {@code statuses} y {@code q} se anaden en BE-06 porque la pantalla ya
 * filtraba por ellos EN CLIENTE sobre la lista completa. Sin ellos en el
 * servidor no se puede paginar: acumular paginas dejaria el filtro viendo solo
 * lo ya cargado, que es peor que traer 200 filas de golpe.
 *
 * <p>
 * {@code statuses} es una lista y no un valor suelto porque la pestana
 * "Cerradas" de la pantalla son dos estados a la vez (cobrada y cancelada);
 * vacia = todos. {@code q} es texto libre sobre nombre o documento del
 * propietario; null o vacio = sin filtro. {@code branchId} null = todas las
 * sedes de la empresa (multi-sucursal, Fase C).
 */
public record SearchOpenAccountsCommand(Long companyId, Long ownerId, Boolean enabled,
        List<OpenAccountStatus> statuses, String q, int page, int pageSize, Long branchId) {

    public SearchOpenAccountsCommand {
        statuses = statuses == null ? List.of() : List.copyOf(statuses);
    }
}
