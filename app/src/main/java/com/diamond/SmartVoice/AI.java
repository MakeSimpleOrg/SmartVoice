package com.diamond.SmartVoice;

import android.util.Log;

import com.diamond.SmartVoice.Controllers.UDevice;
import com.diamond.SmartVoice.Controllers.UScene;

import java.util.Arrays;
import java.util.Locale;

/**
 * @author Dmitriy Ponomarev
 */
public class AI {
    private static final String TAG = AI.class.getSimpleName();

    private static boolean matches(String s1, String s2, int accuracy) {
        if (s1.equalsIgnoreCase(s2))
            return true;
        String[] split1 = s1.split(" ");
        String[] split2 = s2.split(" ");
        if (split1.length != split2.length || split1.length == 0)
            return false;
        String c1, c2;
        for (int i = 0; i < split1.length; i++) {
            c1 = split1[i].substring(0, Math.min(accuracy, split1[i].length()));
            c2 = split2[i].substring(0, Math.min(accuracy, split2[i].length()));
            if (!c1.equalsIgnoreCase(c2))
                return false;
        }
        Log.w(TAG, "совпадение: " + Arrays.toString(split1) + ", оригинал: " + s1 + ", " + s2 + ", точность: " + accuracy);
        return true;
    }

    private static boolean matchesOnOff(String name, String str, int accuracy) {
        str = " " + str + " ";

        str = str.replaceAll(" в ", " ");
        str = str.replaceAll(" у ", " ");
        str = str.replaceAll(" с ", " ");
        str = str.replaceAll(" на ", " ");
        str = str.replaceAll(" рядом с ", " ");
        str = str.replaceAll(" около ", " ");

        str = str.replaceAll(" a ", " ");
        str = str.replaceAll(" in ", " ");
        str = str.replaceAll(" at ", " ");
        str = str.replaceAll(" on ", " ");
        str = str.replaceAll(" off ", " ");
        str = str.replaceAll(" near ", " ");
        str = str.replaceAll(" the ", " ");
        str = str.replaceAll(" to ", " ");

        str = str.trim();

        String[] s = str.split(" ");
        if (s.length > 2 && name.split(" ").length > 1) {
            String str2 = s[2] + " " + s[1];
            return (str.contains("включи") || str.contains("выключи") || str.contains("открой") || str.contains("закрой") || str.contains("switch") || str.contains("turn") || str.contains("open") || str.contains("close")) && matches(name, str2, accuracy);
        }
        return false;
    }

    private static String replaceMistakes(String str) {
        str = str.toLowerCase(Locale.getDefault());
        str = str.replaceAll("цвет", "свет");
        str = str.replaceAll("банный", "ванна");
        str = str.replaceAll("лунный свет", "ванна свет");
        return str.trim();
    }

    private static UDevice[] findDevices(UDevice[] devices, String[] strs, int accuracy) {
        int count = 0;
        d1:
        for (UDevice d : devices)
            for (String str : strs) {
                str = replaceMistakes(str);
                if (matches(d.ai_name, str, accuracy)) {
                    count++;
                    continue d1;
                } else if (matchesOnOff(d.ai_name, str, accuracy)) {
                    count++;
                    continue d1;
                }
            }
        if (count == 0)
            return null;
        UDevice[] result = new UDevice[count];
        int i = 0;
        d2:
        for (UDevice d : devices) {
            d.ai_flag = 0;
            for (String str : strs) {
                str = replaceMistakes(str);
                if (matches(d.ai_name, str, accuracy)) {
                    result[i++] = d;
                    continue d2;
                } else if (matchesOnOff(d.ai_name, str, accuracy)) {
                    result[i++] = d;
                    if (str.contains("включи") || str.contains("закрой") || str.contains(" on") || str.contains("close"))
                        d.ai_flag = 1;
                    else if (str.contains("выключи") || str.contains("открой") || str.contains(" off") || str.contains("open"))
                        d.ai_flag = 2;
                    continue d2;
                }
            }
        }
        return result;
    }

    private static UScene[] findScenes(UScene[] scenes, String[] strs, int accuracy) {
        int count = 0;
        d1:
        for (UScene s : scenes)
            for (String str : strs) {
                str = str.toLowerCase(Locale.getDefault()).trim();
                if (matches(s.ai_name, str, accuracy)) {
                    count++;
                    continue d1;
                }
            }
        if (count == 0)
            return null;
        UScene[] result = new UScene[count];
        int i = 0;
        d2:
        for (UScene s : scenes)
            for (String str : strs) {
                str = str.toLowerCase(Locale.getDefault()).trim();
                if (matches(s.ai_name, str, accuracy)) {
                    result[i++] = s;
                    continue d2;
                }
            }
        return result;
    }

    public static UDevice[] getDevices(UDevice[] devices, String[] strs) {
        UDevice[] result = findDevices(devices, strs, 5);
        if (result == null)
            result = findDevices(devices, strs, 4);
        if (result == null)
            result = findDevices(devices, strs, 3);
        return result;
    }

    public static UScene[] getScenes(UScene[] scenes, String[] strs) {
        UScene[] result = findScenes(scenes, strs, 5);
        if (result == null)
            result = findScenes(scenes, strs, 4);
        if (result == null)
            result = findScenes(scenes, strs, 3);
        return result;
    }

    public static String replaceTrash(String name) {
        String sanitizedName = name.replaceAll("[0-9:/-]", "");
        sanitizedName = sanitizedName.replaceAll("\\s+", " ");
        return sanitizedName.toLowerCase(Locale.getDefault()).trim();
    }
}
