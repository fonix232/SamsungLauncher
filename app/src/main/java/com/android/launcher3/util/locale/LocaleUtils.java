package com.android.launcher3.util.locale;

import android.icu.text.AlphabeticIndex;
import android.icu.text.AlphabeticIndex.ImmutableIndex;
import android.icu.text.UnicodeSet;
import android.os.LocaleList;
import android.util.Log;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.util.locale.hanzi.HanziToBPMF;
import com.android.launcher3.util.locale.hanzi.HanziToPinyin;
import com.android.launcher3.util.locale.hanzi.HanziToPinyin.Token;
import com.android.launcher3.util.locale.hanzi.HanziToStroke;
import com.android.launcher3.util.locale.hanzi.KeypadNumberUtils;
import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

public class LocaleUtils {
    private static final String CHINA_LOCALE = "zh_CN_#Hans";
    private static final String CHINESE_LANGUAGE = Locale.CHINESE.getLanguage();
    private static final boolean DEBUG = false;
    private static final String ENGLISH_LANGUAGE = Locale.ENGLISH.getLanguage();
    private static final String HONGKONG_LOCALE = "zh_HK_#Hant";
    private static final String JAPANESE_LANGUAGE = Locale.JAPANESE.getLanguage();
    private static final String KOREAN_LANGUAGE = Locale.KOREAN.getLanguage();
    private static final Locale LOCALE_ARABIC = new Locale("ar");
    private static final Locale LOCALE_GREEK = new Locale("el");
    private static final Locale LOCALE_HEBREW = new Locale("he");
    private static final Locale LOCALE_HINDI = new Locale("hi");
    private static final Locale LOCALE_SERBIAN = new Locale("sr");
    private static final Locale LOCALE_THAI = new Locale("th");
    private static final Locale LOCALE_UKRAINIAN = new Locale("uk");
    private static final int MAX_LOOKUP_NAME_LENGTH = 30;
    private static final String TAG = "LocaleUtils";
    private static final String TAIWAN_LOCALE = "zh_TW_#Hant";
    private static LocaleUtils sInstance;
    private static Locale sLocale;
    private LocaleUtilsBase mUtils;

    private static class LocaleUtilsBase {
        protected static ImmutableIndex mAlphabeticIndex;
        protected String mInflowLabel;
        protected String mOverflowLabel;
        protected String mUnderflowLabel;

        public LocaleUtilsBase() {
            AlphabeticIndex ai = initAlphabeticIndex();
            this.mUnderflowLabel = ai.getUnderflowLabel();
            this.mOverflowLabel = ai.getOverflowLabel();
            this.mInflowLabel = ai.getInflowLabel();
            LocaleList localeList = LocaleList.getDefault();
            if (localeList != null && localeList.size() > 1) {
                for (int i = 0; i < localeList.size(); i++) {
                    ai.addLabels(new Locale[]{localeList.get(i)});
                }
            }
            mAlphabeticIndex = ai.setMaxLabelCount(400).addLabels(new Locale[]{Locale.ENGLISH}).addLabels(new Locale[]{Locale.JAPANESE}).addLabels(new Locale[]{Locale.KOREAN}).addLabels(new Locale[]{LocaleUtils.LOCALE_THAI}).addLabels(new Locale[]{LocaleUtils.LOCALE_ARABIC}).addLabels(new Locale[]{LocaleUtils.LOCALE_HEBREW}).addLabels(new Locale[]{LocaleUtils.LOCALE_GREEK}).addLabels(new Locale[]{LocaleUtils.LOCALE_UKRAINIAN}).addLabels(new Locale[]{LocaleUtils.LOCALE_HINDI}).addLabels(new UnicodeSet(2784, 2784)).addLabels(new UnicodeSet(3648, 3653)).addLabels(new UnicodeSet(1569, 1574)).addLabels(new UnicodeSet(4353, 4353)).addLabels(new UnicodeSet(4356, 4356)).addLabels(new UnicodeSet(4360, 4360)).addLabels(new UnicodeSet(4362, 4362)).addLabels(new UnicodeSet(4365, 4365)).addLabels(new UnicodeSet(6016, 6109)).addLabels(new UnicodeSet(6112, 6121)).addLabels(new UnicodeSet(6128, 6137)).addLabels(new UnicodeSet(12593, 12593)).addLabels(new UnicodeSet(12596, 12596)).addLabels(new UnicodeSet(12599, 12599)).addLabels(new UnicodeSet(12601, 12601)).addLabels(new UnicodeSet(12609, 12610)).addLabels(new UnicodeSet(12613, 12613)).addLabels(new UnicodeSet(12615, 12616)).addLabels(new UnicodeSet(12618, 12622)).addLabels(new Locale[]{LocaleUtils.LOCALE_SERBIAN}).buildImmutableIndex();
        }

        protected AlphabeticIndex initAlphabeticIndex() {
            return new AlphabeticIndex(Locale.getDefault());
        }

        public String getSortKey(String name) {
            return name;
        }

        public String getConsistKey(String name) {
            String result = name;
            String trimmedName = name.replace(String.valueOf(' '), " ").trim();
            if (trimmedName.length() <= 0) {
                return result;
            }
            int bucketIndex = mAlphabeticIndex.getBucketIndex(trimmedName);
            if (bucketIndex < 0) {
                return trimmedName.substring(0, 1);
            }
            String label = mAlphabeticIndex.getBucket(bucketIndex).getLabel();
            if (label == null || label.equals(this.mUnderflowLabel) || label.equals(this.mOverflowLabel)) {
                return trimmedName.substring(0, 1);
            }
            return label;
        }

        public Iterator<String> getNameLookupKeys(String name) {
            return null;
        }
    }

    private static class JapaneseContactUtils extends LocaleUtilsBase {
        private static final String INFLOW_LABEL = "inflow_label";
        private static final String JAPANESE_MISC_LABEL = "他";
        private static final boolean USE_JAPANESE_MISC_LABEL = true;

        private JapaneseContactUtils() {
        }

        protected AlphabeticIndex initAlphabeticIndex() {
            AlphabeticIndex ai = new AlphabeticIndex(Locale.getDefault());
            ai.setInflowLabel(INFLOW_LABEL);
            return ai;
        }

        public String getSortKey(String name) {
            return super.getSortKey(name);
        }

        public String getConsistKey(String name) {
            String result = name;
            String trimmedName = name.replace(String.valueOf(' '), " ").trim();
            if (trimmedName.length() > 0) {
                int bucketIndex = mAlphabeticIndex.getBucketIndex(trimmedName);
                if (bucketIndex < 0) {
                    return super.getConsistKey(name);
                }
                String label = mAlphabeticIndex.getBucket(bucketIndex).getLabel();
                if (label == null || label.equals(this.mUnderflowLabel) || label.equals(this.mOverflowLabel)) {
                    result = trimmedName.substring(0, 1);
                } else if (label.equals(this.mInflowLabel)) {
                    result = JAPANESE_MISC_LABEL;
                } else {
                    result = label;
                }
            }
            return result;
        }

        public Iterator<String> getNameLookupKeys(String name) {
            return super.getNameLookupKeys(name);
        }
    }

    private static class SimplifiedChineseUtils extends LocaleUtilsBase {
        private static Iterator<String> getPinyinNameLookupKeys(String name) {
            HashSet<String> keys = new HashSet();
            ArrayList<Token> tokens = HanziToPinyin.getInstance().get(name);
            int tokenCount = tokens.size();
            StringBuilder keyPinyin = new StringBuilder();
            StringBuilder keyInitial = new StringBuilder();
            StringBuilder keyOriginal = new StringBuilder();
            for (int i = tokenCount - 1; i >= 0; i--) {
                Token token = (Token) tokens.get(i);
                if (3 != token.type) {
                    if (2 == token.type) {
                        keyPinyin.insert(0, token.target);
                        keyInitial.insert(0, token.target.charAt(0));
                    } else if (1 == token.type) {
                        if (keyPinyin.length() > 0) {
                            keyPinyin.insert(0, ' ');
                        }
                        if (keyOriginal.length() > 0) {
                            keyOriginal.insert(0, ' ');
                        }
                        keyPinyin.insert(0, token.source);
                        keyInitial.insert(0, token.source.charAt(0));
                    }
                    keyOriginal.insert(0, token.source);
                    keys.add(keyOriginal.toString());
                    keys.add(keyPinyin.toString());
                    keys.add(keyInitial.toString());
                }
            }
            return keys.iterator();
        }

        public String getSortKey(String name) {
            ArrayList<Token> tokens = HanziToPinyin.getInstance().get(name);
            if (tokens == null || tokens.size() <= 0) {
                return name;
            }
            StringBuilder sb = new StringBuilder();
            Iterator it = tokens.iterator();
            while (it.hasNext()) {
                Token token = (Token) it.next();
                if (2 == token.type) {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(token.target);
                    sb.append(' ');
                    sb.append(token.source);
                } else {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(token.source);
                }
            }
            return sb.toString();
        }

        public String getConsistKey(String name) {
            String trimmedName = name.replace(String.valueOf(' '), " ").trim();
            if (trimmedName.length() <= 0 || !LocaleUtils.isCJKUnicodeBlock(UnicodeBlock.of(name.charAt(0)))) {
                return super.getConsistKey(name);
            }
            ArrayList<Token> tokens = HanziToPinyin.getInstance().get(trimmedName.substring(0, 1));
            if (tokens == null || tokens.size() <= 0) {
                return trimmedName.substring(0, 1);
            }
            StringBuilder sb = new StringBuilder();
            Iterator it = tokens.iterator();
            if (it.hasNext()) {
                Token token = (Token) it.next();
                if (2 == token.type) {
                    sb.append(token.target);
                } else {
                    sb.append(token.source);
                }
            }
            return sb.toString().substring(0, 1);
        }

        public Iterator<String> getNameLookupKeys(String name) {
            return getPinyinNameLookupKeys(name);
        }
    }

    private static class TraditionalChineseHKUtils extends SimplifiedChineseUtils {
        public String getSortKey(String name) {
            String tmpDisplayName = name;
            if (name != null && name.length() > 0) {
                tmpDisplayName = name.substring(0, 1);
            }
            ArrayList<HanziToStroke.Token> tokens = HanziToStroke.getIntance().get(tmpDisplayName);
            if (tokens == null || tokens.size() <= 0) {
                return super.getSortKey(name);
            }
            StringBuilder sb = new StringBuilder();
            Iterator it = tokens.iterator();
            while (it.hasNext()) {
                HanziToStroke.Token token = (HanziToStroke.Token) it.next();
                if (2 != token.type) {
                    return super.getSortKey(name);
                }
                sb.append(name);
                sb.append(' ');
                sb.appendCodePoint(164);
                sb.append(token.source);
                sb.append(token.target);
            }
            return sb.toString();
        }

        public String getConsistKey(String name) {
            String trimmedName = name.replace(String.valueOf(' '), " ").trim();
            if (trimmedName.length() > 0 && LocaleUtils.isCJKUnicodeBlock(UnicodeBlock.of(name.charAt(0)))) {
                ArrayList<HanziToStroke.Token> tokens = HanziToStroke.getIntance().get(trimmedName.substring(0, 1));
                if (tokens != null && tokens.size() > 0) {
                    Iterator it = tokens.iterator();
                    if (it.hasNext()) {
                        HanziToStroke.Token token = (HanziToStroke.Token) it.next();
                        if (2 == token.type) {
                            return String.valueOf(token.target.length());
                        }
                        return super.getConsistKey(name);
                    }
                }
            }
            return super.getConsistKey(name);
        }

        public Iterator<String> getNameLookupKeys(String name) {
            return getStrokeNameLookupKeys(name);
        }

        private Iterator<String> getStrokeNameLookupKeys(String name) {
            HashSet<String> keys = new HashSet();
            int maxNameLength = name.length();
            if (maxNameLength > 30) {
                maxNameLength = 30;
            }
            ArrayList<HanziToStroke.Token> tokens = HanziToStroke.getIntance().get(name.substring(0, maxNameLength));
            if (tokens == null) {
                return null;
            }
            int tokenCount = tokens.size();
            StringBuilder keyStroke = new StringBuilder();
            StringBuilder keyInitial = new StringBuilder();
            StringBuilder keyOrignal = new StringBuilder();
            StringBuilder keyStrokeNotMap = new StringBuilder();
            StringBuilder keyInitialNotMap = new StringBuilder();
            for (int i = tokenCount - 1; i >= 0; i--) {
                HanziToStroke.Token token = (HanziToStroke.Token) tokens.get(i);
                if (2 == token.type) {
                    if (i == 0) {
                        keyStroke.insert(0, KeypadNumberUtils.makeActionCodeHKTW(token.target));
                        keyStrokeNotMap.insert(0, token.target);
                    } else {
                        keyStroke.insert(0, KeypadNumberUtils.convertTraditionalChineseKeypadLettersToDigits(token.target.charAt(0)));
                        keyStrokeNotMap.insert(0, token.target.charAt(0));
                    }
                    keyInitial.insert(0, KeypadNumberUtils.convertTraditionalChineseKeypadLettersToDigits(token.target.charAt(0)));
                    keyInitialNotMap.insert(0, token.target.charAt(0));
                } else if (1 == token.type) {
                    if (keyStroke.length() > 0) {
                        keyStroke.insert(0, ' ');
                        keyStrokeNotMap.insert(0, ' ');
                    }
                    if (keyOrignal.length() > 0) {
                        keyOrignal.insert(0, ' ');
                    }
                    if (i == 0) {
                        keyStroke.insert(0, KeypadNumberUtils.makeActionCodeHKTW(token.source));
                        keyStrokeNotMap.insert(0, token.source);
                    } else {
                        keyStroke.insert(0, KeypadNumberUtils.convertTraditionalChineseKeypadLettersToDigits(token.source.charAt(0)));
                        keyStrokeNotMap.insert(0, token.source.charAt(0));
                    }
                    keyInitial.insert(0, KeypadNumberUtils.convertTraditionalChineseKeypadLettersToDigits(token.source.charAt(0)));
                    keyInitialNotMap.insert(0, token.source.charAt(0));
                } else {
                    if (i == 0) {
                        keyStroke.insert(0, KeypadNumberUtils.makeActionCodeHKTW(token.source));
                        keyStrokeNotMap.insert(0, token.source);
                    } else {
                        keyStroke.insert(0, KeypadNumberUtils.convertTraditionalChineseKeypadLettersToDigits(token.source.charAt(0)));
                        keyStrokeNotMap.insert(0, token.source.charAt(0));
                    }
                    keyInitial.insert(0, KeypadNumberUtils.convertTraditionalChineseKeypadLettersToDigits(token.source.charAt(0)));
                    keyInitialNotMap.insert(0, token.source.charAt(0));
                }
                keyOrignal.insert(0, token.source);
                keys.add(keyOrignal.toString());
                keys.add(keyInitial.toString());
                keys.add(keyInitialNotMap.toString());
                if (i == 0) {
                    keys.add(keyStroke.toString());
                    keys.add(keyStrokeNotMap.toString());
                }
            }
            return keys.iterator();
        }
    }

    private static class TraditionalChineseTWUtils extends SimplifiedChineseUtils {
        private static Iterator<String> getBPMFNameLookupKeys(String name) {
            HashSet<String> keys = new HashSet();
            int maxNameLength = name.length();
            if (maxNameLength > 30) {
                maxNameLength = 30;
            }
            ArrayList<HanziToBPMF.Token> tokens = HanziToBPMF.getIntance().get(name.substring(0, maxNameLength));
            if (tokens == null) {
                return null;
            }
            int tokenCount = tokens.size();
            StringBuilder keyBpmf = new StringBuilder();
            StringBuilder keyInitial = new StringBuilder();
            StringBuilder keyOrignal = new StringBuilder();
            StringBuilder keyBpmfNotMap = new StringBuilder();
            StringBuilder keyInitialNotMap = new StringBuilder();
            for (int i = tokenCount - 1; i >= 0; i--) {
                HanziToBPMF.Token token = (HanziToBPMF.Token) tokens.get(i);
                if (2 == token.type) {
                    keyBpmf.insert(0, KeypadNumberUtils.makeActionCodeHKTW(token.target));
                    keyInitial.insert(0, KeypadNumberUtils.convertTraditionalChineseKeypadLettersToDigits(token.target.charAt(0)));
                    keyBpmfNotMap.insert(0, token.target);
                    keyInitialNotMap.insert(0, token.target.charAt(0));
                } else if (1 == token.type) {
                    if (keyBpmf.length() > 0) {
                        keyBpmf.insert(0, ' ');
                        keyBpmfNotMap.insert(0, ' ');
                    }
                    if (keyOrignal.length() > 0) {
                        keyOrignal.insert(0, ' ');
                    }
                    keyBpmf.insert(0, KeypadNumberUtils.makeActionCodeHKTW(token.source));
                    keyInitial.insert(0, KeypadNumberUtils.convertTraditionalChineseKeypadLettersToDigits(token.source.charAt(0)));
                    keyBpmfNotMap.insert(0, token.source);
                    keyInitialNotMap.insert(0, token.source.charAt(0));
                } else {
                    keyBpmf.insert(0, KeypadNumberUtils.makeActionCodeHKTW(token.source));
                    keyInitial.insert(0, KeypadNumberUtils.convertTraditionalChineseKeypadLettersToDigits(token.source.charAt(0)));
                    keyBpmfNotMap.insert(0, token.source);
                    keyInitialNotMap.insert(0, token.source.charAt(0));
                }
                keyOrignal.insert(0, token.source);
                keys.add(keyOrignal.toString());
                keys.add(keyBpmf.toString());
                keys.add(keyBpmfNotMap.toString());
                if (!keyInitial.toString().equals(keyBpmf.toString())) {
                    keys.add(keyInitial.toString());
                    keys.add(keyInitialNotMap.toString());
                }
            }
            return keys.iterator();
        }

        public String getSortKey(String name) {
            ArrayList<HanziToBPMF.Token> tokens = HanziToBPMF.getIntance().get(name);
            if (tokens == null || tokens.size() <= 0) {
                return super.getSortKey(name);
            }
            StringBuilder sb = new StringBuilder();
            Iterator it = tokens.iterator();
            while (it.hasNext()) {
                HanziToBPMF.Token token = (HanziToBPMF.Token) it.next();
                if (2 == token.type) {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(token.target);
                    sb.append(' ');
                    sb.append(token.source);
                } else {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(token.source);
                }
            }
            return sb.toString();
        }

        public String getConsistKey(String name) {
            String trimmedName = name.replace(String.valueOf(' '), " ").trim();
            if (trimmedName.length() > 0 && LocaleUtils.isCJKUnicodeBlock(UnicodeBlock.of(name.charAt(0)))) {
                ArrayList<HanziToBPMF.Token> tokens = HanziToBPMF.getIntance().get(trimmedName.substring(0, 1));
                if (tokens != null && tokens.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    Iterator it = tokens.iterator();
                    if (it.hasNext()) {
                        HanziToBPMF.Token token = (HanziToBPMF.Token) it.next();
                        if (2 == token.type) {
                            sb.append(token.target);
                        } else {
                            sb.append(token.source);
                        }
                    }
                    return sb.toString().substring(0, 1);
                }
            }
            return super.getConsistKey(name);
        }

        public Iterator<String> getNameLookupKeys(String name) {
            return getBPMFNameLookupKeys(name);
        }
    }

    private LocaleUtils() {
        Log.i(TAG, "LocaleUtils : primary = " + Locale.getDefault() + ", list = " + LocaleList.getDefault());
        sLocale = Locale.getDefault();
        if (isChineseTW()) {
            this.mUtils = new TraditionalChineseTWUtils();
        } else if (isChineseHK()) {
            this.mUtils = new TraditionalChineseHKUtils();
        } else if (isChinesePinyinSearching() || (!(!LauncherFeature.isChinaModel() || KOREAN_LANGUAGE.equals(sLocale.getLanguage()) || JAPANESE_LANGUAGE.equals(sLocale.getLanguage())) || CHINESE_LANGUAGE.equals(sLocale.getLanguage()))) {
            this.mUtils = new SimplifiedChineseUtils();
        } else if (JAPANESE_LANGUAGE.equals(sLocale.getLanguage())) {
            this.mUtils = new JapaneseContactUtils();
        } else {
            this.mUtils = new LocaleUtilsBase();
        }
    }

    public static synchronized LocaleUtils getInstance() {
        LocaleUtils localeUtils;
        synchronized (LocaleUtils.class) {
            if (sInstance == null || !Locale.getDefault().equals(sLocale)) {
                sInstance = new LocaleUtils();
            }
            localeUtils = sInstance;
        }
        return localeUtils;
    }

    private static boolean isCJKUnicodeBlock(UnicodeBlock block) {
        return block == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || block == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || block == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B || block == UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || block == UnicodeBlock.CJK_RADICALS_SUPPLEMENT || block == UnicodeBlock.CJK_COMPATIBILITY || block == UnicodeBlock.CJK_COMPATIBILITY_FORMS || block == UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS || block == UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT;
    }

    public static boolean isChineseLookupSearching() {
        return isChineseHK() || isChineseTW() || isChinesePinyinSearching();
    }

    public static boolean isChinesePinyinSearching() {
        return Locale.getDefault().toString().equals(CHINA_LOCALE) || (LauncherFeature.isChinaModel() && ENGLISH_LANGUAGE.equals(Locale.getDefault().getLanguage()));
    }

    public static boolean isChinesePinyinSortingOnApps() {
        return LauncherFeature.isChinaModel() && Locale.getDefault().toString().equals(CHINA_LOCALE);
    }

    public static boolean isChineseHK() {
        return Locale.getDefault().toString().equals(HONGKONG_LOCALE);
    }

    public static boolean isChineseTW() {
        return Locale.getDefault().toString().equals(TAIWAN_LOCALE);
    }

    public Iterator<String> getNameLookupKeys(String name) {
        return this.mUtils.getNameLookupKeys(name);
    }

    public String getSortKey(String name) {
        return this.mUtils.getSortKey(name);
    }

    public String getConsistKey(String name) {
        return this.mUtils.getConsistKey(name);
    }

    public String makeSectionString(String appName, boolean upperCase) {
        if (appName == null) {
            return "";
        }
        String section = appName.replace(String.valueOf(' '), " ").trim();
        if (section.length() <= 0) {
            Log.w(TAG, "cannot make sectionString");
            return section;
        } else if (!Character.isLetterOrDigit(section.charAt(0))) {
            return "&";
        } else {
            if (upperCase) {
                section = section.toUpperCase();
            }
            return getConsistKey(section);
        }
    }
}
