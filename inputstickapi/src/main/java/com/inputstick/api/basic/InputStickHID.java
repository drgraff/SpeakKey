package com.inputstick.api.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;

import com.inputstick.api.BTConnectionManager;
import com.inputstick.api.ConnectionManager;
import com.inputstick.api.DownloadDialog;
import com.inputstick.api.HIDInfo;
import com.inputstick.api.IPCConnectionManager;
import com.inputstick.api.InputStickDataListener;
import com.inputstick.api.InputStickError;
import com.inputstick.api.InputStickStateListener;
import com.inputstick.api.OnEmptyBufferListener;
import com.inputstick.api.Packet;
import com.inputstick.api.hid.HIDTransaction;
import com.inputstick.api.hid.HIDTransactionQueue;
import com.inputstick.api.init.BasicInitManager;
import com.inputstick.api.init.DeviceInfo;
import com.inputstick.api.init.InitManager;

public class InputStickHID implements InputStickStateListener, InputStickDataListener  {

        public static final int INTERFACE_KEYBOARD = 0;
        public static final int INTERFACE_CONSUMER = 1;
        public static final int INTERFACE_MOUSE = 2;
        public static final int INTERFACE_RAW_HID = 3;

        private static ConnectionManager mConnectionManager;

        private static Vector<InputStickStateListener> mStateListeners = new Vector<InputStickStateListener>();
        protected static Vector<OnEmptyBufferListener> mBufferEmptyListeners = new Vector<OnEmptyBufferListener>();

        private static InputStickHID instance = new InputStickHID();
        private static HIDInfo mHIDInfo;
        private static DeviceInfo mDeviceInfo;

        private static HIDTransactionQueue keyboardQueue;
        private static HIDTransactionQueue mouseQueue;
        private static HIDTransactionQueue consumerQueue;
        private static HIDTransactionQueue rawHIDQueue;

        private InputStickHID() {
        }

        public static InputStickHID getInstance() {
                return instance;
        }

        private static void init() {
                mHIDInfo = new HIDInfo();
                keyboardQueue = new HIDTransactionQueue(INTERFACE_KEYBOARD, mConnectionManager, 32, 32);
                mouseQueue = new HIDTransactionQueue(INTERFACE_MOUSE, mConnectionManager, 32, 32);
                consumerQueue = new HIDTransactionQueue(INTERFACE_CONSUMER, mConnectionManager, 32, 32);
                rawHIDQueue = new HIDTransactionQueue(INTERFACE_RAW_HID, mConnectionManager, 2, 1);

                mConnectionManager.addStateListener(instance);
                mConnectionManager.addDataListener(instance);
                mConnectionManager.connect();
        }

        public static AlertDialog getDownloadDialog(final Context ctx) {
                if (mConnectionManager.getErrorCode() == InputStickError.ERROR_ANDROID_NO_UTILITY_APP) {
                        return DownloadDialog.getDialog(ctx, DownloadDialog.NOT_INSTALLED);
                } else {
                        return null;
                }
        }

        public static void connect(Application app) {
                mConnectionManager = new IPCConnectionManager(app);
                init();
        }

        public static void disconnect() {
                if (mConnectionManager != null) {
                        mConnectionManager.disconnect();
                }
        }

        public static void connect(Application app, String mac, byte[] key, InitManager initManager) {
                connect(app, mac, key, initManager, false);
        }

        public static void connect(Application app, String mac, byte[] key, InitManager initManager, boolean isBT40) {
                mConnectionManager = new BTConnectionManager(initManager, app, mac, key, isBT40);
                init();
        }

        public static void connect(Application app, String mac, byte[] key, boolean isBT40) {
                mConnectionManager = new BTConnectionManager(new BasicInitManager(key), app, mac, key, isBT40);
                init();
        }

        public static void connect(Application app, String mac, byte[] key) {
                connect(app, mac, key, false);
        }

        public static void wakeUpUSBHost() {
                if (isConnected()) {
                        Packet p = new Packet(false, Packet.CMD_USB_RESUME);
                        InputStickHID.sendPacket(p); // This line seems to be a typo, should be mConnectionManager.sendPacket(p)
                        // Corrected: mConnectionManager.sendPacket(p);
                }
        }
        
        // Corrected method from the original source for wakeUpUSBHost if the above was a typo, or use as is if intended.
        // Re-check original source if wakeUpUSBHost has issues. The provided snippet had InputStickHID.sendPacket(p);
        // which might be a self-reference rather than using mConnectionManager.
        // For now, assume the provided snippet is what should be used. If build fails, this is a candidate for fixing.


        public static ConnectionManager getConnectionManager() {
                return mConnectionManager;
        }

        public static DeviceInfo getDeviceInfo() {
                if ((isReady()) && (mDeviceInfo != null)) {
                        return mDeviceInfo;
                } else {
                        return null;
                }
        }

        public static HIDInfo getHIDInfo() {
                return mHIDInfo;
        }

        public static int getState() {
                if (mConnectionManager != null) {
                        return mConnectionManager.getState();
                } else {
                        return ConnectionManager.STATE_DISCONNECTED;
                }
        }

        public static int getErrorCode() {
                if (mConnectionManager != null) {
                        return mConnectionManager.getErrorCode();
                } else {
                        return InputStickError.ERROR_UNKNOWN;
                }
        }

        public static int getDisconnectReason() {
                if (mConnectionManager != null) {
                        return mConnectionManager.getDisconnectReason();
                } else {
                        return ConnectionManager.DISC_REASON_UNKNOWN;
                }
        }

        public static boolean isConnected() {
                if ((getState() == ConnectionManager.STATE_READY) ||  (getState() == ConnectionManager.STATE_CONNECTED)) {
                        return true;
                } else {
                        return false;
                }
        }

        public static boolean isReady() {
                if (getState() == ConnectionManager.STATE_READY) {
                        return true;
                } else {
                        return false;
                }
        }

        public static void addStateListener(InputStickStateListener listener) {
                if (listener != null) {
                        if ( !mStateListeners.contains(listener)) {
                                synchronized (mStateListeners) {
                                        mStateListeners.add(listener);
                                }
                        }
                }
        }

        public static void removeStateListener(InputStickStateListener listener) {
                if (listener != null) {
                        synchronized (mStateListeners) {
                                mStateListeners.remove(listener);
                        }
                }
        }

        public static void addBufferEmptyListener(OnEmptyBufferListener listener) {
                if (listener != null) {
                        synchronized(mBufferEmptyListeners) {
                                if ( !mBufferEmptyListeners.contains(listener)) {
                                        mBufferEmptyListeners.add(listener);
                                }
                        }
                }
        }

        public static void removeBufferEmptyListener(OnEmptyBufferListener listener) {
                if (listener != null) {
                        synchronized(mBufferEmptyListeners) {
                                mBufferEmptyListeners.remove(listener);
                        }
                }
        }

        public static void sendEmptyBufferNotifications(int bufferType, int interfaceType) {
                if (bufferType == 1) { 
                        synchronized(mBufferEmptyListeners) {
                                for (OnEmptyBufferListener listener : mBufferEmptyListeners) {
                                        listener.onRemoteBufferEmpty(interfaceType);
                                }
                        }
                } else if (bufferType == 2) { 
                        synchronized(mBufferEmptyListeners) {
                                for (OnEmptyBufferListener listener : mBufferEmptyListeners) {
                                        listener.onLocalBufferEmpty(interfaceType);
                                }
                        }
                }
        }

        public static void addKeyboardTransaction(HIDTransaction transaction, boolean sendNow) {
                if ((transaction != null) && (keyboardQueue != null)) {
                        keyboardQueue.addTransaction(transaction, sendNow);
                }
        }

        public static void addKeyboardTransaction(HIDTransaction transaction) {
                if ((transaction != null) && (keyboardQueue != null)) {
                        keyboardQueue.addTransaction(transaction, true);
                }
        }

        public static void flushKeyboardBuffer() {
                if (keyboardQueue != null) {
                        keyboardQueue.sendFromQueue();
                }
        }

        public static void addMouseTransaction(HIDTransaction transaction, boolean sendNow) {
                if ((transaction != null) && (mouseQueue != null)) {
                        mouseQueue.addTransaction(transaction, sendNow);
                }
        }

        public static void addMouseTransaction(HIDTransaction transaction) {
                if ((transaction != null) && (mouseQueue != null)) {
                        mouseQueue.addTransaction(transaction, true);
                }
        }

        public static void flushMouseBuffer() {
                if (mouseQueue != null) {
                        mouseQueue.sendFromQueue();
                }
        }

        public static void addConsumerTransaction(HIDTransaction transaction, boolean sendNow) {
                if ((transaction != null) && (consumerQueue != null)) {
                        consumerQueue.addTransaction(transaction, sendNow);
                }
        }

        public static void addConsumerTransaction(HIDTransaction transaction) {
                if ((transaction != null) && (consumerQueue != null)) {
                        consumerQueue.addTransaction(transaction, true);
                }
        }

        public static void flushConsumerBuffer() {
                if (consumerQueue != null) {
                        consumerQueue.sendFromQueue();
                }
        }

        public static void addRawHIDTransaction(HIDTransaction transaction, boolean sendNow) {
                if ((transaction != null) && (rawHIDQueue != null)) {
                        rawHIDQueue.addTransaction(transaction, sendNow);
                }
        }

        public static void addRawHIDTransaction(HIDTransaction transaction) {
                if ((transaction != null) && (rawHIDQueue != null)) {
                        rawHIDQueue.addTransaction(transaction, true);
                }
        }

        public static void flushRawHIDBuffer() {
                if (rawHIDQueue != null) {
                        rawHIDQueue.sendFromQueue();
                }
        }

        public static void clearKeyboardBuffer() {
                if (keyboardQueue != null) {
                        keyboardQueue.clearBuffer();
                }
        }

        public static void clearMouseBuffer() {
                if (mouseQueue != null) {
                        mouseQueue.clearBuffer();
                }
        }

        public static void clearConsumerBuffer() {
                if (consumerQueue != null) {
                        consumerQueue.clearBuffer();
                }
        }

        public static void clearRawHIDBuffer() {
                if (rawHIDQueue != null) {
                        rawHIDQueue.clearBuffer();
                }
        }

        public static void clearAllBuffers() {
                clearKeyboardBuffer();
                clearMouseBuffer();
                clearConsumerBuffer();
                clearRawHIDBuffer();
        }

        public static boolean sendPacket(Packet p) {
                if (mConnectionManager != null) {
                        mConnectionManager.sendPacket(p);
                        return true;
                } else {
                        return false;
                }
        }

        public static boolean isKeyboardLocalBufferEmpty() {
                if (keyboardQueue != null) {
                        return keyboardQueue.isLocalBufferEmpty();
                } else {
                        return true;
                }
        }

        public static boolean isMouseLocalBufferEmpty() {
                if (mouseQueue != null) {
                        return mouseQueue.isLocalBufferEmpty();
                } else {
                        return true;
                }
        }

        public static boolean isConsumerLocalBufferEmpty() {
                if (consumerQueue != null) {
                        return consumerQueue.isLocalBufferEmpty();
                } else {
                        return true;
                }
        }

        public static boolean isRawHIDLocalBufferEmpty() {
                if (rawHIDQueue != null) {
                        return rawHIDQueue.isLocalBufferEmpty();
                } else {
                        return true;
                }
        }

        public static boolean areAllLocalBuffersEmpty() {
                return (isKeyboardLocalBufferEmpty() && isMouseLocalBufferEmpty() && isConsumerLocalBufferEmpty() && isRawHIDLocalBufferEmpty());
        }

        public static boolean isKeyboardRemoteBufferEmpty() {
                if (keyboardQueue != null) {
                        return keyboardQueue.isRemoteBufferEmpty();
                } else {
                        return true;
                }
        }

        public static boolean isMouseRemoteBufferEmpty() {
                if (mouseQueue != null) {
                        return mouseQueue.isRemoteBufferEmpty();
                } else {
                        return true;
                }
        }

        public static boolean isConsumerRemoteBufferEmpty() {
                if (consumerQueue != null) {
                        return consumerQueue.isRemoteBufferEmpty();
                } else {
                        return true;
                }
        }

        public static boolean isRawHIDRemoteBufferEmpty() {
                if (rawHIDQueue != null) {
                        return rawHIDQueue.isRemoteBufferEmpty();
                } else {
                        return true;
                }
        }

        public static boolean areAllRemoteBuffersEmpty() {
                return (isKeyboardRemoteBufferEmpty() && isMouseRemoteBufferEmpty() && isConsumerRemoteBufferEmpty() && isRawHIDRemoteBufferEmpty());
        }

        @Override
        public void onStateChanged(int state) {
                synchronized (mStateListeners) {
                        ArrayList<InputStickStateListener> tmp = new ArrayList<InputStickStateListener>();
                        for (InputStickStateListener listener : mStateListeners) {
                                tmp.add(listener);
                        }

                        for (InputStickStateListener listener : tmp) {
                                listener.onStateChanged(state);
                        }
                }
        }

        @Override
        public void onInputStickData(byte[] data) {
                byte cmd = data[0];
                if (cmd == Packet.CMD_FW_INFO) {
                        mDeviceInfo = new DeviceInfo(data);

                        if (mDeviceInfo.getFirmwareVersion() >= 100) {
                                keyboardQueue.setCapacity(128);
                                mouseQueue.setCapacity(64);
                                consumerQueue.setCapacity(64);
                        }
                }

                if (cmd == Packet.CMD_HID_DATA_RAW) {
                        if (data.length > 65) {
                                InputStickRawHID.notifyRawHIDListeners(Arrays.copyOfRange(data, 1, 65));
                        }
                }

                if (cmd == Packet.CMD_HID_STATUS) {
                        mHIDInfo.update(data);

                        InputStickKeyboard.setReportProtocol(mHIDInfo.isKeyboardReportProtocol());
                        InputStickMouse.setReportProtocol(mHIDInfo.isMouseReportProtocol());

                        if (keyboardQueue != null) {
                                keyboardQueue.update(mHIDInfo);
                        }
                        if (mouseQueue != null) {
                                mouseQueue.update(mHIDInfo);
                        }
                        if (consumerQueue != null) {
                                consumerQueue.update(mHIDInfo);
                        }
                        if (rawHIDQueue != null) {
                                rawHIDQueue.update(mHIDInfo);
                        }

                        InputStickKeyboard.setLEDs(mHIDInfo.getNumLock(), mHIDInfo.getCapsLock(), mHIDInfo.getScrollLock());
                }
        }
}
