import assert from "node:assert/strict";
import { test } from "node:test";

import {
  classifyCommit,
  highestLevel,
  nextVersion,
} from "./dev-version.mjs";
import {
  projectPomVersion,
  setProjectPomVersion,
} from "./version-files.mjs";

const POM = `<?xml version="1.0" encoding="UTF-8"?>
<project>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.0</version>
    </parent>
    <groupId>com.vetsoftware</groupId>
    <artifactId>vetsoftware</artifactId>
    <version>1.1.0-dev.3</version>
    <name>vetsoftware</name>
    <properties>
        <java.version>25</java.version>
    </properties>
</project>
`;

test("clasifica el tipo convencional detras del gitmoji", () => {
  assert.equal(classifyCommit(":sparkles: feat(kardex): saldo").level, "minor");
  assert.equal(classifyCommit(":bug: fix(audit): cadena").level, "patch");
  assert.equal(classifyCommit(":zap: perf(query): indice").level, "patch");
  assert.equal(classifyCommit(":memo: docs: notas").level, "none");
  assert.equal(classifyCommit(":recycle: refactor(pos): extraer").level, "none");
});

test("clasifica el gitmoji unicode igual que la forma :codigo:", () => {
  assert.equal(classifyCommit("✨ feat(kardex): saldo").level, "minor");
  assert.equal(classifyCommit("🐛 fix(audit): cadena").level, "patch");
});

test("un tipo desconocido o un asunto libre no bumpea", () => {
  assert.equal(classifyCommit("Merge pull request #42 from x").level, "none");
  assert.equal(classifyCommit("wip: algo").level, "none");
  assert.equal(classifyCommit("").level, "none");
});

test("el ! del header y el footer BREAKING CHANGE son major", () => {
  assert.equal(classifyCommit(":boom: feat(api)!: cambia contrato").level, "major");
  assert.equal(classifyCommit(":boom: feat!: cambia contrato").level, "major");
  assert.equal(
    classifyCommit(":memo: docs: notas\n\nBREAKING CHANGE: se elimina el campo").level,
    "major",
  );
  assert.equal(
    classifyCommit(":memo: docs: notas\n\nBREAKING-CHANGE: se elimina el campo").level,
    "major",
  );
});

test("gana el nivel mas alto del merge", () => {
  const winner = highestLevel([
    ":memo: docs: notas",
    ":sparkles: feat(kardex): saldo",
    ":wrench: chore: config",
    ":bug: fix(audit): cadena",
  ]);

  assert.equal(winner.level, "minor");
  assert.equal(winner.subject, ":sparkles: feat(kardex): saldo");
});

test("cada nivel mueve su digito y reinicia N en 1", () => {
  assert.equal(
    nextVersion("1.1.0-dev.7", [":boom: feat(api)!: x"]).version,
    "2.0.0-dev.1",
  );
  assert.equal(
    nextVersion("1.1.0-dev.7", [":sparkles: feat(kardex): x"]).version,
    "1.2.0-dev.1",
  );
  assert.equal(
    nextVersion("1.1.0-dev.7", [":bug: fix(audit): x"]).version,
    "1.1.1-dev.1",
  );
});

test("sin cambios que bumpeen solo avanza el contador", () => {
  assert.equal(nextVersion("1.1.0-dev.3", [":memo: docs: x"]).version, "1.1.0-dev.4");
  assert.equal(nextVersion("1.1.0-dev.9", []).version, "1.1.0-dev.10");
});

test("un pom limpio nunca produce una version anterior a la release", () => {
  // Primer merge despues del back-merge: 1.1.0 ya esta publicada, asi que el
  // ciclo nuevo tiene que abrir en 1.1.1-dev.1 aunque no haya nada que bumpee.
  const result = nextVersion("1.1.0", [":memo: docs: x"]);
  assert.equal(result.version, "1.1.1-dev.1");
  assert.equal(result.level, "patch");

  assert.equal(nextVersion("1.1.0", [":sparkles: feat: x"]).version, "1.2.0-dev.1");
  assert.equal(nextVersion("1.1.0", [":boom: feat!: x"]).version, "2.0.0-dev.1");
});

test("una version que no es X.Y.Z ni X.Y.Z-dev.N se rechaza", () => {
  assert.throws(() => nextVersion("1.0.0-SNAPSHOT", []), /neither/);
});

test("el lector de pom ignora la version del parent", () => {
  assert.equal(projectPomVersion(POM), "1.1.0-dev.3");

  const updated = setProjectPomVersion(POM, "1.2.0-dev.1");
  assert.equal(projectPomVersion(updated), "1.2.0-dev.1");
  assert.match(updated, /<artifactId>spring-boot-starter-parent<\/artifactId>\s*<version>4\.1\.0<\/version>/);
});
