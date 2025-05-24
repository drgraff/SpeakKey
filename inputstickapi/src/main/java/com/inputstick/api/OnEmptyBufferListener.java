package com.inputstick.api;

// TODO: Fetch content from https://github.com/inputstick/InputStickAPI-Android/tree/master/InputStickAPI/src/com/inputstick/api/OnEmptyBufferListener.java
public interface OnEmptyBufferListener {
    void onLocalBufferEmpty(int interfaceType);
    void onRemoteBufferEmpty(int interfaceType);
}
