#!/usr/bin/env bash
set -euo pipefail

# This script is intentionally restricted to verified main-push workflow runs.
[[ ${GITHUB_EVENT_NAME:-} == push && ${GITHUB_REF:-} == refs/heads/main ]]
[[ ${GITHUB_SHA:-} =~ ^[0-9a-f]{40}$ ]]
[[ $(git rev-parse HEAD) == "$GITHUB_SHA" ]]
: "${GITHUB_REPOSITORY:?Missing repository}"
version=$(tr -d '\r\n' < version.txt)
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]
tag="v${version}-${GITHUB_SHA:0:12}"
workspace=$(mktemp -d)
trap 'rm -rf "$workspace"' EXIT

cd build/release
shopt -s nullglob
binaries=(*.dmg *.jar)
expected=$(printf '%s\n' \
  "Team-293-Time-${version}-linux-x64.jar" \
  "Team-293-Time-${version}-macos-arm64.jar" \
  "Team-293-Time-${version}-macos-arm64-unsigned.dmg" \
  "Team-293-Time-${version}-macos-x64.jar" \
  "Team-293-Time-${version}-macos-x64-unsigned.dmg" | LC_ALL=C sort)
[[ ${#binaries[@]} == 5 && $(printf '%s\n' "${binaries[@]}" | LC_ALL=C sort) == "$expected" ]]
for binary in "${binaries[@]}"; do
  [[ -s "$binary" ]]
done
sha256sum "${binaries[@]}" > SHA256SUMS

release=''
if release_url=$(gh release view "$tag" --repo "$GITHUB_REPOSITORY" --json apiUrl --jq '.apiUrl' 2>/dev/null); then
  release=$(gh api "$release_url")
  if [[ $(jq -r '.draft' <<< "$release") == false ]]; then
    [[ $(gh api "repos/$GITHUB_REPOSITORY/commits/$tag" --jq '.sha') == "$GITHUB_SHA" ]]
    published_names=$(jq -r '.assets[].name' <<< "$release" | LC_ALL=C sort)
    [[ "$published_names" == "$(printf '%s\n' "$expected" SHA256SUMS | LC_ALL=C sort)" ]]
    # A rerun may produce different DMG bytes. Verify the original release, never replace it.
    gh release download "$tag" --repo "$GITHUB_REPOSITORY" --pattern SHA256SUMS --dir "$workspace"
    [[ $(wc -l < "$workspace/SHA256SUMS") == 5 ]]
    checksum_names=$(cut -c 67- "$workspace/SHA256SUMS" | LC_ALL=C sort)
    [[ "$checksum_names" == "$expected" ]]
    while read -r digest name; do
      [[ "$digest" =~ ^[0-9a-f]{64}$ ]]
      [[ $(jq -r --arg name "$name" '.assets[] | select(.name == $name) | .digest' <<< "$release") == "sha256:$digest" ]]
    done < "$workspace/SHA256SUMS"
    printf 'The verified release already exists; nothing overwritten.\n'
    exit 0
  fi
  # Only this exact commit's draft can be resumed or have assets replaced.
  [[ $(jq -r '.target_commitish' <<< "$release") == "$GITHUB_SHA" ]]
else
  gh release create "$tag" --repo "$GITHUB_REPOSITORY" --target "$GITHUB_SHA" \
    --draft --title "Team 293 Time ${version} · ${GITHUB_SHA:0:12}" --notes 'Build verification complete; uploading release assets.'
fi

# If a tag already exists independently, it must not identify another commit.
if tagged_sha=$(gh api "repos/$GITHUB_REPOSITORY/commits/$tag" --jq '.sha' 2>/dev/null); then
  [[ "$tagged_sha" == "$GITHUB_SHA" ]]
fi
gh release upload "$tag" --repo "$GITHUB_REPOSITORY" --clobber "${binaries[@]}" SHA256SUMS
release_url=$(gh release view "$tag" --repo "$GITHUB_REPOSITORY" --json apiUrl --jq '.apiUrl')
release=$(gh api "$release_url")
[[ $(jq -r '.assets[].name' <<< "$release" | LC_ALL=C sort) == "$(printf '%s\n' "$expected" SHA256SUMS | LC_ALL=C sort)" ]]
while read -r digest name; do
  [[ $(jq -r --arg name "$name" '.assets[] | select(.name == $name) | .digest' <<< "$release") == "sha256:$digest" ]]
done < SHA256SUMS
printf '%s\n' \
  "Automated build of commit $GITHUB_SHA. Formatting, unit tests, native UI tests, and packaged-preview smoke tests passed on macOS Apple silicon, macOS Intel, and Linux x64." \
  '' \
  '- macos-arm64: Apple silicon Mac.' \
  '- macos-x64: Intel Mac.' \
  '- DMGs include Java; no separate Java installation is needed.' \
  '- macOS apps are unsigned and unnotarized. macOS may block first launch; review the source and follow the README security guidance.' \
  '- JARs contain platform-specific JavaFX native libraries and require Java 17 or 21. Choose the JAR matching your OS/architecture.' \
  '- Use --preview to try the kiosk without credentials or saved attendance.' \
  '- Google OAuth credentials and an editable attendance spreadsheet must be configured separately for live attendance. Live Google authorization was not exercised by CI.' \
  '- SHA256SUMS contains checksums for all five binaries.' > "$workspace/notes"

# Latest promotion is serialized separately; every commit still gets its release.
gh release edit "$tag" --repo "$GITHUB_REPOSITORY" --notes-file "$workspace/notes" --draft=false --latest=false
[[ $(gh api "repos/$GITHUB_REPOSITORY/commits/$tag" --jq '.sha') == "$GITHUB_SHA" ]]
printf 'Published all verified binaries and checksums for the tested commit.\n'
