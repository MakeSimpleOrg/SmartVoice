package com.diamond.SmartVoice.Recognizer;

import android.util.Log;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Dmitriy Ponomarev
 */
public class PhonMapper {
    private static final String TAG = PhonMapper.class.getSimpleName();

    /*
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
    */

    private static final Map<String, String> phons = new LinkedHashMap<String, String>() {{
        put("а", "a");
        put("б", "p");
        put("в", "f");
        put("г", "k");
        put("д", "d");
        put("е", "je");
        put("ё", "j oo");
        put("ж", "zh");
        put("з", "z");
        put("и", "i");
        put("й", "j");
        put("к", "k");
        put("л", "l");
        put("м", "m");
        put("н", "n");
        put("о", "ay");
        put("п", "p");
        put("р", "r");
        put("с", "s");
        put("т", "t");
        put("у", "u");
        put("ф", "f");
        put("х", "h");
        put("ц", "c");
        put("ч", "ch");
        put("ш", "sh");
        put("щ", "sch");
        put("ы", "y");
        put("э", "ay");
        put("ю", "u");
        put("я", "a");
    }};

    private static Map<String, String> mPhons = new HashMap<String, String>();

    static {
        //mPhons.put("умный", "uu m n ay j");
        mPhons.put("умныйдом", "uu m n y j d oo m");
        mPhons.put("умный", "uu m n y j");
        mPhons.put("дом", "d oo m");
        mPhons.put("ok", "a kk je j");
        mPhons.put("google", "g u k l");
    }

    public String getPronoun(String str) {
        if (mPhons.containsKey(str))
            return mPhons.get(str);

        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (String ch : str.split("")) {
            String phon = phons.get(ch);
            if (phon != null) {
                if (!first)
                    result.append(" ");
                result.append(phon);
                first = false;
            } else
                Log.w(TAG, "NULL phon: " + ch);
        }
        return result.toString();
    }
}