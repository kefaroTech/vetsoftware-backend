package com.vetsoftware.app.servicechargeopenaccount.application.dto;

import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceRef;
import java.math.BigDecimal;

public record ServiceSummaryDto(Long id, String name, BigDecimal price) {
  public static ServiceSummaryDto from(ServiceRef service) {
    return new ServiceSummaryDto(service.id(), service.name(), service.price());
  }
}
