package com.diamond.SmartVoice.Recognizer;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PhonMapper {
    private static final Map<String, String> phons = new LinkedHashMap<String, String>() {{
        put("а", "a0");
        put("б", "b");
        put("в", "v");
        put("г", "g");
        put("д", "d");
        put("е", "j e0");
        put("ё", "j o1");
        put("ж", "zh");
        put("з", "z");
        put("и", "i0");
        put("й", "j");
        put("к", "k");
        put("л", "lj");
        put("м", "m");
        put("н", "n");
        put("о", "o1");
        put("п", "p");
        put("р", "r");
        put("с", "s");
        put("т", "t");
        put("у", "u0");
        put("ф", "f");
        put("х", "h");
        put("ц", "c");
        put("ч", "ch");
        put("ш", "sh");
        put("щ", "sch");
        put("ы", "y0");
        put("э", "e0");
        put("ю", "j u0");
        put("я", "j");
    }};

    private Map<String, ArrayList<String>> mPhons = new HashMap<String, ArrayList<String>>();

    public String getPronoun(String str) {
        ArrayList<String> phons = getPhons(str);
        return TextUtils.join(" ", phons);
    }

    public ArrayList<String> getPhons(String str) {
        ArrayList<String> phons = new ArrayList<String>();

        str = str.toLowerCase();

        if(mPhons.containsKey(str)) {
            return mPhons.get(str);
        }

        for (String ch : str.split("")) {
            String phon = getPhon(ch);
            if (phon != null) {
                phons.add(phon);
            }
        }

        return phons;
    }

    private String getPhon(String str) {
        return phons.get(str);
    }
}