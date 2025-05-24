package com.inputstick.api.init;

import com.inputstick.api.Util;

public class DeviceInfo {

        private int mFirmwareVersion;
        private int mHardwareVersion;
        private int mBootloaderVersion;
        private byte[] mSerialNumber;

        public DeviceInfo(byte[] data) {
                if (data.length >= 11) {
                        mFirmwareVersion = Util.getShort(data, 1);
                        mHardwareVersion = Util.getShort(data, 3);
                        mBootloaderVersion = Util.getShort(data, 5);
                        mSerialNumber = new byte[4];
                        mSerialNumber[0] = data[7];
                        mSerialNumber[1] = data[8];
                        mSerialNumber[2] = data[9];
                        mSerialNumber[3] = data[10];
                }
        }

        public int getFirmwareVersion() {
                return mFirmwareVersion;
        }

        public int getHardwareVersion() {
                return mHardwareVersion;
        }

        public int getBootloaderVersion() {
                return mBootloaderVersion;
        }

        public byte[] getSerialNumber() {
                return mSerialNumber;
        }

}
