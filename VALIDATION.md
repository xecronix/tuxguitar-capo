# Validation record

Date: 2026-09-29. Target: TuxGuitar 2.0.1, Windows x64, SWT.

## Automated checks

The required upstream Java modules were built from commit `533efa74e6a56bdae28bb776358305607c79cbff` using Temurin JDK 17.0.20.1+1 and Maven 3.9.16. **57 upstream tests passed**. This was an API/module build; native drivers and the full distribution were not rebuilt.

The plugin's Maven `verify` passes **24 tests with no failures or errors**:

- Staging, relative shifts, manual edits on unchecked rows, and two-state selection.
- Whole-selection boundaries, invalid integers/overflow, negative initial values and the lower bound.
- Percussion exclusion, altered tunings, empty eligible-track lists, and unchanged values.
- One native composite Undo record for all changed tracks, with Redo restoring the operation.
- Native edits before and after the capo edit remain in chronological order.
- No-op and abandoned sessions preserve Redo; a new commit replaces it.
- Changed documents/tracks and playback reject application before mutation.
- An injected mutation failure restores every original offset and discards the pending Undo record.
- Native `.tg` round-trip preserves offsets, note frets, strings, and altered tuning.
- The real MIDI sequence parser produces pitches shifted by the committed offsets, unchanged during staging, and restored after Undo.

The integration tests exercise the real host model, persistence, MIDI parser, native track-info records and Undo manager. The live checks below separately verify the desktop action/listener integration.

## Desktop checks

Checked using a separate copy of the installed 2.0.1 application with isolated settings. The installed application and user songs were not changed.

- Tools → Capo Settings opens the native dialog with actual names, `0, 2, 0, 2`, and all eligible rows selected. Percussion is omitted.
- Up stages `1, 3, 1, 3`. Cancel closes without adding Undo; reopening shows the originals.
- OK applies all four changes and marks the song modified.
- One Ctrl+Z restores all four original offsets; Undo becomes unavailable and Redo becomes available.
- Unchanged OK preserves that Redo. One Ctrl+Y reapplies all four changed offsets.
- Invalid text disables group shifts; OK leaves the dialog open with the offending row identified. Escape discards the invalid staged value.
- Saving the generated demo through the application writes offsets `1, 3, 1, 3`; direct inspection of its native XML confirms that tuning and fret 3 remain unchanged, and percussion keeps offset zero.
- A launcher that exposes plugin services through duplicate paths now produces a single Capo Settings menu item.
- A 30-track song scrolls to its final row while the OK and Cancel buttons remain accessible. Long names can use horizontal scrolling; the dialog also sizes its initial width to the content, up to a bounded width.

Playback pitch was verified through the actual MIDI parser, not by an audible listening test. Several edge cases are covered by automated tests rather than repeated UI interactions. Compatibility with other host versions, platforms, and JavaFX has not been tested.

Build logs are in `C:\dev\TuxGuitar\work\upstream-build.log` and `C:\dev\TuxGuitar\work\capo-build.log`. Plugin test reports are in `capo-plugin\target\surefire-reports`.
