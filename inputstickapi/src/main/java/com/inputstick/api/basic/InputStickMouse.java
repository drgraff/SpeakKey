package com.inputstick.api.basic;

import com.inputstick.api.hid.HIDTransaction;
import com.inputstick.api.hid.MouseReport;

public class InputStickMouse {

        public static final byte BUTTON_LEFT = 0x01;
        public static final byte BUTTON_RIGHT = 0x02;
        public static final byte BUTTON_MIDDLE = 0x04;
        public static final byte BUTTON_BACK = 0x08;
        public static final byte BUTTON_FORWARD = 0x10;

        private static boolean mReportProtocol;

        private InputStickMouse() {
        }

        public static void click(byte button) {
                click(button, 1);
        }

        public static void click(byte button, int n) {
                HIDTransaction t = new HIDTransaction();
                for (int i = 0; i < n; i++) {
                        t.addReport(new MouseReport(button, (byte)0, (byte)0, (byte)0));
                        t.addReport(new MouseReport((byte)0, (byte)0, (byte)0, (byte)0));
                }
                InputStickHID.addMouseTransaction(t, true);
        }

        public static void move(byte x, byte y, byte scroll) {
                HIDTransaction t = new HIDTransaction();
                t.addReport(new MouseReport((byte)0, x, y, scroll));
                InputStickHID.addMouseTransaction(t, true);
        }

        public static void press(byte button) {
                HIDTransaction t = new HIDTransaction();
                t.addReport(new MouseReport(button, (byte)0, (byte)0, (byte)0));
                InputStickHID.addMouseTransaction(t, true);
        }

        public static void release(byte button) {
                HIDTransaction t = new HIDTransaction();
                t.addReport(new MouseReport((byte)0, (byte)0, (byte)0, (byte)0));
                InputStickHID.addMouseTransaction(t, true);
        }

        public static void customReport(byte buttons, byte x, byte y, byte scroll) {
                HIDTransaction t = new HIDTransaction();
                t.addReport(new MouseReport(buttons, x, y, scroll));
                InputStickHID.addMouseTransaction(t, true);
        }

        public static boolean isReportProtocol() {
                return mReportProtocol;
        }

        protected static void setReportProtocol(boolean reportProtocol) {
                mReportProtocol = reportProtocol;
        }

}
