package com.vetsoftware.app.withholdingcertificate.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import com.vetsoftware.app.withholdingcertificate.application.port.in.ListMissingWithholdingCertificatesUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.out.WithholdingCertificateRepository;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * El barrido de vencimientos de plataforma: lo que falta por recibir en todas
 * las clinicas antes de que sea tarde.
 *
 * <p>
 * La fecha limite la pone quien llama y no el reloj de este servicio: la
 * consola decide si quiere ver lo que vence hoy o lo que vence en un mes, y un
 * {@code LocalDate.now()} aqui dentro le quitaria esa decision y ademas haria
 * el caso de uso imposible de probar de forma determinista.
 */
@Observed(name = "withholding.certificate.list.missing")
@Service
public class ListMissingWithholdingCertificatesService
        implements
            ListMissingWithholdingCertificatesUseCase {

    private final WithholdingCertificateRepository repository;

    public ListMissingWithholdingCertificatesService(WithholdingCertificateRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<WithholdingCertificateDto> listMissing(LocalDate deadlineBefore, int page,
            int pageSize) {
        return repository.findAllMissing(deadlineBefore, page, pageSize)
                .map(WithholdingCertificateDto::from);
    }
}
