package com.vetsoftware.app.electronicdocument.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.vetsoftware.app.openaccount.application.command.MarkOpenAccountReversedCommand;
import com.vetsoftware.app.openaccount.application.port.in.MarkOpenAccountReversedUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Incidencia #124. Este adaptador sustituye a {@code JpaAccountReversalPort},
 * que estampaba la fila a mano contra el repositorio JPA de {@code openaccount}
 * y por el camino habia copiado la mitad de la regla del reverso —la
 * idempotencia— y omitido la otra —que solo una cuenta CLOSE se reversa—.
 *
 * <p>
 * Lo que estos casos fijan es justamente que aqui <b>no</b> hay regla: que el
 * adaptador delega y nada mas. Reintroducir un {@code if} de estado o de
 * idempotencia en esta clase seria volver a tener dos verdades sobre lo mismo,
 * con la del dominio otra vez sin ejecutarse en produccion.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountReversalAdapter — conecta la validacion DIAN de la nota credito con el reverso de cartera")
class AccountReversalAdapterTest {

    @Mock
    private MarkOpenAccountReversedUseCase markReversed;

    private AccountReversalAdapter adapter;

    @BeforeEach
    void montar() {
        adapter = new AccountReversalAdapter(markReversed);
    }

    @Test
    @DisplayName("traslada la cuenta y la empresa al caso de uso de openaccount")
    void traslada_cuenta_y_empresa() {
        adapter.markReversed(100L, 9L);

        ArgumentCaptor<MarkOpenAccountReversedCommand> captor = ArgumentCaptor
                .forClass(MarkOpenAccountReversedCommand.class);
        verify(markReversed).execute(captor.capture());
        assertThat(captor.getValue().openAccountId()).isEqualTo(100L);
        // Sin la empresa, la lectura del reverso no queda acotada al tenant dueño.
        assertThat(captor.getValue().companyId()).isEqualTo(9L);
    }

    /**
     * Dejar la fecha al dominio no es un detalle de estilo: es lo que hace que una
     * segunda validacion del mismo webhook no reescriba el instante del reverso. Si
     * el adaptador pusiera aqui su {@code LocalDateTime.now()}, la idempotencia
     * volveria a depender de quien llama.
     */
    @Test
    @DisplayName("no decide la fecha del reverso: la deja en null para que la ponga el dominio")
    void no_decide_la_fecha_del_reverso() {
        adapter.markReversed(100L, 9L);

        verify(markReversed).execute(new MarkOpenAccountReversedCommand(100L, 9L, null));
    }
}
