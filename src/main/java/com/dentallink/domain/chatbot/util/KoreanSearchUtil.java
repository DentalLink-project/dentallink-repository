package com.dentallink.domain.chatbot.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 한글 초성 검색 유틸리티 (라이브러리 없이 순수 구현)
 * - 초성만 분리해서 검색 가능
 * - 예: "은지" 검색시 "은지 치과", "은지내과" 모두 검색 가능
 * - 초성 또는 완전한 단어로 검색 가능
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KoreanSearchUtil {

    // 한글 음절의 유니코드 범위
    private static final int KOREAN_CHAR_START = 0xAC00; // '가'
    private static final int KOREAN_CHAR_END = 0xD7A3;   // '힣'
    private static final int KOREAN_INITIAL_COUNT = 19;  // 초성 개수
    private static final int KOREAN_MEDIAL_COUNT = 21;   // 중성 개수

    // 초성 배열
    private static final char[] CHOSUNG = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ',
            'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    /**
     * 한글 음절을 초성으로 변환
     * 예: '은' → 'ㅇ', '지' → 'ㅈ'
     */
    private static char getChosung(char ch) {
        if (ch < KOREAN_CHAR_START || ch > KOREAN_CHAR_END) {
            return ch; // 한글이 아니면 원본 반환
        }

        int temp = ch - KOREAN_CHAR_START;
        int chosung = temp / (KOREAN_MEDIAL_COUNT * 28); // 초성 인덱스
        return CHOSUNG[chosung];
    }

    /**
     * 입력 문자열을 초성으로 변환
     * 예: "은지" → "ㅇㅈ"
     * 예: "김철수" → "ㄱㅊㅅ"
     */
    public static String toChosung(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        try {
            StringBuilder result = new StringBuilder();
            for (char ch : input.toCharArray()) {
                if (ch >= KOREAN_CHAR_START && ch <= KOREAN_CHAR_END) {
                    // 한글 음절 → 초성으로 변환
                    result.append(getChosung(ch));
                } else if ((ch >= 'a' && ch <= 'z') ||
                           (ch >= 'A' && ch <= 'Z') ||
                           (ch >= '0' && ch <= '9') ||
                           ch == ' ' || ch == '-') {
                    // 영문, 숫자, 공백, 하이픈은 그대로 유지
                    result.append(ch);
                }
                // 나머지 특수문자는 제외
            }
            return result.toString();
        } catch (Exception e) {
            log.warn("초성 변환 실패: {}", input, e);
            return input; // 변환 실패 시 원본 반환
        }
    }

    /**
     * 검색어와 데이터베이스 데이터를 매칭
     * - 정확한 문자열 매칭
     * - 초성 매칭
     * - 영문 매칭 (대소문자 구분 없음)
     *
     * 예:
     * - "은지" ✓ "은지 치과"
     * - "ㅇㅈ" ✓ "은지 치과"
     * - "eunji" ✓ "Eunji Dental"
     * - "은" ✓ "은지" (부분 매칭)
     */
    public static boolean matches(String target, String query) {
        if (target == null || query == null) {
            return false;
        }

        if (target.isEmpty() || query.isEmpty()) {
            return false;
        }

        // 1. 원본 문자열로 매칭 (완벽한 일치)
        if (target.toLowerCase().contains(query.toLowerCase())) {
            return true;
        }

        // 2. 초성으로 매칭
        try {
            String targetChosung = toChosung(target);
            String queryChosung = toChosung(query);

            // 초성 문자열 포함 여부 확인
            if (targetChosung.contains(queryChosung)) {
                return true;
            }

            // 3. 검색어가 이미 초성인 경우 직접 비교
            if (isChosung(query) && targetChosung.contains(query)) {
                return true;
            }

            return false;
        } catch (Exception e) {
            log.warn("초성 매칭 실패: target={}, query={}", target, query, e);
            return false;
        }
    }

    /**
     * 문자열이 모두 초성으로만 이루어져 있는지 확인
     * 예: "ㅇㅈ" → true, "은지" → false
     */
    private static boolean isChosung(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        for (char ch : str.toCharArray()) {
            // 초성 문자 범위: ㄱ~ㅍ (U+1100 ~ U+1112)
            if (!((ch >= '\u1100' && ch <= '\u1112') ||
                  (ch >= 'a' && ch <= 'z') ||
                  (ch >= 'A' && ch <= 'Z') ||
                  (ch >= '0' && ch <= '9') ||
                  ch == ' ' || ch == '-')) {
                return false;
            }
        }
        return true;
    }
}
