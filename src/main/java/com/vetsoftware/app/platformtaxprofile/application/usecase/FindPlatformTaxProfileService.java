package com.vetsoftware.app.platformtaxprofile.application.usecase;

import com.vetsoftware.app.platformtaxprofile.application.dto.PlatformTaxProfileDto;
import com.vetsoftware.app.platformtaxprofile.application.port.in.FindPlatformTaxProfileUseCase;
import com.vetsoftware.app.platformtaxprofile.application.port.out.PlatformTaxProfileRepository;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxProfileNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "platform.tax.profile.find")
@Service
public class FindPlatformTaxProfileService implements FindPlatformTaxProfileUseCase {

    private final PlatformTaxProfileRepository repository;

    public FindPlatformTaxProfileService(PlatformTaxProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public PlatformTaxProfileDto findById(Long id) {
        return repository.findById(id).map(PlatformTaxProfileDto::from)
                .orElseThrow(() -> new PlatformTaxProfileNotFoundException(id));
    }
}
