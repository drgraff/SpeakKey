package com.inputstick.api.init;

import com.inputstick.api.ConnectionManager;

public interface InitManager {

        public void setConnectionManager(ConnectionManager connectionManager);
        public void onConnected();
        public void onDisconnected();
        public void onPacketRx(byte[] data);
        public void onInitReady();

        public boolean isReady();
        public boolean isError();
        public int getErrorCode();

}
