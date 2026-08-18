package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.in.ReactivateEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "employee.reactivate")
@Service
public class ReactivateEmployeeService implements ReactivateEmployeeUseCase {
    private final EmployeeRepository repository;

    public ReactivateEmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    /**
     * La empresa viaja hasta el UPDATE y hasta la relectura: aqui no hay un
     * findById previo que valide la propiedad, asi que si la consulta no filtra por
     * empresa, un id ajeno se reactiva sin mas — y reactivar un empleado le
     * devuelve el acceso a quien otra empresa habia expulsado. Cero filas afectadas
     * significa «no existe en TU empresa», que es tambien la respuesta correcta
     * para el empleado de otro tenant: un 404, sin revelar que el id existe.
     *
     * <p>
     * {@code companyId} nulo es el principal cross-tenant (SYSTEM), que si opera
     * global.
     */
    @Override
    @Transactional
    public EmployeeDto execute(Long id, Long companyId) {
        int rows = companyId == null
                ? repository.reactivate(id)
                : repository.reactivate(id, companyId);
        if (rows == 0)
            throw new EmployeeNotFoundException(id);
        return EmployeeDto.from((companyId == null
                ? repository.findById(id)
                : repository.findByIdAndCompanyId(id, companyId))
                .orElseThrow(() -> new EmployeeNotFoundException(id)));
    }
}
