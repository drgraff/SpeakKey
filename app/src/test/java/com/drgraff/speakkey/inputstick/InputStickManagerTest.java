package com.drgraff.speakkey.inputstick;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.drgraff.speakkey.R;
import com.drgraff.speakkey.utils.AppLogManager;
import com.inputstick.api.broadcast.InputStickBroadcast;
import com.inputstick.api.hid.HIDKeycodes;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import java.util.Collections;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(RobolectricTestRunner.class)
@PrepareForTest({
        InputStickBroadcast.class,
        Thread.class,
        TextTagFormatter.class,
        PreferenceManager.class,
        AppLogManager.class,
        Log.class // Added Log to prepare for test
})
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
public class InputStickManagerTest {

    @Mock
    private SharedPreferences mockSharedPreferences;
    @Mock
    private SharedPreferences.Editor mockEditor;
    @Mock
    private TextTagFormatter mockTextTagFormatter;
    @Mock
    private AppLogManager mockAppLogManager;

    private Context context;
    private InputStickManager inputStickManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        ShadowLog.stream = System.out; // Optional: to see Robolectric logs

        context = RuntimeEnvironment.getApplication();

        // Mock static PreferenceManager
        PowerMockito.mockStatic(PreferenceManager.class);
        when(PreferenceManager.getDefaultSharedPreferences(any(Context.class))).thenReturn(mockSharedPreferences);

        // Configure SharedPreferences mocks
        when(mockSharedPreferences.getBoolean(eq("pref_inputstick_format_tags_enabled"), anyBoolean())).thenReturn(true);
        // For TextTagFormatter's internal check (though TextTagFormatter itself is mocked)
        when(mockSharedPreferences.getBoolean(eq(context.getString(R.string.pref_key_formatting_tag_delay_enabled)), anyBoolean())).thenReturn(true);
        when(mockSharedPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor);
        when(mockEditor.commit()).thenReturn(true);


        // Mock static AppLogManager
        PowerMockito.mockStatic(AppLogManager.class);
        when(AppLogManager.getInstance()).thenReturn(mockAppLogManager);

        // Mock static Log methods (optional, but good practice if they are called directly)
        PowerMockito.mockStatic(Log.class);
        when(Log.d(anyString(), anyString())).thenReturn(0);
        when(Log.e(anyString(), anyString())).thenReturn(0);
        when(Log.e(anyString(), anyString(), any(Throwable.class))).thenReturn(0);
        when(Log.w(anyString(), anyString())).thenReturn(0);
        when(Log.i(anyString(), anyString())).thenReturn(0);


        // Mock TextTagFormatter constructor
        PowerMockito.whenNew(TextTagFormatter.class).withAnyArguments().thenReturn(mockTextTagFormatter);

        // Mock static InputStickBroadcast methods
        PowerMockito.mockStatic(InputStickBroadcast.class);
        PowerMockito.doNothing().when(InputStickBroadcast.class, "type", any(Context.class), anyString(), anyString());
        PowerMockito.doNothing().when(InputStickBroadcast.class, "pressAndRelease", any(Context.class), any(Byte.class), any(Byte.class));


        // Mock static Thread.sleep
        PowerMockito.mockStatic(Thread.class);
        PowerMockito.doNothing().when(Thread.class, "sleep", anyLong());

        inputStickManager = new InputStickManager(context);
    }

    @Test
    public void typeText_withSendKeystrokesAction_appliesDelayBeforeAndAfter() throws Exception {
        // Arrange
        final int expectedDelay = 50;
        final String keystrokeSequence = "CTRL_LEFT+KEY_B";
        SendKeystrokesAction testAction = new SendKeystrokesAction(keystrokeSequence, expectedDelay);

        when(mockTextTagFormatter.parseTextToActions(any(Context.class), anyString()))
                .thenReturn(Collections.singletonList(testAction));

        // Act
        inputStickManager.typeText("some text with tag");
        // Ensure background tasks are executed. Robolectric's scheduler should handle this for single-threaded executors.
        // If InputStickManager used a more complex executor, more advanced handling might be needed.
        org.robolectric.Robolectric.flushBackgroundThreadScheduler(); // Process tasks on the background executor

        // Assert
        InOrder inOrder = inOrder(Thread.class, InputStickBroadcast.class, mockAppLogManager); // Include AppLogManager for log verification

        // Verify "BEFORE delay" log
        inOrder.verify(mockAppLogManager).addEntry(eq("DEBUG"), anyString(), eq("Background: Applying BEFORE delay: " + expectedDelay + "ms for " + keystrokeSequence));
        // Verify first Thread.sleep call
        inOrder.verify(Thread.class);
        Thread.sleep(expectedDelay);

        // Verify InputStickBroadcast.pressAndRelease call
        inOrder.verify(InputStickBroadcast.class);
        InputStickBroadcast.pressAndRelease(any(Context.class), eq(HIDKeycodes.CTRL_LEFT), eq(HIDKeycodes.KEY_B));
        
        // Verify "AFTER delay" log
        inOrder.verify(mockAppLogManager).addEntry(eq("DEBUG"), anyString(), eq("Background: Applying AFTER delay: " + expectedDelay + "ms for " + keystrokeSequence));
        // Verify second Thread.sleep call
        inOrder.verify(Thread.class);
        Thread.sleep(expectedDelay);

        // Verify overall calls if needed (though inOrder is more specific)
        PowerMockito.verifyStatic(Thread.class, times(2));
        Thread.sleep(expectedDelay);

        PowerMockito.verifyStatic(InputStickBroadcast.class, times(1));
        InputStickBroadcast.pressAndRelease(any(Context.class), eq(HIDKeycodes.CTRL_LEFT), eq(HIDKeycodes.KEY_B));
    }
}
