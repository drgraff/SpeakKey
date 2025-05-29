package com.drgraff.speakkey.inputstick;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager; // Correct import for PreferenceManager
import com.drgraff.speakkey.R;
import com.drgraff.speakkey.formattingtags.FormattingTag;
import com.drgraff.speakkey.formattingtags.FormattingTagManager;
import com.drgraff.speakkey.inputstick.InputAction;
import com.drgraff.speakkey.inputstick.SendKeystrokesAction;
import com.drgraff.speakkey.inputstick.TextTagFormatter;
import com.drgraff.speakkey.inputstick.TypeTextAction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class TextTagFormatterTest {

    private Context context;
    private SharedPreferences sharedPreferences;
    private FormattingTagManager tagManager;
    private TextTagFormatter textTagFormatter;

    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.getApplication();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        tagManager = new FormattingTagManager(context);
        tagManager.open(); // Open the in-memory database

        // Clear any existing tags to ensure a clean state for each test
        List<FormattingTag> allTags = tagManager.getAllTags();
        for (FormattingTag tag : allTags) {
            tagManager.deleteTag(tag.getId());
        }

        textTagFormatter = new TextTagFormatter();
    }

    @After
    public void tearDown() throws Exception {
        if (tagManager != null && tagManager.isOpen()) {
            tagManager.close();
        }
    }

    private void setupGlobalDelaySetting(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(context.getString(R.string.pref_key_formatting_tag_delay_enabled), enabled);
        editor.commit(); // Use commit for immediate synchronous save in tests
    }

    private FormattingTag createAndAddTag(String name, String openTag, String keystrokes, int delayMs, boolean isActive) {
        FormattingTag tag = new FormattingTag(name, openTag, keystrokes, isActive, delayMs);
        long id = tagManager.addTag(tag);
        assertTrue("Failed to add tag for test setup", id != -1);
        tag.setId(id);
        return tag;
    }

    @Test
    public void testDelayApplied_WhenSettingOnAndTagHasDelay() {
        setupGlobalDelaySetting(true);
        createAndAddTag("Bold", "{B}", "CTRL+B", 100, true);

        List<InputAction> actions = textTagFormatter.parseTextToActions(context, "Hello {B}World{B}");

        assertNotNull(actions);
        // Expected: TypeText("Hello "), SendKeystrokes("CTRL+B", 100), TypeText("World"), SendKeystrokes("CTRL+B", 100)
        // For simplicity, checking first SendKeystrokesAction
        boolean foundAction = false;
        for (InputAction action : actions) {
            if (action instanceof SendKeystrokesAction) {
                SendKeystrokesAction skAction = (SendKeystrokesAction) action;
                if ("CTRL+B".equals(skAction.getKeystrokeSequence())) {
                    assertEquals("Delay should be 100ms when setting is ON and tag has delay", 100, skAction.getDelayMs());
                    foundAction = true;
                    break; // Found the relevant action
                }
            }
        }
        assertTrue("SendKeystrokesAction for {B} not found or delay incorrect.", foundAction);
    }

    @Test
    public void testNoDelay_WhenSettingOffAndTagHasDelay() {
        setupGlobalDelaySetting(false);
        createAndAddTag("Bold", "{B}", "CTRL+B", 100, true);

        List<InputAction> actions = textTagFormatter.parseTextToActions(context, "Hello {B}World{B}");

        assertNotNull(actions);
        boolean foundAction = false;
        for (InputAction action : actions) {
            if (action instanceof SendKeystrokesAction) {
                SendKeystrokesAction skAction = (SendKeystrokesAction) action;
                if ("CTRL+B".equals(skAction.getKeystrokeSequence())) {
                    assertEquals("Delay should be 0ms when setting is OFF, even if tag has delay", 0, skAction.getDelayMs());
                    foundAction = true;
                    break;
                }
            }
        }
        assertTrue("SendKeystrokesAction for {B} not found or delay incorrect.", foundAction);
    }

    @Test
    public void testNoDelay_WhenSettingOnAndTagDelayIsZero() {
        setupGlobalDelaySetting(true);
        createAndAddTag("Italic", "{I}", "CTRL+I", 0, true);

        List<InputAction> actions = textTagFormatter.parseTextToActions(context, "Text {I}style{I}");

        assertNotNull(actions);
        boolean foundAction = false;
        for (InputAction action : actions) {
            if (action instanceof SendKeystrokesAction) {
                SendKeystrokesAction skAction = (SendKeystrokesAction) action;
                if ("CTRL+I".equals(skAction.getKeystrokeSequence())) {
                    assertEquals("Delay should be 0ms when setting is ON but tag delay is 0", 0, skAction.getDelayMs());
                    foundAction = true;
                    break;
                }
            }
        }
        assertTrue("SendKeystrokesAction for {I} not found or delay incorrect.", foundAction);
    }

    @Test
    public void testNoSendKeystrokesAction_WhenNoMatchingTagsInText() {
        setupGlobalDelaySetting(true);
        createAndAddTag("Bold", "{B}", "CTRL+B", 100, true); // Tag exists but not used in text

        List<InputAction> actions = textTagFormatter.parseTextToActions(context, "Just plain text.");

        assertNotNull(actions);
        assertEquals("Should only be one TypeTextAction", 1, actions.size());
        assertTrue("Action should be TypeTextAction", actions.get(0) instanceof TypeTextAction);
        for (InputAction action : actions) {
            assertFalse("Should be no SendKeystrokesAction when no tags match", action instanceof SendKeystrokesAction);
        }
    }

    @Test
    public void testMixedDelays_CorrectlyApplied() {
        setupGlobalDelaySetting(true);
        createAndAddTag("Bold", "{B}", "CTRL+B", 100, true);
        createAndAddTag("Italic", "{I}", "CTRL+I", 0, true);
        createAndAddTag("Underline", "{U}", "CTRL+U", 50, true);

        List<InputAction> actions = textTagFormatter.parseTextToActions(context, "{B}Bold{B} {I}Italic{I} {U}Underline{U}");

        assertNotNull(actions);
        // Expected actions: SK(B,100), TT(Bold), SK(B,100), TT( ), SK(I,0), TT(Italic), SK(I,0), TT( ), SK(U,50), TT(Underline), SK(U,50)
        // Let's verify the delays for the first occurrence of each tag's keystroke action
        int boldDelay = -1, italicDelay = -1, underlineDelay = -1;

        for (InputAction action : actions) {
            if (action instanceof SendKeystrokesAction) {
                SendKeystrokesAction skAction = (SendKeystrokesAction) action;
                if ("CTRL+B".equals(skAction.getKeystrokeSequence()) && boldDelay == -1) {
                    boldDelay = skAction.getDelayMs();
                } else if ("CTRL+I".equals(skAction.getKeystrokeSequence()) && italicDelay == -1) {
                    italicDelay = skAction.getDelayMs();
                } else if ("CTRL+U".equals(skAction.getKeystrokeSequence()) && underlineDelay == -1) {
                    underlineDelay = skAction.getDelayMs();
                }
            }
        }
        assertEquals("Bold tag {B} delay incorrect", 100, boldDelay);
        assertEquals("Italic tag {I} delay incorrect", 0, italicDelay);
        assertEquals("Underline tag {U} delay incorrect", 50, underlineDelay);
    }

    @Test
    public void testInactiveTag_IsNotProcessed() {
        setupGlobalDelaySetting(true);
        createAndAddTag("Bold", "{B}", "CTRL+B", 100, false); // Tag is inactive

        List<InputAction> actions = textTagFormatter.parseTextToActions(context, "Hello {B}World{B}");

        assertNotNull(actions);
        for (InputAction action : actions) {
            assertFalse("No SendKeystrokesAction should be present for an inactive tag", action instanceof SendKeystrokesAction);
        }
        // Expecting "Hello {B}World{B}" to be treated as plain text
        assertEquals(1, actions.size());
        assertTrue(actions.get(0) instanceof TypeTextAction);
        assertEquals("Hello {B}World{B}", ((TypeTextAction)actions.get(0)).getText());
    }
}
