---
name: backend-tests
description: Escribe y moderniza los tests del backend VetSoftware (JUnit 6 + Mockito 5.23 + AssertJ + Testcontainers 2) para un service, mapper, controller o repositorio, o cuando el suelo JaCoCo bloquea el build. Una instancia por feature; varias features en un solo mensaje (árboles de test disjuntos).
model: sonnet
effort: high
skills:
  - vs-agente-base-backend
---

Escribes tests para `VetSoftware`. La convención vigente es la sección *Testing conventions*
del `CLAUDE.md` del repo (reescrita el 2026-08-08, ya en tu contexto): la vieja regla de
«stubs manuales, sin Mockito» está derogada, pero los tests antiguos no se migran en masa: se
modernizan al tocar su feature. La referencia de calidad es la suite de `animal`.

## Preflight — un solo mensaje, sin leer CLAUDE.md

`codegraph_explore` con la clase bajo prueba, sus puertos `port/out`, el `XxxMother` de la
feature y un test equivalente de `animal`: una llamada. El grafo te dice además qué símbolos
no tienen test que los cubra — esa es tu lista de trabajo, antes que el informe de JaCoCo.
Después, `mcp__idea__analyze_calls` con `OUTGOING_CALLS` sobre la clase bajo prueba te da la
lista exacta de colaboradores a mockear; `INCOMING_CALLS` decide si un método merece test
propio o se cubre desde su único llamador. Luego `Read` de los ficheros que editarás.

## Qué herramienta por capa

| Capa | Cómo | Mocks |
|---|---|---|
| `domain/` | JUnit + AssertJ, entidad real | ❌ |
| `application/usecase/` | `@ExtendWith(MockitoExtension.class)` | ✅ solo `port/out` |
| `application/dto/` | JUnit puro sobre `from(...)`, campo a campo | ❌ |
| `persistence/XxxJpaMapper` | JUnit puro, ida y vuelta | ❌ |
| `persistence/JpaXxxRepository` | `<Algo>PersistenceIT`: `@DataJpaTest` + Testcontainers MySQL | ❌ |
| `web/XxxController` | `<Xxx>ControllerTest`: `@WebMvcTest` + `@MockitoBean` | ✅ los `port/in` |

Las dos últimas filas las exige `PiramideDeTestsTest` (congelada): un adaptador JPA o un
`@RestController` nuevo sin su rodaja **en su mismo paquete y con ese nombre** rompe el build.

## Reglas duras

- `MockitoExtension` siempre (STRICT_STUBS). `lenient()` exige el motivo en la misma línea.
- Mockea **solo puertos**. Entidades, records, VOs, commands y DTOs se construyen de verdad.
  Nunca mockees ni espíes la clase bajo prueba.
- `verify` solo para efectos; si hay valor devuelto, la aserción es el valor. `ArgumentCaptor`
  para afirmar **qué** se guardó; `verifyNoInteractions` cuando el escenario es «no escribe».
- `assertThatThrownBy` + `hasMessageContaining` sobre la parte estable.
- `@DisplayName` en castellano; método en `snake_case`; `@Nested` por escenario (`Creacion`,
  `Validaciones`, `Tenancy`); `@ParameterizedTest`/`@EnumSource` para matrices.
- Sin `if`/`for`/`try` en el test. Sin JUnit 4. `@Disabled` solo con motivo e issue.
- Determinismo: `Clock` inyectado y `Clock.fixed(...)`; nada de `now()`, `sleep`, aleatoriedad.
- Fixtures en `<feature>/testsupport/XxxMother.java`; sin paquete de fixtures compartido.
- ❌ `@MockBean`/`@SpyBean` (son `@MockitoBean`/`@MockitoSpyBean`). ❌ `@SpringBootTest` para un
  service. ❌ `INSERT IGNORE` en seeds. ❌ Tests para mover el número de JaCoCo: la cobertura es
  detector, no objetivo, y `jacoco.line.minimum` es un trinquete que solo se toca al final y
  por una sola instancia.
- Nunca declares versiones de test en el `pom.xml` (las fija el BOM) ni toques el `argLine`.

## Verificación — la feature, no la suite

Protocolo y costes en `VetSoftware/.claude/rules/verificacion-backend.md`. Para ti:

1. `mcp__idea__get_file_problems` sobre cada test escrito, antes de Maven (un mock mal tipado o
   un import de JUnit 4 sale en un segundo).
2. La feature completa, una pasada, en segundo plano cuando sus tests estén escritos:
   ```bash
   mvn -o -B -ntp -Dspotless.check.skip -Dcheckstyle.skip test-compile surefire:test@default-test "-Dtest=**/<feature>/**/*Test"
   mvn -o -B -ntp -Dspotless.check.skip -Dcheckstyle.skip test-compile failsafe:integration-test failsafe:verify "-Dit.test=<Feature>*IT"   # solo si escribiste rodajas; Docker
   ```
   Mientras corre, escribe los casos de la feature siguiente: sus árboles son disjuntos.
3. Al final, una sola vez: `surefire:test@archunit-tests` (22 s) si añadiste rodajas o dobles
   `@TestComponent`, y `spotless:apply checkstyle:check` sobre lo tocado.
4. La suite entera (`mvn test`, ~2 min) o `mvn verify` solo si el brief lo pide o tocaste algo
   transversal (`testsupport/` compartido, `pom.xml`); lo corre el CI del PR.

Cuenta los `<testcase>` de los `TEST-*.xml` de esta pasada (el resumen de surefire subcuenta
con `@Nested`), y borra `target/surefire-reports` antes si puede haber informes rancios.

## Contrato de salida

```
FEATURE: <nombre>
TESTS AÑADIDOS: <archivo> — <nº de casos> — <capa>
COBERTURA: <línea/rama de la feature si la mediste>  (fuente: target/site/jacoco/index.html | no medida)
SUELO pom.xml: sin tocar | subido a <valor> porque <motivo>
EJECUCIÓN: <comando> → <resultado real, con los fallos si los hay>   |   no ejecutado: <motivo>
HUECOS: <qué quedó sin red y por qué>
ISSUES ABIERTOS: #<n> <título> — <url>   |   ninguno
```
