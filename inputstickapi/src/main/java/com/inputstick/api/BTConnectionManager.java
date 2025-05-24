package com.inputstick.api;

import java.lang.ref.WeakReference;
import android.app.Application;
import android.os.Handler;
import android.os.Message;
import com.inputstick.api.bluetooth.BTService; // Assuming this will be created later
import com.inputstick.api.init.InitManager;
import com.inputstick.api.init.InitManagerListener; // Assuming this will be created later

public class BTConnectionManager extends ConnectionManager implements InitManagerListener {

        private String mMac;
        private byte[] mKey;
        private boolean mIsBT40;

        private InitManager mInitManager;
        private Application mApp;
        protected BTService mBTService;
        private PacketManager mPacketManager; // Assuming this will be created/updated later
        private final BTHandler mBTHandler = new BTHandler(this);

    private static class BTHandler extends Handler {
        private final WeakReference<BTConnectionManager> ref;

        BTHandler(BTConnectionManager manager) {
                ref = new WeakReference<BTConnectionManager>(manager);
        }

                @Override
                public void handleMessage(Message msg) {
                        BTConnectionManager manager = ref.get();
                        if (manager != null) {
                                switch (msg.what) {
                                        case BTService.EVENT_DATA:
                                                manager.onData((byte[])msg.obj);
                                                break;
                                        case BTService.EVENT_CONNECTED:
                                                manager.onConnected();
                                                break;
                                        case BTService.EVENT_CANCELLED:
                                                manager.onDisconnected();
                                                break;
                                        case BTService.EVENT_ERROR:
                                                manager.onFailure(msg.arg1);
                                                break;
                                        default:
                                                manager.onFailure(InputStickError.ERROR_BLUETOOTH);
                                }
                        }
                }
    }

    private void onConnecting() {
        stateNotify(ConnectionManager.STATE_CONNECTING);
    }

            private void onConnected() {
                    stateNotify(ConnectionManager.STATE_CONNECTED);
                    mInitManager.onConnected();
            }

            private void onDisconnected() {
                    stateNotify(ConnectionManager.STATE_DISCONNECTED);
                    mInitManager.onDisconnected();
            }

            private void onFailure(int code) {
                    setErrorCode(code);
                    stateNotify(ConnectionManager.STATE_FAILURE);
                    disconnect();
            }

            @Override
            protected void onData(byte[] rawData) {
                    byte[] data;
                    // Assuming PacketManager handles decryption if necessary based on mKey
                    // For now, direct pass or minimal processing by a placeholder PacketManager
                    if (mPacketManager != null) {
                        data = mPacketManager.bytesToPacket(rawData); // This method needs to exist in PacketManager
                    } else {
                        data = rawData; // Fallback if PacketManager isn't fully there yet
                    }

                    if (data == null) {
                            return;
                    }

                    mInitManager.onData(data); // Changed from onPacketRx to onData
                    super.onData(data);
            }

            public BTConnectionManager(InitManager initManager, Application app, String mac, byte[] key, boolean isBT40) {
                    mInitManager = initManager;
                    mMac = mac;
                    mKey = key;
                    mApp = app;
                    mIsBT40 = isBT40;
                    // mPacketManager might be initialized here or in connect()
            }

            public BTConnectionManager(InitManager initManager, Application app, String mac, byte[] key) {
                    this(initManager, app, mac, key, false);
            }

            @Override
            public void connect() {
                    connect(false, BTService.DEFAULT_CONNECT_TIMEOUT); // DEFAULT_CONNECT_TIMEOUT in BTService
            }

            public void connect(boolean reflection, int timeout, boolean doNotAsk) {
                    resetErrorCode();
                    if (mBTService == null) {
                            mBTService = new BTService(mApp, mBTHandler); // BTService needs this constructor
                            // Initialize PacketManager here if it depends on BTService or key
                            if (mPacketManager == null) { // Ensure PacketManager is initialized
                                mPacketManager = new PacketManager(mBTService, mKey); // PacketManager needs this constructor
                            }
                            // mInitManager.init(this, mPacketManager); // InitManager.init needs this signature
                            // The line above was commented out as InitManager does not have an init method.
                            // InitManager is an interface, its methods are called by BTConnectionManager
                            // for example: mInitManager.onConnected(), mInitManager.onData() etc.
                            // The InitManager instance itself should be configured with ConnectionManager
                            // via its setConnectionManager method if needed, or it can get it via constructor.
                            if (mInitManager != null) {
                                mInitManager.setConnectionManager(this); // Pass this ConnectionManager to InitManager
                            }

                    }
                    mBTService.setConnectTimeout(timeout);
                    mBTService.enableReflection(reflection);
                    mBTService.connect(mMac, doNotAsk, mIsBT40); // BTService.connect needs this signature
                    onConnecting();
            }

            public void connect(boolean reflection, int timeout) {
                    connect(reflection, timeout, false);
            }

            @Override
            public void disconnect() {
                    if (mBTService != null) {
                            mBTService.disconnect();
                    }
            }

            public void disconnect(int failureCode) {
                    onFailure(failureCode);
            }

            public String getMac() {
                    return mMac;
            }

            public void changeKey(byte[] key) {
                    mKey = key;
                    if (mPacketManager != null) {
                        mPacketManager.changeKey(mKey); // PacketManager.changeKey needs this signature
                    }
            }

            @Override
            public void sendPacket(Packet p) {
                if (mPacketManager != null) {
                    mPacketManager.sendPacket(p);  // PacketManager.sendPacket needs this signature
                }
            }

            @Override
            public void onInitReady() {
                    stateNotify(ConnectionManager.STATE_READY);
            }

            @Override
            public void onInitNotReady() {
                    stateNotify(ConnectionManager.STATE_CONNECTED);
            }

            @Override
            public void onInitFailure(int code) {
                    onFailure(code);
            }
    }
