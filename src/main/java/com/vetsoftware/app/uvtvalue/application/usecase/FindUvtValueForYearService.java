package com.vetsoftware.app.uvtvalue.application.usecase;

import com.vetsoftware.app.uvtvalue.application.dto.UvtValueDto;
import com.vetsoftware.app.uvtvalue.application.port.in.FindUvtValueForYearUseCase;
import com.vetsoftware.app.uvtvalue.application.port.out.UvtValueRepository;
import com.vetsoftware.app.uvtvalue.domain.UvtValueNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "uvt.find")
@Service
public class FindUvtValueForYearService implements FindUvtValueForYearUseCase {

    private final UvtValueRepository repository;

    public FindUvtValueForYearService(UvtValueRepository repository) {
        this.repository = repository;
    }

    /**
     * Si el ano no esta publicado, <strong>falla</strong>. No cae en el ano
     * anterior ni en el ultimo conocido: esa cortesia produciria una liquidacion
     * con la cifra de otro ano y nadie se enteraria hasta la revision.
     */
    @Override
    public UvtValueDto findByYear(int fiscalYear, Long companyId) {
        return repository.findByFiscalYear(fiscalYear).map(UvtValueDto::from)
                .orElseThrow(() -> new UvtValueNotFoundException(fiscalYear));
    }
}
