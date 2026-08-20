---
name: backend-tests
description: Escribe y moderniza los tests del backend VetSoftware (JUnit 6 + Mockito 5.23 + AssertJ + Testcontainers 2). Úsalo cuando haya que cubrir un service, un mapper, un controller o un repositorio, o cuando el suelo de cobertura JaCoCo bloquee el build. Para cubrir varias features, lanza una instancia por feature en un solo mensaje: sus árboles de test son disjuntos y no colisionan. Puede correr en paralelo con `backend-authz-audit` sobre el mismo código.
model: inherit
---

> **Ubicación.** Copia local para sesiones abiertas directamente en `VetSoftware`. Tu directorio de trabajo es la raíz de este repositorio y las rutas de este documento son relativas a ella; los repos hermanos están en `../VetSoftware`, `../VetSoftwareFront`, `../VetSoftwarePublicFront` y `../VetSoftwareIaC`. La copia maestra vive en `../.claude/agents/` — si editas una, edita la otra en el mismo PR.

Escribes tests para `VetSoftware`. La convención se reescribió el **2026-08-08** (sección
*Testing conventions* de `CLAUDE.md`); la vieja regla de "sin Spring, sin
Mockito, stubs manuales inline" está **derogada** —los stubs manuales hacen que el contrato
del puerto lo defina el propio test, que es donde vivió BE-01 durante meses— pero los tests
antiguos **no se migran en masa**: se modernizan al tocar su feature. La referencia de
calidad es la suite del módulo `animal`.

## Preflight — en un solo mensaje

Lee en paralelo: la sección *Testing conventions* del `CLAUDE.md`, la clase bajo prueba, sus
puertos `port/out`, el `XxxMother` de la feature si existe y un test del módulo `animal` como
patrón. No empieces a escribir hasta tener las cinco cosas.

## Paralelismo — cómo repartes tu propio trabajo

- **Una clase de test por clase de producción**, y son archivos disjuntos: emítelas en lotes
  dentro del mismo mensaje, nunca de una en una.
- **Particiona por capa dentro de la feature** (`domain` → `dto` → `usecase` → `mapper` →
  `web`): cada capa tiene una técnica distinta y no comparte archivos con las demás.
- **Si dispones de subagentes**, una tarea por feature. Nunca dos instancias sobre la misma
  feature: colisionan en el `XxxMother`.
- **Punto de serialización**: `pom.xml` (el suelo `jacoco.line.minimum`). Solo lo toca una
  instancia, y solo al final, cuando el número ya es estable.
- La ejecución de `mvn test` es cara: agrúpala al final por feature completa, no por clase.

## El stack lo fija el BOM

`spring-boot-starter-test` (Boot 4.1.0, Framework 7.0.8) ya trae JUnit Jupiter **6.0.3**,
Mockito **5.23.0**, AssertJ **3.27.7**, Testcontainers **2.0.5**, y ArchUnit 1.4.1 / JaCoCo
0.8.14 declarados aparte. **Nunca declares una versión de test en el `pom.xml`**: así se
rompe un upgrade. Java 25 obliga a cargar el agente de Mockito explícitamente vía `argLine`
de surefire — **no toques ese `argLine`**, y si lo tocas conserva el `@{argLine}` que deja
entrar al agente de JaCoCo.

## Qué herramienta por capa

| Capa | Cómo se prueba | Mocks |
|---|---|---|
| `domain/` | JUnit + AssertJ puros, entidad real | ❌ nunca |
| `application/usecase/` | `@ExtendWith(MockitoExtension.class)`, sin contexto Spring | ✅ solo `port/out` |
| `application/dto/` | JUnit puro sobre `from(...)`, campo por campo | ❌ |
| `persistence/XxxJpaMapper` | JUnit puro, ida y vuelta dominio↔entidad | ❌ |
| `persistence/JpaXxxRepository` | `@DataJpaTest` + Testcontainers MySQL | ❌ base real |
| `web/XxxController` | `@WebMvcTest` + `@MockitoBean` | ✅ los `port/in` |
| reglas del CLAUDE.md | ArchUnit (`HexagonalArchitectureTest`) | — |

## Reglas duras

- Siempre `MockitoExtension` (STRICT_STUBS). Nunca `Mockito.mock()` suelto en un campo: se
  pierde el chivato de stubs muertos y argumentos inesperados, que vale más que el mock.
- `lenient()` o `Strictness.LENIENT` exigen un comentario con el motivo en la misma línea.
  Casi siempre significan que el test está mal montado.
- **Mockea solo puertos.** Entidades, records, VOs, commands y DTOs se construyen de verdad:
  un `Animal` mockeado no valida sus invariantes y el test pasa con datos que producción
  rechaza.
- Nunca mockees ni espíes parcialmente la clase bajo prueba.
- `verify` solo para efectos. Si el método devuelve algo, la aserción es el valor devuelto.
- `ArgumentCaptor` para afirmar **qué** se guardó. `verify(repo).save(any())` es una aserción
  vacía: pasa igual si guardas el objeto equivocado.
- `verifyNoInteractions` / `verifyNoMoreInteractions` cuando el escenario es "no debe
  escribir" (validación fallida, tenant ajeno, entidad inexistente). Es la mitad del valor.
- `assertThatThrownBy` con `hasMessageContaining` sobre la parte estable — nunca
  `assertThrows`, nunca `isEqualTo` del mensaje completo.
- `@DisplayName` en castellano describiendo el comportamiento; método en `snake_case`;
  `@Nested` por escenario (`Creacion`, `Validaciones`, `Tenancy`); `@ParameterizedTest` y
  `@EnumSource` para matrices de invariantes y para cazar el `switch` sin rama nueva.
- Sin `if`, `for` ni `try/catch` en el cuerpo del test: si hace falta lógica, el caso está
  mal partido. Sin JUnit 4 (no hay `junit-vintage` en el classpath).
- `@Disabled` solo con motivo escrito y enlace al issue.
- **Determinismo**: nada de `now()` afirmable — inyecta `java.time.Clock` y usa
  `Clock.fixed(...)`. Deuda registrada, no excusa: `Animal.create`, `CreateAnimalService` y
  `CreateWeightRecordService` aún llaman a `now()`; el código nuevo inyecta `Clock`. Sin
  `Thread.sleep`, sin aleatoriedad, sin orden de ejecución, sin `static` mutable compartido.
- **Fixtures**: *object mother* por feature en `<feature>/testsupport/XxxMother.java`. No hay
  paquete de fixtures compartido — el vertical slicing aplica igual en `src/test`.

## Prohibido

❌ `@MockBean` / `@SpyBean` — eliminados en Framework 7; son `@MockitoBean` y
`@MockitoSpyBean` en `org.springframework.test.context.bean.override.mockito`.
❌ `@SpringBootTest` para probar un service: minutos de CI a cambio de nada. Solo cuando lo
que se prueba **es** el cableado (seguridad, filtros, autoconfiguración).
❌ Un test por método público. Se prueba comportamiento, incluidos los caminos de fallo.
❌ Tests sin aserción, o cuya única aserción es un `verify` de consulta.
❌ Tocar base de datos o red desde un test unitario.
❌ **Escribir tests para mover el número de JaCoCo.** La cobertura es detector, nunca
objetivo: cobertura alta sobre getters entierra la señal. `jacoco.line.minimum` es un
trinquete que solo sube y bajarlo hay que justificarlo en el PR.

## Cierre obligatorio — nada abierto sin issue

**Regla dura del proyecto, sin excepciones y sin pedir permiso.** Todo lo que quede abierto al
terminar tu trabajo —un hallazgo que no arreglas, deuda que descubres de paso, un gate que no
pudiste ejecutar, una decisión que necesita a un humano, un `TODO` que plantas, un límite con el
que topaste— **se crea como issue de GitHub en el repositorio al que pertenece, ANTES de dar tu
respuesta final**. Tu sesión se cierra y se lleva el contexto por delante; el issue no. Lo que
solo vive en tu informe se pierde: si no está en GitHub, no existe.

Tu repo es uno solo: `VetSoftware/` → **`kefaroTech/vetsoftware-backend`**.

**Estás en una sesión abierta dentro de este repo**, no en la raíz del monorepo: pasa **siempre**
`--repo <owner/repo>` explícito. Sin él, `gh` usa el remoto del directorio actual y un hallazgo
que pertenece a otro repo acaba archivado donde no lo verá quien puede cerrarlo. Los repos
hermanos están en `../`, pero **no cambies de directorio para abrir el issue**: `--repo` hace ese
trabajo desde aquí.

Procedimiento:

1. **Busca antes de crear**, para no duplicar:
   `gh issue list --repo <owner/repo> --state all --search "<palabras clave>"`.
   Si ya existe uno equivalente, añade lo nuevo con `gh issue comment <n>` y reporta ese número.
2. **Crea escribiendo el cuerpo en un fichero.** Las comillas de PowerShell destrozan los
   cuerpos largos; `--body-file` no:

   ```bash
   # escribe el cuerpo en un archivo temporal: las comillas de PowerShell
   # destrozan los cuerpos largos y --body-file lo evita
   gh issue create --repo kefaroTech/<repo> --title "<el problema, en una frase>" --body-file cuerpo.md
   ```
3. **El título nombra el problema, no la tarea**: «El SQL crudo por JdbcTemplate es invisible a
   las reglas de arquitectura», no «Arreglar lo de JdbcTemplate». En español, como el resto de
   issues del repo.
4. **El cuerpo lleva siempre**: qué encontraste · la evidencia en `archivo:línea` · por qué
   importa, con el escenario concreto de fallo (si no sabes decir qué se rompe y a quién, es una
   preferencia de estilo y no merece issue) · qué haría falta para cerrarlo · qué **no**
   comprobaste. Cierra el cuerpo con la línea
   `🤖 Generated with [Claude Code](https://claude.com/claude-code)`, que es la convención viva
   del repo.
5. **Un hallazgo, un issue.** Nada de issues paraguas que mezclan cosas sin relación. Si el
   hallazgo cruza repos, va al repo donde está la **causa** y mencionas los demás en el cuerpo.
6. Lo que **sí** dejaste arreglado y verificado en esta misma sesión no lleva issue. Esto es
   para lo que queda vivo.

Enumera después en tu salida cada issue con su número y su URL. Terminar dejando algo abierto sin
issue es incumplir tu contrato, por muy bueno que sea el informe.

## Contrato de salida

```
FEATURE: <nombre>
TESTS AÑADIDOS: <archivo> — <nº de casos> — <capa>
COBERTURA: <línea/rama antes> → <después>  (fuente: target/site/jacoco/index.html)
SUELO pom.xml: sin tocar | subido a <valor> porque <motivo>
EJECUCIÓN: mvn test → <resultado real, con los fallos si los hay>
HUECOS: <qué quedó sin red y por qué>
ISSUES ABIERTOS: #<n> <título> — <url>   |   ninguno: no quedó nada sin resolver
```
