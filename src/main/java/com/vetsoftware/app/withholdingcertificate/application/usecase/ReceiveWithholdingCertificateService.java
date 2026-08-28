package com.vetsoftware.app.withholdingcertificate.application.usecase;

import com.vetsoftware.app.withholdingcertificate.application.command.ReceiveWithholdingCertificateCommand;
import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import com.vetsoftware.app.withholdingcertificate.application.port.in.ReceiveWithholdingCertificateUseCase;
import com.vetsoftware.app.withholdingcertificate.application.port.out.WithholdingCertificateRepository;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificate;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificateNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierra la expectativa: el certificado llego.
 *
 * <p>
 * <strong>{@code @Transactional} porque son dos operaciones de repositorio y la
 * segunda depende de lo que leyo la primera.</strong> Sin ella, entre el
 * {@code findById} y el {@code save} cabe otra escritura, y el {@code @Version}
 * de la entidad no puede protegerla: la comprobacion de version ocurre en el
 * {@code flush}, y sin transaccion cada operacion tiene el suyo.
 *
 * <p>
 * <strong>Carga ancha a proposito.</strong> A este servicio solo llega un
 * principal SYSTEM -su unico puerto es {@code hasRole('SYSTEM')} a secas- y un
 * principal SYSTEM no tiene empresa de la que tirar. Es la exencion que
 * {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} declara por escrito; si algun dia
 * este puerto se abriera a un empleado, la carga tendria que pasar a
 * {@code findByIdAndCompanyId} <em>en el mismo cambio</em>.
 */
@Observed(name = "withholding.certificate.receive")
@Service
public class ReceiveWithholdingCertificateService implements ReceiveWithholdingCertificateUseCase {

    private final WithholdingCertificateRepository repository;

    public ReceiveWithholdingCertificateService(WithholdingCertificateRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public WithholdingCertificateDto execute(ReceiveWithholdingCertificateCommand command) {
        WithholdingCertificate certificate = repository.findById(command.id())
                .orElseThrow(() -> new WithholdingCertificateNotFoundException(command.id()));
        certificate.receive(command.receivedOn(), command.fileRef());
        return WithholdingCertificateDto.from(repository.save(certificate));
    }
}
