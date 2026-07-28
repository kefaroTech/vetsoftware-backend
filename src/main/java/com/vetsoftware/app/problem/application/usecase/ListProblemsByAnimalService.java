package com.vetsoftware.app.problem.application.usecase;

import com.vetsoftware.app.problem.application.dto.ProblemDto;
import com.vetsoftware.app.problem.application.port.in.ListProblemsByAnimalUseCase;
import com.vetsoftware.app.problem.application.port.out.ProblemRepository;
import com.vetsoftware.app.problem.application.query.ListProblemsByAnimalQuery;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "problem.list.by.animal")
@Service
public class ListProblemsByAnimalService implements ListProblemsByAnimalUseCase {
    private final ProblemRepository repository;

    public ListProblemsByAnimalService(ProblemRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ProblemDto> execute(ListProblemsByAnimalQuery query) {
        return repository.findByAnimalIdAndCompanyId(query.animalId(), query.companyId())
            .stream().map(ProblemDto::from).toList();
    }
}
