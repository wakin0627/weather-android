#!/usr/bin/env bash
# 发版：构建 → 生成本地清单 → 建 GitHub Release → 上传附件
# 用法：GITHUB_TOKEN=xxx ./release.sh "本次更新说明（可多行）"
set -euo pipefail

OWNER="wakin0627"
REPO="weather-app"
BRANCH="main"

NOTES="${1:-}"
if [[ -z "$NOTES" ]]; then
  echo "用法: GITHUB_TOKEN=xxx ./release.sh \"更新说明\"" >&2
  exit 1
fi
if [[ -z "${GITHUB_TOKEN:-}" ]]; then
  echo "缺少 GITHUB_TOKEN" >&2
  exit 1
fi

cd "$(dirname "$0")"
GRADLE_FILE="app/build.gradle.kts"
VERSION_CODE=$(grep -oE 'versionCode = [0-9]+' "$GRADLE_FILE" | grep -oE '[0-9]+')
VERSION_NAME=$(grep -oE 'versionName = "[^"]+"' "$GRADLE_FILE" | sed -E 's/.*"(.*)"/\1/')
TAG="v${VERSION_NAME}"

echo "==> 版本 ${VERSION_NAME} (code ${VERSION_CODE}) tag ${TAG}"

echo "==> 构建"
./gradlew assembleDebug --console=plain -q

APK="app/build/outputs/apk/debug/app-debug.apk"
SHA=$(sha256sum "$APK" | cut -d' ' -f1)
DIST="dist"
mkdir -p "$DIST"
APK_NAME="weather-${TAG}.apk"
cp "$APK" "$DIST/$APK_NAME"

python3 - "$DIST/version.json" "$VERSION_CODE" "$VERSION_NAME" "$NOTES" "$APK_NAME" "$SHA" <<'PY'
import json, sys
path, code, name, notes, apk, sha = sys.argv[1:7]
json.dump({
    "versionCode": int(code),
    "versionName": name,
    "notes": notes,
    "apk": apk,
    "sha256": sha,
}, open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
PY

echo "==> 创建 Release ${TAG}"
RESP=$(curl -sS -X POST \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "User-Agent: weather-release" \
  "https://api.github.com/repos/${OWNER}/${REPO}/releases" \
  -d "$(python3 -c 'import json,sys;print(json.dumps({"tag_name":sys.argv[1],"target_commitish":sys.argv[2],"name":sys.argv[1],"body":sys.argv[3]}))' "$TAG" "$BRANCH" "$NOTES")")

UPLOAD=$(echo "$RESP" | python3 -c 'import json,sys;d=json.load(sys.stdin);print(d.get("upload_url","").split("{")[0] or "")')
HTML=$(echo "$RESP" | python3 -c 'import json,sys;d=json.load(sys.stdin);print(d.get("html_url",""))')
if [[ -z "$UPLOAD" ]]; then
  echo "创建 Release 失败：$RESP" >&2
  exit 1
fi

for f in "$DIST/version.json" "$DIST/$APK_NAME"; do
  echo "==> 上传 $(basename "$f")"
  curl -sS -X POST \
    -H "Authorization: Bearer $GITHUB_TOKEN" \
    -H "Content-Type: $( [[ "$f" == *.json ]] && echo application/json || echo application/vnd.android.package-archive )" \
    -H "Accept: application/vnd.github+json" \
    -H "User-Agent: weather-release" \
    --data-binary "@$f" \
    "${UPLOAD}?name=$(basename "$f")" > /dev/null
done

echo "==> 完成：$HTML"
