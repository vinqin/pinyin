package edu.stu.pinyin;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author vinqin
 * @version create time -- Mon Jul 3 CST 2018
 */
public class PinyinHelper {
    static final Map<String, String> PINYIN_TABLE = PinyinResource.getPinyinResource();
    static final Map<String, String> MULTI_PINYIN_TABLE = PinyinResource.getMultiPinyinResource();
    private static final String[] CHINESE_WORDS = new String[MULTI_PINYIN_TABLE.size()];

    static {
        int count = 0;
        for (String word : MULTI_PINYIN_TABLE.keySet()) {
            CHINESE_WORDS[count++] = word;
        }
    }

    static final TreeMapTrie TREE_MAP_TRIE = TreeMapTrie.getInstance(CHINESE_WORDS);
    private static final String PINYIN_SEPARATOR = ","; // 拼音分隔符
    private static final char CHINESE_LING = '〇';
    private static final String ALL_UNMARKED_VOWEL = "aeiouv";
    private static final String ALL_MARKED_VOWEL = "āáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜ"; // 所有带声调的拼音字母

    private PinyinHelper() {
    }

    /**
     * Convert pinyin to the format which contains tone with number
     * <p>
     * eg: <br/>
     * "mā,ma,duì,nǐ,liǎo,rú,zhǐ,zhǎng" convert to
     * [ma1, ma5, dui4, ni3, liao3, ru2, zhi3, zhang3]
     * </p>
     *
     * @param pinyinString the pinyin string which contains tone with mark originally
     * @return pinyin contains tone with number
     */
    static String[] convertWithToneNumber(String pinyinString) {
        String[] pinyinArray = pinyinString.split(PINYIN_SEPARATOR);
        for (int i = pinyinArray.length - 1; i > -1; i--) { // 从后往前替换音标
            boolean hasMarkedChar = false; // 拼音pinyinArray[i]是否有音标
            String originalPinyin = pinyinArray[i].replace("ü", "v"); // 将拼音中的ü替换为v

            for (int j = originalPinyin.length() - 1; j > -1; j--) {
                char oldChar = originalPinyin.charAt(j);

                // 搜索带声调的拼音字母，如果存在则替换为对应不带声调的英文字母
                if (oldChar < 'a' || oldChar > 'z') {
                    int indexOfFirstMarked = ALL_MARKED_VOWEL.indexOf(oldChar);
                    int toneNumber = indexOfFirstMarked % 4 + 1; //声调数
                    char newChar = ALL_UNMARKED_VOWEL.charAt(indexOfFirstMarked / 4);
                    pinyinArray[i] = originalPinyin.replace(oldChar, newChar) + toneNumber;
                    hasMarkedChar = true;
                    break;
                }
            }

            if (!hasMarkedChar) {
                // 找不到带声调的拼音字母说明是轻声，用数字5表示
                pinyinArray[i] = originalPinyin + 5;
            }

        }

        return pinyinArray;
    }

    /**
     * Convert pinyin to the format which contains tone without mark
     * <p>
     * eg: <br/>
     * "mā,ma,duì,nǐ,liǎo,rú,zhǐ,zhǎng" convert to
     * [ma, ma, dui, ni, liao, ru, zhi, zhang]
     * </p>
     *
     * @param pinyinString the pinyin string which contains tone with mark originally
     * @return pinyin contains tone without mark
     */
    static String[] convertWithoutTone(String pinyinString) {
        String[] pinyinArray;

        for (int i = ALL_MARKED_VOWEL.length() - 1; i > -1; i--) {
            char oldChar = ALL_MARKED_VOWEL.charAt(i);
            char newChar = ALL_UNMARKED_VOWEL.charAt(i / 4);
            pinyinString = pinyinString.replace(oldChar, newChar);
        }

        pinyinArray = pinyinString.replace("ü", "v").split(PINYIN_SEPARATOR);
        return pinyinArray;
    }

    /**
     * Convert the pinyin which contains tone with mark originally to specified
     * {@linkplain edu.stu.pinyin.PinyinFormat pinyin format}.
     *
     * @param pinyinString the pinyin string which contains tone with mark
     * @param pinyinFormat pinyin format
     * @return specified format of pinyin
     */
    static String[] formatPinyin(String pinyinString, PinyinFormat pinyinFormat) {
        switch (pinyinFormat) {
            case WITH_TONE_MARK:
                return pinyinString.split(PINYIN_SEPARATOR);
            case WITH_TONE_NUMBER:
                return convertWithToneNumber(pinyinString);
            case WITHOUT_TONE:
                return convertWithoutTone(pinyinString);
            default:
                return new String[0];
        }
    }

    /**
     * Convert one single Chinese character to specified {@linkplain edu.stu.pinyin.PinyinFormat pinyin format}
     *
     * @param c            Chinese character
     * @param pinyinFormat pinyin format
     * @return specified format of pinyin
     */
    public static String[] convertToPinyinArray(char c, PinyinFormat pinyinFormat) {
        String pinyinString = PINYIN_TABLE.get(String.valueOf(c));
        if ((null != pinyinString) && (!pinyinString.equals("null"))) {
            Set<String> set = new LinkedHashSet<>();
            set.addAll(Arrays.asList(formatPinyin(pinyinString, pinyinFormat))); // remove all duplicated elements
            return set.toArray(new String[set.size()]);
        }
        return new String[0];
    }

    /**
     * Convert one single Chinese character to pinyin with tone mark
     *
     * @param c Chinese character
     * @return pinyin with tone mark
     */
    public static String[] convertToPinyinArray(char c) {
        return convertToPinyinArray(c, PinyinFormat.WITH_TONE_MARK);
    }

    /**
     * Convert string containing Chinese characters to specified {@linkplain edu.stu.pinyin.PinyinFormat pinyin format}
     *
     * @param statement    the string which contains Chinese characters
     * @param separator    the sign to separate each pinyin
     * @param pinyinFormat the format of pinyin
     * @return the string of pinyin with specified format
     */
    public static String convertToPinyinString(String statement, String separator, PinyinFormat pinyinFormat) {
        Map<String, Boolean> resultMap = TREE_MAP_TRIE.splitByMultiPinyin(statement);
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Boolean> entry : resultMap.entrySet()) {
            String chineseStr = entry.getKey();
            boolean multiPinyinFlag = entry.getValue();

            if (multiPinyinFlag) {// 多音字
                String multiPinyin = MULTI_PINYIN_TABLE.get(chineseStr);
                String[] multiPinyinArray;
                if ((null != multiPinyin) && (!multiPinyin.equals("null"))) {
                    multiPinyinArray = formatPinyin(multiPinyin, pinyinFormat);
                    if (multiPinyinArray.length == 0) {
                        multiPinyinArray = new String[1];
                        multiPinyinArray[0] = chineseStr;
                    }
                } else {
                    multiPinyinArray = new String[1];
                    multiPinyinArray[0] = chineseStr; // 多音字map中找不到对应拼音，则原样返回汉字字符串
                }
                result.append(combineElementOfStringArray(multiPinyinArray, separator));
            } else {
                String[] pinyinArray = new String[chineseStr.length()];
                for (int i = 0, k = 0, len = chineseStr.length(); i < len; i++, k++) {
                    char c = chineseStr.charAt(i);
                    if (!ChineseHelper.isChineseCharacter(c)) {
                        pinyinArray[k] = String.valueOf(c); // if this character is not a Chinese character
                        continue;
                    }
                    String[] tmp = convertToPinyinArray(c, pinyinFormat);
                    if (0 == tmp.length) {
                        pinyinArray[k] = String.valueOf(c);
                    } else {
                        pinyinArray[k] = tmp[0];
                    }
                }
                result.append(combineElementOfStringArray(pinyinArray, separator));
            }
        }

        return result.toString();
    }

    /**
     * Convert string containing Chinese characters to pinyin with tone mark
     *
     * @param statement the string which contains Chinese characters
     * @param separator the sign to separate each pinyin
     * @return the string of pinyin with tone mark
     */
    public static String convertToPinyinString(String statement, String separator) {
        return convertToPinyinString(statement, separator, PinyinFormat.WITH_TONE_MARK);
    }

    private static String combineElementOfStringArray(String[] pinyinArray, String separator) {
        StringBuilder sb = new StringBuilder();
        sb.append(pinyinArray[0]);
        for (int i = 1; i < pinyinArray.length; i++) {
            sb.append(separator);
            sb.append(pinyinArray[i]);
        }
        return sb.toString();
    }

    /**
     * Whether a character has multiple pinyin or not.
     *
     * @param c the character need to validate
     * @return true if c has multiple pinyin, or false
     */
    public static boolean isMultiPinyin(char c) {
        String[] pinyinArray = convertToPinyinArray(c);
        return pinyinArray.length > 1;
    }


}