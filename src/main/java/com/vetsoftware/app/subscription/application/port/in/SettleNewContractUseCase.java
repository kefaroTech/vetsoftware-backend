package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.subscription.application.command.SettleNewContractCommand;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Cobra el primer periodo de un contrato recien firmado y lo activa si el cobro
 * se aprueba.
 *
 * <p>
 * <strong>Es un puerto aparte y no un paso mas de
 * {@link ReplaceSubscriptionFromQuoteUseCase}, por una razon estructural:
 * cobrar es I/O externo y firmar es una transaccion.</strong> La regla dura
 * {@code SIN_IO_EXTERNO_EN_TRANSACCION} sigue la cadena de llamadas, asi que
 * meter el cobro dentro del alta romperia el build en cuanto el adaptador
 * simulado se sustituya por uno real —y, mucho peor, retendria la conexion del
 * pool y los locks del contrato mientras una pasarela remota se lo piensa—.
 * Separarlos es lo que permite que el llamador dispare esto en
 * {@code afterCommit}, con el contrato ya confirmado.
 *
 * <p>
 * <strong>Que pasa si el cobro no llega.</strong> Nada que haya que deshacer:
 * el contrato se queda en el estado en que nacio —{@code TRIALING} si la
 * plataforma concede prueba, {@code ACTIVE} si no— y el cliente conserva su
 * acceso. Esa es la conducta deliberada mientras no hay pasarela: el modelo ya
 * tiene {@code PAST_DUE} y la cobranza ({@code dunning}) para degradar a quien
 * de verdad deba, y ese camino se activa solo cuando exista un cobro real que
 * pueda fallar. Inventar aqui una degradacion por un cobro que hoy nadie
 * intenta dejaria clinicas en solo lectura sin ninguna deuda detras.
 *
 * <p>
 * Es idempotente por construccion: activar un contrato ya {@code ACTIVE} no es
 * una transicion permitida y se descarta sin tocar nada.
 */
public interface SettleNewContractUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    void execute(SettleNewContractCommand command);
}
