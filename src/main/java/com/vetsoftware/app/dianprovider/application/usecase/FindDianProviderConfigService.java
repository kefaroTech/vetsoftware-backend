package com.vetsoftware.app.dianprovider.application.usecase;

import com.vetsoftware.app.dianprovider.application.dto.DianProviderConfigDto;
import com.vetsoftware.app.dianprovider.application.port.in.FindDianProviderConfigUseCase;
import com.vetsoftware.app.dianprovider.application.port.out.DianProviderConfigRepository;
import com.vetsoftware.app.dianprovider.domain.DianProviderConfigNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "dian.provider.config.find")
@Service
public class FindDianProviderConfigService implements FindDianProviderConfigUseCase {
  private final DianProviderConfigRepository repository;

  public FindDianProviderConfigService(DianProviderConfigRepository repository) {
    this.repository = repository;
  }

  @Override
  public DianProviderConfigDto findByCompany(Long companyId) {
    return DianProviderConfigDto.from(
        repository
            .findByCompanyId(companyId)
            .orElseThrow(() -> new DianProviderConfigNotFoundException(companyId)));
  }
}
