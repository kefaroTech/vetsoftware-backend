package com.vetsoftware.app.accountmapping.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.accountmapping.domain.AccountMapping;
import com.vetsoftware.app.accountmapping.domain.MappingKind;
import com.vetsoftware.app.accountmapping.testsupport.AccountMappingMother;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa: compila, persiste
 * y solo se ve en pantalla.
 */
@DisplayName("AccountMappingJpaMapper")
class AccountMappingJpaMapperTest {

    private final AccountMappingJpaMapper mapper = new AccountMappingJpaMapper();

    @Nested
    @DisplayName("toJpa - dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna, incluido el afinado completo")
        void copia_cada_campo_escalar() {
            AccountMapping mapping = AccountMappingMother.mapeoIngresoAbierto();

            AccountMappingJpaEntity entity = mapper.toJpa(mapping);

            assertThat(entity.getId()).isEqualTo(mapping.getId());
            assertThat(entity.getMappingKind()).isEqualTo(mapping.getMappingKind());
            assertThat(entity.getMappingKey()).isEqualTo(mapping.getMappingKey());
            assertThat(entity.getCatalogItemId()).isEqualTo(mapping.getCatalogItemId());
            assertThat(entity.getChargeType()).isEqualTo(mapping.getChargeType());
            assertThat(entity.getTaxTreatment()).isEqualTo(mapping.getTaxTreatment());
            assertThat(entity.getDebitAccountCode()).isEqualTo(mapping.getDebitAccountCode());
            assertThat(entity.getCreditAccountCode()).isEqualTo(mapping.getCreditAccountCode());
            assertThat(entity.getDeferredAccountCode()).isEqualTo(mapping.getDeferredAccountCode());
            assertThat(entity.getValidFrom()).isEqualTo(mapping.getValidFrom());
            assertThat(entity.getValidTo()).isEqualTo(mapping.getValidTo());
            assertThat(entity.getCreatedDate()).isEqualTo(mapping.getCreatedDate());
            assertThat(entity.isEnabled()).isEqualTo(mapping.isEnabled());
        }

        @Test
        @DisplayName("copia la version de un mapeo ya persistido: sin esto el merge seria INSERT")
        void copia_la_version_de_un_mapeo_persistido() {
            AccountMapping mapping = AccountMappingMother.mapeoBancoAbierto();

            AccountMappingJpaEntity entity = mapper.toJpa(mapping);

            assertThat(entity.getId()).isNotNull();
            assertThat(entity.getVersion()).isNotNull().isEqualTo(mapping.getVersion());
        }

        @Test
        @DisplayName("un mapeo nuevo sin id deja la version nula: decide Hibernate el INSERT")
        void mapeo_nuevo_sin_id_deja_la_version_nula() {
            AccountMapping nuevo = AccountMapping.create(MappingKind.BANK, "002", null, null, null,
                    "110501", "220501", null, LocalDate.of(2026, 1, 1), null,
                    LocalDateTime.of(2026, 1, 1, 9, 0));

            AccountMappingJpaEntity entity = mapper.toJpa(nuevo);

            assertThat(entity.getId()).isNull();
            assertThat(entity.getVersion()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain - entidad a dominio")
    class ToDomain {

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada, con version")
        void la_ida_y_vuelta_no_pierde_nada() {
            AccountMapping original = AccountMappingMother.mapeoIngresoAbierto();

            AccountMappingJpaEntity entity = mapper.toJpa(original);
            AccountMapping vuelta = mapper.toDomain(entity);

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }

        @Test
        @DisplayName("un mapeo cerrado conserva su fecha de fin en la vuelta")
        void un_mapeo_cerrado_conserva_su_fecha_de_fin() {
            AccountMapping cerrado = AccountMappingMother
                    .mapeoBancoCerrado(LocalDate.of(2026, 6, 1));

            AccountMappingJpaEntity entity = mapper.toJpa(cerrado);
            AccountMapping vuelta = mapper.toDomain(entity);

            assertThat(vuelta.getValidTo()).isEqualTo(LocalDate.of(2026, 6, 1));
            assertThat(vuelta.isOpen()).isFalse();
        }
    }
}
