package com.vetsoftware.app.companycontactchannel.application.usecase;

import com.vetsoftware.app.companycontactchannel.application.dto.CompanyContactChannelDto;
import com.vetsoftware.app.companycontactchannel.application.port.in.ListCompanyContactChannelsUseCase;
import com.vetsoftware.app.companycontactchannel.application.port.out.CompanyContactChannelRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * La bitacora completa de la empresa: lo vivo y lo revocado, en el mismo
 * listado.
 */
@Observed(name = "company.contact.channel.list.by.company")
@Service
public class ListCompanyContactChannelsService implements ListCompanyContactChannelsUseCase {

    private final CompanyContactChannelRepository repository;

    public ListCompanyContactChannelsService(CompanyContactChannelRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<CompanyContactChannelDto> listByCompany(Long companyId, int page,
            int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize)
                .map(CompanyContactChannelDto::from);
    }
}
