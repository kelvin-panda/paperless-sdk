package com.xlk.paperless.sdk.screen;

/**
 * @author : Administrator
 * created on 2026/2/4 14:31
 */
public interface IScreenRecord {
    void start();
    void stop();
    void screenshot();
    void resume();
    boolean isRunning();
    int getPushCount();
}
