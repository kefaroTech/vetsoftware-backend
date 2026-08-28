package com.vetsoftware.app.companycontactchannel.application.usecase;

import com.vetsoftware.app.companycontactchannel.application.dto.CompanyContactChannelDto;
import com.vetsoftware.app.companycontactchannel.application.port.in.FindCompanyContactChannelUseCase;
import com.vetsoftware.app.companycontactchannel.application.port.out.CompanyContactChannelRepository;
import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannelNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "company.contact.channel.find")
@Service
public class FindCompanyContactChannelService implements FindCompanyContactChannelUseCase {

    private final CompanyContactChannelRepository repository;

    public FindCompanyContactChannelService(CompanyContactChannelRepository repository) {
        this.repository = repository;
    }

    /**
     * El canal de otra empresa sale como <strong>no encontrado</strong> y no como
     * prohibido, y esa es la respuesta correcta: un 403 confirmaria que la fila
     * existe, y con ids consecutivos eso es un censo de por donde se le escribe a
     * la competencia.
     *
     * <p>
     * <strong>Devuelve tambien los revocados.</strong> No es un descuido de
     * filtrado: la lectura por id es la que responde si aquel aviso estaba
     * permitido, y esconder el canal cerrado dejaria esa pregunta sin respuesta
     * justo cuando hace falta contestarla.
     */
    @Override
    public CompanyContactChannelDto findById(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(CompanyContactChannelDto::from)
                .orElseThrow(() -> new CompanyContactChannelNotFoundException(id));
    }
}
