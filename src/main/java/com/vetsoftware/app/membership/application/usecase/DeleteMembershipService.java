package com.vetsoftware.app.membership.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membership.application.port.in.DeleteMembershipUseCase;
import com.vetsoftware.app.membership.application.port.out.MembershipRepository;
import com.vetsoftware.app.membership.domain.MembershipNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "membership.delete")
@Service
public class DeleteMembershipService implements DeleteMembershipUseCase {
    private final MembershipRepository repository;

    public DeleteMembershipService(MembershipRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id, AuthContext auth) {
        repository.findById(id).orElseThrow(() -> new MembershipNotFoundException(id));
        repository.delete(id);
    }
}
