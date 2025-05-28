package com.drgraff.speakkey.inputstick;

public class SendKeystrokesAction implements InputAction {
    private final String keystrokeSequence;
    private final int delayMs;

    public SendKeystrokesAction(String keystrokeSequence, int delayMs) {
        this.keystrokeSequence = keystrokeSequence;
        this.delayMs = delayMs;
    }

    public String getKeystrokeSequence() {
        return keystrokeSequence;
    }

    public int getDelayMs() {
        return delayMs;
    }

    @Override
    public ActionType getType() {
        return ActionType.SEND_KEYSTROKES;
    }
}
