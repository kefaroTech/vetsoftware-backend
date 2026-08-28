package com.vetsoftware.app.accountmapping.application.usecase;

import com.vetsoftware.app.accountmapping.application.command.CreateAccountMappingCommand;
import com.vetsoftware.app.accountmapping.application.dto.AccountMappingDto;
import com.vetsoftware.app.accountmapping.application.port.in.CreateAccountMappingUseCase;
import com.vetsoftware.app.accountmapping.application.port.out.AccountMappingRepository;
import com.vetsoftware.app.accountmapping.application.port.out.AccountingAccountValidationPort;
import com.vetsoftware.app.accountmapping.application.port.out.CatalogItemValidationPort;
import com.vetsoftware.app.accountmapping.domain.AccountMapping;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publica un mapeo concepto → cuenta.
 *
 * <p>
 * <strong>Lo unico que hace este service son las comprobaciones que el dominio
 * no puede hacer: las que preguntan a otra tabla.</strong> Que la clase admita
 * afinado, que el diferido solo quepa en las dos clases de ingreso, que la
 * subclave no venga vacia y que la vigencia no se cierre antes de abrirse son
 * invariantes y viven en el constructor de {@link AccountMapping}.
 *
 * <h2>La comprobacion que ninguna clave foranea puede hacer</h2>
 *
 * <p>
 * Las tres cuentas se comprueban con {@code existsPostableByCode} y no con un
 * {@code exists} pelado. {@code fk_account_mappings_debit} garantiza que la
 * cuenta <em>existe</em>; no que admita asiento. Un mapeo contra un grupo pasa
 * la clave foranea, genera asientos y <b>descuadra el balance de prueba por
 * arrastre sin un solo error</b> — que es exactamente lo que
 * {@code chk_accounting_accounts_postable} existe para impedir del otro lado y
 * lo que aqui hay que impedir del nuestro.
 *
 * <h2>La unicidad no se comprueba preguntando antes</h2>
 *
 * <p>
 * Las dos que importan —{@code uq_account_mappings_case} y
 * {@code uq_account_mappings_current}— las cuida la base sobre columnas
 * generadas. Un {@code exists} previo seria una comprobacion que dos peticiones
 * concurrentes pasarian las dos, y dejaria dos mapeos vigentes para el mismo
 * supuesto: justo lo que el segundo marcador se anadio para cerrar.
 */
@Observed(name = "account.mapping.create")
@Service
public class CreateAccountMappingService implements CreateAccountMappingUseCase {

    private final AccountMappingRepository repository;
    private final AccountingAccountValidationPort accountingAccountValidationPort;
    private final CatalogItemValidationPort catalogItemValidationPort;
    private final Clock clock;

    public CreateAccountMappingService(AccountMappingRepository repository,
            AccountingAccountValidationPort accountingAccountValidationPort,
            CatalogItemValidationPort catalogItemValidationPort, Clock clock) {
        this.repository = repository;
        this.accountingAccountValidationPort = accountingAccountValidationPort;
        this.catalogItemValidationPort = catalogItemValidationPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AccountMappingDto execute(CreateAccountMappingCommand command) {
        requirePostable("debitAccountCode", command.debitAccountCode());
        requirePostable("creditAccountCode", command.creditAccountCode());
        if (command.deferredAccountCode() != null)
            requirePostable("deferredAccountCode", command.deferredAccountCode());
        validateCatalogItem(command);
        AccountMapping mapping = AccountMapping.create(command.mappingKind(), command.mappingKey(),
                command.catalogItemId(), command.chargeType(), command.taxTreatment(),
                command.debitAccountCode(), command.creditAccountCode(),
                command.deferredAccountCode(), command.validFrom(), command.validTo(),
                LocalDateTime.now(clock));
        return AccountMappingDto.from(repository.save(mapping));
    }

    private void requirePostable(String field, String code) {
        if (accountingAccountValidationPort.existsPostableByCode(code))
            return;
        if (!accountingAccountValidationPort.existsByCode(code))
            throw new IllegalArgumentException(field + ": accounting account not found: " + code);
        throw new IllegalArgumentException(field + ": accounting account " + code
                + " does not accept postings; only a level 6 account does");
    }

    /**
     * {@code fk_account_mappings_item} es {@code RESTRICT}: sin esta comprobacion
     * un articulo inexistente saldria como error de integridad en vez de como «ese
     * articulo no existe». Que el articulo solo sea legitimo en las dos clases de
     * ingreso lo comprueba el dominio.
     */
    private void validateCatalogItem(CreateAccountMappingCommand command) {
        if (command.catalogItemId() == null)
            return;
        if (!catalogItemValidationPort.existsById(command.catalogItemId()))
            throw new IllegalArgumentException(
                    "Catalog item not found: " + command.catalogItemId());
    }
}
