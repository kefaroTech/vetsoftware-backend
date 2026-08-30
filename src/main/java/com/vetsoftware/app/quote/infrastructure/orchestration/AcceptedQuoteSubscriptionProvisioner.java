package com.vetsoftware.app.quote.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.quote.application.port.out.SubscriptionProvisioningPort;
import com.vetsoftware.app.subscription.application.command.ReplaceSubscriptionFromQuoteCommand;
import com.vetsoftware.app.subscription.application.command.SettleNewContractCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.port.in.ReplaceSubscriptionFromQuoteUseCase;
import com.vetsoftware.app.subscription.application.port.in.SettleNewContractUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * El unico fichero de la rodaja {@code quote} que sabe que existen los
 * contratos.
 *
 * <p>
 * Hace dos cosas, y estan a los dos lados del commit a proposito:
 *
 * <ol>
 * <li><strong>Firmar, dentro de la transaccion de la aceptacion.</strong>
 * {@code ReplaceSubscriptionFromQuoteUseCase} es {@code @Transactional} con
 * propagacion {@code REQUIRED}, asi que se une a la que abrio
 * {@code AcceptQuoteService}: o quedan la aceptacion y el contrato, o no queda
 * ninguna de las dos. Nada se captura aqui — ver el javadoc del puerto: un
 * adaptador que se tragara el fallo reabriria la ventana que este diseño
 * cierra.</li>
 * <li><strong>Cobrar, despues del commit.</strong> Cobrar es I/O externo y no
 * puede ocurrir dentro de la transaccion; ademas, un cobro disparado antes del
 * commit se habria entregado igual aunque la firma revirtiera despues, y de un
 * cobro emitido no se vuelve (BE-18).</li>
 * </ol>
 *
 * <p>
 * <strong>Por que la escalada a {@code SYSTEM} es legitima y no un rodeo del
 * gate.</strong> Los dos puertos que se invocan aqui estan cerrados a
 * {@code hasRole('SYSTEM')} porque firmar y activar contratos son actos de
 * plataforma, no operaciones del cliente. Quien llega hasta aqui ya paso por
 * {@code AcceptQuoteUseCase}, que revalido con {@code @authz.isMyCompany} que
 * la cotizacion es de la empresa del principal; la empresa viaja <em>ya
 * decidida</em> dentro del command y ninguno de los dos casos de uso la lee del
 * principal, asi que la escalada no cambia sobre que filas se actua, solo
 * permite actuar. Mismo cableado que {@code PlatformQuoteIssuerAdapter} y que
 * {@code PlatformCatalogSubscriptionCreator}.
 *
 * <p>
 * <strong>Lo que hay que vigilar al mantener.</strong> Si algun dia
 * {@code ReplaceSubscriptionFromQuoteService} empezara a resolver la empresa
 * desde el principal en vez de recibirla en el command, esta escalada le daria
 * el de plataforma y el contrato se firmaria sin empresa. Hoy la recibe.
 */
@Component
public class AcceptedQuoteSubscriptionProvisioner implements SubscriptionProvisioningPort {

    private static final Logger log = LoggerFactory
            .getLogger(AcceptedQuoteSubscriptionProvisioner.class);

    private final ReplaceSubscriptionFromQuoteUseCase replaceUseCase;
    private final SettleNewContractUseCase settleUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public AcceptedQuoteSubscriptionProvisioner(ReplaceSubscriptionFromQuoteUseCase replaceUseCase,
            SettleNewContractUseCase settleUseCase, SystemAuthRunner systemAuthRunner) {
        this.replaceUseCase = replaceUseCase;
        this.settleUseCase = settleUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public void provisionFromAcceptedQuote(Long quoteId, Long companyId) {
        SubscriptionDto contract = systemAuthRunner.call(() -> replaceUseCase
                .execute(new ReplaceSubscriptionFromQuoteCommand(quoteId, companyId)));
        settleAfterCommit(new SettleNewContractCommand(contract.id(), companyId));
    }

    /**
     * El cobro, diferido al commit.
     *
     * <p>
     * <strong>En su propio metodo y con clase anonima, no con un lambda.</strong>
     * La regla {@code EFECTOS_ASINCRONOS_DESPUES_DEL_COMMIT} se detiene en el
     * primer metodo que habla con {@code TransactionSynchronizationManager}; un
     * {@code afterCommit} escrito como lambda le queda atribuido al metodo que lo
     * declara y da falso positivo.
     *
     * <p>
     * <strong>Sin transaccion activa se cobra en el acto.</strong> Es la rama de
     * guarda que exige el patron del repositorio: {@code registerSynchronization}
     * lanzaria {@code IllegalStateException} si nadie abrio transaccion —un test
     * unitario, o un llamador sin {@code @Transactional}—.
     *
     * <p>
     * <strong>El callback nunca lanza.</strong> Despues del commit la transaccion
     * ya confirmo, y una excepcion aqui se propagaria al llamador convirtiendo una
     * aceptacion correcta en un 500: el cliente veria un error habiendo firmado su
     * contrato. Un cobro que falla deja el contrato donde nacio, que es la conducta
     * documentada en {@code SettleNewContractUseCase}.
     */
    private void settleAfterCommit(SettleNewContractCommand command) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            settle(command);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                settle(command);
            }
        });
    }

    private void settle(SettleNewContractCommand command) {
        try {
            systemAuthRunner.run(() -> settleUseCase.execute(command));
        } catch (RuntimeException exception) {
            log.warn("No se pudo liquidar el primer periodo del contrato {}: {}",
                    command.subscriptionId(), exception.getMessage());
        }
    }
}
