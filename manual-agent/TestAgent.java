package com.heypixel.heypixelmod.obsoverlay.utils;

import java.lang.instrument.Instrumentation;

public class TestAgent {
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("Test Agent loaded successfully!");
        System.out.println("Agent args: " + agentArgs);
        System.out.println("Instrumentation: " + inst);
    }
}