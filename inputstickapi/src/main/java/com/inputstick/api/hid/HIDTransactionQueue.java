package com.inputstick.api.hid;

import java.util.LinkedList;

import com.inputstick.api.ConnectionManager;
import com.inputstick.api.HIDInfo;
import com.inputstick.api.Packet;
import com.inputstick.api.basic.InputStickHID;

public class HIDTransactionQueue {

        private ConnectionManager mConnectionManager;

        private LinkedList<HIDTransaction> mQueue;
        private HIDTransaction mCurrentTransaction;
        private HIDReport mCurrentReport;

        private int mInterfaceType;
        private int mLocalCapacity;
        private int mRemoteCapacity;
        private boolean mReportProtocol;

        public HIDTransactionQueue(int interfaceType, ConnectionManager connectionManager, int initialLocalCapacity, int initialRemoteCapacity) {
                mInterfaceType = interfaceType;
                mConnectionManager = connectionManager;
                mLocalCapacity = initialLocalCapacity;
                mRemoteCapacity = initialRemoteCapacity;
                mQueue = new LinkedList<HIDTransaction>();
                mReportProtocol = true;
        }

        public synchronized void addTransaction(HIDTransaction transaction, boolean sendNow) {
                if (mQueue.size() < mLocalCapacity) {
                        mQueue.add(transaction);
                        if (sendNow) {
                                sendFromQueue();
                        }
                } else {
                        //buffer full!
                }
        }

        public synchronized void sendFromQueue() {
                if (mConnectionManager.isReady()) {
                        if (mCurrentTransaction == null) {
                                if (mQueue.size() > 0) {
                                        mCurrentTransaction = mQueue.removeFirst();
                                }
                        }

                        if (mCurrentTransaction != null) {
                                if (mCurrentReport == null) {
                                        mCurrentReport = mCurrentTransaction.getNextReport();
                                }
                                while (mCurrentReport != null) {
                                        if (isRemoteBufferFull()) {
                                                return;
                                        } else {
                                                Packet p = new Packet(true, Packet.CMD_HID_DATA, mCurrentReport.getBytes());
                                                mConnectionManager.sendPacket(p);
                                                mRemoteCapacity--;
                                                mCurrentReport = mCurrentTransaction.getNextReport();
                                        }
                                }
                                if (mCurrentTransaction.isEmpty()) {
                                        mCurrentTransaction = null;
                                }
                        }
                }

                if (isLocalBufferEmpty()) {
                        InputStickHID.sendEmptyBufferNotifications(2, mInterfaceType);
                }
        }


        public synchronized void clearBuffer() {
                if (mCurrentTransaction != null) {
                        while(mCurrentTransaction.getNextReport() != null) {
                        }
                        mCurrentTransaction = null;
                }
                mQueue.clear();
        }

        public synchronized boolean isLocalBufferEmpty() {
                if ((mCurrentTransaction == null) && (mQueue.size() == 0)) {
                        return true;
                } else {
                        return false;
                }
        }

        public synchronized boolean isRemoteBufferEmpty() {
                boolean empty = false;
                switch(mInterfaceType) {
                        case InputStickHID.INTERFACE_KEYBOARD:
                                if (InputStickHID.getHIDInfo().isKeyboardReady()) {
                                        empty = true;
                                }
                                break;
                        case InputStickHID.INTERFACE_MOUSE:
                                if (InputStickHID.getHIDInfo().isMouseReady()) {
                                        empty = true;
                                }
                                break;
                        case InputStickHID.INTERFACE_CONSUMER:
                                if (InputStickHID.getHIDInfo().isConsumerReady()) {
                                        empty = true;
                                }
                                break;
                        case InputStickHID.INTERFACE_RAW_HID:
                                if (InputStickHID.getHIDInfo().isRawHIDReady()) {
                                        empty = true;
                                }
                                break;
                }
                return empty;
        }

        public synchronized boolean isRemoteBufferFull() {
                if (mRemoteCapacity <= 0) {
                        return true;
                } else {
                        return false;
                }
        }

        public synchronized void setCapacity(int capacity) {
                mLocalCapacity = capacity;
        }

        public synchronized void update(HIDInfo hidInfo) {
                switch(mInterfaceType) {
                        case InputStickHID.INTERFACE_KEYBOARD:
                                mReportProtocol = hidInfo.isKeyboardReportProtocol();
                                if (InputStickHID.getHIDInfo().isKeyboardReady()) {
                                        if (mRemoteCapacity == 0) { //was full, now is empty
                                                InputStickHID.sendEmptyBufferNotifications(1, mInterfaceType);
                                        }
                                        mRemoteCapacity = mLocalCapacity; //assume that entire buffer was sent
                                }
                                break;
                        case InputStickHID.INTERFACE_MOUSE:
                                mReportProtocol = hidInfo.isMouseReportProtocol();
                                if (InputStickHID.getHIDInfo().isMouseReady()) {
                                        if (mRemoteCapacity == 0) { //was full, now is empty
                                                InputStickHID.sendEmptyBufferNotifications(1, mInterfaceType);
                                        }
                                        mRemoteCapacity = mLocalCapacity; //assume that entire buffer was sent
                                }
                                break;
                        case InputStickHID.INTERFACE_CONSUMER:
                                if (InputStickHID.getHIDInfo().isConsumerReady()) {
                                        if (mRemoteCapacity == 0) { //was full, now is empty
                                                InputStickHID.sendEmptyBufferNotifications(1, mInterfaceType);
                                        }
                                        mRemoteCapacity = mLocalCapacity; //assume that entire buffer was sent
                                }
                                break;
                        case InputStickHID.INTERFACE_RAW_HID:
                                if (InputStickHID.getHIDInfo().isRawHIDReady()) {
                                        if (mRemoteCapacity == 0) { //was full, now is empty
                                                InputStickHID.sendEmptyBufferNotifications(1, mInterfaceType);
                                        }
                                        mRemoteCapacity = mLocalCapacity; //assume that entire buffer was sent
                                }
                                break;
                }
                sendFromQueue();
        }

        public boolean getReportProtocol() {
                return mReportProtocol;
        }

}
