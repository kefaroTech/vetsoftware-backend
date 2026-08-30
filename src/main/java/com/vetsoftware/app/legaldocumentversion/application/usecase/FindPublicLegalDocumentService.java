package com.vetsoftware.app.legaldocumentversion.application.usecase;

import com.vetsoftware.app.legaldocumentversion.application.dto.LegalDocumentVersionDto;
import com.vetsoftware.app.legaldocumentversion.application.port.in.FindPublicLegalDocumentUseCase;
import com.vetsoftware.app.legaldocumentversion.application.port.out.LegalDocumentVersionRepository;
import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentVersionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * Clase propia que implementa <strong>solo</strong> el puerto anonimo.
 *
 * <p>
 * No comparte servicio con ningun caso de uso autenticado a proposito: un
 * servicio que implementa dos puertos con gates distintos acaba teniendo el
 * gate mas debil de los dos el dia que alguien reordena los metodos.
 *
 * <p>
 * Y no recibe {@code companyId} <strong>ni lo consulta</strong>. La rodaja
 * aguanta el puerto anonimo por construccion:
 * {@code LegalDocumentVersionJpaEntity} no tiene columna de empresa ni ninguna
 * asociacion que alcance {@code CompanyJpaEntity}, y
 * {@code LegalDocumentVersionRepository} no declara ni un metodo que reciba
 * {@code companyId} -que es la señal con la que
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} decide-. Ademas devuelve una fila,
 * no un listado.
 */
@Observed(name = "legaldocument.public.current")
@Service
public class FindPublicLegalDocumentService implements FindPublicLegalDocumentUseCase {

    private final LegalDocumentVersionRepository repository;

    public FindPublicLegalDocumentService(LegalDocumentVersionRepository repository) {
        this.repository = repository;
    }

    @Override
    public LegalDocumentVersionDto findCurrentByCode(String code) {
        return repository.findCurrentByCode(code).map(LegalDocumentVersionDto::from)
                .orElseThrow(() -> new LegalDocumentVersionNotFoundException(code));
    }
}
