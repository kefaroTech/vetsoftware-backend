package com.vetsoftware.app.company.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.application.port.out.AnimalChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.application.port.out.ConsultationChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.DayCareChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.DewormingChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.DiagnosticImagingChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.EmployeeChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.HospitalizationChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.LaboratoryTestChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.OwnerChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.PermissionChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.PrescriptionChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.RoleChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.SpaChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.SurgeryChildrenQueryPort;
import com.vetsoftware.app.company.application.port.out.VaccinationChildrenQueryPort;
import com.vetsoftware.app.company.domain.CompanyHasActiveChildrenException;
import com.vetsoftware.app.company.domain.CompanyNotFoundException;
import com.vetsoftware.app.company.testsupport.CompanyMother;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteCompanyService")
class DeleteCompanyServiceTest {

    @Mock
    private CompanyRepository repository;
    @Mock
    private AnimalChildrenQueryPort animalChildrenQueryPort;
    @Mock
    private OwnerChildrenQueryPort ownerChildrenQueryPort;
    @Mock
    private EmployeeChildrenQueryPort employeeChildrenQueryPort;
    @Mock
    private VaccinationChildrenQueryPort vaccinationChildrenQueryPort;
    @Mock
    private SurgeryChildrenQueryPort surgeryChildrenQueryPort;
    @Mock
    private HospitalizationChildrenQueryPort hospitalizationChildrenQueryPort;
    @Mock
    private DewormingChildrenQueryPort dewormingChildrenQueryPort;
    @Mock
    private DiagnosticImagingChildrenQueryPort diagnosticImagingChildrenQueryPort;
    @Mock
    private LaboratoryTestChildrenQueryPort laboratoryTestChildrenQueryPort;
    @Mock
    private PrescriptionChildrenQueryPort prescriptionChildrenQueryPort;
    @Mock
    private SpaChildrenQueryPort spaChildrenQueryPort;
    @Mock
    private DayCareChildrenQueryPort dayCareChildrenQueryPort;
    @Mock
    private ConsultationChildrenQueryPort consultationChildrenQueryPort;
    @Mock
    private PermissionChildrenQueryPort permissionChildrenQueryPort;
    @Mock
    private RoleChildrenQueryPort roleChildrenQueryPort;

    @InjectMocks
    private DeleteCompanyService service;

    private void empresaExiste() {
        when(repository.findById(CompanyMother.COMPANY_ID))
                .thenReturn(Optional.of(CompanyMother.clinicaNorte()));
    }

    /**
     * Orden exacto en que {@code DeleteCompanyService.execute} evalua los quince
     * puertos de hijos activos. El chequeo es una cadena de ifs secuencial, no un
     * bucle: por eso el corte temprano (que los puertos posteriores al que dispara
     * la excepcion no se toquen) es observable y vale la pena verificar.
     */
    private List<Object> puertosEnOrden() {
        return List.of(animalChildrenQueryPort, ownerChildrenQueryPort, employeeChildrenQueryPort,
                vaccinationChildrenQueryPort, surgeryChildrenQueryPort,
                hospitalizationChildrenQueryPort, dewormingChildrenQueryPort,
                diagnosticImagingChildrenQueryPort, laboratoryTestChildrenQueryPort,
                prescriptionChildrenQueryPort, spaChildrenQueryPort, dayCareChildrenQueryPort,
                consultationChildrenQueryPort, permissionChildrenQueryPort, roleChildrenQueryPort);
    }

    private Object[] puertosPosterioresA(int indice) {
        List<Object> ordenados = puertosEnOrden();
        return ordenados.subList(indice + 1, ordenados.size()).toArray();
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("sin hijos activos en ninguna de las quince entidades, borra la empresa")
        void sin_hijos_activos_borra_la_empresa() {
            empresaExiste();

            service.execute(CompanyMother.COMPANY_ID);

            verify(repository).delete(CompanyMother.COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("empresa inexistente")
    class EmpresaInexistente {

        @Test
        @DisplayName("no consulta ningun hijo ni escribe")
        void no_consulta_ningun_hijo_ni_escribe() {
            when(repository.findById(CompanyMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(CompanyMother.COMPANY_ID))
                    .isInstanceOf(CompanyNotFoundException.class)
                    .hasMessageContaining("Company not found: 9");

            verify(repository, never()).delete(any());
            verifyNoInteractions(puertosEnOrden().toArray());
        }
    }

    @Nested
    @DisplayName("hijos activos: aborta en el primero que encuentra, en orden")
    class HijosActivos {

        @Test
        @DisplayName("animales activos: aborta antes de consultar los otros catorce puertos")
        void animales_activos_aborta() {
            empresaExiste();
            when(animalChildrenQueryPort.existsActiveByCompanyId(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(CompanyMother.COMPANY_ID))
                    .isInstanceOf(CompanyHasActiveChildrenException.class)
                    .hasMessageContaining("has active animal children");

            verify(repository, never()).delete(any());
            verifyNoInteractions(puertosPosterioresA(0));
        }

        @Test
        @DisplayName("propietarios activos: aborta antes de consultar los siguientes trece puertos")
        void propietarios_activos_aborta() {
            empresaExiste();
            when(ownerChildrenQueryPort.existsActiveByCompanyId(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(CompanyMother.COMPANY_ID))
                    .isInstanceOf(CompanyHasActiveChildrenException.class)
                    .hasMessageContaining("has active owner children");

            verify(repository, never()).delete(any());
            verifyNoInteractions(puertosPosterioresA(1));
        }

        @Test
        @DisplayName("empleados activos: aborta antes de consultar los siguientes doce puertos")
        void empleados_activos_aborta() {
            empresaExiste();
            when(employeeChildrenQueryPort.existsActiveByCompanyId(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(CompanyMother.COMPANY_ID))
                    .isInstanceOf(CompanyHasActiveChildrenException.class)
                    .hasMessageContaining("has active employee children");

            verify(repository, never()).delete(any());
            verifyNoInteractions(puertosPosterioresA(2));
        }

        @Test
        @DisplayName("vacunaciones activas: aborta antes de consultar los siguientes once puertos")
        void vacunaciones_activas_aborta() {
            empresaExiste();
            when(vaccinationChildrenQueryPort.existsActiveByCompanyId(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(CompanyMother.COMPANY_ID))
                    .isInstanceOf(CompanyHasActiveChildrenException.class)
                    .hasMessageContaining("has active vaccination children");

            verify(repository, never()).delete(any());
            verifyNoInteractions(puertosPosterioresA(3));
        }

        @Test
        @DisplayName("cirugias activas: aborta antes de consultar los siguientes diez puertos")
        void cirugias_activas_aborta() {
            empresaExiste();
            when(surgeryChildrenQueryPort.existsActiveByCompanyId(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(CompanyMother.COMPANY_ID))
                    .isInstanceOf(CompanyHasActiveChildrenException.class)
                    .hasMessageContaining("has active surgery children");

            verify(repository, never()).delete(any());
            verifyNoInteractions(puertosPosterioresA(4));
        }

        @Test
        @DisplayName("hospitalizaciones activas: aborta antes de consultar los siguientes nueve puertos")
        void hospitalizaciones_activas_aborta() {
            empresaExiste();
            when(hospitalizationChildrenQueryPort.existsActiveByCompanyId(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(CompanyMother.COMPANY_ID))
                    .isInstanceOf(CompanyHasActiveChildrenException.class)
                    .hasMessageContaining("has active hospitalization children");

            verify(repository, never()).delete(any());
            verifyNoInteractions(puertosPosterioresA(5));
        }

        @Test
        @DisplayName("desparasitaciones activas: aborta antes de consultar los siguientes ocho puertos")
        void desparasitaciones_activas_aborta() {
            empresaExiste();
            when(dewormingChildrenQueryPort.existsActiveByCompanyId(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(CompanyMother.COMPANY_ID))
                    .isInstanceOf(CompanyHasActiveChildrenException.class)
                    .hasMessageContaining("has active deworming children");

            verify(repository, never()).delete(any());
            verifyNoInteractions(puertosPosterioresA(6));
        }

        @Test
        @DisplayName("imagenes diagnosticas activas: aborta antes de consultar los siguientes siete puertos")
        void imagenes_diagnosticas_activas_aborta() {
            empresaExiste();
            when(diagnosticImagingChildrenQueryPort
                    .existsActiveByCompanyId(CompanyMother.COMPANY_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(CompanyMother.COMPANY_ID))
                    .isInstanceOf(CompanyHasActiveChildrenException.class)
                    .hasMessageContaining("has active diagnosticImaging children");

            verify(repository, never()).delete(any());
            verifyNoInteractions(puertosPosterioresA(7));
        }

        @Test
        @DisplayName("laboratorios activos: aborta antes de consultar los siguientes seis puertos")
        void laboratorios_activos_aborta() {
            empresaExiste();
            when(laboratoryTestChildrenQueryPort.existsActiveByCompanyId(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(CompanyMother.COMPANY_ID))
                    .isInstanceOf(CompanyHasActiveChildrenException.class)
                    .hasMessageContaining("has active laboratoryTest children");

            verify(repository, never()).delete(any());
            verifyNoInteractions(puertosPosterioresA(8));
        }

        @Test
        @DisplayName("prescripciones activas: aborta antes de consultar los siguientes cinco puertos")
        void prescripciones_activas_aborta() {
            empresaExiste();
            when(prescriptionChildrenQueryPort.existsActiveByCompanyId(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(CompanyMother.COMPANY_ID))
                    .isInstanceOf(CompanyHasActiveChildrenException.class)
                    .hasMessageContaining("has active prescription children");

            verify(repository, never()).delete(any());
            verifyNoInteractions(puertosPosterioresA(9));
        }

        @Test
        @DisplayName("spa activo: aborta antes de consultar los siguientes cuatro puertos")
        void spa_activo_aborta() {
            empresaExiste();
            when(spaChildrenQueryPort.existsActiveByCompanyId(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(CompanyMother.COMPANY_ID))
                    .isInstanceOf(CompanyHasActiveChildrenException.class)
                    .hasMessageContaining("has active spa children");

            verify(repository, never()).delete(any());
            verifyNoInteractions(puertosPosterioresA(10));
        }

        @Test
        @DisplayName("guarderias activas: aborta antes de consultar los siguientes tres puertos")
        void guarderias_activas_aborta() {
            empresaExiste();
            when(dayCareChildrenQueryPort.existsActiveByCompanyId(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(CompanyMother.COMPANY_ID))
                    .isInstanceOf(CompanyHasActiveChildrenException.class)
                    .hasMessageContaining("has active dayCare children");

            verify(repository, never()).delete(any());
            verifyNoInteractions(puertosPosterioresA(11));
        }

        @Test
        @DisplayName("consultas activas: aborta antes de consultar los siguientes dos puertos")
        void consultas_activas_aborta() {
            empresaExiste();
            when(consultationChildrenQueryPort.existsActiveByCompanyId(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(CompanyMother.COMPANY_ID))
                    .isInstanceOf(CompanyHasActiveChildrenException.class)
                    .hasMessageContaining("has active consultation children");

            verify(repository, never()).delete(any());
            verifyNoInteractions(puertosPosterioresA(12));
        }

        @Test
        @DisplayName("permisos activos: aborta antes de consultar el ultimo puerto")
        void permisos_activos_aborta() {
            empresaExiste();
            when(permissionChildrenQueryPort.existsActiveByCompanyId(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(CompanyMother.COMPANY_ID))
                    .isInstanceOf(CompanyHasActiveChildrenException.class)
                    .hasMessageContaining("has active permission children");

            verify(repository, never()).delete(any());
            verifyNoInteractions(puertosPosterioresA(13));
        }

        @Test
        @DisplayName("roles activos: es el ultimo chequeo de la cadena, tambien aborta el borrado")
        void roles_activos_aborta() {
            empresaExiste();
            when(roleChildrenQueryPort.existsActiveByCompanyId(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(CompanyMother.COMPANY_ID))
                    .isInstanceOf(CompanyHasActiveChildrenException.class)
                    .hasMessageContaining("has active role children");

            verify(repository, never()).delete(any());
            assertThat(puertosPosterioresA(14)).isEmpty();
        }
    }
}
