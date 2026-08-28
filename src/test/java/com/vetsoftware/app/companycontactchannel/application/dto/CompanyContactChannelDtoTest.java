package com.vetsoftware.app.companycontactchannel.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannel;
import com.vetsoftware.app.companycontactchannel.domain.ContactChannelType;
import com.vetsoftware.app.companycontactchannel.domain.ContactPurpose;
import com.vetsoftware.app.companycontactchannel.testsupport.CompanyContactChannelMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CompanyContactChannelDto — proyeccion del canal")
class CompanyContactChannelDtoTest {

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("copia los once campos del canal vivo")
        void copia_los_once_campos_del_canal_vivo() {
            CompanyContactChannel canal = CompanyContactChannelMother.primario(8500L);

            CompanyContactChannelDto dto = CompanyContactChannelDto.from(canal);

            assertThat(dto.id()).isEqualTo(8500L);
            assertThat(dto.companyId()).isEqualTo(CompanyContactChannelMother.COMPANY_ID);
            assertThat(dto.channelType()).isEqualTo(ContactChannelType.EMAIL);
            assertThat(dto.address()).isEqualTo(CompanyContactChannelMother.CORREO);
            assertThat(dto.purpose()).isEqualTo(ContactPurpose.BILLING);
            assertThat(dto.authorizedAt()).isEqualTo(CompanyContactChannelMother.AUTORIZADO_EL);
            assertThat(dto.authorizationEvidence())
                    .isEqualTo(CompanyContactChannelMother.EVIDENCIA);
            assertThat(dto.revokedAt()).isNull();
            assertThat(dto.revokedReason()).isNull();
            assertThat(dto.primary()).isTrue();
            assertThat(dto.createdDate()).isEqualTo(CompanyContactChannelMother.CREADO_EL);
        }

        @Test
        @DisplayName("publica la revocacion en vez de esconderla")
        void publica_la_revocacion() {
            // Filtrar los revocados aqui seria borrar la prueba en la capa de arriba
            // despues de haberse negado a borrarla en la base. El canal cerrado sale con
            // su fecha y su motivo, que es lo que responde si aquel aviso estaba
            // permitido.
            CompanyContactChannelDto dto = CompanyContactChannelDto
                    .from(CompanyContactChannelMother.revocado(8500L));

            assertThat(dto.revokedAt()).isEqualTo(CompanyContactChannelMother.REVOCADO_EL);
            assertThat(dto.revokedReason()).isEqualTo(CompanyContactChannelMother.MOTIVO);
        }

        @Test
        @DisplayName("no publica la version: es la barandilla del bloqueo, no un dato")
        void no_publica_la_version() {
            // Si algun dia aparece aqui, alguien la devolvera desde el cliente y habra
            // dos controles de concurrencia: el de Hibernate y uno inventado.
            assertThat(CompanyContactChannelDto.class.getRecordComponents())
                    .extracting(java.lang.reflect.RecordComponent::getName)
                    .doesNotContain("version");
        }
    }
}
