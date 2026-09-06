#!/usr/bin/env bash
# Lance l'application en trouvant un JDK tout seul.
#
# Pourquoi ce script : sur beaucoup de postes, le « java » du PATH est un JRE — il n'a
# pas de compilateur. Maven échoue alors avec un message trompeur :
#     Fatal error compiling: error: release version 21 not supported
# Ce n'est ni un problème de Lombok ni de Spring, seulement l'absence de javac.
#
#   ./run.sh                    démarre l'application (http://localhost:8080)
#   ./run.sh --demo=all         la démo console
#   ./run.sh --demo=6           un chapitre
set -euo pipefail
cd "$(dirname "$0")"

version_of() {   # renvoie la version majeure du JDK dont on donne le répertoire
  "$1/bin/java" -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1
}

find_jdk() {
  # 1. un JAVA_HOME déjà correct
  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/javac" ]]; then
    echo "$JAVA_HOME"; return
  fi
  # 2. un javac dans le PATH
  if command -v javac >/dev/null 2>&1; then
    dirname "$(dirname "$(readlink -f "$(command -v javac)")")"; return
  fi
  # 3. les emplacements habituels — on préfère 21, puis 17, puis le plus récent
  local candidates=()
  for dir in "$HOME"/.jdks/* "$HOME"/.sdkman/candidates/java/* /usr/lib/jvm/* /opt/java/*; do
    [[ -x "$dir/bin/javac" ]] && candidates+=("$dir")
  done
  [[ ${#candidates[@]} -eq 0 ]] && return
  for wanted in 21 17; do
    for dir in "${candidates[@]}"; do
      [[ "$(version_of "$dir")" == "$wanted" ]] && { echo "$dir"; return; }
    done
  done
  echo "${candidates[0]}"
}

JDK="$(find_jdk || true)"
if [[ -z "$JDK" ]]; then
  cat >&2 <<'MSG'
Aucun JDK trouvé (un JRE ne suffit pas : il n'a pas de compilateur).

Installez-en un, puis relancez :
  Fedora / RHEL   sudo dnf install java-21-openjdk-devel
  Debian /Ubuntu  sudo apt install openjdk-21-jdk
  macOS           brew install openjdk@21
  IntelliJ        Project Structure > SDKs > + > Download JDK (21)

Ou pointez JAVA_HOME vous-même :
  JAVA_HOME=/chemin/vers/le/jdk ./run.sh
MSG
  exit 1
fi

export JAVA_HOME="$JDK"
echo "🐢 JDK $(version_of "$JDK") — $JDK"

if [[ $# -gt 0 ]]; then
  exec ./mvnw spring-boot:run -Dspring-boot.run.arguments="$*"
else
  exec ./mvnw spring-boot:run
fi
