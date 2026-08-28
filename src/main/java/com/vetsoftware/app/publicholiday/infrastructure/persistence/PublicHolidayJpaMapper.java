package com.vetsoftware.app.publicholiday.infrastructure.persistence;

import com.vetsoftware.app.publicholiday.domain.PublicHoliday;
import org.springframework.stereotype.Component;

@Component
public class PublicHolidayJpaMapper {

    public PublicHolidayJpaEntity toJpa(PublicHoliday holiday) {
        PublicHolidayJpaEntity entity = new PublicHolidayJpaEntity();
        entity.setId(holiday.getId());
        entity.setHolidayDate(holiday.getHolidayDate());
        entity.setName(holiday.getName());
        entity.setNominalDate(holiday.getNominalDate());
        entity.setMoved(holiday.isMoved());
        entity.setLegalReference(holiday.getLegalReference());
        entity.setCreatedDate(holiday.getCreatedDate());
        entity.setEnabled(holiday.isEnabled());
        return entity;
    }

    public PublicHoliday toDomain(PublicHolidayJpaEntity entity) {
        return new PublicHoliday(entity.getId(), entity.getHolidayDate(), entity.getName(),
                entity.getNominalDate(), entity.isMoved(), entity.getLegalReference(),
                entity.getCreatedDate(), entity.isEnabled());
    }
}
