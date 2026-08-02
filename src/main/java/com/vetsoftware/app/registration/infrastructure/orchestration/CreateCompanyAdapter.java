package com.vetsoftware.app.registration.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.port.in.CreateCompanyUseCase;
import com.vetsoftware.app.registration.application.port.out.CompanyCreator;
import org.springframework.stereotype.Component;

@Component
public class CreateCompanyAdapter implements CompanyCreator {

  private final CreateCompanyUseCase createCompanyUseCase;
  private final SystemAuthRunner systemAuthRunner;

  public CreateCompanyAdapter(
      CreateCompanyUseCase createCompanyUseCase, SystemAuthRunner systemAuthRunner) {
    this.createCompanyUseCase = createCompanyUseCase;
    this.systemAuthRunner = systemAuthRunner;
  }

  @Override
  public CompanyResult create(
      String name,
      String identifier,
      String address,
      String contactNumber,
      Long cityId,
      Long membershipId) {
    var dto =
        systemAuthRunner.call(
            () ->
                createCompanyUseCase.execute(
                    new CreateCompanyCommand(
                        name, identifier, address, contactNumber, cityId, membershipId)));
    return new CompanyResult(dto.id(), dto.name(), dto.identifier());
  }
}
