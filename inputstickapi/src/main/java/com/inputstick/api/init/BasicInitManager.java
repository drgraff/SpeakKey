package com.inputstick.api.init;

import com.inputstick.api.ConnectionManager;
import com.inputstick.api.InputStickError;
import com.inputstick.api.Packet;
import com.inputstick.api.Util;
import com.inputstick.api.security.InputStickSecurity;

public class BasicInitManager implements InitManager {

        private static final int MAX_PACKET_SIZE = 64;

        private static final int STATE_INIT_NONE = 0;
        private static final int STATE_INIT_CHALLENGE_SENT = 1;
        private static final int STATE_INIT_RESPONSE_SENT = 2;
        private static final int STATE_INIT_DONE = 3;
        private static final int STATE_INIT_ERROR = 4;

        private byte[] mKey;
        private ConnectionManager mConnectionManager;

        private int mInitState;
        private int mErrorCode;

        public BasicInitManager(byte[] key) {
                this.mKey = key;
                mInitState = STATE_INIT_NONE;
        }

        @Override
        public void setConnectionManager(ConnectionManager connectionManager) {
                mConnectionManager = connectionManager;
        }

        @Override
        public void onConnected() {
                mInitState = STATE_INIT_NONE;
                mErrorCode = InputStickError.ERROR_NONE;
                Packet p = new Packet(true, Packet.CMD_GET_CHALLENGE);
                mConnectionManager.sendPacket(p);
                mInitState = STATE_INIT_CHALLENGE_SENT;
        }

        @Override
        public void onDisconnected() {
                mInitState = STATE_INIT_NONE;
        }

        @Override
        public void onPacketRx(byte[] data) {
                Packet p = new Packet(data);
                byte cmd = p.getCommand();

                if (mInitState == STATE_INIT_CHALLENGE_SENT) {
                        if (cmd == Packet.CMD_CHALLENGE) {
                                byte[] payload = p.getPayload();
                                if (payload != null) {
                                        if (payload.length == 16) {
                                                byte[] response = InputStickSecurity.getResponse(mKey, payload);
                                                if (response == null) {
                                                        mErrorCode = InputStickError.ERROR_INIT_CHALLENGE;
                                                        mInitState = STATE_INIT_ERROR;
                                                } else {
                                                        Packet resp = new Packet(true, Packet.CMD_AUTH_RESPONSE, response);
                                                        mConnectionManager.sendPacket(resp);
                                                        mInitState = STATE_INIT_RESPONSE_SENT;
                                                }
                                        } else {
                                                mErrorCode = InputStickError.ERROR_INIT_CHALLENGE;
                                                mInitState = STATE_INIT_ERROR;
                                        }
                                } else {
                                        mErrorCode = InputStickError.ERROR_INIT_CHALLENGE;
                                        mInitState = STATE_INIT_ERROR;
                                }
                        } else {
                                mErrorCode = InputStickError.ERROR_INIT_CHALLENGE;
                                mInitState = STATE_INIT_ERROR;
                        }
                } else if (mInitState == STATE_INIT_RESPONSE_SENT) {
                        if (cmd == Packet.CMD_ERROR) {
                                byte[] payload = p.getPayload();
                                if (payload != null) {
                                        if (payload.length > 0) {
                                                if (payload[0] == Packet.CMD_AUTH_RESPONSE) { //error response to CMD_AUTH_RESPONSE
                                                        mErrorCode = InputStickError.ERROR_INIT_AUTH;
                                                        mInitState = STATE_INIT_ERROR;
                                                } else { //other error
                                                        mErrorCode = InputStickError.ERROR_FW_UNKNOWN_CMD_RESPONSE;
                                                        mInitState = STATE_INIT_ERROR;
                                                }
                                        } else { //other error
                                                mErrorCode = InputStickError.ERROR_FW_UNKNOWN_CMD_RESPONSE;
                                                mInitState = STATE_INIT_ERROR;
                                        }
                                } else { //other error
                                        mErrorCode = InputStickError.ERROR_FW_UNKNOWN_CMD_RESPONSE;
                                        mInitState = STATE_INIT_ERROR;
                                }
                        } else {
                                mInitState = STATE_INIT_DONE;
                                onInitReady();
                        }
                }

                if (mInitState == STATE_INIT_ERROR) {
                        mConnectionManager.disconnect();
                } else {
                        if (mInitState == STATE_INIT_DONE) {
                                //pass other packets to HID
                                InputStickHID.getInstance().onInputStickData(data);
                        }
                }
        }

        @Override
        public void onInitReady() {
                mConnectionManager.stateNotify(ConnectionManager.STATE_READY);
        }

        @Override
        public boolean isReady() {
                if (mInitState == STATE_INIT_DONE) {
                        return true;
                } else {
                        return false;
                }
        }

        @Override
        public boolean isError() {
                if (mInitState == STATE_INIT_ERROR) {
                        return true;
                } else {
                        return false;
                }
        }

        @Override
        public int getErrorCode() {
                return mErrorCode;
        }

}
