package com.vetsoftware.app.companycontactchannel.application.usecase;

import com.vetsoftware.app.companycontactchannel.application.dto.CompanyContactChannelDto;
import com.vetsoftware.app.companycontactchannel.application.port.in.ListUsableCompanyContactChannelsUseCase;
import com.vetsoftware.app.companycontactchannel.application.port.out.CompanyContactChannelRepository;
import com.vetsoftware.app.companycontactchannel.domain.ContactPurpose;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * Por donde se le puede escribir hoy a esta empresa para este fin.
 *
 * <p>
 * Es la consulta que la cobranza ejecuta antes de mandar nada, y la que hace
 * que el incumplimiento sea demostrablemente imposible en vez de improbable.
 */
@Observed(name = "company.contact.channel.list.usable")
@Service
public class ListUsableCompanyContactChannelsService
        implements
            ListUsableCompanyContactChannelsUseCase {

    private final CompanyContactChannelRepository repository;

    public ListUsableCompanyContactChannelsService(CompanyContactChannelRepository repository) {
        this.repository = repository;
    }

    /**
     * Los totales son los de la consulta y no se recalculan sobre el contenido ya
     * paginado: {@code PageResult.map} conserva los metadatos intactos.
     */
    @Override
    public PageResult<CompanyContactChannelDto> listUsable(Long companyId, ContactPurpose purpose,
            int page, int pageSize) {
        return repository.findAllUsableByCompanyIdAndPurpose(companyId, purpose, page, pageSize)
                .map(CompanyContactChannelDto::from);
    }
}
