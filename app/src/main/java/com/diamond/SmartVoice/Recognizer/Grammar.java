package com.diamond.SmartVoice.Recognizer;

public class Grammar {
    private final Dict mDict;
    private final PhonMapper mPhonMapper;

    public Grammar(PhonMapper phonMapper) {
        mDict = new Dict();
        mPhonMapper = phonMapper;
    }

    public String getDict() {
        return mDict.toString();
    }

    public void addWords(String text) {
        String[] words = text.split(" ");
        for (String word : words) {
            mDict.add(word, mPhonMapper.getPronoun(word));
        }
    }
}