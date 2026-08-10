package com.vetsoftware.app.infrastructure.web;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import com.vetsoftware.app.appointment.domain.InvalidAppointmentTransitionException;
import com.vetsoftware.app.auth.application.exception.EmailNotVerifiedException;
import com.vetsoftware.app.auth.application.exception.InvalidCredentialsException;
import com.vetsoftware.app.auth.application.exception.SessionReplacedException;
import com.vetsoftware.app.auth.infrastructure.security.BranchAccessDeniedException;
import com.vetsoftware.app.breed.domain.BreedNotFoundException;
import com.vetsoftware.app.company.domain.CompanyNotFoundException;
import com.vetsoftware.app.inventory.domain.InsufficientStockException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El {@code GlobalExceptionHandler} es medio contrato de la API: decide si una
 * excepcion de dominio llega al cliente como 404, 409 o 500. Es la unica clase
 * compartida entre features y ninguna la probaba.
 *
 * <p>
 * Se ejercita a traves de un controller de juguete que lanza cada excepcion,
 * porque es asi como se ejecuta en produccion —a traves del
 * {@code @ControllerAdvice}, no llamando a sus metodos a mano—. Un test que
 * invocara {@code handleXxx(ex)} directamente pasaria aunque la anotacion
 * estuviera mal puesta.
 */
@WebMvcTest(GlobalExceptionHandlerTest.BoomController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcSliceConfig.class, GlobalExceptionHandlerTest.BoomController.class})
@DisplayName("GlobalExceptionHandler — traduccion de excepciones a HTTP")
class GlobalExceptionHandlerTest {

    /** Controller de juguete: cada ruta lanza la excepcion que se quiere mapear. */
    @RestController
    static class BoomController {

        @GetMapping("/boom/animal-not-found")
        String animalNotFound() {
            throw new AnimalNotFoundException(7L);
        }

        @GetMapping("/boom/breed-not-found")
        String breedNotFound() {
            throw new BreedNotFoundException(8L);
        }

        @GetMapping("/boom/company-not-found")
        String companyNotFound() {
            throw new CompanyNotFoundException(9L);
        }

        @GetMapping("/boom/illegal-argument")
        String illegalArgument() {
            throw new IllegalArgumentException("name is required");
        }

        @GetMapping("/boom/insufficient-stock")
        String insufficientStock() {
            throw new InsufficientStockException("Stock insuficiente para el producto 5");
        }

        @GetMapping("/boom/invalid-credentials")
        String invalidCredentials() {
            throw new InvalidCredentialsException();
        }

        @GetMapping("/boom/session-replaced")
        String sessionReplaced() {
            throw new SessionReplacedException();
        }

        @GetMapping("/boom/email-not-verified")
        String emailNotVerified() {
            throw new EmailNotVerifiedException("ana@x.co");
        }

        @GetMapping("/boom/branch-access-denied")
        String branchAccessDenied() {
            throw new BranchAccessDeniedException("sede ajena");
        }

        @GetMapping("/boom/invalid-appointment-transition")
        String invalidAppointmentTransition() {
            throw new InvalidAppointmentTransitionException(
                    com.vetsoftware.app.appointment.domain.AppointmentStatus.CANCELLED,
                    com.vetsoftware.app.appointment.domain.AppointmentStatus.COMPLETED);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    static Stream<Arguments> mapeos() {
        return Stream.of(arguments("/boom/animal-not-found", 404),
                arguments("/boom/breed-not-found", 404), arguments("/boom/company-not-found", 404),
                arguments("/boom/illegal-argument", 400),
                arguments("/boom/insufficient-stock", 409),
                arguments("/boom/invalid-credentials", 401),
                arguments("/boom/session-replaced", 401),
                arguments("/boom/branch-access-denied", 403),
                arguments("/boom/invalid-appointment-transition", 409));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("mapeos")
    @DisplayName("cada excepcion de dominio sale con su estado HTTP")
    void cada_excepcion_sale_con_su_estado(String ruta, int esperado) throws Exception {
        mockMvc.perform(get(ruta)).andExpect(status().is(esperado));
    }

    @Nested
    @DisplayName("forma del cuerpo")
    class FormaDelCuerpo {

        @Test
        @DisplayName("responde un ProblemDetail con code y detail, no una traza")
        void responde_un_problem_detail_con_code_y_detail() throws Exception {
            mockMvc.perform(get("/boom/animal-not-found")).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.detail").value("Animal not found: 7"))
                    .andExpect(jsonPath("$.title").exists());
        }

        @Test
        @DisplayName("el conflicto de stock explica el motivo al cajero")
        void el_conflicto_de_stock_explica_el_motivo() throws Exception {
            mockMvc.perform(get("/boom/insufficient-stock")).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail")
                            .value(org.hamcrest.Matchers.containsString("Stock insuficiente")));
        }

        @Test
        @DisplayName("una credencial invalida no filtra por que fallo")
        void una_credencial_invalida_no_filtra_el_motivo() throws Exception {
            mockMvc.perform(get("/boom/invalid-credentials")).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").exists());
        }
    }

    @Test
    @DisplayName("una ruta inexistente no la absorbe el handler: sigue siendo 404")
    void una_ruta_inexistente_sigue_siendo_404() throws Exception {
        mockMvc.perform(get("/boom/no-existe")).andExpect(status().isNotFound());
    }
}
