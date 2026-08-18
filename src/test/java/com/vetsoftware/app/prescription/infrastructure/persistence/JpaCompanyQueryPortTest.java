package com.vetsoftware.app.prescription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.prescription.domain.CompanyRef;
import com.vetsoftware.app.prescription.testsupport.PrescriptionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyQueryPort (prescription)")
class JpaCompanyQueryPortTest {

    @Mock
    private CompanyJpaRepository companyJpaRepository;
    @Mock
    private CompanyJpaEntity companyEntity;

    @InjectMocks
    private JpaCompanyQueryPort port;

    @Test
    @DisplayName("findById mapea la entidad encontrada a CompanyRef")
    void find_by_id_mapea_la_entidad() {
        when(companyJpaRepository.findById(PrescriptionMother.COMPANY_ID))
                .thenReturn(Optional.of(companyEntity));
        when(companyEntity.getId()).thenReturn(PrescriptionMother.COMPANY_ID);
        when(companyEntity.getName()).thenReturn("Veterinaria Test");
        when(companyEntity.getIdentifier()).thenReturn("900123456");

        Optional<CompanyRef> result = port.findById(PrescriptionMother.COMPANY_ID);

        assertThat(result).contains(
                new CompanyRef(PrescriptionMother.COMPANY_ID, "Veterinaria Test", "900123456"));
    }

    @Test
    @DisplayName("findById vacio cuando la empresa no existe")
    void find_by_id_vacio_cuando_no_existe() {
        when(companyJpaRepository.findById(PrescriptionMother.COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThat(port.findById(PrescriptionMother.COMPANY_ID)).isEmpty();
    }
}
