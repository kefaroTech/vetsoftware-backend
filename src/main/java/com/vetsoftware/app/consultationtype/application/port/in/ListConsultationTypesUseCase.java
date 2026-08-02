package com.vetsoftware.app.consultationtype.application.port.in;

import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import java.util.List;

public interface ListConsultationTypesUseCase {
    List<ConsultationTypeDto> listAll();
}
