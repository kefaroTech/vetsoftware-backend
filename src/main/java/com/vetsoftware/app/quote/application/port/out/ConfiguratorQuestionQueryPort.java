package com.vetsoftware.app.quote.application.port.out;

import com.vetsoftware.app.quote.domain.ConfiguratorQuestionRef;
import java.util.Optional;

/**
 * Lee la pregunta del configurador solo para copiar su codigo en la respuesta.
 */
public interface ConfiguratorQuestionQueryPort {
    Optional<ConfiguratorQuestionRef> findById(Long questionId);
}
