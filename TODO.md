# TODO

## New features to consider

### Weather widget

Future goal — no research yet. A small widget rendered next to the clock /
date on the home screen, showing:

- A basic weather icon (sun / cloud / rain / etc.)
- A short text description ("Partly cloudy")
- Current temperature, plus today's high and low

To revisit later. When picking it up, decisions to make: data source
(OpenWeather, Open-Meteo, etc., with redistribution-friendly terms),
location source (system fused-location vs. a manually configured
lat/lng), refresh cadence, and offline / failure rendering.

### Out of scope notes

- The Kotlin source namespace is still `app.olauncher` everywhere. This is
  internal-only and has no user-facing effect; deferred per current direction.
- The `applicationId` is already `app.kzlauncher` (debug:
  `app.kzlauncher.debug`), so the fork installs side-by-side with the
  upstream Olauncher.
