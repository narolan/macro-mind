package com.macromind;

import com.macromind.app.AppRunner;

/** Starts MacroMind. All wiring lives in {@link AppRunner}; this class only launches it. */
public final class Main {

    private Main() {}

    public static void main(String[] args) throws Exception {
        AppRunner.start();
    }
}
