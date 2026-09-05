# Verificación del backend — proporcional al cambio, nunca «todo por si acaso»

Regla para la sesión principal y para todo subagente que toque `VetSoftware/`. Sustituye al
hábito de cerrar cada tarea con `mvn verify`: el CI del PR ejecuta la suite unitaria, las
rodajas de integración, el suelo de cobertura y los gates de fuente en paralelo; aquí se
verifica **lo que el cambio pudo romper**, una sola vez, y con la forma más barata que da el
mismo veredicto.

## Coste medido (2026-09-05, esta máquina, cachés calientes)

| Qué | Cómo | s |
|---|---|---|
| ¿Compila este fichero? | `mcp__idea__get_file_problems` (IntelliJ) | ~1 |
| Una clase de test | `surefire:test@default-test -Dtest=Clase` | 8 |
| Los tests de una feature (`animal`, 22 clases) | `surefire:test@default-test "-Dtest=**/animal/**/*Test"` | 22 |
| Las 25 reglas de ArchUnit, una pasada | `surefire:test@archunit-tests` | 22 |
| Formato + estilo de UN fichero | `spotless:apply` + `checkstyle:check` con filtro | 4 + 2 |
| Una rodaja `*IT` (Testcontainers MySQL + migraciones) | `failsafe:integration-test -Dit.test=…` | 149 |
| Recompilar tras tocar UN fichero (Maven recompila los 5.823) | `compile` | 25 |
| `mvn compile` sin cambios | 6,5 en caliente; 34 la primera invocación fría del día | 6,5–34 |
| `mvn test -Dtest=Clase` (forma antigua) | la fase `test` arrastra la ejecución `archunit-tests` entera (+21 s) | 28–30 |
| `mvn test -Dtest=HexagonalArchitectureTest` (forma antigua) | la clase corre dos veces | 41–100 |
| `mvn verify` completo (15.753 unitarias + 31 ArchUnit + 2.263 `*IT` + JaCoCo + gates) | todo | **1.109** (18,5 min) |

Todas las formas baratas llevan el goal por ejecución (`surefire:test@default-test`,
`surefire:test@archunit-tests`) en vez de la fase `test`: **la fase arrastra la ejecución
`archunit-tests` entera (31 tests, ~21 s) en cada invocación**, porque `-Dtest` no la filtra.
`-o -B -ntp -Dspotless.check.skip -Dcheckstyle.skip` recortan lo demás, que es menor: spotless +
checkstyle en caliente son 1–2 s (hasta ~30 s solo en la primera invocación fría del día) y `-o`
evita consultar el remoto. Si `-o` falla por un artefacto ausente, repite sin `-o` una vez.

## Los cuatro niveles

**0 — Tras cada fichero escrito (segundos).** `mcp__idea__get_file_problems` con
`errorsOnly: true`. Ningún Maven para saber si compila: un `compile` recompila el módulo entero.

**1 — Bucle de la feature (~8–25 s).** Solo los tests de lo que tocaste:

```bash
mvn -o -B -ntp -Dspotless.check.skip -Dcheckstyle.skip test-compile surefire:test@default-test "-Dtest=**/<feature>/**/*Test"
```

**2 — Una vez al final, en segundo plano, en cuanto el árbol esté consistente:**

```bash
mvn -o -B -ntp -Dspotless.check.skip -Dcheckstyle.skip test-compile surefire:test@archunit-tests
mvn -o -B -ntp -Dspotless.check.skip -Dcheckstyle.skip test-compile failsafe:integration-test failsafe:verify "-Dit.test=<Feature>*IT,<OtraFeature>*IT"
mvn -o -B -ntp spotless:apply checkstyle:check "-DspotlessFiles=.*(Clase1|Clase2)[.]java" "-Dcheckstyle.includes=com/vetsoftware/app/<feature>/.../Clase1.java,com/vetsoftware/app/<feature>/.../Clase2.java"
```

- ArchUnit **una** pasada por tarea, no una por agente ni por bloque.
- **Todas** las rodajas de la tarea en **una** invocación de failsafe: cada invocación levanta
  MySQL y aplica las 220 migraciones. Necesita Docker (`docker info`); si no está, decláralo
  `no ejecutado`. `SchemaMigrationsIT` + una `<Feature>PersistenceIT` validan a la vez que el
  changeset aplica y que el metamodelo completo de Hibernate casa con el esquema.
- `-DspotlessFiles` recibe una regex Java y Maven se come un backslash: `.*(A|B)[.]java`,
  nunca `[/\]`. Tras tocar Javadoc, `spotless:apply` antes de `check`.
- Si tocaste un `record` de `web/request` o `web/response`, el contrato lo regenera
  `api-contract-sync`; no lo hagas dos veces.

**3 — `mvn verify` completo (18,5 min medidos), solo cuando está justificado:**

- `pom.xml`, `config/`, `application*.yml`, `.mvn/`, `scripts/quality/`.
- Un changeset de Liquibase nuevo **y** el brief pide certeza antes del PR (si no, basta el
  nivel 2 con `SchemaMigrationsIT`).
- `shared/`, `infrastructure/` transversal, `auth/`, `security`, `GlobalExceptionHandler`.
- El usuario o el brief lo piden, o `/vs-verify`.
- GitHub Actions está bloqueado por facturación: entonces el `verify` local es el único gate.

Siempre en segundo plano con el código de salida a fichero
(`> verify.log 2>&1; echo $? > verify.exit`), y **nada más toca Maven mientras corre**.

## Un solo Maven a la vez

`target/`, `surefire-reports`, `failsafe-reports` y `config/archunit/violation-store` son
compartidos: dos Maven simultáneos sobre `VetSoftware/` producen informes que no corresponden
a ningún árbol y pueden podar el store. En un abanico, el brief nombra a la **única** instancia
que ejecuta Maven; las demás verifican con IntelliJ y dejan los comandos escritos. IntelliJ
compila en el mismo `target/classes`: no lances `build_project` con un Maven vivo.

## Lo que no hay que repetir

- Una verificación cuyo resultado no pudo cambiar: tras editar solo comentarios, tras una
  respuesta sin ediciones, o la misma pasada que ya corrió otro agente de la tanda.
- `compile` seguido de `test` en dos comandos: son dos recompilaciones de 25 s; `test-compile`
  dentro del mismo comando que ejecuta los tests.
- La suite entera «para estar seguros»: la corre el CI del PR con las guardas anti-falso-verde
  que este repo ya tiene.

## Trampas medidas

- **ArchUnit rojo impide llegar a failsafe** en `mvn verify`; con `failsafe:integration-test`
  directo las rodajas sí corren.
- **Informes rancios**: `target/*-reports` no se limpia entre pasadas. Cuenta `<testcase>` de
  los `TEST-*.xml` **de esta pasada** (`find -newer`), no el `Tests run` del resumen, que
  subcuenta con `@Nested`. Borra el directorio antes si dudas.
- **`mvn … | tail` devuelve el exit de `tail`**: código a fichero y lee el texto.
- **Un proceso en segundo plano puede morir con su shell**: `.exit` ausente con el proceso
  ausente es muerte, no cero. Relanza una vez; si muere dos, para y dilo.
- **El `violation-store` se poda solo** al ejecutar ArchUnit; `git checkout --` en bloque
  destruye rodajas legítimas. Solo una pasada limpia sobre un árbol quieto distingue.
