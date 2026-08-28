package com.vetsoftware.app.revenuerecognitionline.application.usecase;

import com.vetsoftware.app.revenuerecognitionline.application.dto.RevenueRecognitionLineDto;
import com.vetsoftware.app.revenuerecognitionline.application.port.in.FindRevenueRecognitionLineUseCase;
import com.vetsoftware.app.revenuerecognitionline.application.port.out.RevenueRecognitionLineRepository;
import com.vetsoftware.app.revenuerecognitionline.domain.RevenueRecognitionLineNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * Un renglon por su id.
 *
 * <p>
 * <strong>Llama a {@code findById} y no a {@code findByIdAndCompanyId}, y eso
 * es correcto aqui.</strong> {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} exime a
 * los servicios cuyos puertos estan <em>todos</em> cerrados a
 * {@code hasRole('SYSTEM')}: a un principal SYSTEM no se le puede pedir que
 * acote por su empresa porque no tiene ninguna, y exigirselo seria exigirle
 * pasar {@code null} y no encontrar nunca nada. La variante acotada existe en
 * el puerto de salida y la usa {@code listByCompany}; el dia que alguien abra
 * este metodo por permiso, tiene que pasar a llamarla.
 */
@Observed(name = "revenue.recognition.find")
@Service
public class FindRevenueRecognitionLineService implements FindRevenueRecognitionLineUseCase {

    private final RevenueRecognitionLineRepository repository;

    public FindRevenueRecognitionLineService(RevenueRecognitionLineRepository repository) {
        this.repository = repository;
    }

    @Override
    public RevenueRecognitionLineDto findById(Long id) {
        return repository.findById(id).map(RevenueRecognitionLineDto::from)
                .orElseThrow(() -> new RevenueRecognitionLineNotFoundException(id));
    }
}
