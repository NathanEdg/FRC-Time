# Team 293 Time

A JavaFX attendance kiosk for **FRC Team 293, SPIKE**. Type your numeric member ID and press Enter to check in or out. Meeting hours are saved to Google Sheets.

The app uses Team 293's official gold, charcoal, and silver identity. Registration asks for **first name, last name, and email only**. Ethnicity, role, and gender are not collected or written.

## Download

[Download a build from GitHub Releases](https://github.com/NathanEdg/FRC-Time/releases). A tested build is published after each push to `main`.

- **Apple silicon Mac:** choose `macos-arm64-unsigned.dmg`.
- **Intel Mac:** choose `macos-x64-unsigned.dmg`.
- Open the DMG and drag **Team 293 Time.app** into Applications. The app includes its own Java runtime.
- **JAR:** choose `macos-arm64`, `macos-x64`, or `linux-x64` to match your machine. Install Java 17 or 21 of the same architecture, then run `java -jar <downloaded-file>.jar --preview` to try it, or omit `--preview` for live attendance.

macOS builds are **unsigned and unnotarized**. macOS may block them on first launch. Only open a build you trust; follow [Apple's guidance for opening an app from an unidentified developer](https://support.apple.com/guide/mac-help/open-a-mac-app-from-an-unknown-developer-mh40616/mac), including **System Settings → Privacy & Security → Open Anyway** when available. Do not disable Gatekeeper globally. Apple signing/notarization is not configured.

Each release includes `SHA256SUMS`: use `shasum -a 256 <file>` on macOS or `sha256sum <file>` on Linux to compare a download. Checksums detect corruption; they are not an Apple signature. Credentials, tokens, and attendance are not included in downloads.

## Run from source

Install **JDK 17 or 21**. JavaFX and other dependencies are downloaded by Gradle.

```sh
./gradlew run --args="--preview"
```

Preview opens the real kiosk and registration dialog without Google credentials, network access, or saved attendance. The footer identifies preview mode.

For live attendance:

```sh
./gradlew run
```

Runtime files are stored in **~/.team293-time/**. Set `TEAM293_TIME_DATA_DIR` to use another directory. Existing configuration and tokens are never automatically deleted.

1. Create a Google Cloud project, enable the Google Sheets API, and create an OAuth client of type **Desktop app**. If the consent app is in testing, add the Google account as a test user.
2. Download the client JSON to `~/.team293-time/credentials.json`, or set `TEAM293_TIME_CREDENTIALS` to its path.
3. Create an attendance spreadsheet and give the Google account edit access.
4. Start the app, paste the spreadsheet link, and complete Google authorization in the browser.

The app creates missing **Current** and **Date Log** tabs and initializes empty tabs.

## Configuration

The app creates `config.json` in its data directory on first run. Template defaults:

```json
{
  "sheet": "",
  "handsFreeMode": "false",
  "idLength": "6",
  "idLengthFallback": "7",
  "applicationName": "Team_293_Time"
}
```

Both ID lengths accept digits only and retain leading zeroes. Hands-free mode removes the attendance confirmation dialog; new members still complete registration. F11 or **Full screen** toggles kiosk mode. A custom stylesheet can be placed at `<data directory>/styles/style.css`.

Credentials and OAuth tokens are loaded only at runtime and are excluded from distributions. The fork does not prompt users to install upstream GrizzlyTime releases.

## Sheet layout and existing data

New **Current** tabs contain these nine columns, in order:

| A | B | C | D | E | F | G | H | I |
|---|---|---|---|---|---|---|---|---|
| Member ID | First Name | Last Name | Email | Last Login | Last Logout | Hours | Total Hours | Logged In |

**Date Log** starts with Member ID, First Name, and Last Name; dated hour columns follow. Total hours support values beyond 24 hours. New dates expand the sheet when necessary.

Existing GrizzlyTime sheets are supported with identity in A–D and attendance in G–K. Historical demographic columns are not used by attendance logic or updated, but whole-tab Google Sheets responses can include their existing values in application memory. Remove those columns using the backup procedure below if they must not be downloaded. Header matching ignores capitalization and punctuation; `Student ID` and `ID` are accepted as alternatives to `Member ID`.

**To remove historical ethnicity, role, and gender columns from an existing sheet**, first make a backup in Google Drive, then remove **L (ethnicity), F (gender), and E (role), in that order**. Confirm the remaining columns match the nine-column layout above before restarting. The app never deletes historical columns automatically.

Unrecognized populated tabs stop setup before attendance writes. For headerless old sheets, add a header row with the appropriate identity and attendance labels; do not overwrite the first member's row. Extra or reordered attendance columns require arranging the sheet to one of the documented layouts.

New records are appended after existing rows. A blank member ID does not make a historical row reusable; registration and Date Log repairs leave those rows untouched.

A check-out saves session hours, daily hours, total hours, and check-out state in one values batch. Failed saves display an error. If a network failure leaves the result uncertain, check the spreadsheet before submitting again. The original policy remains: an open meeting from a previous calendar day is closed without adding hours, and the member must check in again.

## Build and verify

```sh
./gradlew check
./gradlew uiTest
./gradlew installDist shadowJar
# macOS, using JDK 21:
./gradlew macDmg
bash scripts/smoke-test.sh java -jar build/libs/*-all.jar
bash scripts/smoke-test.sh 'build/macos/image/Team 293 Time.app/Contents/MacOS/Team 293 Time'
```

`uiTest` needs a desktop display; it checks native controls and writes kiosk/dialog captures to `build/review/`. Other tests run without Google credentials or a display.

Launch a local distribution with `build/install/team-293-time/bin/team-293-time`. The runnable bundled JAR is in `build/libs/`. These Java-required distributions include JavaFX native libraries for the **OS and architecture used to build them**; build on each target platform for distribution. `macDmg` creates the self-contained app under `build/macos/image/` and the installer under `build/macos/installer/`.

GitHub Actions checks formatting, headless and native UI tests, distributions, and packaged preview launches on Apple silicon, Intel macOS, and Linux x64. Linux UI tests run under Xvfb; its JAR is also smoke-tested on Java 17. Only a successful push-to-main build can publish a release, tagged `v<version>-<commit prefix>`. Releases are drafted until all five binaries and checksums are uploaded. Successful reruns verify and retain the original release instead of replacing published assets. Windows releases are not built.

## Attribution

Adapted from [YCSRobotics/GrizzlyTime](https://github.com/YCSRobotics/GrizzlyTime), originally developed by Dalton Smith for FRC Team 66, Grizzly Robotics. The original MIT license is retained in [license.txt](license.txt).

Team 293 identity and logo: [293 SPIKE](https://293spike.com/). The team logo remains the property of its owners; this project does not redefine the team's official identity.

Built with JavaFX, Google API Client, Google Sheets API, and org.json.
