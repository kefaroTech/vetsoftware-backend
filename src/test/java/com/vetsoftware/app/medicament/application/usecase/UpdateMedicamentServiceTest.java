package com.vetsoftware.app.medicament.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicament.application.command.UpdateMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.CompanyRef;
import com.vetsoftware.app.medicament.domain.Medicament;
import com.vetsoftware.app.medicament.domain.MedicamentNameAlreadyExistsException;
import com.vetsoftware.app.medicament.domain.MedicamentNotFoundException;
import com.vetsoftware.app.medicament.testsupport.MedicamentMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateMedicamentService")
class UpdateMedicamentServiceTest {

    private static final CompanyRef COMPANY = MedicamentMother.CLINICA;
    private static final Long COMPANY_ID = MedicamentMother.COMPANY_ID;
    private static final Long OTRA_EMPRESA = 77L;

    @Mock
    private MedicamentRepository repository;

    @InjectMocks
    private UpdateMedicamentService service;

    @Test
    @DisplayName("actualiza name/description conservando el scope general/company existente")
    void actualiza_conservando_el_scope_existente() {
        Medicament existente = Medicament.create("Suero", "Original", COMPANY, false);
        when(repository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(existente));
        when(repository.existsActiveByNameAndCompanyIdExcludingId("Suero fisiologico", COMPANY_ID,
                1L)).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MedicamentDto dto = service.execute(
                new UpdateMedicamentCommand(1L, "Suero fisiologico", "Actualizado", COMPANY_ID));

        assertThat(dto.name()).isEqualTo("Suero fisiologico");
        assertThat(dto.description()).isEqualTo("Actualizado");
        ArgumentCaptor<Medicament> guardado = ArgumentCaptor.forClass(Medicament.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getCompany()).isEqualTo(COMPANY);
        assertThat(guardado.getValue().isGeneral()).isFalse();
    }

    @Test
    @DisplayName("sin empresa (camino SYSTEM) carga sin acotar")
    void sin_empresa_carga_sin_acotar() {
        Medicament existente = Medicament.create("Suero", "Original", COMPANY, false);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.existsActiveByNameAndCompanyIdExcludingId("Suero fisiologico", COMPANY_ID,
                1L)).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.execute(new UpdateMedicamentCommand(1L, "Suero fisiologico", "Actualizado", null));

        verify(repository, never()).findByIdAndCompanyId(any(), any());
        verify(repository).save(any());
    }

    @Test
    @DisplayName("lanza MedicamentNotFoundException si el medicamento no existe")
    void lanza_not_found_si_no_existe() {
        when(repository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service
                .execute(new UpdateMedicamentCommand(1L, "Suero", "Actualizado", COMPANY_ID)))
                .isInstanceOf(MedicamentNotFoundException.class).hasMessageContaining("1");

        verify(repository, never()).save(any());
    }

    @Nested
    @DisplayName("Validaciones — unicidad del nombre")
    class Validaciones {

        @Test
        @DisplayName("un nombre ya ocupado en el ambito no se guarda ni muta la fila cargada")
        void nombre_ocupado_no_se_guarda() {
            Medicament existente = Medicament.create("Suero", "Original", COMPANY, false);
            when(repository.findByIdAndCompanyId(1L, COMPANY_ID))
                    .thenReturn(Optional.of(existente));
            when(repository.existsActiveByNameAndCompanyIdExcludingId("Analgesico", COMPANY_ID, 1L))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(
                    new UpdateMedicamentCommand(1L, "Analgesico", "Actualizado", COMPANY_ID)))
                    .isInstanceOf(MedicamentNameAlreadyExistsException.class)
                    .hasMessageContaining("Analgesico");

            verify(repository, never()).save(any());
            // La guarda va ANTES del update del dominio: la instancia cargada tiene que
            // quedar intacta, porque el caso de uso corre @Transactional y una entidad
            // gestionada mutada se volcaria en el flush aunque nadie llamara a save.
            assertThat(existente.getName()).isEqualTo("Suero");
            assertThat(existente.getDescription()).isEqualTo("Original");
        }
    }

    /**
     * El ambito con el que se comprueba la unicidad sale de la FILA cargada y no
     * del command. La edicion conserva el scope del medicamento, asi que el nombre
     * tiene que estar libre donde la fila ya vive; con el {@code companyId} del
     * command, el camino SYSTEM ({@code null}) habria comprobado el vademecum de
     * plataforma mientras editaba una fila de empresa, y habria dejado pasar un
     * nombre que la base rechaza despues por el indice unico por propietario.
     */
    @Nested
    @DisplayName("Ambito de la guarda — sale de la fila, no del command")
    class AmbitoDeLaGuarda {

        @Test
        @DisplayName("camino SYSTEM sobre una fila DE EMPRESA consulta con el companyId de la fila")
        void system_sobre_fila_de_empresa_usa_el_ambito_de_la_fila() {
            Medicament deEmpresa = Medicament.create("Suero", "Original", COMPANY, false);
            when(repository.findById(1L)).thenReturn(Optional.of(deEmpresa));
            when(repository.existsActiveByNameAndCompanyIdExcludingId(any(), any(), any()))
                    .thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(
                    new UpdateMedicamentCommand(1L, "Suero fisiologico", "Actualizado", null));

            ArgumentCaptor<Long> ambito = ArgumentCaptor.forClass(Long.class);
            verify(repository).existsActiveByNameAndCompanyIdExcludingId(eq("Suero fisiologico"),
                    ambito.capture(), eq(1L));
            assertThat(ambito.getValue()).isEqualTo(COMPANY_ID);
        }

        @Test
        @DisplayName("una fila GENERAL (sin empresa) consulta el vademecum de plataforma, con ambito nulo")
        void fila_general_usa_el_ambito_nulo() {
            Medicament general = MedicamentMother.activoGeneral();
            when(repository.findById(MedicamentMother.MEDICAMENT_ID))
                    .thenReturn(Optional.of(general));
            when(repository.existsActiveByNameAndCompanyIdExcludingId(any(), any(), any()))
                    .thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MedicamentDto dto = service.execute(new UpdateMedicamentCommand(
                    MedicamentMother.MEDICAMENT_ID, "Amoxicilina trihidrato", "Revisado", null));

            ArgumentCaptor<Long> ambito = ArgumentCaptor.forClass(Long.class);
            verify(repository).existsActiveByNameAndCompanyIdExcludingId(
                    eq("Amoxicilina trihidrato"), ambito.capture(),
                    eq(MedicamentMother.MEDICAMENT_ID));
            assertThat(ambito.getValue()).isNull();
            assertThat(dto.general()).isTrue();
            assertThat(dto.company()).isNull();
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * El {@code isMyCompany} del puerto solo prueba que el llamante declara SU
         * empresa. Cargando por id a secas el efecto no era un rechazo sino una edicion
         * del vademecum ajeno, asi que el corte tiene que estar en la carga.
         */
        @Test
        @DisplayName("el medicamento de otra empresa no se carga ni se guarda")
        void el_medicamento_de_otra_empresa_no_se_guarda() {
            when(repository.findByIdAndCompanyId(1L, OTRA_EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(new UpdateMedicamentCommand(1L, "Robado", "Robado", OTRA_EMPRESA)))
                    .isInstanceOf(MedicamentNotFoundException.class).hasMessageContaining("1");

            verify(repository, never()).findById(any());
            verify(repository, never()).save(any());
            verify(repository, never()).existsActiveByNameAndCompanyIdExcludingId(any(), any(),
                    any());
        }
    }
}
