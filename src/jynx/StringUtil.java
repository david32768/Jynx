package jynx;

import java.util.stream.Collectors;

import static jynx.Message.M69;
import static jynx.Message.M80;
import static jynx.Message.M83;
import static jynx.Message.M905;

public class StringUtil {

    // Process Unicode escapes
    public static String unescapeUnicode(String token) {
        StringBuilder sb = new StringBuilder(token.length());
        StringState state = StringState.QUOTE;
        for (int i = 0;i < token.length();i++) {
            char c = token.charAt(i);
            switch(state) {
                case QUOTE:
                    if (c == '\\') state = StringState.SLASH;
                    else sb.append(c);
                    break;
                case SLASH:
                    if (c == 'u') {
                        try {
                            String unicode = token.substring(i+1, i+5);
                            c = (char)Integer.parseInt(unicode, 16);
                            i += 4;
                        } catch (NumberFormatException | IndexOutOfBoundsException ex) {
                            sb.append('\\'); // Bad unicode sequence;
                        }
                    } else sb.append('\\');
                    sb.append(c);
                    state = StringState.QUOTE;
                    break;
                default:
                    throw new LogAssertionError(M905,state); // "unexpected StringState %s"
            }
        }
        if (state == StringState.SLASH) {
            sb.append('\\');
        }
        return sb.toString();
    }

    private static final String ESCAPE_FROM = "\b\t\n\f\r\\\"'";
    private static final String ESCAPE_TO = "btnfr\\\"'";

    // Process escape sequences
    public static String unescapeSequence(String token) {
        StringBuilder sb = new StringBuilder(token.length());
        StringState state = StringState.QUOTE;
        for (int i = 0;i < token.length();i++) {
            char c = token.charAt(i);
            switch(state) {
                case QUOTE:
                    if (c == '\"') {
                        // "Embedded naked quote"
                        throw new LogIllegalArgumentException(M69);
                    }
                    if (c == '\\') state = StringState.SLASH;
                    else sb.append(c);
                    break;
                case SLASH:
                    int index = ESCAPE_TO.indexOf(c);
                    if (index >= 0) {
                        c = ESCAPE_FROM.charAt(index);
                    } else {
                        try {
                            int len = token.length() - i;
                            len = Math.min(len, 3);
                            if (c > '3') {
                                len = Math.min(len,2);
                            } 
                            String octal = token.substring(i, i+len);
                            c = (char)Integer.parseInt(octal, 8);
                            i += len - 1;
                        } catch (NumberFormatException ex) {
                            // "Bad octal sequence"
                            throw new LogIllegalArgumentException(M80);
                        }
                    }
                    sb.append(c);
                    state = StringState.QUOTE;
                    break;
                default:
                    throw new LogAssertionError(M905,state); // "unexpected StringState %s"
            }
        }
        if (state != StringState.QUOTE) {
            // "Bad escape sequence"
            throw new LogIllegalArgumentException(M83);
        }
        return sb.toString();
    }

    public static String unescapeString(String token) {
        if (token != null && token.charAt(0) == '\"') {
            token = token.substring(1, token.length() - 1);
        } else {
            return token;
        }
        token = unescapeUnicode(token);
        return unescapeSequence(token);
    }

    public static String escapeChar(char c) {
        StringBuilder sb = new StringBuilder();
        int index = ESCAPE_FROM.indexOf(c);
        if (index >= 0) {
            sb.append('\\');
            c = ESCAPE_TO.charAt(index);
            sb.append(c);
        } else if (isPrintable(c)) {
            sb.append(c);
        } else {
            String unicode = String.format("\\u%04x",(int)c);
            sb.append(unicode);
        }
        return sb.toString();
    }
    
    public static String stringEscape(String token) {
        StringBuilder sb = new StringBuilder(token.length());
        sb.append('"');
        for (int i = 0;i < token.length();i++) {
            char c = token.charAt(i);
            sb.append(escapeChar(c));
        }
        sb.append('"');
        return sb.toString();
    }
    
    public static boolean isVisible(int c) {
        return c > 0x20 && c <= 0x7f; // disallow blank in non-quoted tokens
    }
    
    public static boolean isPrintable(int c) {
        return c >= 0x20 && c <= 0x7f;
    }
    
    private static boolean isVisible(String str) {
        return str.chars()
                .allMatch(StringUtil::isVisible);
    }
    
    public static boolean isPrintable(String str) {
        return str.chars()
                .allMatch(StringUtil::isPrintable);
    }
    
    public static String highlightUnprintables(String str) {
        return str.chars()
                .map(c->isPrintable(c)? c: '?')
                .mapToObj(String::valueOf)
                .collect(Collectors.joining());
    }
    
    public static boolean isLowerCaseAlpha(String str) {
        return str.chars()
                .allMatch(Character::isLowerCase);
    }
    
    public static String unicodeEscape(String str) {
        return str.chars()
                .sequential()
                .mapToObj(c->isPrintable(c)?String.valueOf((char)c):String.format("\\u%04x",c))
                .collect(Collectors.joining());
    }

    private static String nameEscape(String token) {
        return '\'' + unicodeEscape(token) + '\'';
    }
    
    public static String escapeName(String str) {
        if (str == null) {
            return null;
        }
        if (isVisible(str) && !isLowerCaseAlpha(str)) { // if not unicode nor possible reserved word
            return str;
        }
        return nameEscape(str);
    }
    
    public static String visible(String str) {
        if (str == null) {
            return null;
        }
        if (str.isEmpty()) {
            return "''";
        }
        if (str.charAt(0) == '\"') {
            str = unescapeString(str);
            return stringEscape(str);
        }
        if (StringUtil.isVisible(str) && !str.trim().isEmpty()) {
            return str;
        } else {
            return nameEscape(str);
        }
    }
    
    public static String printable(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        if (str.charAt(0) == '\"') {
            str = unescapeString(str);
            return stringEscape(str);
        }
        if (StringUtil.isPrintable(str)) {
            return str;
        } else {
            return nameEscape(str);
        }
    }
    
}
