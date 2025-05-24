package com.inputstick.api.layout;

import com.inputstick.api.hid.HIDKeycodes;

public class US_KeyboardLayout extends KeyboardLayout {

        public US_KeyboardLayout() {
                super();
                //lower case
                mKeycodes['a'][0] = NONE;                                       mKeycodes['a'][1] = HIDKeycodes.KEY_A;
                mKeycodes['b'][0] = NONE;                                       mKeycodes['b'][1] = HIDKeycodes.KEY_B;
                mKeycodes['c'][0] = NONE;                                       mKeycodes['c'][1] = HIDKeycodes.KEY_C;
                mKeycodes['d'][0] = NONE;                                       mKeycodes['d'][1] = HIDKeycodes.KEY_D;
                mKeycodes['e'][0] = NONE;                                       mKeycodes['e'][1] = HIDKeycodes.KEY_E;
                mKeycodes['f'][0] = NONE;                                       mKeycodes['f'][1] = HIDKeycodes.KEY_F;
                mKeycodes['g'][0] = NONE;                                       mKeycodes['g'][1] = HIDKeycodes.KEY_G;
                mKeycodes['h'][0] = NONE;                                       mKeycodes['h'][1] = HIDKeycodes.KEY_H;
                mKeycodes['i'][0] = NONE;                                       mKeycodes['i'][1] = HIDKeycodes.KEY_I;
                mKeycodes['j'][0] = NONE;                                       mKeycodes['j'][1] = HIDKeycodes.KEY_J;
                mKeycodes['k'][0] = NONE;                                       mKeycodes['k'][1] = HIDKeycodes.KEY_K;
                mKeycodes['l'][0] = NONE;                                       mKeycodes['l'][1] = HIDKeycodes.KEY_L;
                mKeycodes['m'][0] = NONE;                                       mKeycodes['m'][1] = HIDKeycodes.KEY_M;
                mKeycodes['n'][0] = NONE;                                       mKeycodes['n'][1] = HIDKeycodes.KEY_N;
                mKeycodes['o'][0] = NONE;                                       mKeycodes['o'][1] = HIDKeycodes.KEY_O;
                mKeycodes['p'][0] = NONE;                                       mKeycodes['p'][1] = HIDKeycodes.KEY_P;
                mKeycodes['q'][0] = NONE;                                       mKeycodes['q'][1] = HIDKeycodes.KEY_Q;
                mKeycodes['r'][0] = NONE;                                       mKeycodes['r'][1] = HIDKeycodes.KEY_R;
                mKeycodes['s'][0] = NONE;                                       mKeycodes['s'][1] = HIDKeycodes.KEY_S;
                mKeycodes['t'][0] = NONE;                                       mKeycodes['t'][1] = HIDKeycodes.KEY_T;
                mKeycodes['u'][0] = NONE;                                       mKeycodes['u'][1] = HIDKeycodes.KEY_U;
                mKeycodes['v'][0] = NONE;                                       mKeycodes['v'][1] = HIDKeycodes.KEY_V;
                mKeycodes['w'][0] = NONE;                                       mKeycodes['w'][1] = HIDKeycodes.KEY_W;
                mKeycodes['x'][0] = NONE;                                       mKeycodes['x'][1] = HIDKeycodes.KEY_X;
                mKeycodes['y'][0] = NONE;                                       mKeycodes['y'][1] = HIDKeycodes.KEY_Y;
                mKeycodes['z'][0] = NONE;                                       mKeycodes['z'][1] = HIDKeycodes.KEY_Z;
                //upper case
                mKeycodes['A'][0] = SHIFT;                                      mKeycodes['A'][1] = HIDKeycodes.KEY_A;
                mKeycodes['B'][0] = SHIFT;                                      mKeycodes['B'][1] = HIDKeycodes.KEY_B;
                mKeycodes['C'][0] = SHIFT;                                      mKeycodes['C'][1] = HIDKeycodes.KEY_C;
                mKeycodes['D'][0] = SHIFT;                                      mKeycodes['D'][1] = HIDKeycodes.KEY_D;
                mKeycodes['E'][0] = SHIFT;                                      mKeycodes['E'][1] = HIDKeycodes.KEY_E;
                mKeycodes['F'][0] = SHIFT;                                      mKeycodes['F'][1] = HIDKeycodes.KEY_F;
                mKeycodes['G'][0] = SHIFT;                                      mKeycodes['G'][1] = HIDKeycodes.KEY_G;
                mKeycodes['H'][0] = SHIFT;                                      mKeycodes['H'][1] = HIDKeycodes.KEY_H;
                mKeycodes['I'][0] = SHIFT;                                      mKeycodes['I'][1] = HIDKeycodes.KEY_I;
                mKeycodes['J'][0] = SHIFT;                                      mKeycodes['J'][1] = HIDKeycodes.KEY_J;
                mKeycodes['K'][0] = SHIFT;                                      mKeycodes['K'][1] = HIDKeycodes.KEY_K;
                mKeycodes['L'][0] = SHIFT;                                      mKeycodes['L'][1] = HIDKeycodes.KEY_L;
                mKeycodes['M'][0] = SHIFT;                                      mKeycodes['M'][1] = HIDKeycodes.KEY_M;
                mKeycodes['N'][0] = SHIFT;                                      mKeycodes['N'][1] = HIDKeycodes.KEY_N;
                mKeycodes['O'][0] = SHIFT;                                      mKeycodes['O'][1] = HIDKeycodes.KEY_O;
                mKeycodes['P'][0] = SHIFT;                                      mKeycodes['P'][1] = HIDKeycodes.KEY_P;
                mKeycodes['Q'][0] = SHIFT;                                      mKeycodes['Q'][1] = HIDKeycodes.KEY_Q;
                mKeycodes['R'][0] = SHIFT;                                      mKeycodes['R'][1] = HIDKeycodes.KEY_R;
                mKeycodes['S'][0] = SHIFT;                                      mKeycodes['S'][1] = HIDKeycodes.KEY_S;
                mKeycodes['T'][0] = SHIFT;                                      mKeycodes['T'][1] = HIDKeycodes.KEY_T;
                mKeycodes['U'][0] = SHIFT;                                      mKeycodes['U'][1] = HIDKeycodes.KEY_U;
                mKeycodes['V'][0] = SHIFT;                                      mKeycodes['V'][1] = HIDKeycodes.KEY_V;
                mKeycodes['W'][0] = SHIFT;                                      mKeycodes['W'][1] = HIDKeycodes.KEY_W;
                mKeycodes['X'][0] = SHIFT;                                      mKeycodes['X'][1] = HIDKeycodes.KEY_X;
                mKeycodes['Y'][0] = SHIFT;                                      mKeycodes['Y'][1] = HIDKeycodes.KEY_Y;
                mKeycodes['Z'][0] = SHIFT;                                      mKeycodes['Z'][1] = HIDKeycodes.KEY_Z;
                //numbers
                mKeycodes['1'][0] = NONE;                                       mKeycodes['1'][1] = HIDKeycodes.KEY_1;
                mKeycodes['2'][0] = NONE;                                       mKeycodes['2'][1] = HIDKeycodes.KEY_2;
                mKeycodes['3'][0] = NONE;                                       mKeycodes['3'][1] = HIDKeycodes.KEY_3;
                mKeycodes['4'][0] = NONE;                                       mKeycodes['4'][1] = HIDKeycodes.KEY_4;
                mKeycodes['5'][0] = NONE;                                       mKeycodes['5'][1] = HIDKeycodes.KEY_5;
                mKeycodes['6'][0] = NONE;                                       mKeycodes['6'][1] = HIDKeycodes.KEY_6;
                mKeycodes['7'][0] = NONE;                                       mKeycodes['7'][1] = HIDKeycodes.KEY_7;
                mKeycodes['8'][0] = NONE;                                       mKeycodes['8'][1] = HIDKeycodes.KEY_8;
                mKeycodes['9'][0] = NONE;                                       mKeycodes['9'][1] = HIDKeycodes.KEY_9;
                mKeycodes['0'][0] = NONE;                                       mKeycodes['0'][1] = HIDKeycodes.KEY_0;
                //symbols
                mKeycodes['!'][0] = SHIFT;                                      mKeycodes['!'][1] = HIDKeycodes.KEY_1;
                mKeycodes['@'][0] = SHIFT;                                      mKeycodes['@'][1] = HIDKeycodes.KEY_2;
                mKeycodes['#'][0] = SHIFT;                                      mKeycodes['#'][1] = HIDKeycodes.KEY_3;
                mKeycodes['$'][0] = SHIFT;                                      mKeycodes['$'][1] = HIDKeycodes.KEY_4;
                mKeycodes['%'][0] = SHIFT;                                      mKeycodes['%'][1] = HIDKeycodes.KEY_5;
                mKeycodes['^'][0] = SHIFT;                                      mKeycodes['^'][1] = HIDKeycodes.KEY_6;
                mKeycodes['&'][0] = SHIFT;                                      mKeycodes['&'][1] = HIDKeycodes.KEY_7;
                mKeycodes['*'][0] = SHIFT;                                      mKeycodes['*'][1] = HIDKeycodes.KEY_8;
                mKeycodes['('][0] = SHIFT;                                      mKeycodes['('][1] = HIDKeycodes.KEY_9;
                mKeycodes[')'][0] = SHIFT;                                      mKeycodes[')'][1] = HIDKeycodes.KEY_0;

                mKeycodes['-'][0] = NONE;                                       mKeycodes['-'][1] = HIDKeycodes.KEY_MINUS;
                mKeycodes['_'][0] = SHIFT;                                      mKeycodes['_'][1] = HIDKeycodes.KEY_MINUS;
                mKeycodes['='][0] = NONE;                                       mKeycodes['='][1] = HIDKeycodes.KEY_EQUALS;
                mKeycodes['+'][0] = SHIFT;                                      mKeycodes['+'][1] = HIDKeycodes.KEY_EQUALS;
                mKeycodes['['][0] = NONE;                                       mKeycodes['['][1] = HIDKeycodes.KEY_LEFT_BRACKET;
                mKeycodes['{'][0] = SHIFT;                                      mKeycodes['{'][1] = HIDKeycodes.KEY_LEFT_BRACKET;
                mKeycodes[']'][0] = NONE;                                       mKeycodes[']'][1] = HIDKeycodes.KEY_RIGHT_BRACKET;
                mKeycodes['}'][0] = SHIFT;                                      mKeycodes['}'][1] = HIDKeycodes.KEY_RIGHT_BRACKET;
                mKeycodes['\\'][0] = NONE;                                      mKeycodes['\\'][1] = HIDKeycodes.KEY_BACKSLASH;
                mKeycodes['|'][0] = SHIFT;                                      mKeycodes['|'][1] = HIDKeycodes.KEY_BACKSLASH;
                mKeycodes[';'][0] = NONE;                                       mKeycodes[';'][1] = HIDKeycodes.KEY_SEMICOLON;
                mKeycodes[':'][0] = SHIFT;                                      mKeycodes[':'][1] = HIDKeycodes.KEY_SEMICOLON;
                mKeycodes['\''][0] = NONE;                                      mKeycodes['\''][1] = HIDKeycodes.KEY_APOSTROPHE;
                mKeycodes['"'][0] = SHIFT;                                      mKeycodes['"'][1] = HIDKeycodes.KEY_APOSTROPHE;
                mKeycodes['`'][0] = NONE;                                       mKeycodes['`'][1] = HIDKeycodes.KEY_GRAVE;
                mKeycodes['~'][0] = SHIFT;                                      mKeycodes['~'][1] = HIDKeycodes.KEY_GRAVE;
                mKeycodes[','][0] = NONE;                                       mKeycodes[','][1] = HIDKeycodes.KEY_COMA;
                mKeycodes['<'][0] = SHIFT;                                      mKeycodes['<'][1] = HIDKeycodes.KEY_COMA;
                mKeycodes['.'][0] = NONE;                                       mKeycodes['.'][1] = HIDKeycodes.KEY_DOT;
                mKeycodes['>'][0] = SHIFT;                                      mKeycodes['>'][1] = HIDKeycodes.KEY_DOT;
                mKeycodes['/'][0] = NONE;                                       mKeycodes['/'][1] = HIDKeycodes.KEY_SLASH;
                mKeycodes['?'][0] = SHIFT;                                      mKeycodes['?'][1] = HIDKeycodes.KEY_SLASH;

                mKeycodes[' '][0] = NONE;                                       mKeycodes[' '][1] = HIDKeycodes.KEY_SPACEBAR;
        }

        @Override
        public String getCode() {
                return "en-US"; // Standard code for US English
        }

        @Override
        public String getName() {
                return "US English";
        }

        @Override
        public String getDisplayName() {
                return "US English";
        }

}
