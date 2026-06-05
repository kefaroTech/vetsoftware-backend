package com.vetsoftware.app.promotion.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.promotion.application.command.CreatePromotionCommand;
import com.vetsoftware.app.promotion.application.dto.PromotionDto;
import com.vetsoftware.app.promotion.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.promotion.application.port.out.PromotionRepository;
import com.vetsoftware.app.promotion.application.port.out.PromotionTargetQueryPort;
import com.vetsoftware.app.promotion.domain.ApplicationType;
import com.vetsoftware.app.promotion.domain.CompanyRef;
import com.vetsoftware.app.promotion.domain.Promotion;
import com.vetsoftware.app.promotion.domain.PromotionStatus;
import com.vetsoftware.app.promotion.domain.PromotionType;
import com.vetsoftware.app.promotion.domain.ValueType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CreatePromotionServiceTest {

    private Promotion savedPromotion;

    private final PromotionRepository repository = new PromotionRepository() {
        @Override public Promotion save(Promotion p) {
            savedPromotion = new Promotion(1L, p.getName(), p.getPromotionType(), p.getApplicationType(),
                    p.getApplicationItem(), p.getValueType(), p.getValue(), p.getStartDate(), p.getEndDate(),
                    p.getPromotionStatus(), p.getCompany(), p.getCreatedDate(), p.isEnabled());
            return savedPromotion;
        }
        @Override public Optional<Promotion> findById(Long id) { return Optional.ofNullable(savedPromotion); }
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

    private CreatePromotionCommand command(BigDecimal value, ValueType valueType,
                                           LocalDateTime start, LocalDateTime end) {
        return new CreatePromotionCommand("Black Friday", PromotionType.DISCOUNT, ApplicationType.PRODUCT,
                7L, valueType, value, start, end, PromotionStatus.ACTIVE, 5L);
    }

    @Test
    void creates_promotion_loading_company_and_validating_target() {
        var service = new CreatePromotionService(repository,
                companyQueryPort(Optional.of(new CompanyRef(5L, "Acme", "900123"))),
                targetQueryPort(true));

        PromotionDto dto = service.execute(command(new BigDecimal("15.00"), ValueType.PERCENTAGE,
                LocalDateTime.parse("2026-06-01T00:00:00"), LocalDateTime.parse("2026-06-30T23:59:59")));

        assertEquals(1L, dto.id());
        assertEquals("Black Friday", dto.name());
        assertEquals(PromotionType.DISCOUNT, dto.promotionType());
        assertEquals(7L, dto.applicationItem());
        assertEquals(5L, dto.company().id());
        assertTrue(dto.enabled());
    }

    @Test
    void fails_when_company_not_found() {
        var service = new CreatePromotionService(repository,
                companyQueryPort(Optional.empty()), targetQueryPort(true));

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(command(new BigDecimal("15.00"), ValueType.PERCENTAGE,
                        LocalDateTime.parse("2026-06-01T00:00:00"), LocalDateTime.parse("2026-06-30T23:59:59"))));
    }

    @Test
    void fails_when_application_item_not_found() {
        var service = new CreatePromotionService(repository,
                companyQueryPort(Optional.of(new CompanyRef(5L, "Acme", "900123"))),
                targetQueryPort(false));

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(command(new BigDecimal("15.00"), ValueType.PERCENTAGE,
                        LocalDateTime.parse("2026-06-01T00:00:00"), LocalDateTime.parse("2026-06-30T23:59:59"))));
    }

    @Test
    void rejects_negative_value_in_domain() {
        var service = new CreatePromotionService(repository,
                companyQueryPort(Optional.of(new CompanyRef(5L, "Acme", "900123"))),
                targetQueryPort(true));

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(command(new BigDecimal("-1.00"), ValueType.VALUE,
                        LocalDateTime.parse("2026-06-01T00:00:00"), LocalDateTime.parse("2026-06-30T23:59:59"))));
    }

    @Test
    void rejects_end_date_before_start_date_in_domain() {
        var service = new CreatePromotionService(repository,
                companyQueryPort(Optional.of(new CompanyRef(5L, "Acme", "900123"))),
                targetQueryPort(true));

        assertThrows(IllegalArgumentException.class,
                () -> service.execute(command(new BigDecimal("10.00"), ValueType.VALUE,
                        LocalDateTime.parse("2026-06-30T00:00:00"), LocalDateTime.parse("2026-06-01T00:00:00"))));
    }
}
