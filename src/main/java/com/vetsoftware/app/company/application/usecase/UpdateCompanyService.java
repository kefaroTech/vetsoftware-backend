package com.vetsoftware.app.company.application.usecase;

import com.vetsoftware.app.company.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.UpdateCompanyUseCase;
import com.vetsoftware.app.company.application.port.out.CityQueryPort;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.application.port.out.MembershipQueryPort;
import com.vetsoftware.app.company.domain.CityRef;
import com.vetsoftware.app.company.domain.Company;
import com.vetsoftware.app.company.domain.CompanyNotFoundException;
import com.vetsoftware.app.company.domain.MembershipRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "company.update")
@Service
public class UpdateCompanyService implements UpdateCompanyUseCase {
  private final CompanyRepository repository;
  private final CityQueryPort cityQueryPort;
  private final MembershipQueryPort membershipQueryPort;

  public UpdateCompanyService(
      CompanyRepository repository,
      CityQueryPort cityQueryPort,
      MembershipQueryPort membershipQueryPort) {
    this.repository = repository;
    this.cityQueryPort = cityQueryPort;
    this.membershipQueryPort = membershipQueryPort;
  }

  @Override
  @Transactional
  public CompanyDto execute(UpdateCompanyCommand command) {
    Company company =
        repository
            .findById(command.id())
            .orElseThrow(() -> new CompanyNotFoundException(command.id()));
    CityRef city =
        cityQueryPort
            .findById(command.cityId())
            .orElseThrow(() -> new IllegalArgumentException("City not found: " + command.cityId()));
    MembershipRef membership =
        membershipQueryPort
            .findById(command.membershipId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Membership not found: " + command.membershipId()));
    company.update(
        command.name(),
        command.identifier(),
        command.address(),
        command.contactNumber(),
        city,
        membership);
    return CompanyDto.from(repository.save(company));
  }
}
