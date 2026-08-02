#!/usr/bin/env bash
set -euo pipefail

paper_version="${1:?必须提供 Paper 版本}"
workspace="$(mktemp -d)"
server_pid=""

cleanup() {
  if [[ -n "$server_pid" ]] && kill -0 "$server_pid" 2>/dev/null; then
    kill -TERM "$server_pid" 2>/dev/null || true
    wait "$server_pid" 2>/dev/null || true
  fi
  rm -rf "$workspace"
}
trap cleanup EXIT

metadata_url="https://fill.papermc.io/v3/projects/paper/versions/${paper_version}/builds/latest"
download_url="$(curl --fail --silent --show-error --location "$metadata_url" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["downloads"]["server:default"]["url"])')"

curl --fail --silent --show-error --location "$download_url" --output "$workspace/paper.jar"
mkdir -p "$workspace/plugins"
cp build/libs/EssentialsC-*.jar "$workspace/plugins/EssentialsC.jar"
printf 'eula=true\n' > "$workspace/eula.txt"
printf 'online-mode=false\nserver-port=0\nenable-query=false\n' > "$workspace/server.properties"

(
  cd "$workspace"
  java -Xms512M -Xmx1G -jar paper.jar --nogui > server.log 2>&1
) &
server_pid=$!

for _ in $(seq 1 120); do
  if grep -Fq 'EssentialsC v1.4.0 已启用' "$workspace/server.log"; then
    if grep -Eq 'UnsupportedClassVersionError|Error occurred while enabling EssentialsC|Could not load.*EssentialsC' "$workspace/server.log"; then
      cat "$workspace/server.log"
      exit 1
    fi
    echo "Paper ${paper_version} 启动验证通过。"
    exit 0
  fi
  if ! kill -0 "$server_pid" 2>/dev/null; then
    cat "$workspace/server.log"
    exit 1
  fi
  sleep 1
done

cat "$workspace/server.log"
echo "Paper ${paper_version} 启动验证超时。" >&2
exit 1
