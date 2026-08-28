#!/usr/bin/env bash

set -eo pipefail

JITPACK_GROUP_ARTIFACT="com.github.anandbagmar/teswiz"

RUN_UNIT_TESTS=true
CURRENT_VERSION=""
VERSION=""
RELEASE_NOTES=""
TEMP_NOTES=""
JAR_FILE=""
SOURCES_JAR_FILE=""
FAT_JAR_FILE=""
REUPLOAD_MISSING_ARTIFACTS=false
RELEASE_UPLOAD_URL=""

print_usage() {
  cat <<EOF
Usage: ./publishRelease.sh [--reupload-missing-artifacts] [--version <version>]

Options:
  --reupload-missing-artifacts  Re-upload only missing artifacts to an existing GitHub release.
  --version <version>           Use an explicit release version.
  -h, --help                    Show this help message.
EOF
}

parse_args() {
  while [ "$#" -gt 0 ]; do
    case "$1" in
      --reupload-missing-artifacts)
        REUPLOAD_MISSING_ARTIFACTS=true
        ;;
      --version)
        shift
        if [ -z "$1" ]; then
          echo "❌ Error: --version requires a value."
          print_usage
          exit 1
        fi
        VERSION="$1"
        ;;
      -h|--help)
        print_usage
        exit 0
        ;;
      *)
        echo "❌ Error: Unknown option '$1'"
        print_usage
        exit 1
        ;;
    esac
    shift
  done
}

check_working_tree_clean() {
  if [ -n "$(git status --porcelain)" ]; then
    echo "⚠️ Warning: You have uncommitted changes. Please commit or stash them before releasing."
    exit 1
  fi
}

ensure_on_main_branch() {
  local current_branch
  current_branch=$(git rev-parse --abbrev-ref HEAD)

  if [ "$current_branch" != "main" ]; then
    echo "❌ Error: Releases must be run from 'main'. Current branch: $current_branch"
    exit 1
  fi
}

sync_with_remote_main() {
  echo "🔄 Rebasing local main with origin/main before release build..."
  git fetch origin main
  git rebase origin/main
}

detect_current_version() {
  CURRENT_VERSION=$(grep -E '^\s*def\s+teswizVersion\s*=\s*' build.gradle | sed -E 's/.*"([^"]+)".*/\1/')
  if [ -z "$CURRENT_VERSION" ]; then
    echo "❌ Error: Could not determine current version from build.gradle"
    exit 1
  fi
}

prompt_release_version() {
  IFS='.' read -r major minor patch <<< "$CURRENT_VERSION"
  local next_patch=$((patch + 1))
  local suggested_version="$major.$minor.$next_patch"

  echo "Current version: $CURRENT_VERSION"
  read -p "Enter version to use [$suggested_version]: " user_version
  VERSION=${user_version:-$suggested_version}
}

prompt_run_tests() {
  read -p "Run unit tests before release? [Y/n]: " run_tests_confirmation
  if [[ "$run_tests_confirmation" == "n" || "$run_tests_confirmation" == "N" ]]; then
    RUN_UNIT_TESTS=false
  fi
}

build_release_notes() {
  local last_tag
  local raw_release_notes
  last_tag=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
  if [ -z "$last_tag" ]; then
    echo "No previous tags found. Collecting all commits..."
    raw_release_notes=$(git log --pretty=format:'%s')
  else
    echo "Collecting commits since last tag: $last_tag"
    raw_release_notes=$(git log "$last_tag"..HEAD --pretty=format:'%s')
  fi

  # Filter noisy automation/meta commits so release notes stay user-focused.
  RELEASE_NOTES=$(echo "$raw_release_notes" | sed -E \
    -e '/^🔄 Update README with latest commit ID[[:space:]]*-[[:space:]]*[a-f0-9]+$/d' \
    -e '/^Update README with latest commit ID[[:space:]]*-[[:space:]]*[a-f0-9]+$/d' \
    -e '/^Release [0-9]+\.[0-9]+\.[0-9]+$/d')

  if [ -z "$RELEASE_NOTES" ]; then
    echo "⚠️ Warning: No user-facing commits found since the last tag."
    RELEASE_NOTES="- Maintenance and dependency updates."
  else
    RELEASE_NOTES=$(echo "$RELEASE_NOTES" | sed -E 's/^/- /')
  fi

  TEMP_NOTES=$(mktemp)
  trap 'rm -f "$TEMP_NOTES"' EXIT
  echo "$RELEASE_NOTES" > "$TEMP_NOTES"
}

confirm_release() {
  echo -e "\n========================================"
  echo "Proposed Release Version: $VERSION"
  echo "Run unit tests before release: $RUN_UNIT_TESTS"
  echo -e "Proposed Release Notes:\n$RELEASE_NOTES"
  echo "========================================\n"

  read -p "Do you want to proceed with building and publishing release $VERSION? (y/n): " confirm
  if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
    echo "Release process aborted."
    exit 0
  fi
}

update_version_in_project_files() {
  echo "🔄 Updating version to $VERSION in project files..."

  sed -i '' -E 's/(def teswizVersion = ")[^"]*(")/\1'"$VERSION"'\2/' build.gradle

  if [ -f package.json ]; then
    local package_version
    package_version=$(node -p "require('./package.json').version" 2>/dev/null || echo "")
    if [ "$package_version" = "$VERSION" ]; then
      echo "  ℹ️ package.json already at version $VERSION; skipping npm version update."
    else
      npm version "$VERSION" --no-git-tag-version
    fi
  fi

  if [ -f README.md ]; then
    sed -i '' -E 's/(release-)[^-]+(-blue.svg\))/\1'"$VERSION"'\2/' README.md
  fi

  if [ -f Changelog.MD ]; then
    local temp_changelog
    temp_changelog=$(mktemp)
    echo -e "## $VERSION\n$RELEASE_NOTES\n" > "$temp_changelog"
    cat Changelog.MD >> "$temp_changelog"
    mv "$temp_changelog" Changelog.MD
  fi
}

build_project() {
  if [ "$RUN_UNIT_TESTS" = true ]; then
    echo "⚙️ Building project and running tests..."
    ./gradlew clean build shadowJar
  else
    echo "⚙️ Building project without running tests..."
    ./gradlew clean build shadowJar -x test
  fi
}

verify_build_outputs() {
  JAR_FILE="build/libs/teswiz-$VERSION.jar"
  SOURCES_JAR_FILE="build/libs/teswiz-$VERSION-sources.jar"
  FAT_JAR_FILE="build/libs/teswiz-$VERSION-all.jar"

  if [ ! -f "$JAR_FILE" ]; then
    echo "❌ Error: Built JAR file not found at $JAR_FILE"
    exit 1
  fi
  if [ ! -f "$FAT_JAR_FILE" ]; then
    echo "❌ Error: Built fat JAR file not found at $FAT_JAR_FILE"
    exit 1
  fi
}

resolve_release_upload_url() {
  local raw_upload_url
  raw_upload_url=$(gh release view "$VERSION" --json uploadUrl --jq '.uploadUrl' 2>/dev/null || true)
  if [ -z "$raw_upload_url" ] || [ "$raw_upload_url" = "null" ]; then
    echo "❌ Error: Could not resolve release upload URL for '$VERSION'."
    exit 1
  fi

  # GitHub returns a templated URL ending in {?name,label}; remove the template.
  RELEASE_UPLOAD_URL=${raw_upload_url%%\{*}
}

upload_asset_with_progress() {
  local artifact="$1"
  local artifact_name
  local artifact_size_bytes
  local artifact_size_mb
  local gh_token

  artifact_name=$(basename "$artifact")
  artifact_size_bytes=$(stat -f%z "$artifact" 2>/dev/null || echo "0")
  artifact_size_mb=$(awk -v bytes="$artifact_size_bytes" 'BEGIN { printf "%.1f", bytes/1024/1024 }')

  echo "  📤 Uploading $artifact_name ($artifact_size_mb MB)..."

  gh release delete-asset "$VERSION" "$artifact_name" -y >/dev/null 2>&1 || true

  gh_token=$(gh auth token)
  curl --fail --silent --show-error --progress-bar \
    -X POST \
    -H "Authorization: Bearer $gh_token" \
    -H "Content-Type: application/octet-stream" \
    --data-binary @"$artifact" \
    "$RELEASE_UPLOAD_URL?name=$artifact_name" \
    -o /dev/null

  echo "  ✅ Uploaded: $artifact_name"
}

upload_artifacts_with_progress() {
  local artifacts=("$JAR_FILE" "$SOURCES_JAR_FILE" "$FAT_JAR_FILE")
  local total=${#artifacts[@]}
  local i=1

  resolve_release_upload_url
  for artifact in "${artifacts[@]}"; do
    echo "  ($i/$total)"
    upload_asset_with_progress "$artifact"
    i=$((i+1))
  done
}

reupload_missing_artifacts() {
  echo "🔁 Re-uploading missing artifacts for GitHub Release $VERSION..."

  if ! gh release view "$VERSION" >/dev/null 2>&1; then
    echo "❌ Error: Release '$VERSION' does not exist on GitHub."
    exit 1
  fi

  local existing_assets
  existing_assets=$(gh release view "$VERSION" --json assets --jq '.assets[].name' 2>/dev/null || true)
  local uploaded_any=false

  resolve_release_upload_url

  for artifact in "$JAR_FILE" "$SOURCES_JAR_FILE" "$FAT_JAR_FILE"; do
    local artifact_name
    artifact_name=$(basename "$artifact")

    if echo "$existing_assets" | grep -Fxq "$artifact_name"; then
      echo "  ✅ Already present: $artifact_name"
      continue
    fi

    echo "  📤 Missing artifact found, uploading: $artifact_name"
    upload_asset_with_progress "$artifact"
    uploaded_any=true
  done

  if [ "$uploaded_any" = false ]; then
    echo "  ✅ No missing artifacts found."
  else
    echo "  ✅ Missing artifacts re-upload complete."
  fi
}

commit_tag_and_push() {
  echo "📦 Committing, tagging, and pushing changes to GitHub..."
  git add build.gradle package.json package-lock.json README.md Changelog.MD
  git commit -m "Release $VERSION"
  git push origin main
  git tag "$VERSION"
  git push origin "$VERSION"
}

# Jitpack builds directly from the git tag - it doesn't depend on GitHub Release assets at all -
# so kicking it off now lets it build in the background while the (slow, ~500MB) fat jar upload
# happens next. A short wait guards against Jitpack querying GitHub before the tag has propagated;
# if Jitpack still doesn't recognize the tag ("isTag": false) on the first try, retry once after a
# longer delay. Either way this is best-effort - Jitpack builds lazily on first real consumer
# request regardless, so a failed trigger here doesn't block the release.
trigger_jitpack_build() {
  local jitpack_url="https://jitpack.io/api/builds/$JITPACK_GROUP_ARTIFACT/$VERSION"
  echo "🔗 Triggering Jitpack build for $JITPACK_GROUP_ARTIFACT $VERSION..."

  sleep 5
  if [ "$(jitpack_is_tag_visible "$jitpack_url")" != "true" ]; then
    echo "  Tag not yet visible to Jitpack, retrying in 20s..."
    sleep 20
    if [ "$(jitpack_is_tag_visible "$jitpack_url")" != "true" ]; then
      echo "  ⚠️ Jitpack still hasn't picked up the tag - it will build lazily on first consumer request instead."
    fi
  fi
  echo -e "\nJitpack build queued: https://jitpack.io/#$JITPACK_GROUP_ARTIFACT/$VERSION"
}

jitpack_is_tag_visible() {
  local jitpack_url="$1"
  local response
  response=$(curl -s "$jitpack_url" || echo '{}')
  echo "$response" | jq -r '.isTag // false'
}

create_github_release() {
  echo "🚀 Creating GitHub Release $VERSION..."
  gh release create "$VERSION" \
    --title "$VERSION" \
    --notes-file "$TEMP_NOTES"
}

upload_fat_jar() {
  echo "📦 Uploading release artifacts with progress indicators..."
  upload_artifacts_with_progress
}

prune_old_release_artifacts() {
  echo "🧹 Pruning older release artifacts (keeping top 3)..."
  local idx=0
  gh release list --limit 100 --json tagName --jq '.[].tagName' | while read -r tag; do
    if [ $idx -ge 3 ]; then
      echo "  Pruning artifacts from older release: $tag"
      local assets
      assets=$(gh release view "$tag" --json assets --jq '.assets[].name' 2>/dev/null || echo "")
      for asset in $assets; do
        echo "    Deleting asset: $asset"
        gh release delete-asset "$tag" "$asset" -y
      done
    fi
    idx=$((idx+1))
  done
}

main() {
  parse_args "$@"
  ensure_on_main_branch

  if [ -z "$VERSION" ]; then
    detect_current_version
    prompt_release_version
  fi

  if [ "$REUPLOAD_MISSING_ARTIFACTS" = true ]; then
    verify_build_outputs
    reupload_missing_artifacts
    echo -e "\n✅ Artifact re-upload flow completed for release $VERSION!"
    exit 0
  fi

  check_working_tree_clean
  sync_with_remote_main
  prompt_run_tests
  build_release_notes
  confirm_release

  update_version_in_project_files
  build_project
  verify_build_outputs

  commit_tag_and_push
  trigger_jitpack_build
  create_github_release
  upload_fat_jar
  prune_old_release_artifacts

  echo -e "\n✅ Release $VERSION successfully published!"
}

main "$@"
