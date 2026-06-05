package com.vetsoftware.app.promotion.application.port.out;

import com.vetsoftware.app.promotion.domain.ApplicationType;

public interface PromotionTargetQueryPort {
    boolean exists(ApplicationType type, Long itemId, Long companyId);
}
