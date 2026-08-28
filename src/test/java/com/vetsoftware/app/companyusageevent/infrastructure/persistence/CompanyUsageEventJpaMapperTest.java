package com.vetsoftware.app.companyusageevent.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.companyusageevent.domain.CompanyUsageEvent;
import com.vetsoftware.app.companyusageevent.domain.UsageBranch;
import com.vetsoftware.app.companyusageevent.testsupport.CompanyUsageEventMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CompanyUsageEventJpaMapper")
class CompanyUsageEventJpaMapperTest {

    private final CompanyUsageEventJpaMapper mapper = new CompanyUsageEventJpaMapper();

    @Nested
    @DisplayName("toJpa")
    class ToJpa {

        @Test
        @DisplayName("reparte la referencia a la columna de su rama y deja las otras tres en null")
        void reparte_la_referencia_a_la_columna_de_su_rama() {
            CompanyUsageEvent evento = CompanyUsageEventMother.hechoSinCargo();

            CompanyUsageEventJpaEntity entity = mapper.toJpa(evento);

            assertThat(entity.getUsageAnimalId()).isEqualTo(CompanyUsageEventMother.ANIMAL_ID);
            assertThat(entity.getUsageOwnerId()).isNull();
            assertThat(entity.getUsageAppointmentId()).isNull();
            assertThat(entity.getUsageElectronicDocumentId()).isNull();
            assertThat(entity.getLimitDimensionCode()).isEqualTo("ANIMAL");
            assertThat(entity.getPeriodKey()).isEqualTo(CompanyUsageEventMother.PERIOD_KEY.value());
            assertThat(entity.isBillable()).isEqualTo(evento.isBillable());
            assertThat(entity.getChargeId()).isNull();
            assertThat(entity.getOccurredAt()).isEqualTo(evento.getOccurredAt());
            assertThat(entity.getCreatedDate()).isEqualTo(evento.getCreatedDate());
            assertThat(entity.getVersion()).isEqualTo(evento.getVersion());
        }

        @Test
        @DisplayName("una rama distinta escribe su propia columna, no la de ANIMAL")
        void una_rama_distinta_escribe_su_propia_columna() {
            CompanyUsageEvent evento = new CompanyUsageEvent(CompanyUsageEventMother.EVENT_ID,
                    CompanyUsageEventMother.COMPANY_ID, CompanyUsageEventMother.DIMENSION_ID,
                    UsageBranch.INVOICE, 555L, CompanyUsageEventMother.OCCURRED_AT,
                    CompanyUsageEventMother.PERIOD_KEY, true, null, CompanyUsageEventMother.CREADO,
                    0L);

            CompanyUsageEventJpaEntity entity = mapper.toJpa(evento);

            assertThat(entity.getUsageElectronicDocumentId()).isEqualTo(555L);
            assertThat(entity.getUsageOwnerId()).isNull();
            assertThat(entity.getUsageAnimalId()).isNull();
            assertThat(entity.getUsageAppointmentId()).isNull();
            assertThat(entity.getLimitDimensionCode()).isEqualTo("INVOICE");
        }

        @Test
        @DisplayName("la version viaja para que el save sea un update y no un insert")
        void la_version_viaja_para_que_el_save_sea_un_update() {
            CompanyUsageEvent evento = CompanyUsageEventMother.hechoConCargo();

            CompanyUsageEventJpaEntity entity = mapper.toJpa(evento);

            assertThat(entity.getId()).isEqualTo(CompanyUsageEventMother.EVENT_ID);
            assertThat(entity.getVersion()).isEqualTo(1L);
            assertThat(entity.getChargeId()).isEqualTo(CompanyUsageEventMother.CHARGE_ID);
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("reconstruye el hecho leyendo la rama desde el codigo de dimension")
        void reconstruye_el_hecho_leyendo_la_rama() {
            CompanyUsageEventJpaEntity entity = entidadAnimal();

            CompanyUsageEvent evento = mapper.toDomain(entity);

            assertThat(evento.getBranch()).isEqualTo(UsageBranch.ANIMAL);
            assertThat(evento.getUsageReferenceId()).isEqualTo(CompanyUsageEventMother.ANIMAL_ID);
            assertThat(evento.getPeriodKey()).isEqualTo(CompanyUsageEventMother.PERIOD_KEY);
            assertThat(evento.getVersion()).isEqualTo(entity.getVersion());
        }

        @Test
        @DisplayName("una fila corrupta sin referencia en su columna falla en voz alta")
        void una_fila_corrupta_sin_referencia_falla_en_voz_alta() {
            CompanyUsageEventJpaEntity entity = entidadAnimal();
            entity.setUsageAnimalId(null);

            assertThatThrownBy(() -> mapper.toDomain(entity))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("has axis 'ANIMAL' but no reference");
        }
    }

    private static CompanyUsageEventJpaEntity entidadAnimal() {
        CompanyUsageEventJpaEntity entity = new CompanyUsageEventJpaEntity();
        entity.setId(CompanyUsageEventMother.EVENT_ID);
        entity.setCompanyId(CompanyUsageEventMother.COMPANY_ID);
        entity.setLimitDimensionId(CompanyUsageEventMother.DIMENSION_ID);
        entity.setLimitDimensionCode("ANIMAL");
        entity.setUsageAnimalId(CompanyUsageEventMother.ANIMAL_ID);
        entity.setOccurredAt(CompanyUsageEventMother.OCCURRED_AT);
        entity.setPeriodKey(CompanyUsageEventMother.PERIOD_KEY.value());
        entity.setBillable(true);
        entity.setCreatedDate(CompanyUsageEventMother.CREADO);
        entity.setVersion(2L);
        return entity;
    }
}
