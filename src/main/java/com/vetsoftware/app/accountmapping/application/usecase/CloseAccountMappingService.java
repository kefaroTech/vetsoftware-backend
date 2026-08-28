package com.vetsoftware.app.accountmapping.application.usecase;

import com.vetsoftware.app.accountmapping.application.command.CloseAccountMappingCommand;
import com.vetsoftware.app.accountmapping.application.dto.AccountMappingDto;
import com.vetsoftware.app.accountmapping.application.port.in.CloseAccountMappingUseCase;
import com.vetsoftware.app.accountmapping.application.port.out.AccountMappingRepository;
import com.vetsoftware.app.accountmapping.domain.AccountMapping;
import com.vetsoftware.app.accountmapping.domain.AccountMappingNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pone fecha de fin a un mapeo vigente.
 *
 * <p>
 * <strong>Leer, cerrar y guardar es un ciclo con bloqueo optimista, y de eso
 * depende que no se pierda un cierre.</strong> El dominio conserva la version
 * al construir la instancia cerrada y el {@code save} vuelve con ella en el
 * {@code WHERE}. Que el mapeo no estuviera ya cerrado lo decide el dominio: la
 * base <b>no lo cuida</b>, porque {@code current_mapping_marker} vale
 * {@code NULL} en un mapeo cerrado y la unicidad sobre columna nula no
 * restringe nada.
 */
@Observed(name = "account.mapping.close")
@Service
public class CloseAccountMappingService implements CloseAccountMappingUseCase {

    private final AccountMappingRepository repository;

    public CloseAccountMappingService(AccountMappingRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public AccountMappingDto execute(CloseAccountMappingCommand command) {
        AccountMapping mapping = repository.findById(command.id())
                .orElseThrow(() -> new AccountMappingNotFoundException(command.id()));
        return AccountMappingDto.from(repository.save(mapping.close(command.validTo())));
    }
}
