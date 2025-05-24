package com.inputstick.api.hid;

public class RawHIDReport extends HIDReport {

        private byte[] mReport;

        public RawHIDReport(byte[] data) {
                if (data.length > 63) {
                        throw new IllegalArgumentException("Raw HID report can not be longer than 63 bytes!");
                }
                mReport = new byte[data.length + 1];
                mReport[0] = REPORT_ID_RAW;
                for (int i = 0; i < data.length; i++) {
                        mReport[i+1] = data[i];
                }
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
                return REPORT_ID_RAW;
        }

}
