package com.inputstick.api.basic;

import com.inputstick.api.hid.ConsumerReport;
import com.inputstick.api.hid.HIDTransaction;

public class InputStickConsumer {

        public static final int CONSUMER_FAST_FORWARD = 0x0001;
        public static final int CONSUMER_REWIND = 0x0002;
        public static final int CONSUMER_SCAN_NEXT_TRACK = 0x0004;
        public static final int CONSUMER_SCAN_PREV_TRACK = 0x0008;
        public static final int CONSUMER_STOP = 0x0010;
        public static final int CONSUMER_PLAY_PAUSE = 0x0020;
        public static final int CONSUMER_MUTE = 0x0040;
        public static final int CONSUMER_VOLUME_INCREMENT = 0x0080;
        public static final int CONSUMER_VOLUME_DECREMENT = 0x0100;

        public static final int CONSUMER_WWW_HOME = 0x0200;
        public static final int CONSUMER_WWW_BACK = 0x0400;
        public static final int CONSUMER_WWW_FORWARD = 0x0800;
        public static final int CONSUMER_WWW_STOP = 0x1000;
        public static final int CONSUMER_WWW_REFRESH = 0x2000;
        public static final int CONSUMER_WWW_BOOKMARKS = 0x4000;

        public static final int SYSTEM_POWER_DOWN = 0x00010000;
        public static final int SYSTEM_SLEEP = 0x00020000;
        public static final int SYSTEM_WAKE_UP = 0x00040000;

        private InputStickConsumer() {
        }

        public static void control(int action) {
                HIDTransaction t = new HIDTransaction();
                t.addReport(new ConsumerReport(action));
                t.addReport(new ConsumerReport(0));
                InputStickHID.addConsumerTransaction(t, true);
        }

}
