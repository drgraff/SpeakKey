package com.inputstick.api.hid;

public class KeyboardReport extends HIDReport {

        private byte[] mReport;

        public KeyboardReport(byte modifier, byte key) {
                mReport = new byte[9];
                mReport[0] = REPORT_ID_KEYBOARD; //reportID
                mReport[1] = modifier; //modifier
                mReport[2] = 0; //reserved
                mReport[3] = key; //key1
                mReport[4] = 0; //key2
                mReport[5] = 0; //key3
                mReport[6] = 0; //key4
                mReport[7] = 0; //key5
                mReport[8] = 0; //key6
        }

        public KeyboardReport(byte modifier, byte key0, byte key1, byte key2, byte key3, byte key4, byte key5) {
                mReport = new byte[9];
                mReport[0] = REPORT_ID_KEYBOARD; //reportID
                mReport[1] = modifier; //modifier
                mReport[2] = 0; //reserved
                mReport[3] = key0; //key1
                mReport[4] = key1; //key2
                mReport[5] = key2; //key3
                mReport[6] = key3; //key4
                mReport[7] = key4; //key5
                mReport[8] = key5; //key6
        }

        @Override
        public byte[] getBytes() {
                return mReport;
        }

        @Override
        public byte getBytesCnt() {
                return (byte)mReport.length;
        }

        @Override
        public byte getReportID() {
                return REPORT_ID_KEYBOARD;
        }

}
