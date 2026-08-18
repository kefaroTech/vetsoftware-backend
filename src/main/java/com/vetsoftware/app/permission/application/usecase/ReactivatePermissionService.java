package com.vetsoftware.app.permission.application.usecase;

import com.vetsoftware.app.permission.application.dto.PermissionDto;
import com.vetsoftware.app.permission.application.port.in.ReactivatePermissionUseCase;
import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import com.vetsoftware.app.permission.domain.PermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "permission.reactivate")
@Service
public class ReactivatePermissionService implements ReactivatePermissionUseCase {
    private final PermissionRepository repository;

    public ReactivatePermissionService(PermissionRepository repository) {
        this.repository = repository;
    }

    /**
     * La empresa viaja hasta el UPDATE y hasta la relectura: en la reactivacion no
     * hay findById previo que valide la propiedad, asi que el WHERE es la unica
     * barrera. {@code companyId} nulo es el principal cross-tenant (SYSTEM), que si
     * opera global.
     */
    @Override
    @Transactional
    public PermissionDto execute(Long id, Long companyId) {
        int rows = companyId == null
                ? repository.reactivate(id)
                : repository.reactivate(id, companyId);
        if (rows == 0)
            throw new PermissionNotFoundException(id);
        return PermissionDto.from((companyId == null
                ? repository.findById(id)
                : repository.findByIdAndCompanyId(id, companyId))
                .orElseThrow(() -> new PermissionNotFoundException(id)));
    }
}
