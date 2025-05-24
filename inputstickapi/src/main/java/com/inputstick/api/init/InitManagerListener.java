package com.inputstick.api.init;

public interface InitManagerListener {
    void onInitReady();
    void onInitNotReady();
    void onInitFailure(int code);
}
