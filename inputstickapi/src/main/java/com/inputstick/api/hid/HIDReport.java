package com.inputstick.api.hid;

public abstract class HIDReport {

        public static final byte REPORT_ID_KEYBOARD = 0x01;
        public static final byte REPORT_ID_MOUSE = 0x02;
        public static final byte REPORT_ID_CONSUMER = 0x03;
        public static final byte REPORT_ID_TOUCHSCREEN = 0x04;
        public static final byte REPORT_ID_GAMEPAD = 0x05;
        public static final byte REPORT_ID_RAW = 0x06;

        public abstract byte[] getBytes();
        public abstract byte getBytesCnt();
        public abstract byte getReportID();

}
