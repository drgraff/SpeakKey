package com.inputstick.api.basic;

import java.util.Vector;

import com.inputstick.api.InputStickRawHIDListener;
import com.inputstick.api.hid.HIDTransaction;
import com.inputstick.api.hid.RawHIDReport;

public class InputStickRawHID {
	
	private static Vector<InputStickRawHIDListener> mRawHIDListeners = new Vector<InputStickRawHIDListener>();
	
	private InputStickRawHID() {		
	}
	
	public static void customReport(byte[] data) {
		HIDTransaction t = new HIDTransaction();
		t.addReport(new RawHIDReport(data));		
		InputStickHID.addRawHIDTransaction(t, true);
	}
	
	public static void addRawHIDListener(InputStickRawHIDListener listener) {
		if (listener != null) {
			if ( !mRawHIDListeners.contains(listener)) {
				mRawHIDListeners.add(listener);
			}
		}
	}
	
	public static void removeRawHIDListener(InputStickRawHIDListener listener) {
		if (listener != null) {
			mRawHIDListeners.remove(listener);
		}
	}
	
	protected static void notifyRawHIDListeners(byte[] data) {		
		for (InputStickRawHIDListener listener : mRawHIDListeners) {
			listener.onRawHIDData(data);
		}
	}

}
