package com.drgraff.speakkey.formattingtags;

import android.content.Context;
import com.drgraff.speakkey.formattingtags.FormattingTag;
import com.drgraff.speakkey.formattingtags.FormattingTagManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class FormattingTagManagerTest {

    private FormattingTagManager tagManager;
    private Context context;

    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.getApplication();
        tagManager = new FormattingTagManager(context);
        tagManager.open(); // Open in-memory database

        // Clean up tags before each test to ensure a fresh state
        List<FormattingTag> allTags = tagManager.getAllTags();
        for (FormattingTag tag : allTags) {
            tagManager.deleteTag(tag.getId());
        }
    }

    @After
    public void tearDown() throws Exception {
        if (tagManager != null && tagManager.isOpen()) {
            tagManager.close();
        }
    }

    @Test
    public void testAddAndGetTag_WithSpecificDelay() {
        FormattingTag newTag = new FormattingTag("Test Tag", "{test}", "CTRL+T", true, 250);
        long id = tagManager.addTag(newTag);
        assertTrue("Failed to add tag, ID should not be -1", id != -1);
        newTag.setId(id); // Set the ID on the original object for completeness

        FormattingTag retrievedTag = tagManager.getTag(id);
        assertNotNull("Retrieved tag should not be null", retrievedTag);
        assertEquals("Name should match", "Test Tag", retrievedTag.getName());
        assertEquals("Opening tag text should match", "{test}", retrievedTag.getOpeningTagText());
        assertEquals("Keystroke sequence should match", "CTRL+T", retrievedTag.getKeystrokeSequence());
        assertTrue("Tag should be active", retrievedTag.isActive());
        assertEquals("DelayMs should match the value set", 250, retrievedTag.getDelayMs());
    }

    @Test
    public void testAddAndGetTag_WithZeroDelay() {
        FormattingTag newTag = new FormattingTag("Zero Delay Tag", "{zero}", "CTRL+Z", true, 0);
        long id = tagManager.addTag(newTag);
        assertTrue("Failed to add tag with zero delay", id != -1);
        newTag.setId(id);

        FormattingTag retrievedTag = tagManager.getTag(id);
        assertNotNull("Retrieved tag with zero delay should not be null", retrievedTag);
        assertEquals("Name should match", "Zero Delay Tag", retrievedTag.getName());
        assertEquals("DelayMs should be 0", 0, retrievedTag.getDelayMs());
    }

    @Test
    public void testUpdateTag_ChangesDelay() {
        FormattingTag tag = new FormattingTag("Update Test", "{upd}", "ALT+U", true, 50);
        long id = tagManager.addTag(tag);
        assertTrue("Failed to add tag for update test", id != -1);
        tag.setId(id);

        // Update the delay
        tag.setDelayMs(300);
        int updatedRows = tagManager.updateTag(tag);
        assertEquals("Number of updated rows should be 1", 1, updatedRows);

        FormattingTag retrievedTag = tagManager.getTag(id);
        assertNotNull("Retrieved tag after update should not be null", retrievedTag);
        assertEquals("DelayMs should be updated to 300", 300, retrievedTag.getDelayMs());
    }

    @Test
    public void testGetActiveTags_ReturnsTagsWithCorrectDelay() {
        tagManager.addTag(new FormattingTag("Active With Delay", "{ad}", "CTRL+A", true, 120));
        tagManager.addTag(new FormattingTag("Active No Delay", "{an}", "CTRL+N", true, 0));
        tagManager.addTag(new FormattingTag("Inactive With Delay", "{id}", "CTRL+I", false, 150));

        List<FormattingTag> activeTags = tagManager.getActiveTags();
        assertEquals("Should be 2 active tags", 2, activeTags.size());

        boolean foundActiveWithDelay = false;
        boolean foundActiveNoDelay = false;

        for (FormattingTag tag : activeTags) {
            if ("Active With Delay".equals(tag.getName())) {
                assertEquals("Delay for 'Active With Delay' should be 120", 120, tag.getDelayMs());
                foundActiveWithDelay = true;
            } else if ("Active No Delay".equals(tag.getName())) {
                assertEquals("Delay for 'Active No Delay' should be 0", 0, tag.getDelayMs());
                foundActiveNoDelay = true;
            }
        }
        assertTrue("Did not find 'Active With Delay' tag", foundActiveWithDelay);
        assertTrue("Did not find 'Active No Delay' tag", foundActiveNoDelay);
    }
}
