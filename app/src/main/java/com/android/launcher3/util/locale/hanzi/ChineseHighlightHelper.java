package com.android.launcher3.util.locale.hanzi;

import com.android.launcher3.util.locale.hanzi.HanziToPinyin.Token;
//import com.samsung.android.util.SemLog;
import java.util.ArrayList;
import java.util.Iterator;

public class ChineseHighlightHelper {
    private static final int LIMIT_TEXT = 50;
    private static final String TAG = "ChineseHighlightHelper";

    private static ArrayList<String> aggregateMultiPinyins(String[] arrayA, ArrayList<String> arrayB) {
        int lengthAA = arrayA.length;
        int lengthBB = arrayB.size();
        ArrayList<String> mArray = new ArrayList();
        if (lengthAA > 0) {
            for (String anArrayA : arrayA) {
                StringBuilder builder = new StringBuilder();
                builder.append(anArrayA);
                builder.append('|');
                if (lengthBB > 0) {
                    for (int j = 0; j < lengthBB; j++) {
                        mArray.add(builder.toString() + ((String) arrayB.get(j)));
                    }
                } else {
                    mArray.add(builder.toString());
                }
            }
        }
        return mArray;
    }

    private static ArrayList<String> buildMultiPinyinArrayList(String[][] array, int arrayLength) {
        ArrayList<String> mArray = new ArrayList();
        for (int i = arrayLength - 1; i >= 0; i--) {
            mArray = aggregateMultiPinyins(array[i], mArray);
        }
        return mArray;
    }

    private static String[] getMultiPinYinWithPrefixWithoutTokenSource(String[] array, String prefix) {
        int arraySize = array.length;
        for (int i = 0; i < arraySize; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(prefix);
            sb.append('|');
            sb.append(array[i]);
            array[i] = sb.toString();
        }
        return array;
    }

    private static ArrayList<String> getArrayListWithLastSuffix(ArrayList<String> array, String suffix) {
        for (int i = 0; i < array.size(); i++) {
            array.set(i, ((String) array.get(i)) + suffix);
        }
        return array;
    }

    private static ArrayList<String> getMultiPinyinsForName(String name) {
        ArrayList<Token> tokens = HanziToMultiPinyin.getInstance().get(name);
        String[][] arrayB = new String[3][];
        if (tokens == null || tokens.size() <= 0) {
            return new ArrayList();
        }
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        int count = 0;
        Iterator it = tokens.iterator();
        while (it.hasNext()) {
            Token token = (Token) it.next();
            if (2 == token.type) {
                if (sb.length() > 0) {
                    sb.append('|');
                    sb2.append('|');
                }
                String[] arrayA = token.target.split(",");
                if (arrayA.length <= 1) {
                    sb.append(token.target);
                    sb2.append(token.target);
                } else if (count < 3) {
                    sb.append(arrayA[0]);
                    arrayB[count] = getMultiPinYinWithPrefixWithoutTokenSource(arrayA, sb2.toString());
                    sb2 = new StringBuilder();
                    count++;
                } else {
                    sb.append(arrayA[0]);
                    sb2.append(arrayA[0]);
                }
                sb.append('|');
                sb.append(token.source);
            } else {
                if (sb.length() > 0) {
                    sb.append('|');
                }
                sb.append(token.source);
            }
        }
        if (arrayB.length > 0) {
            return getArrayListWithLastSuffix(buildMultiPinyinArrayList(arrayB, count), sb2.toString());
        }
        ArrayList<String> arrayC = new ArrayList();
        arrayC.add(sb.toString());
        return arrayC;
    }

    private static String[] getDuoYinziString(String inString) {
        String[] mOut = new String[]{"", ""};
        if (inString.length() > 1) {
            mOut[0] = inString;
            mOut[1] = inString;
        } else {
            ArrayList<String> multiPinyin = getMultiPinyinsForName(inString);
            int pinyinCount = 0;
            if (multiPinyin != null) {
                pinyinCount = multiPinyin.size();
            }
            if (multiPinyin == null || pinyinCount <= 1) {
                mOut[0] = HanziToPinyin.getInstance().getPinyin(inString.charAt(0)).toUpperCase();
                mOut[1] = HanziToPinyin.getInstance().getPinyin(inString.charAt(0)).toUpperCase();
            } else {
                for (int i = 0; i < 2; i++) {
                    String[] array = ((String) multiPinyin.get(i)).split("\\|");
                    for (int j = array.length - 1; j >= 0; j--) {
                        if (!"".equals(array[j])) {
                            mOut[i] = array[j];
                        }
                    }
                }
            }
        }
        return mOut;
    }

    private static int getCompareNormalizedString(String target, String reg) {
        String[] cpStrDuo = getDuoYinziString(target);
        String[] cpStr = new String[]{NameNormalizer.normalize(cpStrDuo[0]), NameNormalizer.normalize(cpStrDuo[1])};
        String normalizedReg = NameNormalizer.normalize(reg);
        if (normalizedReg == null) {
            return 0;
        }
        if ((cpStr[0] != null && cpStr[0].indexOf(normalizedReg) == 0) || (cpStr[1] != null && cpStr[1].indexOf(normalizedReg) == 0)) {
            return 1;
        }
        String normalizedTarget = NameNormalizer.normalize(target);
        if (normalizedTarget == null || normalizedTarget.indexOf(normalizedReg) != 0) {
            return 0;
        }
        return 1;
    }

    private static boolean isLatin(String prefix) {
        return prefix.matches("\\p{ASCII}+");
    }

    public static boolean findPosToPinyin(CharSequence text, String prefix, int[] indexes) {
        if (prefix == null || text == null) {
            return false;
        }
        if (text.length() > LIMIT_TEXT && !isLatin(prefix)) {
            return false;
        }
        int textLength = text.length();
        int prefixLength = prefix.length();
        if (prefixLength == 0) {
            return false;
        }
        int count;
        boolean cpResult = false;
        ArrayList<Token> targetToken = HanziToPinyin.getInstance().get(text.toString());
        ArrayList<Integer> targetAddr = new ArrayList();
        ArrayList<String> targetStr = new ArrayList();
        ArrayList<String> regArray = new ArrayList();
        int[] targetInitialAddr = new int[targetToken.size()];
        for (count = 0; count < prefixLength; count++) {
            if (prefix.charAt(count) != ' ') {
                regArray.add(String.valueOf(prefix.charAt(count)));
            }
        }
        for (count = 0; count < textLength; count++) {
            if (text.charAt(count) != ' ') {
                targetAddr.add(Integer.valueOf(count));
                targetStr.add(String.valueOf(Character.toUpperCase(text.charAt(count))));
            }
        }
        int tAddr = 0;
        count = 0;
        while (count < targetToken.size()) {
            try {
                targetInitialAddr[count] = tAddr;
                tAddr += ((Token) targetToken.get(count)).source.length();
                count++;
            } catch (RuntimeException e) {
                //SemLog.secE(TAG, "search highlight RuntimeException!");
                return false;
            } catch (Exception e2) {
                //SemLog.secE(TAG, "search highlight exception!");
                return false;
            }
        }
        for (count = 0; count < targetInitialAddr.length; count++) {
            cpResult = false;
            int tCount = targetInitialAddr[count];
            int chk1;
            String tStr;
            int chk2;
            if (getCompareNormalizedString((String) targetStr.get(tCount), (String) regArray.get(0)) > 0) {
                cpResult = true;
                indexes[0] = ((Integer) targetAddr.get(tCount)).intValue();
                if (regArray.size() == 1) {
                    return true;
                }
                int tCheck1 = -1;
                int tCheck2 = -1;
                if (targetStr.size() > tCount + 1 && regArray.size() > 1 && regArray.size() <= targetStr.size()) {
                    tCheck1 = getCompareNormalizedString((String) targetStr.get(tCount + 1), (String) regArray.get(1));
                    if (tCheck1 > 0 && regArray.size() + tCount > targetStr.size()) {
                        tCheck1 = 0;
                    }
                }
                if (targetInitialAddr.length > count + 1 && regArray.size() > 1 && regArray.size() <= targetStr.size()) {
                    tCheck2 = getCompareNormalizedString((String) targetStr.get(targetInitialAddr[count + 1]), (String) regArray.get(1));
                    if (tCheck2 > 0 && regArray.size() + tCount > targetStr.size()) {
                        tCheck2 = 0;
                    }
                }
                if (tCheck1 > 0) {
                    for (chk1 = 1; chk1 < regArray.size(); chk1++) {
                        if (getCompareNormalizedString((String) targetStr.get(tCount + chk1), (String) regArray.get(chk1)) > 0) {
                            indexes[chk1] = ((Integer) targetAddr.get(tCount + chk1)).intValue();
                        } else {
                            cpResult = false;
                        }
                    }
                } else if (tCheck2 > 0) {
                    for (chk1 = 1; chk1 < regArray.size(); chk1++) {
                        if (getCompareNormalizedString((String) targetStr.get(targetInitialAddr[count + chk1]), (String) regArray.get(chk1)) > 0) {
                            indexes[chk1] = ((Integer) targetAddr.get(targetInitialAddr[count + chk1])).intValue();
                        } else {
                            cpResult = false;
                        }
                    }
                } else {
                    if (regArray.size() > 1) {
                        if (getCompareNormalizedString(String.valueOf(((String) targetStr.get(tCount)).charAt(0)), (String) regArray.get(0)) > 0) {
                            tStr = "";
                            String[] tStrD = new String[]{"", ""};
                            ArrayList<String> tStrArray = new ArrayList();
                            chk1 = 0;
                            int tStrLength = 0;
                            while (tStrLength < regArray.size()) {
                                tStrD = getDuoYinziString((String) targetStr.get(tCount + chk1));
                                if (chk1 == 0) {
                                    tStrArray.add(tStrD[0]);
                                    if (!tStrD[0].equals(tStrD[1])) {
                                        tStrArray.add(tStrD[1]);
                                    }
                                } else {
                                    int tStrSize = tStrArray.size();
                                    for (chk2 = 0; chk2 < tStrSize; chk2++) {
                                        String tmp1 = (String) tStrArray.get(chk2);
                                        tStrArray.set(chk2, tmp1 + tStrD[0]);
                                        if (!tStrD[0].equals(tStrD[1])) {
                                            tStrArray.add(tmp1 + tStrD[1]);
                                        }
                                    }
                                }
                                tStrLength += tStrD[0].length() > tStrD[1].length() ? tStrD[0].length() : tStrD[1].length();
                                chk1++;
                            }
                            for (int chk3 = 0; chk3 < tStrArray.size(); chk3++) {
                                if (getCompareNormalizedString((String) tStrArray.get(chk3), prefix.trim()) > 0) {
                                    for (chk2 = 0; chk2 < regArray.size(); chk2++) {
                                        if (chk2 < chk1) {
                                            indexes[chk2] = ((Integer) targetAddr.get(tCount + chk2)).intValue();
                                        } else {
                                            indexes[chk2] = ((Integer) targetAddr.get((tCount + chk1) - 1)).intValue();
                                        }
                                    }
                                    return true;
                                }
                            }
                            cpResult = false;
                        }
                    }
                    cpResult = false;
                }
                if (cpResult) {
                    return cpResult;
                }
            } else if (regArray.size() > 1) {
                if (getCompareNormalizedString(String.valueOf(((String) targetStr.get(tCount)).charAt(0)), (String) regArray.get(0)) > 0) {
                    tStr = "";
                    chk1 = 0;
                    while (tStr.length() < regArray.size()) {
                        StringBuffer sb = new StringBuffer();
                        sb.append(tStr);
                        sb.append(HanziToPinyin.getInstance().getPinyin(((String) targetStr.get(count + chk1)).charAt(0)));
                        tStr = sb.toString();
                        chk1++;
                    }
                    if (getCompareNormalizedString(tStr, prefix.trim()) <= 0) {
                        return false;
                    }
                    for (chk2 = 0; chk2 < regArray.size(); chk2++) {
                        if (chk2 < chk1) {
                            indexes[chk2] = ((Integer) targetAddr.get(count + chk2)).intValue();
                        } else {
                            indexes[chk2] = ((Integer) targetAddr.get((count + chk1) - 1)).intValue();
                        }
                    }
                    return true;
                }
            } else {
                continue;
            }
        }
        return cpResult;
    }
}
