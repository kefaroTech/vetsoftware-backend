package com.vetsoftware.app.animal.infrastructure.web;

import com.vetsoftware.app.animal.application.command.CreateWeightRecordCommand;
import com.vetsoftware.app.animal.application.dto.WeightRecordDto;
import com.vetsoftware.app.animal.application.port.in.CreateWeightRecordUseCase;
import com.vetsoftware.app.animal.application.port.in.DeleteWeightRecordUseCase;
import com.vetsoftware.app.animal.application.port.in.FindLatestWeightRecordUseCase;
import com.vetsoftware.app.animal.application.port.in.ListWeightRecordsByAnimalUseCase;
import com.vetsoftware.app.animal.infrastructure.web.request.CreateWeightRecordRequest;
import com.vetsoftware.app.animal.infrastructure.web.response.WeightRecordResponse;
import com.vetsoftware.app.auth.infrastructure.security.Authz;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Serie temporal del peso del animal. El peso "actual" del animal se deriva del
 * último registro.
 */
@RestController
@RequestMapping("/animals/{animalId}/weight-records")
public class WeightRecordController {
    private final CreateWeightRecordUseCase createUseCase;
    private final ListWeightRecordsByAnimalUseCase listUseCase;
    private final FindLatestWeightRecordUseCase findLatestUseCase;
    private final DeleteWeightRecordUseCase deleteUseCase;
    private final Authz authz;

    public WeightRecordController(CreateWeightRecordUseCase createUseCase,
            ListWeightRecordsByAnimalUseCase listUseCase,
            FindLatestWeightRecordUseCase findLatestUseCase,
            DeleteWeightRecordUseCase deleteUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.listUseCase = listUseCase;
        this.findLatestUseCase = findLatestUseCase;
        this.deleteUseCase = deleteUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WeightRecordResponse create(@PathVariable Long animalId,
            @Valid @RequestBody CreateWeightRecordRequest request) {
        return toResponse(createUseCase
                .execute(new CreateWeightRecordCommand(animalId, request.value(), request.unit(),
                        request.measuredAt(), request.note(), authz.currentCompanyId())));
    }

    @GetMapping
    public List<WeightRecordResponse> listByAnimal(@PathVariable Long animalId) {
        return listUseCase.listByAnimal(animalId, authz.currentCompanyId()).stream()
                .map(this::toResponse).toList();
    }

    @GetMapping("/latest")
    public WeightRecordResponse findLatest(@PathVariable Long animalId) {
        return toResponse(findLatestUseCase.findLatest(animalId, authz.currentCompanyId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long animalId, @PathVariable Long id) {
        deleteUseCase.execute(id, animalId, authz.currentCompanyId());
    }

    private WeightRecordResponse toResponse(WeightRecordDto dto) {
        return new WeightRecordResponse(dto.id(), dto.animalId(), dto.animalName(),
                dto.animalCode(), dto.value(), dto.unit(), dto.measuredAt(), dto.source(),
                dto.sourceId(), dto.note(), dto.createdDate());
    }
}
