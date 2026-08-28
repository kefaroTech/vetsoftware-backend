package com.vetsoftware.app.externalinvoicingoutage.application.usecase;

import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.FindExternalInvoicingOutageUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageRepository;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Una caida por su identificador.
 *
 * <p>
 * <strong>Llama a {@code findById} a secas y eso aqui es correcto</strong>, no
 * una fuga: {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} exige la variante acotada
 * solo cuando el puerto de salida <em>tambien</em> la declara, y este no puede
 * declararla porque la tabla no tiene {@code company_id}. El servicio ademas
 * solo alcanza {@code ROLE_SYSTEM}, que es la exencion escrita de esa regla: un
 * principal SYSTEM no tiene empresa.
 */
@Observed(name = "external.invoicing.outage.find")
@Service
public class FindExternalInvoicingOutageService implements FindExternalInvoicingOutageUseCase {

    private final ExternalInvoicingOutageRepository repository;

    public FindExternalInvoicingOutageService(ExternalInvoicingOutageRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public ExternalInvoicingOutageDto execute(Long id) {
        return repository.findById(id).map(ExternalInvoicingOutageDto::from)
                .orElseThrow(() -> new ExternalInvoicingOutageNotFoundException(id));
    }
}
