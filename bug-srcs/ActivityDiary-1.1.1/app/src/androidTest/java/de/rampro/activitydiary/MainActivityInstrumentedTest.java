package de.rampro.activitydiary;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.view.Menu;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.rampro.activitydiary.ui.main.MainActivity;

import static org.junit.Assert.assertEquals;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityInstrumentedTest {

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("de.rampro.activitydiary.debug", appContext.getPackageName());
    }

    @Test
    public void createMainActivity() throws Exception {
/*        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertNotNull(activity);

        assertTrue(menu.findItem(R.id.action_add_activity).isEnabled());*/

    }
}
