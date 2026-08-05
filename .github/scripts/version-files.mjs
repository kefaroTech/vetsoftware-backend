// Lectura y escritura de los archivos que llevan la version del proyecto.
//
// Lo comparten release-version.mjs (versiones limpias X.Y.Z de una release) y
// dev-version.mjs (pre-releases X.Y.Z-dev.N de develop). Vive aparte porque el
// pom tiene un matiz que no conviene reimplementar: el primer <version> del
// archivo es el del <parent> de Spring Boot, no el del proyecto.
import { existsSync, readFileSync, writeFileSync } from "node:fs";

export const PROJECT_VERSION_FILES = [
  "package.json",
  "package-lock.json",
  "pom.xml",
];

export function readJson(path) {
  return JSON.parse(readFileSync(path, "utf8"));
}

export function writeJson(path, value) {
  writeFileSync(path, `${JSON.stringify(value, null, 2)}\n`);
}

// El proyecto declara su version entre </parent> y <properties>. Buscar el
// primer <version> del archivo devolveria el de spring-boot-starter-parent.
export function projectPomVersion(xml) {
  const parentEnd = xml.indexOf("</parent>");
  const propertiesStart = xml.indexOf("<properties>", parentEnd);
  const versionMatch = xml
    .slice(parentEnd, propertiesStart)
    .match(/<version>([^<]+)<\/version>/);

  if (!versionMatch) throw new Error("the project version was not found in pom.xml");
  return versionMatch[1];
}

export function setProjectPomVersion(xml, nextVersion) {
  const parentEnd = xml.indexOf("</parent>");
  const propertiesStart = xml.indexOf("<properties>", parentEnd);
  const projectHeader = xml.slice(parentEnd, propertiesStart);
  const currentVersion = projectPomVersion(xml);
  const updatedHeader = projectHeader.replace(
    `<version>${currentVersion}</version>`,
    `<version>${nextVersion}</version>`,
  );

  return `${xml.slice(0, parentEnd)}${updatedHeader}${xml.slice(propertiesStart)}`;
}

// La version del proyecto es la del pom; package.json solo la refleja. Cuando
// no hay pom (no deberia ocurrir en este repositorio) se cae a package.json.
export function readProjectVersion() {
  if (existsSync("pom.xml")) {
    return projectPomVersion(readFileSync("pom.xml", "utf8"));
  }

  return readJson("package.json").version;
}

// Escribe la misma version en los tres archivos y devuelve los que cambiaron,
// para que quien llame sepa exactamente que agregar al commit.
export function writeProjectVersion(version) {
  const written = [];

  const packageJson = readJson("package.json");
  if (packageJson.version !== version) {
    packageJson.version = version;
    writeJson("package.json", packageJson);
    written.push("package.json");
  }

  const packageLock = readJson("package-lock.json");
  if (
    packageLock.version !== version ||
    packageLock.packages?.[""]?.version !== version
  ) {
    packageLock.version = version;
    if (packageLock.packages?.[""]) packageLock.packages[""].version = version;
    writeJson("package-lock.json", packageLock);
    written.push("package-lock.json");
  }

  if (existsSync("pom.xml")) {
    const pom = readFileSync("pom.xml", "utf8");
    if (projectPomVersion(pom) !== version) {
      writeFileSync("pom.xml", setProjectPomVersion(pom, version));
      written.push("pom.xml");
    }
  }

  return written;
}
