package com.vetsoftware.app.configurator.application.port.in;

import com.vetsoftware.app.configurator.application.dto.ConfiguratorOptionDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Acotado por pregunta, no por empresa — y por eso también es
 * {@code hasRole('SYSTEM')}: acotar por una FK ajena no cuenta como filtro de
 * tenant (BE-29). Aquí da igual, porque no hay tenant que filtrar, pero la
 * regla se satisface por el rol y no por el {@code questionId}.
 */
public interface ListConfiguratorOptionsByQuestionUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    List<ConfiguratorOptionDto> listByQuestion(Long questionId);
}
