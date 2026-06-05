package com.vetsoftware.app.promotion.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.promotion.application.command.UpdatePromotionCommand;
import com.vetsoftware.app.promotion.application.dto.PromotionDto;
import com.vetsoftware.app.promotion.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.promotion.application.port.out.PromotionRepository;
import com.vetsoftware.app.promotion.application.port.out.PromotionTargetQueryPort;
import com.vetsoftware.app.promotion.domain.ApplicationType;
import com.vetsoftware.app.promotion.domain.CompanyRef;
import com.vetsoftware.app.promotion.domain.Promotion;
import com.vetsoftware.app.promotion.domain.PromotionNotFoundException;
import com.vetsoftware.app.promotion.domain.PromotionStatus;
import com.vetsoftware.app.promotion.domain.PromotionType;
import com.vetsoftware.app.promotion.domain.ValueType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdatePromotionServiceTest {

    private Promotion stored;
    private Promotion savedPromotion;

    private Promotion existing() {
        return new Promotion(1L, "Old", PromotionType.DISCOUNT, ApplicationType.PRODUCT, 7L,
                ValueType.PERCENTAGE, new BigDecimal("10.00"),
                LocalDateTime.parse("2026-06-01T00:00:00"), LocalDateTime.parse("2026-06-30T23:59:59"),
                PromotionStatus.ACTIVE, new CompanyRef(5L, "Acme", "900123"),
                LocalDateTime.parse("2026-05-01T00:00:00"), true);
    }

    private final PromotionRepository repository = new PromotionRepository() {
        @Override public Promotion save(Promotion p) { savedPromotion = p; return p; }
        @Override public Optional<Promotion> findById(Long id) { return Optional.ofNullable(stored); }
        @Override public List<Promotion> findAllByCompanyId(Long companyId) { return List.of(); }
        @Override public void delete(Long id) {}
        @Override public int reactivate(Long id) { return 0; }
    };

    private CompanyQueryPort companyQueryPort(Optional<CompanyRef> result) {
        return companyId -> result;
    }

    private PromotionTargetQueryPort targetQueryPort(boolean exists) {
        return (type, itemId, companyId) -> exists;
    }

    private UpdatePromotionCommand command() {
        return new UpdatePromotionCommand(1L, "New Name", PromotionType.SPECIAL_PRICE, ApplicationType.SERVICE,
                9L, ValueType.VALUE, new BigDecimal("50000.00"),
                LocalDateTime.parse("2026-07-01T00:00:00"), LocalDateTime.parse("2026-07-31T23:59:59"),
                PromotionStatus.INACTIVE, 5L);
    }

    @Test
    void updates_existing_promotion() {
        stored = existing();
        var service = new UpdatePromotionService(repository,
                companyQueryPort(Optional.of(new CompanyRef(5L, "Acme", "900123"))),
                targetQueryPort(true));

        PromotionDto dto = service.execute(command());

        assertEquals("New Name", dto.name());
        assertEquals(PromotionType.SPECIAL_PRICE, dto.promotionType());
        assertEquals(ApplicationType.SERVICE, dto.applicationType());
        assertEquals(9L, dto.applicationItem());
        assertEquals(PromotionStatus.INACTIVE, dto.promotionStatus());
        assertNotNull(savedPromotion);
    }

    @Test
    void fails_when_promotion_not_found() {
        stored = null;
        var service = new UpdatePromotionService(repository,
                companyQueryPort(Optional.of(new CompanyRef(5L, "Acme", "900123"))),
                targetQueryPort(true));

        assertThrows(PromotionNotFoundException.class, () -> service.execute(command()));
    }
}
