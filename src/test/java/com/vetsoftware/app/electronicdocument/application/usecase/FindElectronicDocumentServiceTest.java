package com.vetsoftware.app.electronicdocument.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentNotFoundException;
import com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Lectura de un documento por id. La consulta va SIEMPRE acotada a la empresa
 * del contexto: leer por id pelado seria un IDOR entre inquilinos, y la
 * respuesta ante un documento ajeno tiene que ser indistinguible de "no
 * existe".
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FindElectronicDocumentService — lectura por id")
class FindElectronicDocumentServiceTest {

    @Mock
    private ElectronicDocumentRepository repository;
    @InjectMocks
    private FindElectronicDocumentService service;

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("devuelve el documento de la empresa proyectado a DTO")
        void devuelve_el_documento_proyectado_a_dto() {
            when(repository.findByIdAndCompanyId(55L, 9L))
                    .thenReturn(Optional.of(ElectronicDocumentMother.facturaValidada(55L)));

            ElectronicDocumentDto dto = service.findById(55L, 9L);

            assertThat(dto.id()).isEqualTo(55L);
            assertThat(dto.companyId()).isEqualTo(9L);
            assertThat(dto.cufe()).isEqualTo(ElectronicDocumentMother.CUFE);
            assertThat(dto.dianStatus()).isEqualTo(DianStatus.VALIDADO);
        }

        @Test
        @DisplayName("proyecta lineas y pagos junto con la cabecera")
        void proyecta_lineas_y_pagos() {
            when(repository.findByIdAndCompanyId(55L, 9L))
                    .thenReturn(Optional.of(ElectronicDocumentMother.facturaValidada(55L)));

            ElectronicDocumentDto dto = service.findById(55L, 9L);

            assertThat(dto.lines()).hasSize(1);
            assertThat(dto.payments()).hasSize(1);
            assertThat(dto.taxTotalsByRate()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class Tenancy {

        @Test
        @DisplayName("un documento inexistente para la empresa lanza 'no encontrado'")
        void un_documento_inexistente_lanza_no_encontrado() {
            when(repository.findByIdAndCompanyId(55L, 9L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(55L, 9L))
                    .isInstanceOf(ElectronicDocumentNotFoundException.class)
                    .hasMessageContaining("Electronic document not found: 55");
        }

        @Test
        @DisplayName("consulta acotando por empresa y no hace ninguna escritura")
        void consulta_acotando_por_empresa_y_no_escribe() {
            when(repository.findByIdAndCompanyId(55L, 77L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(55L, 77L))
                    .isInstanceOf(ElectronicDocumentNotFoundException.class);

            verify(repository).findByIdAndCompanyId(55L, 77L);
            verifyNoMoreInteractions(repository);
        }
    }
}
