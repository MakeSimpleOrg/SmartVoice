package com.diamond.SmartVoice;

import android.util.Log;

import java.util.Arrays;

/**
 * Created by diamond on 09.05.2017.
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
        for(int i = 0; i < split1.length; i++)
        {
            c1 = split1[i].substring(0, Math.min(accuracy, split1[i].length()));
            c2 = split2[i].substring(0, Math.min(accuracy, split2[i].length()));
            if(!c1.equalsIgnoreCase(c2))
                return false;
        }
        Log.w(TAG, "совпадение: " + Arrays.toString(split1) + ", оригинал: " + s1 + ", " + s2 + ", точность: " + accuracy);
        return true;
    }

    private static boolean matchesOnOff(String name, String str, int accuracy)
    {
        str = str.replaceAll(" в ", " ");
        str = str.replaceAll(" у ", " ");
        str = str.replaceAll(" на ", " ");
        str = str.replaceAll(" рядом с ", " ");
        str = str.replaceAll(" около ", " ");

        String[] s =  str.split(" ");
        if(s.length > 2 && name.split(" ").length > 1)
        {
            String str2 = s[2] + " " + s[1];
            if((str.contains("включи") || str.contains("выключи") || str.contains("открой") || str.contains("закрой")) && matches(name, str2, accuracy))
                return true;
        }
        return false;
    }

    private static String replaceMistakes(String str)
    {
        str = str.toLowerCase();
        str = str.replaceAll("цвет", "свет");
        str = str.replaceAll("банный", "ванна");
        str = str.replaceAll("лунный свет", "ванна свет");
        return str.trim();
    }

    public static UDevice[] findDevices(UDevice[] devices, String[] strs, int accuracy) {
        int count = 0;
        d1:
        for (UDevice d : devices)
            for (String str : strs) {
                str = replaceMistakes(str);
                if (matches(d.ai_name, str, accuracy)) {
                    count++;
                    continue d1;
                }
                else if(matchesOnOff(d.ai_name, str, accuracy))
                {
                    count++;
                    continue d1;
                }
            }
        if(count == 0)
            return null;
        UDevice[] result = new UDevice[count];
        int i = 0;
        String name2[];
        d2:
        for (UDevice d : devices) {
            for (String str : strs) {
                str = replaceMistakes(str);
                if (matches(d.ai_name, str, accuracy)) {
                    result[i++] = d;
                    continue d2;
                } else if (matchesOnOff(d.ai_name, str, accuracy)) {
                    result[i++] = d;
                    name2 = d.ai_name.split(" ");
                    if (str.contains("включи") || str.contains("закрой"))
                        d.ai_name = name2[0] + " включить " + name2[1] + (name2.length > 2 ? (" " + name2[2]) : "");
                    else if (str.contains("выключи"))
                        d.ai_name = name2[0] + " выключить " + name2[1] + (name2.length > 2 ? (" " + name2[2]) : "");
                    continue d2;
                }
            }
        }
        return result;
    }

    public static UScene[] findScenes(UScene[] scenes, String[] strs, int accuracy) {
        int count = 0;
        d1: for(UScene s : scenes)
            for (String str : strs) {
                str = str.toLowerCase().trim();
                    if (matches(s.ai_name, str, accuracy)) {
                        count++;
                        continue d1;
                    }
            }
        if(count == 0)
            return null;
        UScene[] result = new UScene[count];
        int i = 0;
        d2: for(UScene s : scenes)
            for (String str : strs) {
                str = str.toLowerCase().trim();
                    if (matches(s.ai_name, str, accuracy)) {
                        result[i++] = s;
                        continue d2;
                    }
            }
        return result;
    }

    public static UDevice[] getDevices(UDevice[] devices, String[] strs) {
        UDevice[] result = findDevices(devices, strs, 5);
        if(result == null)
            result = findDevices(devices, strs, 4);
        if(result == null)
            result = findDevices(devices, strs, 3);
        return result;
    }

    public static UScene[] getScenes(UScene[] scenes, String[] strs) {
        UScene[] result = findScenes(scenes, strs, 5);
        if(result == null)
            result = findScenes(scenes, strs, 4);
        if(result == null)
            result = findScenes(scenes, strs, 3);
        return result;
    }

    public static String replaceTrash(String name) {
        name = name.replaceAll("1", "");
        name = name.replaceAll("2", "");
        name = name.replaceAll("3", "");
        name = name.replaceAll("4", "");
        name = name.replaceAll("5", "");
        name = name.replaceAll("6", "");
        name = name.replaceAll("7", "");
        name = name.replaceAll("8", "");
        name = name.replaceAll("9", "");
        name = name.replaceAll("0", "");
        name = name.replaceAll(":", "");
        name = name.replaceAll("/", "");
        name = name.replaceAll("-", "");
        name = name.replaceAll("/", "");
        name = name.replaceAll("  ", " ");
        name = name.trim();
        return name;
    }
}
