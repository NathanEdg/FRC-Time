---
name: Team 293 Time
description: Native attendance kiosk for FRC Team 293, SPIKE
colors:
  primary: "#dfc467"
  charcoal: "#272923"
  ink: "#282923"
  ground: "#f7f6f1"
  silver: "#eaeade"
  focus: "#926f0d"
  secondary-text: "#565947"
  muted-text: "#656654"
  success: "#3e6035"
  error: "#9e3728"
typography:
  headline:
    fontFamily: "System"
    fontSize: "32px"
    fontWeight: 700
  title:
    fontFamily: "System"
    fontSize: "20px"
    fontWeight: 700
  body:
    fontFamily: "System"
    fontSize: "15px"
    fontWeight: 400
rounded:
  control: "8px"
spacing:
  group: "12px"
  section: "24px"
  outer: "32px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.charcoal}"
    rounded: "{rounded.control}"
    padding: "14px 18px"
  input:
    backgroundColor: "#ffffff"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "14px 16px"
---

# Team 293 Time design

## Overview

Native JavaFX desktop kiosk; Operate mode. Team 293's official gold, charcoal, and silver identity carries through familiar native controls. Product behavior is preserved as requested.

The Impeccable flow is adapted to native desktop: actual JavaFX captures replace web comps and browser-specific detectors. The sidecar records native context rather than inventing HTML equivalents.

## Colors

Gold identifies the primary action and text selection. Charcoal anchors identity, warm ground supports reading in a meeting room, and silver distinguishes supporting surfaces. Success and error colors carry attendance state. All rendered text meets the applicable 4.5:1 contrast floor.

## Typography

System fonts preserve native readability across platforms. App name uses the title size; the main heading reduces to 27px in compact windows. Numeric input is 22px; supporting text is 12–14px.

## Layout

Default scene: 840 × 760. Minimum window: 420 × 720. Compact treatment begins below 600px. The work area is centered with a maximum width of 480px and left-aligned content. The ID field and primary button stack at every size. A single team logo anchors the header; the meeting date appears beside it in wider windows. The footer uses a subtle divider; full-screen operation retains the same hierarchy.

## Elevation & Depth

Flat surfaces use tonal separation and restrained input borders. Decorative shadows are absent.

## Shapes

Native buttons and text fields use gently curved corners. The official SPIKE bulldog is the distinctive identity asset; provenance is recorded in src/main/resources/images/README.md.

## Components

Digit filtering supports typed numeric member IDs and preserves leading zeroes. Enter submits. The field shows the configured ID lengths. Busy state disables repeat submissions. Success clears and focuses ID entry; failure retains and selects the ID for correction. Feedback space stays reserved without a redundant idle message. Preview mode is labeled persistently.

Buttons have explicit hover, pressed, disabled, and keyboard-focus states without changing control dimensions. Inputs have a visible focus border. Registration uses three vertically stacked, labeled native text fields, required-value validation, and safe cancellation. Cancel uses a secondary silver treatment instead of competing with the gold primary action. About content can scroll in narrow windows.

## Do's and Don'ts

- Do preserve the official logo and clear attendance state.
- Do keep controls readable and keyboard accessible at minimum window size.
- Don't use decorative effects that obscure ID entry or recovery.
- Don't add demographic fields to registration.
