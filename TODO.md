# TODO

## Promotional / review strings inherited from Olauncher

The fork left the following user-facing strings untouched because they reference
the original Olauncher project (review prompts, share-with-friends asks, the
Olauncher Pro upsell, Amazon affiliate plug). They need a decision: rewrite to
fit kzLauncher, or remove the surfaces that show them entirely.

Files: `app/src/main/res/values*/strings.xml` (English + 19 locales).

### Strings to update or remove

| Name | Current intent | Suggested action |
| --- | --- | --- |
| `olauncher_pro` | Brand label "Olauncher Pro" used in the "More features…" dialog. | Drop the dialog entirely (`Constants.Dialog.PRO_MESSAGE` flow) since we don't sell Pro. |
| `pro_message` | Body of the Pro upsell dialog. | Remove with the dialog above. |
| `more_features` | Settings row that opens the Pro dialog. | Hide the row in `fragment_settings.xml` along with `R.id.moreFeatures` handling. |
| `rate_us_message` | "Leave a 5-star review" nudge. | Either rewrite for kzLauncher or remove the rating nudge. The flow lives in `MainActivity.checkForMessages` (`UserState.RATE`). |
| `review_message` | First-pass review nudge ("Olauncher is free…"). | Same as above — rewrite or drop along with `UserState.REVIEW`. |
| `share_message` | "Help us spread Olauncher" prompt. | Drop along with `UserState.SHARE` and `Constants.Dialog.SHARE`. |
| `support_olauncher_for_free` | Amazon affiliate plug ("Support Olauncher for free"). | Drop entirely — there is no kzLauncher affiliate program. |
| `support_olauncher_message` | Affiliate body copy. | Remove with the above. |

### Suggested cleanup (when ready)

1. Decide whether to keep any review/share/upsell flow at all. For a personal
   fork the simplest answer is "no" — gut everything.
2. If gutting:
   - Remove `Constants.Dialog.{REVIEW,RATE,SHARE,PRO_MESSAGE}` and matching
     `UserState` entries (`REVIEW`, `RATE`, `SHARE`).
   - Remove the matching `showMessageDialog(...)` cases from
     `MainActivity.initObservers`.
   - Trim `MainActivity.checkForMessages` so `UserState.START` is the only
     state we ever sit in (or drop the state machine altogether).
   - Remove the `moreFeatures` settings row + click handler in
     `SettingsFragment` and `fragment_settings.xml` (portrait + landscape).
   - Delete the listed strings from every `values*/strings.xml`.
3. If keeping any of them: rewrite the English copy to fit kzLauncher and
   delete the now-stale localized variants (or accept they'll fall back to
   English until retranslated).

## New features to consider

### Custom fonts

Add a setting for the user to pick the typeface used by the clock, date, and
home/app-drawer labels. Olauncher Pro advertises this; it's the lowest-cost
"Pro" feature to bring over.

Implementation sketch:
1. Bundle a small set of `.ttf` / `.otf` files under `app/src/main/res/font/`.
2. Add a `fontFamily` pref (string key) defaulting to the current sans-serif.
3. Either swap typefaces in code (`ResourcesCompat.getFont(...)` applied to
   each `TextView` in `HomeFragment` / `AppDrawerAdapter`) or define multiple
   `TextDefault` style variants and switch the activity theme.
4. Add a settings row "Font" with a small picker, similar to the existing
   text-size control.

**Research needed before implementing:** pick fonts whose licenses allow
redistribution inside an APK. Good candidates to evaluate:
- SIL Open Font License (OFL) families — Inter, Source Sans 3, Noto Sans,
  IBM Plex Sans, JetBrains Mono, Atkinson Hyperlegible, Lora, Merriweather.
- Apache 2.0 — Roboto, Roboto Mono, Roboto Serif (Google's defaults; already
  available system-side, but bundling avoids version skew across OEMs).
- Avoid Google Fonts that ship under restrictive Ubuntu Font License variants
  unless we're sure of redistribution terms.

Aim for ~3–6 curated faces (one geometric sans, one humanist sans, one serif,
one mono) rather than a giant picker. Verify each license file is committed
alongside the font (most OFL fonts require this).

### Out of scope notes

- The Kotlin source namespace is still `app.olauncher` everywhere. This is
  internal-only and has no user-facing effect; deferred per current direction.
- The `applicationId` is already `app.kzlauncher` (debug:
  `app.kzlauncher.debug`), so the fork installs side-by-side with the
  upstream Olauncher.
