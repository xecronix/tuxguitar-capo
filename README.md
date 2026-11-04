# Capo Settings 1.0.0 for TuxGuitar 2.0.1

Open **Tools → Capo Settings…** to view and change the current song's track pitch offsets. The dialog follows Ronald Weidner's specification and Pencil mockup. It uses TuxGuitar's own UI toolkit and needs no changes to TuxGuitar itself.

## Try the prepared preview

On this computer, run `C:\dev\TuxGuitar\preview\tuxguitar.exe`. This is a separate copy of the installed TuxGuitar 2.0.1 application with the plugin included. Its settings and temporary files stay in the preview folder. The existing installation and its preferences are unchanged.

Open a demo from `C:\dev\TuxGuitar\capo-plugin\examples`, then choose **Tools → Capo Settings…**. The demo songs contain generated notes, not your musical work. Like any editor, the preview saves changes to the file you open; use the supplied demos for your first check.

## Install into the existing application

1. Close TuxGuitar.
2. Copy `tuxguitar-capo-1.0.0.jar` into `C:\Program Files\tuxguitar\share\plugins`. Windows may ask for administrator permission. For a different installation folder, use its `share\plugins` subfolder.
3. Start TuxGuitar. Open **Tools → Plugins** and ensure **Capo Settings** is enabled if its menu item is absent.
4. Choose **Tools → Capo Settings…**.

Keep only one version of this plugin JAR in that folder when upgrading. To disable it, uncheck Capo Settings in Tools → Plugins. To uninstall, close TuxGuitar and remove this plugin JAR. Saved offsets are native TuxGuitar track properties and remain in your songs after uninstalling.

## Use the dialog

- All non-percussion tracks appear in song order, including bass, initially selected.
- **Up / Down** add or subtract one semitone on each selected row. Different starting values keep their differences.
- Type an integer directly to change a single row. Manual edits apply even if that row is unchecked.
- **All Tracks** selects or clears all row checkboxes. It is unchecked for a partial selection.
- The range is **0 through 24**, or **−24 through 24** when negative values are enabled. Existing negatives enable the option automatically. Change all negative values before turning it off.
- An invalid field blocks group shifts and commit. OK identifies the first invalid row so you can correct it. A group shift that would exceed a bound is disabled for the entire selection.
- Values remain staged until **OK**. **Cancel**, **Escape**, and the window close button discard them. There is no live playback preview.
- Stop playback before opening the dialog. Scroll the track list or resize the window for larger songs and long names.

The values are playback pitch offsets. String tunings, written fret numbers, rhythm, track order, and percussion are preserved. In particular, the plugin does not infer or undo tuning changes you made previously to simulate a capo.

## Undo experience

**One changed OK creates one entry in TuxGuitar's normal Undo history.** After the dialog closes, Ctrl+Z restores every changed track together; Ctrl+Y reapplies that operation. Both the group buttons and individual edits belong to that same entry.

For example, start with `0, 2, 0, 2`, press Up twice, and click OK: the song becomes `2, 4, 2, 4`. One Undo restores `0, 2, 0, 2`; one Redo restores `2, 4, 2, 4`.

Other edits retain their usual order: undo a later note edit first, then undo the capo operation. Opening the dialog, changing selections, Cancel, and an unchanged OK do not add entries or erase an available Redo. Making a new committed edit after Undo replaces the Redo branch, as with ordinary TuxGuitar edits.

Undo does not reopen the dialog. Inside a numeric field, text editing is local to that field; the song's capo operation becomes undoable only after OK. Reopening the dialog always reads the song's current offsets. Existing capo history records continue to work if the plugin is disabled during the session.

TuxGuitar retains its own limits: its history is finite and is not stored in a saved song file. Its modified marker can remain set after undoing back to a previously saved state. Save normally when finished.

## Build from source

The host API was built from upstream tag **2.0.1**, commit **533efa74e6a56bdae28bb776358305607c79cbff**. The tag's Maven files call themselves `9.99-SNAPSHOT`; that name does not mean this plugin was built against development HEAD. Use the pinned source and a dedicated Maven repository to avoid mixing host versions.

Local tools already downloaded and verified:

- Eclipse Temurin JDK **17.0.20.1+1**, under `C:\dev\TuxGuitar\.tools`.
- Apache Maven **3.9.16**, in the same folder.
- Checksums and download origins: `.tools\toolchain.json` in the project root.

From PowerShell in `C:\dev\TuxGuitar\capo-plugin`:

```powershell
.\Build.ps1
```

The script loads the project-local toolchain, builds the required upstream Java modules if absent, runs plugin tests, and produces `target\tuxguitar-capo-1.0.0.jar`. It does not install the plugin. Use `-RebuildHost` to rebuild the pinned host modules explicitly.

On another computer, put JDK 17 and Maven on PATH, download the pinned source from <https://github.com/helge17/tuxguitar/tree/533efa74e6a56bdae28bb776358305607c79cbff>, and run:

```powershell
.\Build.ps1 -Upstream 'C:\source\tuxguitar-2.0.1' -Repository 'C:\build\tuxguitar-2.0.1-m2' -RebuildHost
```

Maven needs network access on its first run. The JAR targets Java 9 bytecode, matching the upstream target; the verified build uses JDK 17. This builds the host Java modules needed by the plugin, not all native sound drivers or a complete TuxGuitar installer.

## Compatibility and verification

Built against the pinned 2.0.1 source and tested in a separate Windows x64 SWT copy of the installed **TuxGuitar 2.0.1**, using its bundled **JRE 21.0.7**. See `VALIDATION.md` for the checks performed. Other releases, operating systems, and JavaFX are unverified. The first version's dialog is English.

The plugin JAR contains only the plugin's classes and resources. TuxGuitar and its runtime libraries are supplied by the host application. Upstream TuxGuitar source is available at the pinned link above; its license is LGPL 2.1. This project has not been published or submitted upstream.
