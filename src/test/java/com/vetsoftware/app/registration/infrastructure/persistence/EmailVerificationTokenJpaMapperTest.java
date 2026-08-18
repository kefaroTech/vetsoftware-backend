package com.vetsoftware.app.registration.infrastructure.persistence;

import static com.vetsoftware.app.registration.testsupport.RegistrationMother.COMPANY_ID;
import static com.vetsoftware.app.registration.testsupport.RegistrationMother.EMITIDO;
import static com.vetsoftware.app.registration.testsupport.RegistrationMother.EMPLOYEE_ID;
import static com.vetsoftware.app.registration.testsupport.RegistrationMother.TOKEN_HASH;
import static com.vetsoftware.app.registration.testsupport.RegistrationMother.TOKEN_ID;
import static com.vetsoftware.app.registration.testsupport.RegistrationMother.VIGENTE_HASTA;
import static com.vetsoftware.app.registration.testsupport.RegistrationMother.tokenVigente;
import static com.vetsoftware.app.registration.testsupport.RegistrationMother.tokenYaConsumido;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.registration.domain.EmailVerificationToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EmailVerificationTokenJpaMapper")
class EmailVerificationTokenJpaMapperTest {

    private final EmailVerificationTokenJpaMapper mapper = new EmailVerificationTokenJpaMapper();

    @Test
    @DisplayName("toJpa copia cada campo del dominio a la entidad")
    void to_jpa_copia_cada_campo() {
        EmailVerificationToken dominio = tokenYaConsumido();

        EmailVerificationTokenJpaEntity entidad = mapper.toJpa(dominio);

        assertThat(entidad.getId()).isEqualTo(dominio.getId());
        assertThat(entidad.getTokenHash()).isEqualTo(dominio.getTokenHash());
        assertThat(entidad.getEmployeeId()).isEqualTo(dominio.getEmployeeId());
        assertThat(entidad.getCompanyId()).isEqualTo(dominio.getCompanyId());
        assertThat(entidad.getExpiresAt()).isEqualTo(dominio.getExpiresAt());
        assertThat(entidad.getConsumedAt()).isEqualTo(dominio.getConsumedAt());
    }

    @Test
    @DisplayName("toDomain reconstruye el token con cada campo intacto")
    void to_domain_reconstruye_el_token() {
        EmailVerificationTokenJpaEntity entidad = new EmailVerificationTokenJpaEntity();
        entidad.setId(TOKEN_ID);
        entidad.setTokenHash(TOKEN_HASH);
        entidad.setEmployeeId(EMPLOYEE_ID);
        entidad.setCompanyId(COMPANY_ID);
        entidad.setExpiresAt(VIGENTE_HASTA);
        entidad.setConsumedAt(EMITIDO);

        EmailVerificationToken dominio = mapper.toDomain(entidad);

        assertThat(dominio.getId()).isEqualTo(TOKEN_ID);
        assertThat(dominio.getTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(dominio.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
        assertThat(dominio.getCompanyId()).isEqualTo(COMPANY_ID);
        assertThat(dominio.getExpiresAt()).isEqualTo(VIGENTE_HASTA);
        assertThat(dominio.getConsumedAt()).isEqualTo(EMITIDO);
    }

    @Test
    @DisplayName("ida y vuelta conserva el estado del token")
    void ida_y_vuelta_conserva_el_estado() {
        EmailVerificationToken original = tokenVigente();

        EmailVerificationToken resultado = mapper.toDomain(mapper.toJpa(original));

        assertThat(resultado.getId()).isEqualTo(original.getId());
        assertThat(resultado.getTokenHash()).isEqualTo(original.getTokenHash());
        assertThat(resultado.getEmployeeId()).isEqualTo(original.getEmployeeId());
        assertThat(resultado.getCompanyId()).isEqualTo(original.getCompanyId());
        assertThat(resultado.getExpiresAt()).isEqualTo(original.getExpiresAt());
        assertThat(resultado.getConsumedAt()).isEqualTo(original.getConsumedAt());
    }
}
