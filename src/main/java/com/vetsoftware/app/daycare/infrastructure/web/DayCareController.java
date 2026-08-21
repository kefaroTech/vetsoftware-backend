package com.vetsoftware.app.daycare.infrastructure.web;

import com.vetsoftware.app.daycare.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.daycare.application.dto.CompanySummaryDto;
import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import com.vetsoftware.app.daycare.application.port.in.ListDayCaresUseCase;
import com.vetsoftware.app.daycare.infrastructure.web.response.AnimalSummary;
import com.vetsoftware.app.daycare.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.daycare.infrastructure.web.response.DayCareResponse;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/daycares")
public class DayCareController {
    private final ListDayCaresUseCase listUseCase;

    public DayCareController(ListDayCaresUseCase listUseCase) {
        this.listUseCase = listUseCase;
    }

    @GetMapping
    public List<DayCareResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    private DayCareResponse toResponse(DayCareDto dto) {
        AnimalSummaryDto a = dto.animal();
        CompanySummaryDto c = dto.company();
        return new DayCareResponse(dto.id(), dto.date(), dto.startDate(), dto.endDate(), dto.type(),
                dto.objects(), dto.observations(), new AnimalSummary(a.id(), a.name(), a.code()),
                new CompanySummary(c.id(), c.name(), c.identifier()), dto.createdDate(),
                dto.enabled());
    }
}
