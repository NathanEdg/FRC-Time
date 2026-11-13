# Team 293 Time

A modern JavaFX attendance kiosk for **FRC Team 293, SPIKE**. Scan a USB barcode badge or type a member ID to check in and out. Meeting hours are saved to Google Sheets.

The app uses Team 293's official gold, charcoal, and silver identity. Registration asks for **first name, last name, and email only**. Ethnicity, role, and gender are not collected or written.

## Run

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

The app creates missing **Current** and **Date Log** tabs and initializes empty tabs. Configure your barcode scanner to type digits followed by Enter.

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

Existing GrizzlyTime sheets are supported with identity in A–D and attendance in G–K. Their unused columns are never read or updated. Header matching ignores capitalization and punctuation; `Student ID` and `ID` are accepted as alternatives to `Member ID`.

**To remove historical ethnicity, role, and gender columns from an existing sheet**, first make a backup in Google Drive, then remove **L (ethnicity), F (gender), and E (role), in that order**. Confirm the remaining columns match the nine-column layout above before restarting. The app never deletes historical columns automatically.

Unrecognized populated tabs stop setup before attendance writes. For headerless old sheets, add a header row with the appropriate identity and attendance labels; do not overwrite the first member's row. Extra or reordered attendance columns require arranging the sheet to one of the documented layouts.

A check-out saves session hours, daily hours, total hours, and check-out state in one values batch. Failed saves display an error. If a network failure leaves the result uncertain, check the spreadsheet before scanning again. The original policy remains: an open meeting from a previous calendar day is closed without adding hours, and the member must check in again.

## Build and verify

```sh
./gradlew check
./gradlew uiTest
./gradlew installDist shadowJar
```

`uiTest` needs a desktop display; it checks native controls and writes kiosk/dialog captures to `build/review/`. Other tests run without Google credentials or a display.

Launch a local distribution with `build/install/team-293-time/bin/team-293-time` (or the Windows `.bat` script). The runnable bundled JAR is in `build/libs/`. Distributions include JavaFX native libraries for the **OS and architecture used to build them**, and require JDK 17 or 21 on the target machine; build on each target platform for distribution.

## Attribution

Adapted from [YCSRobotics/GrizzlyTime](https://github.com/YCSRobotics/GrizzlyTime), originally developed by Dalton Smith for FRC Team 66, Grizzly Robotics. The original MIT license is retained in [license.txt](license.txt).

Team 293 identity and logo: [293 SPIKE](https://293spike.com/). The team logo remains the property of its owners; this project does not redefine the team's official identity.

Built with JavaFX, Google API Client, Google Sheets API, and org.json.
