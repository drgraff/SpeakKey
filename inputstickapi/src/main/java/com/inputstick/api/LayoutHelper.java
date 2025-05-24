package com.inputstick.api;

public class LayoutHelper {
    private String code;
    private String name;
    private String displayName;

    public LayoutHelper(String code, String name, String displayName) {
        this.code = code;
        this.name = name;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }
}
