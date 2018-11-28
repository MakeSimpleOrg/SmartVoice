package com.diamond.SmartVoice;

import android.content.SharedPreferences;
import android.util.Log;

import com.diamond.SmartVoice.Controllers.Capability;
import com.diamond.SmartVoice.Controllers.UDevice;
import com.diamond.SmartVoice.Controllers.UScene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

/**
 * @author Dmitriy Ponomarev
 */
public class AI {
    private static final String TAG = AI.class.getSimpleName();

    private static boolean matches_s(UScene s, String s2, int accuracy, SharedPreferences pref) {
        if (matches(s.ai_name, s2, accuracy))
            return true;
        String alias = pref.getString("scene_alias_" + s.getId(), null);
        if (alias == null || alias.isEmpty())
            return false;
        String[] aliases = alias.split(",");
        for (String a : aliases)
            if (matches(a.trim(), s2, accuracy))
                return true;
        return false;
    }

    private static boolean matches_d(UDevice d, String s2, int accuracy, SharedPreferences pref) {
        if (matches(d.getAiName(), s2, accuracy))
            return true;
        String alias = pref.getString("device_alias_" + d.getId(), null);
        if (alias == null || alias.isEmpty())
            return false;
        String[] aliases = alias.split(",");
        for (String a : aliases)
            if (matches(a.trim(), s2, accuracy))
                return true;
        return false;
    }

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

    private static boolean matchesOnOffDim_d(UDevice d, String strarg, int accuracy, SharedPreferences pref) {
        if (matchesOnOffDim(d.getAiName(), strarg, accuracy))
            return true;
        String alias = pref.getString("device_alias_" + d.getId(), null);
        if (alias == null || alias.isEmpty())
            return false;
        String[] aliases = alias.split(",");
        for (String a : aliases)
            if (matchesOnOffDim(a.trim(), strarg, accuracy))
                return true;
        return false;
    }

    private static boolean matchesOnOffDim(String name, String strarg, int accuracy) {
        String str = "     " + strarg + "     ";

        str = str.replaceAll("[0-9:/-]", "");

        str = str.replaceAll(" в ", " ");
        str = str.replaceAll(" у ", " ");
        str = str.replaceAll(" с ", " ");
        str = str.replaceAll(" на ", " ");
        str = str.replaceAll(" рядом с ", " ");
        str = str.replaceAll(" около ", " ");
        str = str.replaceAll(" % ", " ");
        str = str.replaceAll(" процент ", " ");
        str = str.replaceAll(" процентов ", " ");
        str = str.replaceAll(" яркость ", " ");

        str = str.replaceAll(" a ", " ");
        str = str.replaceAll(" in ", " ");
        str = str.replaceAll(" at ", " ");
        str = str.replaceAll(" on ", " ");
        str = str.replaceAll(" off ", " ");
        str = str.replaceAll(" near ", " ");
        str = str.replaceAll(" the ", " ");
        str = str.replaceAll(" to ", " ");
        str = str.replaceAll(" for ", " ");
        str = str.replaceAll(" percent ", " ");
        str = str.replaceAll(" percents ", " ");
        str = str.replaceAll(" brightness ", " ");

        str = str.replaceAll("\\s+", " ");
        str = str.trim();

        String[] s = str.split(" ");
        if (s.length > 2 && name.split(" ").length > 1) {
            String str2 = s[2] + " " + s[1];
            return matches(name, str2, accuracy);
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

    private static UDevice[] findDevices(UDevice[] devices, String[] strs, int accuracy, SharedPreferences pref) {
        ArrayList<UDevice> list = new ArrayList<>();
        String dim = null;
        d1:
        for (UDevice d : devices)
            if (pref.getBoolean("device_enabled_" + d.getId(), true)) {
                d.ai_flag = 0;
                dim = d.getCapabilities().get(Capability.dim);
                for (String str : strs) {
                    str = replaceMistakes(str);
                    if (matches_d(d, str, accuracy, pref)) {
                        list.add(d);
                        continue d1;
                    } else {
                        if (str.contains("%") || str.contains("процент") || str.contains("яркость") || str.contains("percent") || str.contains("brightness")) {
                            if (dim != null && matchesOnOffDim_d(d, str, accuracy, pref)) {
                                String str2 = str.replaceAll(" %", "");
                                str2 = str2.replaceAll("%", "");
                                String[] words = str2.split(" ");
                                for (String word : words) {
                                    if (Character.isDigit(word.charAt(0))) {
                                        try {
                                            d.ai_value = String.valueOf(Integer.parseInt(word));
                                            d.ai_flag = 3;
                                            list.add(d);
                                            continue d1;
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        } else if (str.contains("включи") || str.contains("выключи") || str.contains("открой") || str.contains("закрой") || str.contains("switch") || str.contains("turn") || str.contains("open") || str.contains("close")) {
                            if (matchesOnOffDim_d(d, str, accuracy, pref)) {
                                list.add(d);
                                if (str.contains("включи") || str.contains("закрой") || str.contains(" on") || str.contains("close"))
                                    d.ai_flag = 1;
                                else if (str.contains("выключи") || str.contains("открой") || str.contains(" off") || str.contains("open"))
                                    d.ai_flag = 2;
                                continue d1;
                            }
                        }
                    }
                }
            }
        return list.isEmpty() ? null : list.toArray(new UDevice[list.size()]);
    }

    private static UScene[] findScenes(UScene[] scenes, String[] strs, int accuracy, SharedPreferences pref) {
        ArrayList<UScene> list = new ArrayList<>();
        d1:
        for (UScene s : scenes)
            if (pref.getBoolean("scene_enabled_" + s.getId(), true))
                for (String str : strs) {
                    str = str.toLowerCase(Locale.getDefault()).trim();
                    if (matches_s(s, str, accuracy, pref)) {
                        list.add(s);
                        continue d1;
                    }
                }
        return list.isEmpty() ? null : list.toArray(new UScene[list.size()]);
    }

    public static UDevice[] getDevices(UDevice[] devices, String[] strs, SharedPreferences pref) {
        UDevice[] result = findDevices(devices, strs, 5, pref);
        if (result == null)
            result = findDevices(devices, strs, 4, pref);
        if (result == null)
            result = findDevices(devices, strs, 3, pref);
        return result;
    }

    public static UScene[] getScenes(UScene[] scenes, String[] strs, SharedPreferences pref) {
        UScene[] result = findScenes(scenes, strs, 5, pref);
        if (result == null)
            result = findScenes(scenes, strs, 4, pref);
        if (result == null)
            result = findScenes(scenes, strs, 3, pref);
        return result;
    }

    public static String replaceTrash(String name) {
        String sanitizedName = name.replaceAll("[0-9:/-]", "");
        sanitizedName = sanitizedName.replaceAll("\\s+", " ");
        return sanitizedName.toLowerCase(Locale.getDefault()).trim();
    }
}
