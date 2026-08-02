package com.vetsoftware.app.membershipsubmodule.application.usecase;

import com.vetsoftware.app.membershipsubmodule.application.port.in.DeleteMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipSubModuleRepository;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "membership.submodule.delete")
@Service
public class DeleteMembershipSubModuleService implements DeleteMembershipSubModuleUseCase {
  private final MembershipSubModuleRepository repository;

  public DeleteMembershipSubModuleService(MembershipSubModuleRepository repository) {
    this.repository = repository;
  }

  @Override
  public void execute(Long id) {
    repository.findById(id).orElseThrow(() -> new MembershipSubModuleNotFoundException(id));
    repository.delete(id);
  }
}
