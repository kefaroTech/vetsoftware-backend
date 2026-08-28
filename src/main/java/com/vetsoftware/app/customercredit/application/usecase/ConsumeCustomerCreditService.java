package com.vetsoftware.app.customercredit.application.usecase;

import com.vetsoftware.app.customercredit.application.command.ConsumeCustomerCreditCommand;
import com.vetsoftware.app.customercredit.application.dto.CustomerCreditEntryDto;
import com.vetsoftware.app.customercredit.application.port.in.ConsumeCustomerCreditUseCase;
import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditBalanceRepository;
import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditEntryRepository;
import com.vetsoftware.app.customercredit.domain.CreditLot;
import com.vetsoftware.app.customercredit.domain.CustomerCreditEntry;
import com.vetsoftware.app.customercredit.domain.InsufficientCustomerCreditException;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica saldo a favor, repartiendolo entre los lotes vivos.
 *
 * <p>
 * <strong>El orden de los pasos ES el diseno</strong>, y cambiarlo rompe la
 * unica garantia que da esta clase:
 *
 * <ol>
 * <li><strong>Primero se mueve el saldo</strong>, con la comprobacion dentro de
 * la propia instruccion. Ese {@code UPDATE} toma el bloqueo de la fila resumen
 * y lo mantiene hasta el commit, de modo que cualquier otro consumidor de la
 * misma empresa se queda esperando ahi. Si afecta cero filas, no hay saldo y se
 * aborta.</li>
 * <li><strong>Despues</strong>, ya con la fila bloqueada, se leen los lotes y
 * se reparte. Leerlos antes seria leerlos sin proteccion: dos consumos
 * simultaneos verian los mismos lotes y el reparto de uno pisaria al del
 * otro.</li>
 * </ol>
 *
 * <p>
 * <strong>El caso concreto que evita</strong>, que no da error y descuadra
 * dinero del cliente: saldo vivo de cien mil. La contadora lo aplica a la
 * factura de marzo desde una pestana y el proceso de renovacion lo aplica a la
 * de abril en el mismo segundo. Las dos leen cien mil, las dos escriben ochenta
 * mil, las dos confirman. Saldo menos sesenta mil, y nadie ve un error. Leer el
 * saldo, decidir en memoria y escribir despues <strong>no</strong> equivale a
 * esto, por mucho que el resultado se parezca en las pruebas de un solo hilo.
 *
 * <p>
 * <strong>Se consume primero lo que antes caduca</strong> (D-71). El puerto
 * devuelve los lotes ya en ese orden. Cien mil que caducan en diciembre mas
 * cincuenta mil sin fecha, consumidos ciento veinte mil: gastando por caducidad
 * mas proxima no caduca nada y quedan treinta mil vivos, que es lo mas
 * favorable al cliente. Cada fila escrita anota de que lote salio, porque sin
 * eso la caducidad del remanente deja de ser calculable.
 */
@Observed(name = "customer.credit.consume")
@Service
public class ConsumeCustomerCreditService implements ConsumeCustomerCreditUseCase {

    private final CustomerCreditEntryRepository entryRepository;
    private final CustomerCreditBalanceRepository balanceRepository;
    private final Clock clock;

    public ConsumeCustomerCreditService(CustomerCreditEntryRepository entryRepository,
            CustomerCreditBalanceRepository balanceRepository, Clock clock) {
        this.entryRepository = entryRepository;
        this.balanceRepository = balanceRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<CustomerCreditEntryDto> execute(ConsumeCustomerCreditCommand command) {
        List<CustomerCreditEntry> already = entryRepository.findOperation(command.companyId(),
                command.clientRequestId());
        if (!already.isEmpty())
            return already.stream().map(CustomerCreditEntryDto::from).toList();

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = LocalDate.now(clock);

        // PASO 1 - la barandilla. Toma el bloqueo de la fila y comprueba el saldo en
        // la misma instruccion. Cero filas = no hay saldo, y se aborta antes de
        // haber escrito un solo asiento.
        int moved = balanceRepository.applyDelta(command.companyId(), command.amount().negate(),
                now);
        if (moved == 0)
            throw new InsufficientCustomerCreditException(command.companyId(), command.amount());

        // PASO 2 - ya serializados, se reparte.
        List<CustomerCreditEntry> written = allocate(command, now, today);

        CustomerCreditBalances.refreshNextExpiry(entryRepository, balanceRepository,
                command.companyId(), now);
        return written.stream().map(CustomerCreditEntryDto::from).toList();
    }

    /**
     * Reparte el importe entre los lotes, del que antes caduca al que mas tarde, y
     * escribe un asiento por cada lote tocado.
     *
     * <p>
     * <strong>Que el reparto no llegue a cubrir el importe con el saldo ya
     * descontado significa que la proyeccion y el libro han divergido</strong> —la
     * fila resumen decia que habia y los lotes dicen que no—. Es exactamente el
     * caso en que manda el libro, asi que se aborta la transaccion entera: el
     * {@code UPDATE} del paso 1 se deshace con ella y el saldo vuelve a su sitio.
     */
    private List<CustomerCreditEntry> allocate(ConsumeCustomerCreditCommand command,
            LocalDateTime now, LocalDate today) {
        List<CustomerCreditEntry> written = new ArrayList<>();
        BigDecimal pending = command.amount();
        int index = 0;
        for (CreditLot lot : entryRepository.findOpenLotsByCompanyId(command.companyId())) {
            if (pending.signum() <= 0)
                break;
            BigDecimal taken = lot.take(pending);
            written.add(entryRepository.save(CustomerCreditEntry.consumption(command.companyId(),
                    taken, lot.entryId(), command.originDocumentId(), now, today,
                    lotKey(command.clientRequestId(), index), now)));
            pending = pending.subtract(taken);
            index++;
        }
        if (pending.signum() > 0)
            throw new InsufficientCustomerCreditException(command.companyId(), command.amount());
        return written;
    }

    /**
     * La llave de idempotencia de una fila concreta.
     *
     * <p>
     * {@code uq_cce_idempotency} es {@code (company_id, client_request_id)}, y un
     * consumo escribe N filas: con la llave del cliente repetida en todas
     * colisionarian entre si. El sufijo por lote las separa y sigue siendo
     * determinista, que es lo que hace que el reintento encuentre la operacion
     * entera en vez de duplicarla. La frontera prohibe el separador en la llave del
     * cliente para que un prefijo no pueda confundirse con otro.
     */
    private static String lotKey(String clientRequestId, int index) {
        return clientRequestId + CustomerCreditEntryRepository.OPERATION_SEPARATOR + index;
    }
}
