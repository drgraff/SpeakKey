package com.inputstick.api.hid;

import com.inputstick.api.Util;

public class ConsumerReport extends HIDReport {

        private byte[] mReport;

        public ConsumerReport(int action) {
                mReport = new byte[5];
                mReport[0] = REPORT_ID_CONSUMER; //reportID
                if ((action & 0xFFFF0000) != 0) { //system control
                        action = action >> 16;
                        mReport[1] = (byte)(action & 0xFF);
                        mReport[2] = 0;
                        mReport[3] = 0;
                        mReport[4] = 0;
                } else { //consumer control
                        mReport[1] = 0;
                        mReport[2] = (byte)(action & 0xFF);
                        mReport[3] = (byte)((action & 0xFF00) >> 8);
                        mReport[4] = 0;
                }
        }

        public ConsumerReport(boolean system, byte b1, byte b2, byte b3) {
                mReport = new byte[5];
                mReport[0] = REPORT_ID_CONSUMER; //reportID
                if (system) {
                        mReport[1] = b1; //system control code
                        mReport[2] = 0;
                        mReport[3] = 0;
                        mReport[4] = 0;
                } else {
                        mReport[1] = 0;
                        mReport[2] = b1; //consumer control code LSB
                        mReport[3] = b2; //consumer control code MSB
                        mReport[4] = 0;
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
                return REPORT_ID_CONSUMER;
        }

}
