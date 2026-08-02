import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const [, , mode, version] = process.argv;
const semverPattern = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/;

function fail(message) {
  console.error(`release-version: ${message}`);
  process.exit(1);
}

if (
  !["prepare", "verify"].includes(mode) ||
  !semverPattern.test(version ?? "")
) {
  fail(
    "usage: node .github/scripts/release-version.mjs <prepare|verify> <X.Y.Z>",
  );
}

function parseVersion(value) {
  return value.split(".").map(Number);
}

function compareVersions(left, right) {
  const a = parseVersion(left);
  const b = parseVersion(right);

  for (let index = 0; index < 3; index += 1) {
    if (a[index] !== b[index]) return a[index] - b[index];
  }

  return 0;
}

function git(...args) {
  return execFileSync("git", args, { encoding: "utf8" }).trim();
}

function readJson(path) {
  return JSON.parse(readFileSync(path, "utf8"));
}

function writeJson(path, value) {
  writeFileSync(path, `${JSON.stringify(value, null, 2)}\n`);
}

function projectPomVersion(xml) {
  const parentEnd = xml.indexOf("</parent>");
  const propertiesStart = xml.indexOf("<properties>", parentEnd);
  const versionMatch = xml
    .slice(parentEnd, propertiesStart)
    .match(/<version>([^<]+)<\/version>/);

  if (!versionMatch) fail("the project version was not found in pom.xml");
  return versionMatch[1];
}

function setProjectPomVersion(xml, nextVersion) {
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

function latestVersionTag() {
  const versions = git("tag", "--list", "v*")
    .split(/\r?\n/)
    .map((tag) => tag.trim())
    .filter((tag) => semverPattern.test(tag.slice(1)))
    .map((tag) => tag.slice(1))
    .sort(compareVersions);

  return versions.at(-1);
}

function changelogEntry(previousVersion) {
  const date = new Date().toISOString().slice(0, 10);
  let changes = ["- Primera versión versionada del repositorio."];

  if (previousVersion) {
    const subjects = git(
      "log",
      "--no-merges",
      "--format=%s",
      `v${previousVersion}..HEAD`,
    )
      .split(/\r?\n/)
      .map((subject) => subject.trim())
      .filter(Boolean);

    changes =
      subjects.length > 0
        ? subjects.map((subject) => `- ${subject}`)
        : ["- Sin cambios funcionales."];
  }

  return `## [${version}] - ${date}\n\n### Cambios\n\n${changes.join("\n")}\n\n`;
}

function prepareChangelog(previousVersion) {
  const path = "CHANGELOG.md";
  const header =
    "# Changelog\n\nLos cambios relevantes de cada versión estable se documentan aquí.\n\n";
  const current = existsSync(path) ? readFileSync(path, "utf8") : header;

  if (current.includes(`## [${version}]`)) return;
  if (!current.startsWith("# Changelog"))
    fail('CHANGELOG.md must start with "# Changelog"');

  const firstSection = current.indexOf("\n## ");
  const insertionPoint = firstSection >= 0 ? firstSection + 1 : current.length;
  const prefix = current.slice(0, insertionPoint).trimEnd();
  const suffix = current.slice(insertionPoint).trimStart();
  const next =
    `${prefix}\n\n${changelogEntry(previousVersion)}${suffix}`.trimEnd();
  writeFileSync(path, `${next}\n`);
}

const currentBranch =
  process.env.RELEASE_BRANCH ||
  process.env.GITHUB_REF_NAME ||
  git("branch", "--show-current");
if (currentBranch !== `release/${version}`) {
  fail(
    `expected branch release/${version}, found ${currentBranch || "(detached HEAD)"}`,
  );
}

const latestVersion = latestVersionTag();
if (
  mode === "prepare" &&
  latestVersion &&
  compareVersions(version, latestVersion) <= 0
) {
  fail(
    `version ${version} must be greater than the latest tag v${latestVersion}`,
  );
}

if (mode === "prepare" && git("tag", "--list", `v${version}`)) {
  fail(`tag v${version} already exists`);
}

if (mode === "prepare") {
  const packageJson = readJson("package.json");
  packageJson.version = version;
  writeJson("package.json", packageJson);

  const packageLock = readJson("package-lock.json");
  packageLock.version = version;
  if (packageLock.packages?.[""]) packageLock.packages[""].version = version;
  writeJson("package-lock.json", packageLock);

  if (existsSync("pom.xml")) {
    const pom = readFileSync("pom.xml", "utf8");
    writeFileSync("pom.xml", setProjectPomVersion(pom, version));
  }

  prepareChangelog(latestVersion);
}

const packageJson = readJson("package.json");
const packageLock = readJson("package-lock.json");
if (packageJson.version !== version)
  fail(`package.json is ${packageJson.version}, expected ${version}`);
if (packageLock.version !== version)
  fail(`package-lock.json is ${packageLock.version}, expected ${version}`);
if (packageLock.packages?.[""]?.version !== version) {
  fail(
    `package-lock.json root package is ${packageLock.packages?.[""]?.version}, expected ${version}`,
  );
}

if (existsSync("pom.xml")) {
  const pomVersion = projectPomVersion(readFileSync("pom.xml", "utf8"));
  if (pomVersion !== version)
    fail(`pom.xml is ${pomVersion}, expected ${version}`);
}

if (!readFileSync("CHANGELOG.md", "utf8").includes(`## [${version}]`)) {
  fail(`CHANGELOG.md does not contain a ${version} release section`);
}

console.log(`Release metadata verified for ${version}.`);
