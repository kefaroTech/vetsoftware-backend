package com.vetsoftware.app.withholdingcertificate.application.usecase;

import com.vetsoftware.app.withholdingcertificate.application.command.RegisterWithholdingCertificateCommand;
import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import com.vetsoftware.app.withholdingcertificate.application.port.in.RegisterWithholdingCertificateUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.out.WithholdingCertificateRepository;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificate;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * Abre la expectativa de un certificado.
 *
 * <p>
 * <strong>No comprueba el duplicado antes de insertar, y es una
 * decision.</strong> {@code uq_withholding_certificates_number} ya impide dos
 * certificados del mismo ano con el mismo numero del mismo expedidor; una
 * lectura previa no evitaria la carrera -dos peticiones simultaneas leerian «no
 * existe» las dos- y solo cambiaria de sitio el error. Este caso de uso no es
 * idempotente ni pretende serlo: registrar un certificado es un acto de
 * conciliacion manual, no un cobro que un doble clic pueda duplicar sin que
 * nadie lo vea.
 *
 * <p>
 * <strong>{@code Clock} inyectado, nunca {@code LocalDateTime.now()}
 * pelado.</strong> Es lo unico que permite que un test afirme sobre
 * {@code createdDate} sin caerse el dia que el reloj cruce medianoche entre dos
 * lineas.
 */
@Observed(name = "withholding.certificate.register")
@Service
public class RegisterWithholdingCertificateService
        implements
            RegisterWithholdingCertificateUseCase {

    private final WithholdingCertificateRepository repository;
    private final Clock clock;

    public RegisterWithholdingCertificateService(WithholdingCertificateRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public WithholdingCertificateDto execute(RegisterWithholdingCertificateCommand command) {
        WithholdingCertificate certificate = WithholdingCertificate.register(command.companyId(),
                command.issuedByTaxId(), command.certificateNumber(), command.withholdingType(),
                command.fiscalYear(), command.fiscalPeriodKey(), command.ratePercent(),
                command.certifiedAmount(), command.issuedOn(), command.legalDeadlineOn(),
                LocalDateTime.now(clock));
        return WithholdingCertificateDto.from(repository.save(certificate));
    }
}
