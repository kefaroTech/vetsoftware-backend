package com.vetsoftware.app.service.application.port.out;

import com.vetsoftware.app.service.application.command.SearchServicesCommand;
import com.vetsoftware.app.service.application.dto.PageResult;
import com.vetsoftware.app.service.domain.Service;
import java.util.List;
import java.util.Optional;

public interface ServiceRepository {
    Service save(Service service);

    Optional<Service> findById(Long id);

    Optional<Service> findByIdAndCompanyId(Long id, Long companyId);

    List<Service> findAll();

    List<Service> findAllByCompanyId(Long companyId);

    /**
     * Servicios PAUSADOS (enabled=false) de la empresa, para el listado de
     * reactivación.
     */
    List<Service> findAllDisabledByCompanyId(Long companyId);

    PageResult<Service> search(SearchServicesCommand command);

    void delete(Long id);

    int reactivate(Long id, Long companyId);
}
