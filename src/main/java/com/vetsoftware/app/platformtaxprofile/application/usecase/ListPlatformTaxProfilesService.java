package com.vetsoftware.app.platformtaxprofile.application.usecase;

import com.vetsoftware.app.platformtaxprofile.application.dto.PlatformTaxProfileDto;
import com.vetsoftware.app.platformtaxprofile.application.port.in.ListPlatformTaxProfilesUseCase;
import com.vetsoftware.app.platformtaxprofile.application.port.out.PlatformTaxProfileRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "platform.tax.profile.list")
@Service
public class ListPlatformTaxProfilesService implements ListPlatformTaxProfilesUseCase {

    private final PlatformTaxProfileRepository repository;

    public ListPlatformTaxProfilesService(PlatformTaxProfileRepository repository) {
        this.repository = repository;
    }

    /**
     * Los totales salen de la consulta paginada, nunca recalculados sobre el
     * contenido de la pagina.
     */
    @Override
    public PageResult<PlatformTaxProfileDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(PlatformTaxProfileDto::from);
    }
}
