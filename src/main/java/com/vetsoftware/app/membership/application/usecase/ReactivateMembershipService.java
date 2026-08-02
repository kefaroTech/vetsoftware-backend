package com.vetsoftware.app.membership.application.usecase;

import com.vetsoftware.app.membership.application.dto.MembershipDto;
import com.vetsoftware.app.membership.application.port.in.ReactivateMembershipUseCase;
import com.vetsoftware.app.membership.application.port.out.MembershipRepository;
import com.vetsoftware.app.membership.domain.MembershipNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "membership.reactivate")
@Service
public class ReactivateMembershipService implements ReactivateMembershipUseCase {
    private final MembershipRepository repository;

    public ReactivateMembershipService(MembershipRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public MembershipDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0)
            throw new MembershipNotFoundException(id);
        return MembershipDto.from(
                repository.findById(id).orElseThrow(() -> new MembershipNotFoundException(id)));
    }
}
