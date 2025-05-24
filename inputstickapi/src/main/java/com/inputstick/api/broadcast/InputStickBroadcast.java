package com.inputstick.api.broadcast;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import com.inputstick.api.DownloadDialog;
import com.inputstick.api.Util;


public class InputStickBroadcast {

        private static boolean AUTO_SUPPORT_CHECK;

        public static final String PARAM_REQUEST =              "REQUEST";
        public static final String PARAM_RELEASE =              "RELEASE";
        public static final String PARAM_CLEAR =                "CLEAR";

        public static final String PARAM_TEXT =                 "TEXT";
        public static final String PARAM_LAYOUT =               "LAYOUT";
        public static final String PARAM_MULTIPLIER =   "MULTIPLIER";
        public static final String PARAM_KEY =                  "KEY";
        public static final String PARAM_MODIFIER =     "MODIFIER";
        public static final String PARAM_REPORT_KEYB =  "REPORT_KEYB";
        public static final String PARAM_REPORT_EMPTY = "REPORT_EMPTY";

        public static final String PARAM_REPORT_MOUSE = "REPORT_MOUSE";
        public static final String PARAM_MOUSE_BUTTONS ="MOUSE_BUTTONS";
        public static final String PARAM_MOUSE_CLICKS = "MOUSE_CLICKS";

        public static final String PARAM_CONSUMER =     "CONSUMER";

        public static final String PARAM_REPORT_TOUCH = "REPORT_TOUCHSCREEN";
        public static final String PARAM_TOUCH_CLICKS = "TOUCH_CLICKS";
        public static final String PARAM_TOUCH_X =              "TOUCH_X";
        public static final String PARAM_TOUCH_Y =              "TOUCH_Y";

        public static boolean isSupported(Context ctx, boolean allowMessages) {
                PackageInfo pInfo;
                try {
                        pInfo = ctx.getPackageManager().getPackageInfo("com.inputstick.apps.inputstickutility", 0);
                        if (pInfo.versionCode < 11) {
                                if (allowMessages) {
                                        DownloadDialog.getDialog(ctx, DownloadDialog.NOT_UPDATED).show();
                                }
                                return false;
                        } else {
                                return true;
                        }
                } catch (NameNotFoundException e) {
                        if (allowMessages) {
                                DownloadDialog.getDialog(ctx, DownloadDialog.NOT_INSTALLED).show();
                        }
                        return false;
                }
        }

        public static void setAutoSupportCheck(boolean enabled) {
                AUTO_SUPPORT_CHECK = enabled;
        }

        public static void requestConnection(Context ctx) {
                Intent intent = new Intent();
                intent.putExtra(PARAM_REQUEST, true);
                send(ctx, intent);
        }

        public static void releaseConnection(Context ctx) {
                Intent intent = new Intent();
                intent.putExtra(PARAM_RELEASE, true);
                send(ctx, intent);
        }

        public static void clearQueue(Context ctx) {
                Intent intent = new Intent();
                intent.putExtra(PARAM_CLEAR, true);
                send(ctx, intent);
        }

        public static void type(Context ctx, String text) {
                type(ctx, text, null, 1);
        }

        public static void type(Context ctx, String text, String layoutCode) {
                type(ctx, text, layoutCode, 1);
        }

        public static void type(Context ctx, String text, String layoutCode, int multiplier) {
                Intent intent = new Intent();
                intent.putExtra(PARAM_TEXT, text);
                if (layoutCode != null) {
                        intent.putExtra(PARAM_LAYOUT, layoutCode);
                }
                if (multiplier > 1) {
                        intent.putExtra(PARAM_MULTIPLIER, multiplier);
                }
                send(ctx, intent);
        }

        public static void pressAndRelease(Context ctx, byte modifiers, byte key) {
                pressAndRelease(ctx, modifiers, key, 1);
        }

        public static void pressAndRelease(Context ctx, byte modifiers, byte key, int multiplier) {
                Intent intent = new Intent();
                intent.putExtra(PARAM_MODIFIER, modifiers);
                intent.putExtra(PARAM_KEY, key);
                if (multiplier > 1) {
                        intent.putExtra(PARAM_MULTIPLIER, multiplier);
                }
                send(ctx, intent);
        }

        public static void keyboardReport(Context ctx, byte[] report, boolean addEmptyReport) {
                Intent intent = new Intent();
                intent.putExtra(PARAM_REPORT_KEYB, report);
                if (addEmptyReport) {
                        intent.putExtra(PARAM_REPORT_EMPTY, true);
                }
                send(ctx, intent);
        }

        public static void keyboardReport(Context ctx, byte modifiers, byte key1, byte key2, byte key3, byte key4, byte key5, byte key6, boolean addEmptyReport) {
                byte[] report = new byte[8];
                report[0] = modifiers;
                report[2] = key1;
                report[3] = key2;
                report[4] = key3;
                report[5] = key4;
                report[6] = key5;
                report[7] = key6;
                keyboardReport(ctx, report, addEmptyReport);
        }

        public static void mouseReport(Context ctx, byte[] report) {
                Intent intent = new Intent();
                intent.putExtra(PARAM_REPORT_MOUSE, report);
                send(ctx, intent);
        }

        public static void mouseReport(Context ctx, byte buttons, byte dx, byte dy, byte scroll) {
                byte[] report = new byte[4];
                report[0] = buttons;
                report[1] = dx;
                report[2] = dy;
                report[3] = scroll;
                mouseReport(ctx, report);
        }

        public static void mouseClick(Context ctx, byte buttons, int n) {
                Intent intent = new Intent();
                intent.putExtra(PARAM_MOUSE_BUTTONS, buttons);
                intent.putExtra(PARAM_MOUSE_CLICKS, n);
                send(ctx, intent);
        }

        public static void mouseMove(Context ctx, byte dx, byte dy) {
                mouseReport(ctx, (byte)0x00, dx, dy, (byte)0x00);
        }

        public static void mouseScroll(Context ctx, byte scroll) {
                mouseReport(ctx, (byte)0x00, (byte)0x00, (byte)0x00, scroll);
        }

        public static void consumerControlAction(Context ctx, int action) {
                Intent intent = new Intent();
                intent.putExtra(PARAM_CONSUMER, action);
                send(ctx, intent);
        }

        private static void send(Context ctx, Intent intent) {
                intent.setAction("com.inputstick.apps.inputstickutility.HID");
                intent.setClassName("com.inputstick.apps.inputstickutility", "com.inputstick.apps.inputstickutility.service.HIDReceiver");
                if (AUTO_SUPPORT_CHECK) {
                        if (isSupported(ctx, true)) {
                                ctx.sendBroadcast(intent);
                        }
                } else {
                        ctx.sendBroadcast(intent);
                }
        }

        public static void touchScreenReport(Context ctx, byte[] report) {
                Intent intent = new Intent();
                intent.putExtra(PARAM_REPORT_TOUCH, report);
                send(ctx, intent);
        }

        public static void touchScreenReport(Context ctx, boolean tipSwitch, boolean inRange, int x, int y) {
                byte[] report = new byte[6];
                report[0] = 0x04;
                if (tipSwitch) {
                        report[1] = 0x01;
                }
                if (inRange) {
                        report[1] += 0x02;
                }
                report[2] = Util.getLSB(x);
                report[3] = Util.getMSB(x);
                report[4] = Util.getLSB(y);
                report[5] = Util.getMSB(y);
                touchScreenReport(ctx, report);
        }

        public static void touchScreenMove(Context ctx, int x, int y) {
                touchScreenReport(ctx, false, true, x, y);
        }

        public static void touchScreenClick(Context ctx, int n, int x, int y) {
                Intent intent = new Intent();
                intent.putExtra(PARAM_TOUCH_CLICKS, n);
                intent.putExtra(PARAM_TOUCH_X, x);
                intent.putExtra(PARAM_TOUCH_Y, y);
                send(ctx, intent);
        }

        public static void touchScreenGoOutOfRange(Context ctx) {
                touchScreenReport(ctx, false, false, 0, 0);
        }
}
