package app.tuxguitar.app.tools.custom.capo;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CapoModelTest {
    @Test void groupChangesAreStagedAndPreserveDifferences() {
        TestSongSupport host = new TestSongSupport(0, 2, 0, 2);
        CapoModel model = new CapoModel(host.song);
        assertTrue(model.allSelected());
        assertEquals("Harmony", model.rows.get(0).name);
        model.shift(1);
        assertEquals("1", model.rows.get(0).text);
        assertEquals("3", model.rows.get(1).text);
        for (int i = 0; i < 4; i++) assertEquals(i % 2 * 2, host.song.getTrack(i).getOffset());
        model.selectAll(false);
        model.rows.get(1).selected = true;
        model.rows.get(3).selected = true;
        model.rows.get(0).text = "7";
        model.shift(1);
        assertEquals("7", model.rows.get(0).text);
        assertEquals("4", model.rows.get(1).text);
        assertEquals("1", model.rows.get(2).text);
        assertEquals("4", model.rows.get(3).text);
    }

    @Test void selectionIsTwoStateAndNeverChangesValues() {
        CapoModel model = new CapoModel(new TestSongSupport(0, 2).song);
        model.rows.get(0).selected = false;
        assertFalse(model.allSelected());
        model.selectAll(true);
        assertTrue(model.allSelected());
        model.selectAll(false);
        assertFalse(model.canShift(1));
        assertFalse(model.canShift(-1));
        model.rows.get(1).text = "5";
        assertEquals(1, model.request().changes.size());
        assertEquals("0", model.rows.get(0).text);
    }

    @Test void boundariesBlockTheWholeSelection() {
        CapoModel model = new CapoModel(new TestSongSupport(0, 24).song);
        assertFalse(model.canShift(1));
        assertFalse(model.canShift(-1));
        model.shift(1);
        assertEquals("0", model.rows.get(0).text);
        assertEquals("24", model.rows.get(1).text);
        model.rows.get(1).selected = false;
        assertTrue(model.canShift(1));
        assertTrue(model.setAllowNegative(true));
        assertTrue(model.canShift(-1));
        model.shift(-1);
        assertEquals("-1", model.rows.get(0).text);
        assertFalse(model.setAllowNegative(false));
        assertTrue(model.allowsNegative());
        model.rows.get(0).text = "0";
        assertTrue(model.setAllowNegative(false));
    }

    @ParameterizedTest @ValueSource(strings = {"", " ", "abc", "2.5", "1e1", "--2", "25", "-25", "999999999999999999999"})
    void invalidInputBlocksCommitAndGroupOperationsEvenWhenUnchecked(String text) {
        CapoModel model = new CapoModel(new TestSongSupport(1, 2).song);
        model.rows.get(1).selected = false;
        model.rows.get(1).text = text;
        assertEquals(1, model.firstInvalidRow());
        assertFalse(model.canShift(1));
        assertFalse(model.canShift(-1));
        assertThrows(IllegalStateException.class, model::request);
        model.shift(1);
        assertEquals("1", model.rows.get(0).text);
    }

    @Test void negativeInitialValuesAreRepresentedAndLowerBoundIsEnforced() {
        CapoModel model = new CapoModel(new TestSongSupport(-24, 0).song);
        assertTrue(model.allowsNegative());
        assertFalse(model.canShift(-1));
        assertTrue(model.canShift(1));
        assertEquals(0, model.request().changes.size());
        model.rows.get(0).text = "  +2  ";
        assertEquals(-1, model.firstInvalidRow());
    }

    @Test void excludesPercussionAndDoesNotInferValuesFromTuning() {
        TestSongSupport host = new TestSongSupport(0);
        host.song.getTrack(0).getString(1).setValue(72);
        host.percussion();
        CapoModel model = new CapoModel(host.song);
        assertEquals(1, model.rows.size());
        assertEquals("0", model.rows.get(0).text);
        model.shift(1);
        assertTrue(host.apply(model).success);
        assertEquals(72, host.song.getTrack(0).getString(1).getValue());
        assertEquals(0, host.song.getTrack(1).getOffset());
    }

    @Test void emptySongDisablesSelectionAndShifts() {
        CapoModel model = new CapoModel(new TestSongSupport().song);
        assertTrue(model.rows.isEmpty());
        assertFalse(model.allSelected());
        assertFalse(model.canShift(1));
        assertFalse(model.canShift(-1));
    }

    @Test void revertingStagedValuesToOriginalIsANoop() {
        TestSongSupport host = new TestSongSupport(0, 2);
        CapoModel model = new CapoModel(host.song);
        model.shift(1);
        model.shift(-1);
        assertTrue(model.request().changes.isEmpty());
        assertEquals(0, new CapoModel(host.song).rows.get(0).original);
    }
}
