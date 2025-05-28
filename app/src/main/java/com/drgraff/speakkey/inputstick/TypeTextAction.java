package com.drgraff.speakkey.inputstick;

public class TypeTextAction implements InputAction {
    private final String text;

    public TypeTextAction(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public ActionType getType() {
        return ActionType.TYPE_TEXT;
    }
}
