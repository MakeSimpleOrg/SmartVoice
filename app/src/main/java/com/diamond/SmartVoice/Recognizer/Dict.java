package com.diamond.SmartVoice.Recognizer;

import java.util.HashSet;
import java.util.Set;

public class Dict {
    private final StringBuilder mDict = new StringBuilder();
    private final Set<String> mWords = new HashSet<String>();
    private static final String NL = System.getProperty("line.separator");

    public void add(String key, String value) {
        if (!mWords.contains(key)) {
            mDict.append(key);
            //mDict.append("  "); // two spaces
            mDict.append(" ");
            mDict.append(value);
            mDict.append(NL);
            mWords.add(key);
        }
    }

    public String toString() {
        return mDict.toString();
    }
}