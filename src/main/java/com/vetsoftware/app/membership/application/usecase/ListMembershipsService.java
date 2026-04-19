package com.vetsoftware.app.membership.application.usecase;

import com.vetsoftware.app.membership.application.dto.MembershipDto;
import com.vetsoftware.app.membership.application.port.in.ListMembershipsUseCase;
import com.vetsoftware.app.membership.application.port.out.MembershipRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "membership.list")
@Service
public class ListMembershipsService implements ListMembershipsUseCase {
    private final MembershipRepository repository;

    public ListMembershipsService(MembershipRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MembershipDto> listAll() {
        return repository.findAll().stream().map(MembershipDto::from).toList();
    }
}
