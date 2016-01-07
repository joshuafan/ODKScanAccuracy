package test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import main.AccuracyChecker;

public class TestCorrectnessStats {
	private static final String[][] HEALTH_CONDITIONS = { { "1", "hypertension/pre-eclampsia" }, { "2", "diabetes" },
	        { "3", "under the age of 20" }, { "4", "underweight" }, { "5", "carrying twins or triplets" },
	        { "6", "history of preterm delivery" }, { "7", "history of stillbirth or neonatal birth" },
	        { "8", "other1" }, { "9", "other2" } };

	@Test
	public void testBubbleField() {
		int[] comparison = AccuracyChecker.compareSingleResult("under the age of 20 history of preterm delivery", "3,6",
		        HEALTH_CONDITIONS);
		assertEquals(9, comparison[0]);
		assertEquals(9, comparison[1]);
	}

	@Test
	public void testBubbleFieldSomeIncorrect() {
		int[] comparison = AccuracyChecker.compareSingleResult(
		        "hypertension/pre-eclampsia diabetes under the age of 20", "1,6", HEALTH_CONDITIONS);
		assertEquals(6, comparison[0]);
		assertEquals(9, comparison[1]);
	}

	@Test
	public void testYesNo() {
		int[] comparison = AccuracyChecker.compareSingleResult("yes", "null", null);
		assertEquals(0, comparison[0]);
		assertEquals(0, comparison[1]);
		comparison = AccuracyChecker.compareSingleResult("yes", "no", null);
		assertEquals(0, comparison[0]);
		assertEquals(2, comparison[1]);
		comparison = AccuracyChecker.compareSingleResult("no", "no", null);
		assertEquals(2, comparison[0]);
		assertEquals(2, comparison[1]);
	}

	@Test
	public void testDates() {
		int[] comparison = AccuracyChecker.compareSingleResult("5/7/2015", "05/07/15", null);
		assertEquals(4, comparison[0]);
		assertEquals(4, comparison[1]);
	}
}
