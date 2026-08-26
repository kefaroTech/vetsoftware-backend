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

    /**
     * Reescrito con el arreglo de #590. Este caso afirmaba que la rama sin empresa
     * «carga sin acotar», que es exactamente el comportamiento VULNERABLE que se
     * acaba de cerrar: pasaba en verde cargando el medicamento PRIVADO de una
     * clinica. Lo que la rama hace ahora —y lo unico que debe hacer— es alcanzar
     * una fila del vademecum de plataforma. El caso contrario esta en
     * {@code Tenancy}.
     */
    @Test
    @DisplayName("sin empresa (camino SYSTEM) solo alcanza un medicamento general")
    void sin_empresa_solo_alcanza_un_general() {
        Medicament general = MedicamentMother.activoGeneral();
        when(repository.findById(MedicamentMother.MEDICAMENT_ID)).thenReturn(Optional.of(general));
        when(repository.existsActiveByNameAndCompanyIdExcludingId("Amoxicilina trihidrato", null,
                MedicamentMother.MEDICAMENT_ID)).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MedicamentDto dto = service.execute(new UpdateMedicamentCommand(
                MedicamentMother.MEDICAMENT_ID, "Amoxicilina trihidrato", "Actualizado", null));

        assertThat(dto.general()).isTrue();
        assertThat(dto.company()).isNull();
        ArgumentCaptor<Medicament> guardado = ArgumentCaptor.forClass(Medicament.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getCompany()).isNull();
        assertThat(guardado.getValue().isGeneral()).isTrue();
        verify(repository, never()).findByIdAndCompanyId(any(), any());
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
     * tiene que estar libre donde la fila ya vive.
     *
     * <p>
     * Aqui vivia un caso que afirmaba «camino SYSTEM sobre una fila DE EMPRESA
     * consulta con el companyId de la fila». Con el filtro de #590 esa combinacion
     * ya no existe: la rama sin empresa solo alcanza generales, asi que el ambito
     * de la fila es SIEMPRE nulo por ese camino, y el de la rama del tenant es
     * siempre el de su empresa. Lo que aquel caso codificaba —que la rama SYSTEM
     * pudiera cargar una fila privada— es el defecto, no el contrato: se ha
     * reescrito como
     * {@code Tenancy#el_camino_sin_empresa_no_alcanza_una_fila_de_empresa}.
     */
    @Nested
    @DisplayName("Ambito de la guarda — sale de la fila, no del command")
    class AmbitoDeLaGuarda {

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

        /**
         * El otro lado de #590, y el que estaba abierto. Un principal de plataforma no
         * tiene empresa que acotar, asi que sin el
         * {@code filter(Medicament::isGeneral)} un PUT con el id de una fila PRIVADA la
         * cargaba y la reescribia: una edicion del vademecum de una clinica hecha desde
         * una consola que no deberia poder tocarlo. Un 404 y no un 403: no se revela de
         * quien es la fila.
         */
        @Test
        @DisplayName("el camino sin empresa NO alcanza el medicamento privado de una clinica")
        void el_camino_sin_empresa_no_alcanza_una_fila_de_empresa() {
            Medicament deEmpresa = MedicamentMother.activoDeEmpresa();
            when(repository.findById(MedicamentMother.MEDICAMENT_ID))
                    .thenReturn(Optional.of(deEmpresa));

            assertThatThrownBy(() -> service.execute(new UpdateMedicamentCommand(
                    MedicamentMother.MEDICAMENT_ID, "Robado", "Robado", null)))
                    .isInstanceOf(MedicamentNotFoundException.class)
                    .hasMessageContaining(String.valueOf(MedicamentMother.MEDICAMENT_ID));

            verify(repository, never()).save(any());
            verify(repository, never()).existsActiveByNameAndCompanyIdExcludingId(any(), any(),
                    any());
            // Ni en memoria: el caso de uso corre @Transactional y una entidad
            // gestionada mutada se volcaria en el flush aunque nadie llamara a save.
            assertThat(deEmpresa.getName()).isEqualTo("Suero");
            assertThat(deEmpresa.getCompany()).isEqualTo(COMPANY);
            assertThat(deEmpresa.isGeneral()).isFalse();
        }
    }
}
