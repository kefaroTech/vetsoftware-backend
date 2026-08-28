package com.vetsoftware.app.platformtaxprofile.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.economicactivity.infrastructure.persistence.EconomicActivityJpaEntity;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformDocumentType;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxProfile;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxRegime;
import com.vetsoftware.app.platformtaxprofile.testsupport.PlatformTaxProfileMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PlatformTaxProfileJpaMapper")
class PlatformTaxProfileJpaMapperTest {

    private final PlatformTaxProfileJpaMapper mapper = new PlatformTaxProfileJpaMapper();

    @Nested
    @DisplayName("toJpa")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo del dominio a la entidad, incluida la actividad")
        void copia_cada_campo_del_dominio_a_la_entidad() {
            PlatformTaxProfile perfil = PlatformTaxProfileMother.vigente();
            EconomicActivityJpaEntity actividad = mock(EconomicActivityJpaEntity.class);

            PlatformTaxProfileJpaEntity entity = mapper.toJpa(perfil, actividad);

            assertThat(entity.getId()).isEqualTo(perfil.getId());
            assertThat(entity.getDocumentType()).isEqualTo(perfil.getDocumentType());
            assertThat(entity.getDocumentId()).isEqualTo(perfil.getDocumentId());
            assertThat(entity.getVerificationDigit()).isEqualTo(perfil.getVerificationDigit());
            assertThat(entity.getLegalName()).isEqualTo(perfil.getLegalName());
            assertThat(entity.getTaxRegime()).isEqualTo(perfil.getTaxRegime());
            assertThat(entity.getFiscalEmail()).isEqualTo(perfil.getFiscalEmail());
            assertThat(entity.getCommercialName()).isEqualTo(perfil.getCommercialName());
            assertThat(entity.getEconomicActivity()).isSameAs(actividad);
            assertThat(entity.isSelfWithholder()).isEqualTo(perfil.isSelfWithholder());
            assertThat(entity.getValidFrom()).isEqualTo(perfil.getValidFrom());
            assertThat(entity.getValidTo()).isEqualTo(perfil.getValidTo());
            assertThat(entity.getCreatedDate()).isEqualTo(perfil.getCreatedDate());
            assertThat(entity.getVersion()).isEqualTo(perfil.getVersion());
        }

        @Test
        @DisplayName("la version viaja para que el save sea un update y no un insert")
        void la_version_viaja_para_que_el_save_sea_un_update() {
            PlatformTaxProfile perfil = PlatformTaxProfileMother
                    .cerrada(PlatformTaxProfileMother.VALID_FROM.plusDays(10));

            PlatformTaxProfileJpaEntity entity = mapper.toJpa(perfil, null);

            assertThat(entity.getId()).isEqualTo(PlatformTaxProfileMother.PROFILE_ID);
            assertThat(entity.getVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("la actividad economica es opcional: null entra y null sale")
        void la_actividad_economica_es_opcional() {
            PlatformTaxProfile perfil = PlatformTaxProfileMother.vigenteSinActividad();

            PlatformTaxProfileJpaEntity entity = mapper.toJpa(perfil, null);

            assertThat(entity.getEconomicActivity()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain de lectura, con el @EntityGraph ya hidratado")
    class ToDomainDeLectura {

        @Test
        @DisplayName("reconstruye el dominio leyendo la actividad de la asociacion")
        void reconstruye_el_dominio_leyendo_la_actividad_de_la_asociacion() {
            PlatformTaxProfileJpaEntity entity = entidad();
            EconomicActivityJpaEntity actividad = mock(EconomicActivityJpaEntity.class);
            when(actividad.getId()).thenReturn(PlatformTaxProfileMother.ACTIVIDAD.id());
            when(actividad.getCode()).thenReturn(PlatformTaxProfileMother.ACTIVIDAD.code());
            when(actividad.getName()).thenReturn(PlatformTaxProfileMother.ACTIVIDAD.name());
            entity.setEconomicActivity(actividad);

            PlatformTaxProfile perfil = mapper.toDomain(entity);

            assertThat(perfil.getEconomicActivity()).isEqualTo(PlatformTaxProfileMother.ACTIVIDAD);
        }

        @Test
        @DisplayName("sin actividad asociada, null entra y null sale")
        void sin_actividad_asociada_null_entra_y_null_sale() {
            PlatformTaxProfileJpaEntity entity = entidad();

            PlatformTaxProfile perfil = mapper.toDomain(entity);

            assertThat(perfil.getEconomicActivity()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain de escritura, reusando el ref ya en mano")
    class ToDomainDeEscritura {

        @Test
        @DisplayName("usa el ref recibido y no dispara la hidratacion del proxy")
        void usa_el_ref_recibido_sin_tocar_el_proxy() {
            PlatformTaxProfileJpaEntity entity = entidad();
            EconomicActivityJpaEntity proxySinHidratar = mock(EconomicActivityJpaEntity.class);
            entity.setEconomicActivity(proxySinHidratar);

            PlatformTaxProfile perfil = mapper.toDomain(entity, PlatformTaxProfileMother.ACTIVIDAD);

            assertThat(perfil.getEconomicActivity()).isEqualTo(PlatformTaxProfileMother.ACTIVIDAD);
            verifyNoInteractions(proxySinHidratar);
        }

        @Test
        @DisplayName("la version vuelve intacta")
        void la_version_vuelve_intacta() {
            PlatformTaxProfileJpaEntity entity = entidad();

            PlatformTaxProfile perfil = mapper.toDomain(entity, null);

            assertThat(perfil.getVersion()).isEqualTo(entity.getVersion());
        }
    }

    private static PlatformTaxProfileJpaEntity entidad() {
        PlatformTaxProfileJpaEntity entity = new PlatformTaxProfileJpaEntity();
        entity.setId(PlatformTaxProfileMother.PROFILE_ID);
        entity.setDocumentType(PlatformDocumentType.NIT);
        entity.setDocumentId("900123456");
        entity.setVerificationDigit("7");
        entity.setLegalName("VetSoftware SAS");
        entity.setTaxRegime(PlatformTaxRegime.RESPONSABLE_IVA);
        entity.setFiscalEmail("facturacion@vetsoftware.com");
        entity.setCommercialName("VetSoftware");
        entity.setSelfWithholder(true);
        entity.setValidFrom(PlatformTaxProfileMother.VALID_FROM);
        entity.setCreatedDate(PlatformTaxProfileMother.CREADO);
        entity.setVersion(3L);
        return entity;
    }
}
