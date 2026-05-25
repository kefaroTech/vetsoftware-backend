package com.vetsoftware.app.submodule.application.usecase;

import com.vetsoftware.app.submodule.application.port.in.DeleteSubModuleUseCase;
import com.vetsoftware.app.submodule.application.port.out.MembershipSubModuleChildrenQueryPort;
import com.vetsoftware.app.submodule.application.port.out.SubModuleRepository;
import com.vetsoftware.app.submodule.domain.SubModuleHasActiveChildrenException;
import com.vetsoftware.app.submodule.domain.SubModuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "submodule.delete")
@Service
public class DeleteSubModuleService implements DeleteSubModuleUseCase {
    private final SubModuleRepository repository;
    private final MembershipSubModuleChildrenQueryPort membershipSubModuleChildrenQueryPort;

    public DeleteSubModuleService(
            SubModuleRepository repository,
            MembershipSubModuleChildrenQueryPort membershipSubModuleChildrenQueryPort) {
        this.repository = repository;
        this.membershipSubModuleChildrenQueryPort = membershipSubModuleChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new SubModuleNotFoundException(id));
        if (membershipSubModuleChildrenQueryPort.existsActiveBySubModuleId(id)) {
            throw new SubModuleHasActiveChildrenException(id, "membershipSubModule");
        }
        repository.delete(id);
    }
}
