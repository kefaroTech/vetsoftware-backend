package com.vetsoftware.app.externalinvoicingoutage.application.usecase;

import com.vetsoftware.app.externalinvoicingoutage.application.command.NotifyAffectedCompaniesCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.NotifyAffectedCompaniesUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageRepository;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutage;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anota que ya se aviso a las clinicas alcanzadas.
 *
 * <p>
 * <strong>No envia el aviso: lo registra.</strong> La diferencia no es
 * terminologica. Un envio es un efecto externo que no vuelve, y meterlo en esta
 * transaccion —que puede revertir en el flush por el chequeo de
 * {@code @Version}— dejaria a las clinicas con un correo sobre una marca que no
 * llego a escribirse (BE-18). Quien envie sera otro caso de uso y llamara a
 * este despues, con la hora real del envio.
 *
 * <p>
 * Es idempotente y sobrescribe la marca anterior: durante una caida larga se
 * avisa varias veces y lo que hay que conservar es la ultima vez que se
 * informo, con el alcance ya corregido. Lo unico que la base impide
 * ({@code chk_eio_notified}) es informar antes de que la caida empezara.
 */
@Observed(name = "external.invoicing.outage.notify")
@Service
public class NotifyAffectedCompaniesService implements NotifyAffectedCompaniesUseCase {

    private final ExternalInvoicingOutageRepository repository;

    public NotifyAffectedCompaniesService(ExternalInvoicingOutageRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ExternalInvoicingOutageDto execute(NotifyAffectedCompaniesCommand command) {
        ExternalInvoicingOutage outage = repository.findById(command.id())
                .orElseThrow(() -> new ExternalInvoicingOutageNotFoundException(command.id()));
        return ExternalInvoicingOutageDto.from(repository.save(
                outage.notifyCompanies(command.notifiedAt(), command.affectedCompanyCount())));
    }
}
