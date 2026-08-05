// Versionado automatico de develop: calcula la version X.Y.Z-dev.N que le toca
// al merge que acaba de entrar y, en modo apply, la escribe en los archivos del
// proyecto para que la imagen se construya desde un commit que ya la declara.
//
//   node .github/scripts/dev-version.mjs next    -> imprime la version, no toca nada
//   node .github/scripts/dev-version.mjs apply   -> ademas escribe pom/package/lock
//
// El mapeo tipo convencional -> nivel de bump esta documentado en CLAUDE.md; es
// un contrato del repositorio, no un detalle de este script.
import { execFileSync } from "node:child_process";
import { appendFileSync } from "node:fs";

import { readProjectVersion, writeProjectVersion } from "./version-files.mjs";

export const DEV_VERSION_PATTERN = /^(\d+)\.(\d+)\.(\d+)-dev\.(\d+)$/;
export const CLEAN_VERSION_PATTERN = /^(\d+)\.(\d+)\.(\d+)$/;

// El gitmoji es la parte visual y hay decenas; la decision se toma sobre el tipo
// convencional, que son pocos y estables. El prefijo acepta tanto la forma
// :sparkles: como el caracter unicode, porque commitlint-config-gitmoji admite
// ambas.
const HEADER_PATTERN =
  /^\s*(?::[a-z0-9_+-]+:|[\p{Extended_Pictographic}️‍])*\s*([a-zA-Z]+)(?:\(([^)]*)\))?(!)?:\s*(.+)$/u;

const BREAKING_FOOTER_PATTERN = /^BREAKING[ -]CHANGE:/m;

const TYPE_LEVELS = new Map([
  ["feat", "minor"],
  ["fix", "patch"],
  ["perf", "patch"],
  ["refactor", "none"],
  ["docs", "none"],
  ["style", "none"],
  ["test", "none"],
  ["build", "none"],
  ["ci", "none"],
  ["chore", "none"],
]);

const LEVEL_RANK = { none: 0, patch: 1, minor: 2, major: 3 };

// Separador de registros del git log. NUL no puede aparecer dentro de un mensaje
// de commit, asi que parte la salida sin ambiguedad.
const RECORD_SEPARATOR = "\0";

function git(...args) {
  return execFileSync("git", args, { encoding: "utf8" });
}

// Devuelve el nivel de bump que pide un mensaje de commit completo (asunto mas
// cuerpo). Un tipo desconocido no bumpea: la version igual avanza por N.
export function classifyCommit(message) {
  const text = String(message ?? "").replace(/\r\n/g, "\n");
  const [subject = ""] = text.split("\n");
  const header = HEADER_PATTERN.exec(subject);

  if (BREAKING_FOOTER_PATTERN.test(text)) return { level: "major", subject };
  if (!header) return { level: "none", subject };
  if (header[3] === "!") return { level: "major", subject };

  return {
    level: TYPE_LEVELS.get(header[1].toLowerCase()) ?? "none",
    subject,
  };
}

// Gana el mas alto: un merge con un feat y cuatro chore es un minor.
export function highestLevel(commits) {
  let winner = { level: "none", subject: "" };

  for (const commit of commits) {
    const classified = classifyCommit(commit);
    if (LEVEL_RANK[classified.level] > LEVEL_RANK[winner.level]) {
      winner = classified;
    }
  }

  return winner;
}

// Cuando el digito base se mueve, N vuelve a 1. Y si el pom venia limpio -por un
// back-merge de release- nunca se emite una version por debajo de esa release:
// se fuerza el parche aunque no haya nada que bumpee.
export function nextVersion(current, commits) {
  const dev = DEV_VERSION_PATTERN.exec(current);
  const parsed = dev ?? CLEAN_VERSION_PATTERN.exec(current);

  if (!parsed) {
    throw new Error(
      `the current version ${current} is neither X.Y.Z nor X.Y.Z-dev.N`,
    );
  }

  const [major, minor, patch] = parsed.slice(1, 4).map(Number);
  const counter = dev ? Number(dev[4]) : 0;
  const { level, subject } = highestLevel(commits);

  if (level === "major") {
    return { version: `${major + 1}.0.0-dev.1`, level, subject };
  }
  if (level === "minor") {
    return { version: `${major}.${minor + 1}.0-dev.1`, level, subject };
  }
  if (level === "patch") {
    return { version: `${major}.${minor}.${patch + 1}-dev.1`, level, subject };
  }

  return dev
    ? { version: `${major}.${minor}.${patch}-dev.${counter + 1}`, level, subject }
    : { version: `${major}.${minor}.${patch + 1}-dev.1`, level: "patch", subject };
}

function isResolvableCommit(reference) {
  if (!reference || /^0{7,40}$/.test(reference)) return false;

  try {
    git("cat-file", "-e", `${reference}^{commit}`);
    return true;
  } catch {
    return false;
  }
}

// Los commits que entraron con este merge. Si el rango no resuelve -force push,
// primer push de la rama, historia truncada- se degrada al comportamiento
// seguro: sin commits que evaluar, la version solo avanza por N.
export function commitsInRange({
  before = process.env.DEV_VERSION_BEFORE ?? process.env.GITHUB_EVENT_BEFORE,
  head = process.env.GITHUB_SHA ?? "HEAD",
} = {}) {
  if (!isResolvableCommit(before)) {
    return { commits: [], degraded: true, range: "" };
  }

  const range = `${before}..${head}`;
  let log = "";

  try {
    log = git("log", "--no-merges", "--format=%B%x00", range);
  } catch {
    return { commits: [], degraded: true, range };
  }

  const commits = log
    .split(RECORD_SEPARATOR)
    .map((entry) => entry.trim())
    .filter(Boolean);

  return { commits, degraded: false, range };
}

function emitOutputs(outputs) {
  if (!process.env.GITHUB_OUTPUT) return;

  const lines = Object.entries(outputs)
    .map(([key, value]) => `${key}=${String(value).replace(/\r?\n/g, " ")}`)
    .join("\n");
  appendFileSync(process.env.GITHUB_OUTPUT, `${lines}\n`);
}

function main() {
  const mode = process.argv[2];

  if (!["next", "apply"].includes(mode)) {
    console.error("usage: node .github/scripts/dev-version.mjs <next|apply>");
    process.exit(1);
  }

  const current = readProjectVersion();
  const { commits, degraded, range } = commitsInRange();
  const { version, level, subject } = nextVersion(current, commits);
  const files = mode === "apply" ? writeProjectVersion(version) : [];

  if (degraded) {
    console.error(
      "dev-version: el rango de commits no resolvio; solo se incrementa el contador -dev.N.",
    );
  }
  console.error(
    `dev-version: ${current} -> ${version} (bump ${level}, ${commits.length} commits en ${range || "rango no resuelto"})`,
  );
  if (mode === "apply") {
    console.error(
      `dev-version: archivos escritos: ${files.join(", ") || "ninguno"}`,
    );
  }

  emitOutputs({
    version,
    previous: current,
    level,
    subject,
    degraded: String(degraded),
    commit_count: commits.length,
    files: files.join(" "),
  });

  console.log(version);
}

// Solo corre como CLI; importarlo desde los tests no ejecuta nada.
if (process.argv[1]?.endsWith("dev-version.mjs")) main();
