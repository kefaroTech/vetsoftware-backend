package com.vetsoftware.app.economicactivity.application.port.in;

import com.vetsoftware.app.economicactivity.application.dto.EconomicActivityDto;
import java.util.List;

public interface ListEconomicActivitiesUseCase {
    List<EconomicActivityDto> listAll();
}
