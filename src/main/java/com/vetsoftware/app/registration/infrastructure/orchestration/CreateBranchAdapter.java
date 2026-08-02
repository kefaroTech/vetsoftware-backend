package com.vetsoftware.app.registration.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.branch.application.command.CreateBranchCommand;
import com.vetsoftware.app.branch.application.port.in.CreateBranchUseCase;
import com.vetsoftware.app.registration.application.port.out.BranchCreator;
import org.springframework.stereotype.Component;

@Component
public class CreateBranchAdapter implements BranchCreator {

    private final CreateBranchUseCase createBranchUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public CreateBranchAdapter(CreateBranchUseCase createBranchUseCase,
            SystemAuthRunner systemAuthRunner) {
        this.createBranchUseCase = createBranchUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public void create(String name, String code, String address, String phone, Long cityId,
            Long companyId) {
        systemAuthRunner.run(() -> createBranchUseCase
                .execute(new CreateBranchCommand(name, code, address, phone, cityId, companyId)));
    }
}
