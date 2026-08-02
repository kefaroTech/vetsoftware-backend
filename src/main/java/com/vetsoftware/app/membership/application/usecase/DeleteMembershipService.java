package com.vetsoftware.app.membership.application.usecase;

import com.vetsoftware.app.membership.application.port.in.DeleteMembershipUseCase;
import com.vetsoftware.app.membership.application.port.out.MembershipRepository;
import com.vetsoftware.app.membership.application.port.out.MembershipSubModuleChildrenQueryPort;
import com.vetsoftware.app.membership.domain.MembershipHasActiveChildrenException;
import com.vetsoftware.app.membership.domain.MembershipNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "membership.delete")
@Service
public class DeleteMembershipService implements DeleteMembershipUseCase {
  private final MembershipRepository repository;
  private final MembershipSubModuleChildrenQueryPort membershipSubModuleChildrenQueryPort;

  public DeleteMembershipService(
      MembershipRepository repository,
      MembershipSubModuleChildrenQueryPort membershipSubModuleChildrenQueryPort) {
    this.repository = repository;
    this.membershipSubModuleChildrenQueryPort = membershipSubModuleChildrenQueryPort;
  }

  @Override
  @Transactional
  public void execute(Long id) {
    repository.findById(id).orElseThrow(() -> new MembershipNotFoundException(id));
    if (membershipSubModuleChildrenQueryPort.existsActiveByMembershipId(id)) {
      throw new MembershipHasActiveChildrenException(id, "membershipSubModule");
    }
    repository.delete(id);
  }
}
