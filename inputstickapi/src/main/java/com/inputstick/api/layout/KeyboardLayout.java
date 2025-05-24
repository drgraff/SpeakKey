package com.inputstick.api.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.inputstick.api.LayoutHelper;
import com.inputstick.api.basic.InputStickKeyboard;
import com.inputstick.api.hid.HIDKeycodes;
import com.inputstick.api.hid.HIDTransaction;
import com.inputstick.api.hid.KeyboardReport;

public abstract class KeyboardLayout {

        protected static Map<String, Class<? extends KeyboardLayout>> layouts = new HashMap<String, Class<? extends KeyboardLayout>>();
        protected static ArrayList<LayoutHelper> layoutsList = new ArrayList<LayoutHelper>();

        public static final int FLAG_SHIFT = 0x01;
        public static final int FLAG_ALT = 0x02;
        public static final int FLAG_DEADKEY = 0x04;

        protected static final byte NONE = HIDKeycodes.NONE;
        protected static final byte SHIFT = HIDKeycodes.SHIFT_LEFT;
        protected static final byte ALT = HIDKeycodes.ALT_RIGHT;

        protected byte[][] mKeycodes = new byte[256][2];
        protected byte[] mDeadKeycodes = new byte[256];
        protected boolean mUsesDeadKeys;

        public abstract String getCode();
        public abstract String getName();
        public abstract String getDisplayName(); // was not in original interface but used in US_KeyboardLayout

        public KeyboardLayout() {
                for (int i = 0; i < 256; i++) {
                        mKeycodes[i][0] = 0; //modifier
                        mKeycodes[i][1] = 0; //key
                        mDeadKeycodes[i] = 0; //dead key
                }
                mUsesDeadKeys = false;
        }

        public static void registerLayout(Class<? extends KeyboardLayout> layout) {
                if (layout != null) {
                        try {
                                KeyboardLayout l = layout.newInstance();
                                layouts.put(l.getCode(), layout);
                                layoutsList.add(new LayoutHelper(l.getCode(), l.getName(), l.getDisplayName()));
                        } catch (InstantiationException e) {
                        } catch (IllegalAccessException e) {
                        }
                }
        }

        public static KeyboardLayout getLayout(String layoutCode) {
                if (layouts.containsKey(layoutCode)) {
                        try {
                                return layouts.get(layoutCode).newInstance();
                        } catch (InstantiationException e) {
                                return null;
                        } catch (IllegalAccessException e) {
                                return null;
                        }
                } else {
                        return null;
                }
        }

        public static ArrayList<LayoutHelper> getLayoutsList() {
                return layoutsList;
        }

        public byte getModifiers(char c) {
                return mKeycodes[c][0];
        }

        public byte getKey(char c) {
                return mKeycodes[c][1];
        }

        public byte getDeadKey(char c) {
                return mDeadKeycodes[c];
        }

        public boolean usesDeadKeys() {
                return mUsesDeadKeys;
        }

        public void type(String text) {
                type(text, InputStickKeyboard.TYPING_SPEED_NORMAL);
        }

        public void type(String text, int typingSpeed) {
                HIDTransaction t = new HIDTransaction();
                char c;
                byte mod;
                byte key;
                byte deadKey;
                int cnt = typingSpeed;
                if (cnt < 1) {
                        cnt = 1;
                }

                if (text != null) {
                        for (int i = 0; i < text.length(); i++) {
                                c = text.charAt(i);
                                if (c == '\n') {
                                        addKey(t, NONE, HIDKeycodes.KEY_ENTER, cnt);
                                } else if (c == '\t') {
                                        addKey(t, NONE, HIDKeycodes.KEY_TAB, cnt);
                                } else {
                                        mod = getModifiers(c);
                                        key = getKey(c);
                                        if (key != 0) {
                                                if ((mod & FLAG_DEADKEY) != 0) {
                                                        deadKey = getDeadKey(c);
                                                        addKey(t, (byte)(mod & (~FLAG_DEADKEY)), deadKey, cnt);
                                                        addKey(t, NONE, key, cnt);
                                                } else {
                                                        addKey(t, mod, key, cnt);
                                                }
                                        }
                                }
                        }
                }
                InputStickKeyboard.pressAndRelease((byte)0, (byte)0, 0); //this is not good!
        }

        private void addKey(HIDTransaction t, byte mod, byte key, int cnt) {
                for (int i = 0; i < cnt; i++) {
                        t.addReport(new KeyboardReport(mod, NONE)); // modifier only
                }
                for (int i = 0; i < cnt; i++) {
                        t.addReport(new KeyboardReport(mod, key)); // modifier and key
                }
                for (int i = 0; i < cnt; i++) {
                        t.addReport(new KeyboardReport(NONE, NONE)); // release all
                }
        }

}
