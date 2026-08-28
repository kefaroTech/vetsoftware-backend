package com.vetsoftware.app.companycontactchannel.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannel;
import com.vetsoftware.app.companycontactchannel.domain.ContactChannelType;
import com.vetsoftware.app.companycontactchannel.domain.ContactPurpose;
import com.vetsoftware.app.companycontactchannel.testsupport.CompanyContactChannelMother;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CompanyContactChannelJpaMapper — ida y vuelta dominio/JPA")
class CompanyContactChannelJpaMapperTest {

    private final CompanyContactChannelJpaMapper mapper = new CompanyContactChannelJpaMapper();

    @Nested
    @DisplayName("Ida")
    class Ida {

        @Test
        @DisplayName("lleva los doce campos a la entidad, la version incluida")
        void lleva_los_doce_campos_a_la_entidad() {
            CompanyContactChannel canal = CompanyContactChannelMother.revocadoQueFuePrimario(8500L);

            CompanyContactChannelJpaEntity entidad = mapper.toJpa(canal);

            assertThat(entidad.getId()).isEqualTo(8500L);
            assertThat(entidad.getCompanyId()).isEqualTo(CompanyContactChannelMother.COMPANY_ID);
            assertThat(entidad.getChannelType()).isEqualTo(ContactChannelType.EMAIL);
            assertThat(entidad.getAddress()).isEqualTo(CompanyContactChannelMother.CORREO);
            assertThat(entidad.getPurpose()).isEqualTo(ContactPurpose.BILLING);
            assertThat(entidad.getAuthorizedAt())
                    .isEqualTo(CompanyContactChannelMother.AUTORIZADO_EL);
            assertThat(entidad.getAuthorizationEvidence())
                    .isEqualTo(CompanyContactChannelMother.EVIDENCIA);
            assertThat(entidad.getRevokedAt()).isEqualTo(CompanyContactChannelMother.REVOCADO_EL);
            assertThat(entidad.getRevokedReason()).isEqualTo(CompanyContactChannelMother.MOTIVO);
            assertThat(entidad.isPrimary()).isTrue();
            assertThat(entidad.getCreatedDate()).isEqualTo(CompanyContactChannelMother.CREADO_EL);
            assertThat(entidad.getVersion()).isZero();
        }

        @Test
        @DisplayName("la fecha de autorizacion y la de creacion no se cruzan")
        void la_fecha_de_autorizacion_y_la_de_creacion_no_se_cruzan() {
            // Son distintas en el fixture justo para esto: con el mismo valor en las
            // dos, un mapper que las intercambiara pasaria todas las aserciones. Y
            // authorized_at es la columna que decide si un aviso estaba permitido.
            CompanyContactChannelJpaEntity entidad = mapper
                    .toJpa(CompanyContactChannelMother.vivo(8500L));

            assertThat(entidad.getAuthorizedAt())
                    .isEqualTo(CompanyContactChannelMother.AUTORIZADO_EL);
            assertThat(entidad.getCreatedDate()).isEqualTo(CompanyContactChannelMother.CREADO_EL);
            assertThat(entidad.getAuthorizedAt()).isNotEqualTo(entidad.getCreatedDate());
        }

        @Test
        @DisplayName("la version viaja aunque sea nula: un canal nuevo no la tiene")
        void la_version_viaja_aunque_sea_nula() {
            // Y al reves importa mas: si el mapper NO llevara la version de un canal ya
            // persistido, Hibernate lo trataria como nuevo y la revocacion, en vez de
            // cerrar la fila, insertaria una segunda autorizacion identica. El canal
            // seguiria vivo y la bitacora diria lo contrario de lo que paso.
            assertThat(mapper.toJpa(CompanyContactChannelMother.nuevo()).getVersion()).isNull();
            assertThat(mapper.toJpa(CompanyContactChannelMother.vivo(8500L)).getVersion()).isZero();
        }

        @Test
        @DisplayName("la entidad JPA no declara primary_marker: la calcula el motor")
        void la_entidad_no_declara_primary_marker() {
            // Mapearla haria que Hibernate intentara escribirla y MySQL rechazaria el
            // INSERT: una columna GENERATED ALWAYS ... STORED no admite valor. El fallo
            // aparece en la primera alta y el mensaje no menciona la anotacion.
            assertThat(Arrays.stream(CompanyContactChannelJpaEntity.class.getDeclaredFields())
                    .map(java.lang.reflect.Field::getName)).doesNotContain("primaryMarker");
        }
    }

    @Nested
    @DisplayName("Vuelta")
    class Vuelta {

        @Test
        @DisplayName("reconstruye el dominio con el mismo contenido")
        void reconstruye_el_dominio_con_el_mismo_contenido() {
            CompanyContactChannel original = CompanyContactChannelMother.primario(8500L);

            CompanyContactChannel vuelta = mapper.toDomain(mapper.toJpa(original));

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getCompanyId()).isEqualTo(original.getCompanyId());
            assertThat(vuelta.getChannelType()).isEqualTo(original.getChannelType());
            assertThat(vuelta.getAddress()).isEqualTo(original.getAddress());
            assertThat(vuelta.getPurpose()).isEqualTo(original.getPurpose());
            assertThat(vuelta.getAuthorizedAt()).isEqualTo(original.getAuthorizedAt());
            assertThat(vuelta.getAuthorizationEvidence())
                    .isEqualTo(original.getAuthorizationEvidence());
            assertThat(vuelta.isPrimary()).isTrue();
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
            assertThat(vuelta.getVersion()).isEqualTo(original.getVersion());
        }

        @Test
        @DisplayName("las dos columnas de la revocacion vuelven juntas")
        void las_dos_columnas_de_la_revocacion_vuelven_juntas() {
            // Si el mapper perdiera una de las dos, el dominio la rechazaria al
            // reconstruirse: la bicondicional del CHECK esta tambien en el constructor.
            CompanyContactChannel vuelta = mapper
                    .toDomain(mapper.toJpa(CompanyContactChannelMother.revocado(8500L)));

            assertThat(vuelta.getRevokedAt()).isEqualTo(CompanyContactChannelMother.REVOCADO_EL);
            assertThat(vuelta.getRevokedReason()).isEqualTo(CompanyContactChannelMother.MOTIVO);
            assertThat(vuelta.isRevoked()).isTrue();
        }
    }
}
