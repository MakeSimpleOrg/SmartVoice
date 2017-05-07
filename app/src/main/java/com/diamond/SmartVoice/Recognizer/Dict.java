package com.diamond.SmartVoice.Recognizer;

import android.content.res.AssetManager;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Dict {
    private static final String TAG = Dict.class.getSimpleName();
    private final StringBuilder mDict = new StringBuilder();
    private final Set<String> mWords = new HashSet<String>();
    private static final String NL = System.getProperty("line.separator");

    public void add(String key, String value) {
        if (!mWords.contains(key)) {
            mDict.append(key);
            mDict.append("  "); // two spaces
            mDict.append(value);
            mDict.append(NL);
            mWords.add(key);
        }
    }

    public String toString() {
        return mDict.toString();
    }
}