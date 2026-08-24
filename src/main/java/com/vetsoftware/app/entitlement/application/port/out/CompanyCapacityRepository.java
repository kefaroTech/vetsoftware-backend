package com.vetsoftware.app.entitlement.application.port.out;

import com.vetsoftware.app.entitlement.domain.CapacityUnit;
import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida de los contadores contratados, siempre acotado por empresa.
 */
public interface CompanyCapacityRepository {

    List<CompanyCapacity> findAllByCompanyId(Long companyId);

    Optional<CompanyCapacity> findByCompanyIdAndUnit(Long companyId, CapacityUnit unit);

    List<CompanyCapacity> saveAll(List<CompanyCapacity> capacities);

    /**
     * Suma {@code delta} al consumo, en el motor y en una sola sentencia.
     *
     * @return filas afectadas: 0 si la empresa no tiene contador de esa unidad o si
     *         el movimiento dejaria el consumo en negativo
     */
    int addUsage(Long companyId, CapacityUnit unit, int delta);
}
