package com.vetsoftware.app.membership.application.usecase;

import com.vetsoftware.app.membership.application.dto.MembershipDto;
import com.vetsoftware.app.membership.application.port.in.FindMembershipUseCase;
import com.vetsoftware.app.membership.application.port.out.MembershipRepository;
import com.vetsoftware.app.membership.domain.MembershipNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "membership.find")
@Service
public class FindMembershipService implements FindMembershipUseCase {
  private final MembershipRepository repository;

  public FindMembershipService(MembershipRepository repository) {
    this.repository = repository;
  }

  @Override
  public MembershipDto findById(Long id) {
    return repository
        .findById(id)
        .map(MembershipDto::from)
        .orElseThrow(() -> new MembershipNotFoundException(id));
  }
}
