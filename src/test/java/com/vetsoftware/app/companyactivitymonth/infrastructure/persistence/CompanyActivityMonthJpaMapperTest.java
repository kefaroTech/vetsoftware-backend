package com.vetsoftware.app.companyactivitymonth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.companyactivitymonth.domain.ActivityPeriodKey;
import com.vetsoftware.app.companyactivitymonth.domain.CommercialState;
import com.vetsoftware.app.companyactivitymonth.domain.CompanyActivityMonth;
import com.vetsoftware.app.companyactivitymonth.testsupport.CompanyActivityMonthMother;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CompanyActivityMonthJpaMapper")
class CompanyActivityMonthJpaMapperTest {

    private final CompanyActivityMonthJpaMapper mapper = new CompanyActivityMonthJpaMapper();

    @Nested
    @DisplayName("toJpa")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo del dominio a la entidad, incluida la version")
        void copia_cada_campo_del_dominio_a_la_entidad() {
            CompanyActivityMonth mes = CompanyActivityMonthMother.pagada();

            CompanyActivityMonthJpaEntity entity = mapper.toJpa(mes);

            assertThat(entity.getId()).isEqualTo(mes.getId());
            assertThat(entity.getCompanyId()).isEqualTo(mes.getCompanyId());
            assertThat(entity.getPeriodKey()).isEqualTo(mes.getPeriodKey().value());
            assertThat(entity.getCommercialState()).isEqualTo(mes.getCommercialState());
            assertThat(entity.getActiveDays()).isEqualTo(mes.getActiveDays());
            assertThat(entity.getActiveUsers()).isEqualTo(mes.getActiveUsers());
            assertThat(entity.getRecordsCreated()).isEqualTo(mes.getRecordsCreated());
            assertThat(entity.getMrrSnapshot()).isEqualByComparingTo(mes.getMrrSnapshot());
            assertThat(entity.getCreatedDate()).isEqualTo(mes.getCreatedDate());
            assertThat(entity.getVersion()).isEqualTo(mes.getVersion());
        }

        @Test
        @DisplayName("la version viaja para que el recalculo sea un update y no un insert")
        void la_version_viaja_para_que_el_recalculo_sea_un_update() {
            CompanyActivityMonth mes = new CompanyActivityMonth(CompanyActivityMonthMother.MONTH_ID,
                    CompanyActivityMonthMother.COMPANY_ID, CompanyActivityMonthMother.MARZO_2026,
                    CommercialState.PAID, 20, 5, 340, BigDecimal.TEN,
                    CompanyActivityMonthMother.CREADO, 7L);

            CompanyActivityMonthJpaEntity entity = mapper.toJpa(mes);

            assertThat(entity.getId()).isEqualTo(CompanyActivityMonthMother.MONTH_ID);
            assertThat(entity.getVersion()).isEqualTo(7L);
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("reconstruye el dominio envolviendo el periodo en el value object")
        void reconstruye_el_dominio_envolviendo_el_periodo() {
            CompanyActivityMonthJpaEntity entity = entidad();

            CompanyActivityMonth mes = mapper.toDomain(entity);

            assertThat(mes.getId()).isEqualTo(entity.getId());
            assertThat(mes.getCompanyId()).isEqualTo(entity.getCompanyId());
            assertThat(mes.getPeriodKey()).isEqualTo(new ActivityPeriodKey(entity.getPeriodKey()));
            assertThat(mes.getCommercialState()).isEqualTo(entity.getCommercialState());
            assertThat(mes.getActiveDays()).isEqualTo(entity.getActiveDays());
            assertThat(mes.getActiveUsers()).isEqualTo(entity.getActiveUsers());
            assertThat(mes.getRecordsCreated()).isEqualTo(entity.getRecordsCreated());
            assertThat(mes.getMrrSnapshot()).isEqualByComparingTo(entity.getMrrSnapshot());
            assertThat(mes.getCreatedDate()).isEqualTo(entity.getCreatedDate());
            assertThat(mes.getVersion()).isEqualTo(entity.getVersion());
        }
    }

    private static CompanyActivityMonthJpaEntity entidad() {
        CompanyActivityMonthJpaEntity entity = new CompanyActivityMonthJpaEntity();
        entity.setId(CompanyActivityMonthMother.MONTH_ID);
        entity.setCompanyId(CompanyActivityMonthMother.COMPANY_ID);
        entity.setPeriodKey("2026-03");
        entity.setCommercialState(CommercialState.PAID);
        entity.setActiveDays(20);
        entity.setActiveUsers(5);
        entity.setRecordsCreated(340);
        entity.setMrrSnapshot(new BigDecimal("199990.00"));
        entity.setCreatedDate(CompanyActivityMonthMother.CREADO);
        entity.setVersion(4L);
        return entity;
    }
}
