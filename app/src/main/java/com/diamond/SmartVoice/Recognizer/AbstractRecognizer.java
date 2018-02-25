package com.diamond.SmartVoice.Recognizer;

/**
 * @author Dmitriy Ponomarev
 */
public abstract class AbstractRecognizer {

    public abstract void startListening();

    public abstract void stopListening();

    public abstract void destroy();
}
