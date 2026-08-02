#!/usr/bin/env sh
set -eu
set -f

REPOSITORY_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPOSITORY_ROOT"

STAGED_PATHS="$(git diff --cached --name-only --diff-filter=ACMR)"
if [ -z "$STAGED_PATHS" ]; then
  echo 'Backend quality gate: no staged files; Spotless and Checkstyle skipped.'
  exit 0
fi

JAVA_PATHS=''
SPOTLESS_PATTERNS=''
CHECKSTYLE_INCLUDES=''
RUN_FULL=false
OLD_IFS=$IFS
IFS='
'

# Configuration changes invalidate the whole quality baseline. Detect them
# before building per-file arguments so a repository-wide migration stays fast.
for path in $STAGED_PATHS; do
  case "$path" in
    pom.xml|.gitattributes|.husky/pre-commit|mvnw|mvnw.cmd|.mvn/wrapper/*|config/checkstyle/*|config/spotless/*|scripts/quality/*)
      RUN_FULL=true
      ;;
  esac
done

IFS=$OLD_IFS

# Read the unstaged set once. Calling `git diff` once per staged Java file makes
# repository-wide formatting changes unnecessarily expensive.
UNSTAGED_PATHS="$(git diff --name-only --diff-filter=ACMR)"
if [ -n "$UNSTAGED_PATHS" ]; then
  IFS='
'
  for path in $UNSTAGED_PATHS; do
    case "$path" in
      src/main/java/*.java|src/main/java/**/*.java|src/test/java/*.java|src/test/java/**/*.java)
        if printf '%s\n' "$STAGED_PATHS" | grep -Fqx -- "$path"; then
          echo "Backend quality gate blocked: '$path' also has unstaged changes." >&2
          echo 'Stage the complete file or discard its unstaged changes so the checked snapshot matches the commit.' >&2
          exit 1
        fi
        ;;
    esac
  done
  IFS=$OLD_IFS
fi

if [ "$RUN_FULL" = true ]; then
  echo 'Backend quality gate: quality configuration changed; running the full Spotless and Checkstyle self-check.'
  exec ./mvnw --batch-mode --no-transfer-progress -DskipTests spotless:check checkstyle:check
fi

IFS='
'
for path in $STAGED_PATHS; do
  case "$path" in
    src/main/java/*.java|src/main/java/**/*.java|src/test/java/*.java|src/test/java/**/*.java)
      case "$path" in
        *[!A-Za-z0-9_./-]*)
          echo "Backend quality gate blocked: unsupported Java path '$path'." >&2
          exit 1
          ;;
      esac

      relative_path=${path#src/main/java/}
      relative_path=${relative_path#src/test/java/}
      regex_path=$(printf '%s' "$path" | sed 's/\./\\./g; s#/#[/\\\\]#g')
      spotless_pattern=".*[/\\\\]$regex_path"

      if [ -n "$JAVA_PATHS" ]; then
        JAVA_PATHS="$JAVA_PATHS, $path"
        SPOTLESS_PATTERNS="$SPOTLESS_PATTERNS,$spotless_pattern"
        CHECKSTYLE_INCLUDES="$CHECKSTYLE_INCLUDES,$relative_path"
      else
        JAVA_PATHS=$path
        SPOTLESS_PATTERNS=$spotless_pattern
        CHECKSTYLE_INCLUDES=$relative_path
      fi
      ;;
  esac
done
IFS=$OLD_IFS

if [ -z "$JAVA_PATHS" ]; then
  echo 'Backend quality gate: no staged Java files; Spotless and Checkstyle skipped.'
  exit 0
fi

echo "Backend quality gate staged Java files: $JAVA_PATHS"
exec ./mvnw --batch-mode --no-transfer-progress -DskipTests \
  "-DspotlessFiles=$SPOTLESS_PATTERNS" \
  "-Dcheckstyle.includes=$CHECKSTYLE_INCLUDES" \
  spotless:check checkstyle:check
