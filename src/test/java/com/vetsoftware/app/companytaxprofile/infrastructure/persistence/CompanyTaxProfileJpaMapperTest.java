package com.vetsoftware.app.companytaxprofile.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType;
import com.vetsoftware.app.companytaxprofile.domain.CompanyRef;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfile;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileResponsibility;
import com.vetsoftware.app.companytaxprofile.domain.EconomicActivityRef;
import com.vetsoftware.app.companytaxprofile.domain.TaxRegime;
import com.vetsoftware.app.companytaxprofile.testsupport.CompanyTaxProfileMother;
import com.vetsoftware.app.companytaxprofile.testsupport.ReflectionEntities;
import com.vetsoftware.app.economicactivity.infrastructure.persistence.EconomicActivityJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El grueso de la ida y vuelta domino<->entidad ya lo ejercita
 * {@code CompanyTaxProfilePersistenceIT} contra MySQL real. Este test cierra
 * las dos ramas que esa rodaja no puede alcanzar porque las garantiza el
 * esquema (la FK de {@code company_id} es {@code NOT NULL}) o porque el
 * agregado que persiste siempre trae {@code createdDate}.
 */
@DisplayName("CompanyTaxProfileJpaMapper")
class CompanyTaxProfileJpaMapperTest {

    private final CompanyTaxProfileJpaMapper mapper = new CompanyTaxProfileJpaMapper();

    @Test
    @DisplayName("toDomain de una fila sin compania (violaria la FK NOT NULL) falla en el dominio, no en el mapper")
    void toDomain_sin_compania_falla_en_el_dominio() {
        CompanyTaxProfileJpaEntity entity = new CompanyTaxProfileJpaEntity();
        entity.setCompanyDocumentType(CompanyDocumentType.CEDULA_CIUDADANIA);
        entity.setCompanyDocumentId(CompanyTaxProfileMother.CEDULA);
        entity.setLegalName(CompanyTaxProfileMother.RAZON_SOCIAL);
        entity.setTaxRegime(TaxRegime.NO_RESPONSABLE_IVA);
        entity.setFiscalEmail(CompanyTaxProfileMother.EMAIL_FISCAL);
        entity.setCreatedDate(CompanyTaxProfileMother.CREADO);
        entity.setEnabled(true);
        // company y economicActivity quedan sin asignar (null): el mapper no los
        // defiende, el dominio si.

        assertThatThrownBy(() -> mapper.toDomain(entity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company is required");
    }

    @Test
    @DisplayName("toJpa sin fecha de creacion en el perfil sella cada responsabilidad con la hora actual")
    void toJpa_sin_fecha_de_creacion_sella_las_responsabilidades_con_ahora() {
        LocalDateTime antes = LocalDateTime.now().minusSeconds(5);
        CompanyTaxProfile profile = new CompanyTaxProfile(null, CompanyTaxProfileMother.CLINICA,
                CompanyDocumentType.NIT, CompanyTaxProfileMother.NIT,
                CompanyTaxProfileMother.NIT_DV, CompanyTaxProfileMother.RAZON_SOCIAL,
                TaxRegime.RESPONSABLE_IVA, CompanyTaxProfileMother.EMAIL_FISCAL, null, null,
                List.of(CompanyTaxProfileMother.O13), null, true);

        CompanyTaxProfileJpaEntity entity = mapper.toJpa(profile, null, null);

        assertThat(entity.getResponsibilities()).hasSize(1);
        LocalDateTime selladaEn = entity.getResponsibilities().get(0).getCreatedDate();
        assertThat(selladaEn).isNotNull().isAfter(antes);
    }

    @Test
    @DisplayName("toDomain de una fila completa mapea company y actividad economica, y solo las responsabilidades habilitadas")
    void toDomain_de_una_fila_completa_mapea_refs_y_responsabilidades_habilitadas()
            throws Exception {
        CompanyJpaEntity company = ReflectionEntities.newInstance(CompanyJpaEntity.class);
        company.setId(CompanyTaxProfileMother.COMPANY_ID);
        company.setName("Clinica Norte");
        company.setIdentifier("900123456-8");

        EconomicActivityJpaEntity economicActivity = ReflectionEntities
                .newInstance(EconomicActivityJpaEntity.class);
        economicActivity.setId(5L);
        economicActivity.setCode("7500");
        economicActivity.setName("Actividades veterinarias");

        CompanyTaxProfileResponsibilityJpaEntity vigente = new CompanyTaxProfileResponsibilityJpaEntity();
        vigente.setCode("O-13");
        vigente.setEnabled(true);
        CompanyTaxProfileResponsibilityJpaEntity dadaDeBaja = new CompanyTaxProfileResponsibilityJpaEntity();
        dadaDeBaja.setCode("O-99");
        dadaDeBaja.setEnabled(false);

        CompanyTaxProfileJpaEntity entity = new CompanyTaxProfileJpaEntity();
        entity.setId(CompanyTaxProfileMother.PROFILE_ID);
        entity.setCompany(company);
        entity.setCompanyDocumentType(CompanyDocumentType.NIT);
        entity.setCompanyDocumentId(CompanyTaxProfileMother.NIT);
        entity.setCompanyDocumentVerificationDigit(CompanyTaxProfileMother.NIT_DV);
        entity.setLegalName(CompanyTaxProfileMother.RAZON_SOCIAL);
        entity.setTaxRegime(TaxRegime.RESPONSABLE_IVA);
        entity.setFiscalEmail(CompanyTaxProfileMother.EMAIL_FISCAL);
        entity.setEconomicActivity(economicActivity);
        entity.setCreatedDate(CompanyTaxProfileMother.CREADO);
        entity.setEnabled(true);
        entity.getResponsibilities().add(vigente);
        entity.getResponsibilities().add(dadaDeBaja);

        CompanyTaxProfile domain = mapper.toDomain(entity);

        assertThat(domain.getCompany()).isEqualTo(
                new CompanyRef(CompanyTaxProfileMother.COMPANY_ID, "Clinica Norte", "900123456-8"));
        assertThat(domain.getEconomicActivity())
                .isEqualTo(new EconomicActivityRef(5L, "7500", "Actividades veterinarias"));
        assertThat(domain.getResponsibilities()).extracting(CompanyTaxProfileResponsibility::code)
                .containsExactly("O-13");
    }

    @Test
    @DisplayName("toJpa con perfil completo mapea todos los campos y sella cada responsabilidad con la fecha del perfil, no con ahora")
    void toJpa_con_perfil_completo_respeta_la_fecha_del_perfil() throws Exception {
        CompanyJpaEntity company = ReflectionEntities.newInstance(CompanyJpaEntity.class);
        company.setId(CompanyTaxProfileMother.COMPANY_ID);
        EconomicActivityJpaEntity economicActivity = ReflectionEntities
                .newInstance(EconomicActivityJpaEntity.class);
        economicActivity.setId(5L);

        CompanyTaxProfile profile = CompanyTaxProfileMother.perfilNit();

        CompanyTaxProfileJpaEntity entity = mapper.toJpa(profile, company, economicActivity);

        assertThat(entity.getId()).isEqualTo(CompanyTaxProfileMother.PROFILE_ID);
        assertThat(entity.getCompany()).isSameAs(company);
        assertThat(entity.getEconomicActivity()).isSameAs(economicActivity);
        assertThat(entity.getCompanyDocumentType()).isEqualTo(CompanyDocumentType.NIT);
        assertThat(entity.getCompanyDocumentId()).isEqualTo(CompanyTaxProfileMother.NIT);
        assertThat(entity.getCompanyDocumentVerificationDigit())
                .isEqualTo(CompanyTaxProfileMother.NIT_DV);
        assertThat(entity.getLegalName()).isEqualTo(CompanyTaxProfileMother.RAZON_SOCIAL);
        assertThat(entity.getTaxRegime()).isEqualTo(TaxRegime.RESPONSABLE_IVA);
        assertThat(entity.getFiscalEmail()).isEqualTo(CompanyTaxProfileMother.EMAIL_FISCAL);
        assertThat(entity.getCommercialName()).isEqualTo(CompanyTaxProfileMother.NOMBRE_COMERCIAL);
        assertThat(entity.getCreatedDate()).isEqualTo(CompanyTaxProfileMother.CREADO);
        assertThat(entity.isEnabled()).isTrue();
        assertThat(entity.getResponsibilities())
                .extracting(CompanyTaxProfileResponsibilityJpaEntity::getCode)
                .containsExactly("O-13", "O-15");
        assertThat(entity.getResponsibilities())
                .extracting(CompanyTaxProfileResponsibilityJpaEntity::getCreatedDate)
                .containsExactly(CompanyTaxProfileMother.CREADO, CompanyTaxProfileMother.CREADO);
    }
}
