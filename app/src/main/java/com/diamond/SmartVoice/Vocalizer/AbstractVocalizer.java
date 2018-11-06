package com.diamond.SmartVoice.Vocalizer;

/**
 * @author Dmitriy Ponomarev
 */
public abstract class AbstractVocalizer {

    public abstract void speak(String text);

    public abstract void destroy();
}
