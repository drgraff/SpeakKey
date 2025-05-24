package com.inputstick.api.hid;

public class MouseReport extends HIDReport {

        private byte[] mReport;

        public MouseReport(byte buttons, byte x, byte y, byte scroll) {
                mReport = new byte[5];
                mReport[0] = REPORT_ID_MOUSE; //reportID
                mReport[1] = buttons; //buttons
                mReport[2] = x; //X
                mReport[3] = y; //Y
                mReport[4] = scroll; //scroll wheel
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
                return REPORT_ID_MOUSE;
        }

}
