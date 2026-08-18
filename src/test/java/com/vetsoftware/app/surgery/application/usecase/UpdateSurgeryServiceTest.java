package com.vetsoftware.app.surgery.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.surgery.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.surgery.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.surgery.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import com.vetsoftware.app.surgery.application.port.out.SurgeryTypeQueryPort;
import com.vetsoftware.app.surgery.domain.Surgery;
import com.vetsoftware.app.surgery.domain.SurgeryNotFoundException;
import com.vetsoftware.app.surgery.testsupport.SurgeryMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateSurgeryService")
class UpdateSurgeryServiceTest {

    @Mock
    private SurgeryRepository repository;
    @Mock
    private SurgeryTypeQueryPort surgeryTypeQueryPort;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private ConsultationQueryPort consultationQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private UpdateSurgeryService service;

    @Captor
    private ArgumentCaptor<Surgery> surgeryCaptor;

    private void todasLasReferenciasDelComandoExisten() {
        when(surgeryTypeQueryPort.findAvailableByIdAndCompanyId(SurgeryMother.CASTRACION.id(),
                SurgeryMother.COMPANY_ID)).thenReturn(Optional.of(SurgeryMother.CASTRACION));
        when(animalQueryPort.findByIdAndCompanyId(SurgeryMother.MICHI.id(),
                SurgeryMother.COMPANY_ID)).thenReturn(Optional.of(SurgeryMother.MICHI));
        when(consultationQueryPort.findByIdAndCompanyId(SurgeryMother.OTRA_CONSULTA.id(),
                SurgeryMother.COMPANY_ID)).thenReturn(Optional.of(SurgeryMother.OTRA_CONSULTA));
        when(companyQueryPort.findById(SurgeryMother.COMPANY_ID))
                .thenReturn(Optional.of(SurgeryMother.CLINICA));
    }

    @Test
    @DisplayName("busca la cirugia acotada por la empresa del comando, nunca por id a secas")
    void busca_la_cirugia_acotada_por_la_empresa_del_comando() {
        when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID, SurgeryMother.COMPANY_ID))
                .thenReturn(Optional.of(SurgeryMother.cirugiaValida()));
        todasLasReferenciasDelComandoExisten();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(SurgeryMother.comandoActualizar());

        verify(repository).findByIdAndCompanyId(SurgeryMother.SURGERY_ID, SurgeryMother.COMPANY_ID);
        verify(repository, never()).findById(any());
    }

    @Test
    @DisplayName("reemplaza cada referencia por la resuelta en los puertos")
    void reemplaza_cada_referencia_por_la_resuelta() {
        when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID, SurgeryMother.COMPANY_ID))
                .thenReturn(Optional.of(SurgeryMother.cirugiaValida()));
        todasLasReferenciasDelComandoExisten();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(SurgeryMother.comandoActualizar());

        verify(repository).save(surgeryCaptor.capture());
        Surgery actualizada = surgeryCaptor.getValue();
        assertThat(actualizada.getSurgeryType()).isEqualTo(SurgeryMother.CASTRACION);
        assertThat(actualizada.getAnimal()).isEqualTo(SurgeryMother.MICHI);
        assertThat(actualizada.getConsultation()).isEqualTo(SurgeryMother.OTRA_CONSULTA);
        assertThat(actualizada.getCompany()).isEqualTo(SurgeryMother.CLINICA);
        assertThat(actualizada.getDescription()).isEqualTo("Castracion electiva");
    }

    @Test
    @DisplayName("una consulta ausente en el comando no consulta el puerto de consulta")
    void consulta_ausente_no_consulta_el_puerto() {
        when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID, SurgeryMother.COMPANY_ID))
                .thenReturn(Optional.of(SurgeryMother.cirugiaValida()));
        when(surgeryTypeQueryPort.findAvailableByIdAndCompanyId(SurgeryMother.CASTRACION.id(),
                SurgeryMother.COMPANY_ID)).thenReturn(Optional.of(SurgeryMother.CASTRACION));
        when(animalQueryPort.findByIdAndCompanyId(SurgeryMother.MICHI.id(),
                SurgeryMother.COMPANY_ID)).thenReturn(Optional.of(SurgeryMother.MICHI));
        when(companyQueryPort.findById(SurgeryMother.COMPANY_ID))
                .thenReturn(Optional.of(SurgeryMother.CLINICA));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        com.vetsoftware.app.surgery.application.command.UpdateSurgeryCommand comandoSinConsulta = new com.vetsoftware.app.surgery.application.command.UpdateSurgeryCommand(
                SurgeryMother.SURGERY_ID, SurgeryMother.FECHA.plusDays(5),
                SurgeryMother.CASTRACION.id(), "Castracion electiva", "Anestesia local",
                "Observaciones nuevas", "Sangrado leve", SurgeryMother.MICHI.id(), null,
                SurgeryMother.COMPANY_ID);

        service.execute(comandoSinConsulta);

        verifyNoInteractions(consultationQueryPort);
    }

    @Test
    @DisplayName("cirugia inexistente no toca ningun otro puerto")
    void cirugia_inexistente_no_toca_ningun_otro_puerto() {
        when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID, SurgeryMother.COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(SurgeryMother.comandoActualizar()))
                .isInstanceOf(SurgeryNotFoundException.class)
                .hasMessageContaining("Surgery not found: " + SurgeryMother.SURGERY_ID);

        verifyNoInteractions(surgeryTypeQueryPort, animalQueryPort, consultationQueryPort,
                companyQueryPort);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("tipo de cirugia inexistente no consulta animal, consulta ni empresa, y no guarda")
    void tipo_de_cirugia_inexistente() {
        when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID, SurgeryMother.COMPANY_ID))
                .thenReturn(Optional.of(SurgeryMother.cirugiaValida()));
        when(surgeryTypeQueryPort.findAvailableByIdAndCompanyId(SurgeryMother.CASTRACION.id(),
                SurgeryMother.COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(SurgeryMother.comandoActualizar()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SurgeryType not found: " + SurgeryMother.CASTRACION.id());

        verifyNoInteractions(animalQueryPort, consultationQueryPort, companyQueryPort);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("animal inexistente no consulta la consulta ni la empresa, y no guarda")
    void animal_inexistente() {
        when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID, SurgeryMother.COMPANY_ID))
                .thenReturn(Optional.of(SurgeryMother.cirugiaValida()));
        when(surgeryTypeQueryPort.findAvailableByIdAndCompanyId(SurgeryMother.CASTRACION.id(),
                SurgeryMother.COMPANY_ID)).thenReturn(Optional.of(SurgeryMother.CASTRACION));
        when(animalQueryPort.findByIdAndCompanyId(SurgeryMother.MICHI.id(),
                SurgeryMother.COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(SurgeryMother.comandoActualizar()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Animal not found: " + SurgeryMother.MICHI.id());

        verifyNoInteractions(consultationQueryPort, companyQueryPort);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("consulta inexistente no consulta la empresa, y no guarda")
    void consulta_inexistente() {
        when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID, SurgeryMother.COMPANY_ID))
                .thenReturn(Optional.of(SurgeryMother.cirugiaValida()));
        when(surgeryTypeQueryPort.findAvailableByIdAndCompanyId(SurgeryMother.CASTRACION.id(),
                SurgeryMother.COMPANY_ID)).thenReturn(Optional.of(SurgeryMother.CASTRACION));
        when(animalQueryPort.findByIdAndCompanyId(SurgeryMother.MICHI.id(),
                SurgeryMother.COMPANY_ID)).thenReturn(Optional.of(SurgeryMother.MICHI));
        when(consultationQueryPort.findByIdAndCompanyId(SurgeryMother.OTRA_CONSULTA.id(),
                SurgeryMother.COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(SurgeryMother.comandoActualizar()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                        "Consultation not found: " + SurgeryMother.OTRA_CONSULTA.id());

        verifyNoInteractions(companyQueryPort);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("empresa inexistente no guarda")
    void empresa_inexistente() {
        when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID, SurgeryMother.COMPANY_ID))
                .thenReturn(Optional.of(SurgeryMother.cirugiaValida()));
        todasLasReferenciasDelComandoExisten();
        when(companyQueryPort.findById(SurgeryMother.COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(SurgeryMother.comandoActualizar()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company not found: " + SurgeryMother.COMPANY_ID);

        verify(repository, never()).save(any());
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        /**
         * El {@code @authz.isMyCompany(#command.companyId)} del puerto solo prueba que
         * el actor declara SU empresa. Sin acotar la carga, el {@code update} posterior
         * reescribiria el company de la cirugia ajena: apropiacion.
         */
        @Test
        @DisplayName("una cirugia de otra empresa es un 404 y no se guarda nada")
        void cirugia_de_otra_empresa_no_se_apropia() {
            when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID,
                    SurgeryMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SurgeryMother.comandoActualizar()))
                    .isInstanceOf(SurgeryNotFoundException.class)
                    .hasMessageContaining("Surgery not found: " + SurgeryMother.SURGERY_ID);

            verify(repository, never()).save(any());
            verify(repository, never()).findById(any());
            verifyNoInteractions(surgeryTypeQueryPort, animalQueryPort, consultationQueryPort,
                    companyQueryPort);
        }

        /**
         * La cuarta forma del defecto, y la que sobrevive a la de arriba: aqui la
         * cirugia <b>si es mia</b> y lo ajeno es la <b>referencia</b>. Nadie puede
         * robarme la fila —eso lo cierra la carga acotada—, pero si se podia colgarla
         * del animal de otro tenant: una cirugia de mi empresa en la historia clinica
         * de la vecina. La referencia se resolvia con {@code findById(animalId)}, sin
         * empresa; el {@code verify} de la empresa es la asercion que lo caza.
         */
        @Test
        @DisplayName("no puede reapuntar la cirugia propia al animal de otra empresa")
        void no_puede_reapuntar_al_animal_de_otra_empresa() {
            when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID,
                    SurgeryMother.COMPANY_ID))
                    .thenReturn(Optional.of(SurgeryMother.cirugiaValida()));
            when(surgeryTypeQueryPort.findAvailableByIdAndCompanyId(SurgeryMother.CASTRACION.id(),
                    SurgeryMother.COMPANY_ID)).thenReturn(Optional.of(SurgeryMother.CASTRACION));
            when(animalQueryPort.findByIdAndCompanyId(SurgeryMother.MICHI.id(),
                    SurgeryMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SurgeryMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + SurgeryMother.MICHI.id());

            verify(animalQueryPort).findByIdAndCompanyId(SurgeryMother.MICHI.id(),
                    SurgeryMother.COMPANY_ID);
            verify(repository, never()).save(any());
            verifyNoInteractions(consultationQueryPort, companyQueryPort);
        }

        /**
         * El catalogo de tipos mezcla filas generales con las privadas de cada empresa,
         * asi que la variante acotada es «general O mia»: el tipo general sigue
         * sirviendo y el privado del vecino deja de servir.
         */
        @Test
        @DisplayName("no puede reapuntar la cirugia propia a un tipo privado de otra empresa")
        void no_puede_reapuntar_a_un_tipo_de_otra_empresa() {
            when(repository.findByIdAndCompanyId(SurgeryMother.SURGERY_ID,
                    SurgeryMother.COMPANY_ID))
                    .thenReturn(Optional.of(SurgeryMother.cirugiaValida()));
            when(surgeryTypeQueryPort.findAvailableByIdAndCompanyId(SurgeryMother.CASTRACION.id(),
                    SurgeryMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SurgeryMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "SurgeryType not found: " + SurgeryMother.CASTRACION.id());

            verify(surgeryTypeQueryPort).findAvailableByIdAndCompanyId(
                    SurgeryMother.CASTRACION.id(), SurgeryMother.COMPANY_ID);
            verify(repository, never()).save(any());
            verifyNoInteractions(animalQueryPort, consultationQueryPort, companyQueryPort);
        }
    }
}
