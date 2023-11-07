package com.jrasp.agent.core.json;

/*
Public Domain.
*/

import java.io.Reader;

/**
 * The XMLTokener extends the JSONTokener to provide additional methods
 * for the parsing of XML texts.
 * @author JSON.org
 * @version 2015-12-09
 */
public class XMLTokener extends JSONTokener {


   /** The table of entity values. It initially contains Character values for
    * amp, apos, gt, lt, quot.
    */
   public static final java.util.HashMap<String, Character> entity;

   static {
       entity = new java.util.HashMap<String, Character>(8);
       entity.put("amp",  XML.AMP);
       entity.put("apos", XML.APOS);
       entity.put("gt",   XML.GT);
       entity.put("lt",   XML.LT);
       entity.put("quot", XML.QUOT);
   }

    /**
     * Construct an XMLTokener from a Reader.
     * @param r A source reader.
     */
    public XMLTokener(Reader r) {
        super(r);
    }

    /**
     * Construct an XMLTokener from a string.
     * @param s A source string.
     */
    public XMLTokener(String s) {
        super(s);
    }

    /**
     * Get the text in the CDATA block.
     * @return The string up to the <code>]]&gt;</code>.
     * @throws JSONException If the <code>]]&gt;</code> is not found.
     */
    public String nextCDATA() throws JSONException {
        char         c;
        int          i;
        StringBuilder sb = new StringBuilder();
        while (more()) {
            c = next();
            sb.append(c);
            i = sb.length() - 3;
            if (i >= 0 && sb.charAt(i) == ']' &&
                          sb.charAt(i + 1) == ']' && sb.charAt(i + 2) == '>') {
                sb.setLength(i);
                return sb.toString();
            }
        }
        throw syntaxError("Unclosed CDATA");
    }


    /**
     * Get the next XML outer token, trimming whitespace. There are two kinds
     * of tokens: the <pre>{@code '<' }</pre> character which begins a markup
     * tag, and the content
     * text between markup tags.
     *
     * @return  A string, or a <pre>{@code '<' }</pre> Character, or null if
     * there is no more source text.
     * @throws JSONException if a called function has an error
     */
    public Object nextContent() throws JSONException {
        char         c;
        StringBuilder sb;
        do {
            c = next();
        } while (Character.isWhitespace(c));
        if (c == 0) {
            return null;
        }
        if (c == '<') {
            return XML.LT;
        }
        sb = new StringBuilder();
        for (;;) {
            if (c == 0) {
                return sb.toString().trim();
            }
            if (c == '<') {
                back();
                return sb.toString().trim();
            }
            if (c == '&') {
                sb.append(nextEntity(c));
            } else {
                sb.append(c);
            }
            c = next();
        }
    }


    /**
     * <pre>{@code
     * Return the next entity. These entities are translated to Characters:
     *     &amp;  &apos;  &gt;  &lt;  &quot;.
     * }</pre>
     * @param ampersand An ampersand character.
     * @return  A Character or an entity String if the entity is not recognized.
     * @throws JSONException If missing ';' in XML entity.
     */
    public Object nextEntity(@SuppressWarnings("unused") char ampersand) throws JSONException {
        StringBuilder sb = new StringBuilder();
        for (;;) {
            char c = next();
            if (Character.isLetterOrDigit(c) || c == '#') {
                sb.append(Character.toLowerCase(c));
            } else if (c == ';') {
                break;
            } else {
                throw syntaxError("Missing ';' in XML entity: &" + sb);
            }
        }
        String string = sb.toString();
        return unescapeEntity(string);
    }
    
    /**
     * Unescape an XML entity encoding;
     * @param e entity (only the actual entity value, not the preceding & or ending ;
     * @return
     */
    static String unescapeEntity(String e) {
        // validate
        if (e == null || e.isEmpty()) {
            return "";
        }
        // if our entity is an encoded unicode point, parse it.
        if (e.charAt(0) == '#') {
            int cp;
            if (e.charAt(1) == 'x' || e.charAt(1) == 'X') {
                // hex encoded unicode
                cp = Integer.parseInt(e.substring(2), 16);
            } else {
                // decimal encoded unicode
                cp = Integer.parseInt(e.substring(1));
            }
            return new String(new int[] {cp},0,1);
        } 
        Character knownEntity = entity.get(e);
        if(knownEntity==null) {
            // we don't know the entity so keep it encoded
            return '&' + e + ';';
        }
        return knownEntity.toString();
    }


    /**
     * <pre>{@code 
     * Returns the next XML meta token. This is used for skipping over <!...>
     * and <?...?> structures.
     *  }</pre>
     * @return <pre>{@code Syntax characters (< > / = ! ?) are returned as
     *  Character, and strings and names are returned as Boolean. We don't care
     *  what the values actually are.
     *  }</pre>
     * @throws JSONException If a string is not properly closed or if the XML
     *  is badly structured.
     */
    public Object nextMeta() throws JSONException {
        char c;
        char q;
        do {
            c = next();
        } while (Character.isWhitespace(c));
        switch (c) {
        case 0:
            throw syntaxError("Misshaped meta tag");
        case '<':
            return XML.LT;
        case '>':
            return XML.GT;
        case '/':
            return XML.SLASH;
        case '=':
            return XML.EQ;
        case '!':
            return XML.BANG;
        case '?':
            return XML.QUEST;
        case '"':
        case '\'':
            q = c;
            for (;;) {
                c = next();
                if (c == 0) {
                    throw syntaxError("Unterminated string");
                }
                if (c == q) {
                    return Boolean.TRUE;
                }
            }
        default:
            for (;;) {
                c = next();
                if (Character.isWhitespace(c)) {
                    return Boolean.TRUE;
                }
                switch (c) {
                case 0:
                    throw syntaxError("Unterminated string");
                case '<':
                case '>':
                case '/':
                case '=':
                case '!':
                case '?':
                case '"':
                case '\'':
                    back();
                    return Boolean.TRUE;
                }
            }
        }
    }


    /**
     * <pre>{@code
     * Get the next XML Token. These tokens are found inside of angle
     * brackets. It may be one of these characters: / > = ! ? or it
     * may be a string wrapped in single quotes or double quotes, or it may be a
     * name.
     * }</pre>
     * @return a String or a Character.
     * @throws JSONException If the XML is not well formed.
     */
    public Object nextToken() throws JSONException {
        char c;
        char q;
        StringBuilder sb;
        do {
            c = next();
        } while (Character.isWhitespace(c));
        switch (c) {
        case 0:
            throw syntaxError("Misshaped element");
        case '<':
            throw syntaxError("Misplaced '<'");
        case '>':
            return XML.GT;
        case '/':
            return XML.SLASH;
        case '=':
            return XML.EQ;
        case '!':
            return XML.BANG;
        case '?':
            return XML.QUEST;

// Quoted string

        case '"':
        case '\'':
            q = c;
            sb = new StringBuilder();
            for (;;) {
                c = next();
                if (c == 0) {
                    throw syntaxError("Unterminated string");
                }
                if (c == q) {
                    return sb.toString();
                }
                if (c == '&') {
                    sb.append(nextEntity(c));
                } else {
                    sb.append(c);
                }
            }
        default:

// Name

            sb = new StringBuilder();
            for (;;) {
                sb.append(c);
                c = next();
                if (Character.isWhitespace(c)) {
                    return sb.toString();
                }
                switch (c) {
                case 0:
                    return sb.toString();
                case '>':
                case '/':
                case '=':
                case '!':
                case '?':
                case '[':
                case ']':
                    back();
                    return sb.toString();
                case '<':
                case '"':
                case '\'':
                    throw syntaxError("Bad character in a name");
                }
            }
        }
    }


    /**
     * Skip characters until past the requested string.
     * If it is not found, we are left at the end of the source with a result of false.
     * @param to A string to skip past.
     */
    // The Android implementation of JSONTokener has a public method of public void skipPast(String to)
    // even though ours does not have that method, to have API compatibility, our method in the subclass
    // should match.
    public void skipPast(String to) {
        boolean b;
        char c;
        int i;
        int j;
        int offset = 0;
        int length = to.length();
        char[] circle = new char[length];

        /*
         * First fill the circle buffer with as many characters as are in the
         * to string. If we reach an early end, bail.
         */

        for (i = 0; i < length; i += 1) {
            c = next();
            if (c == 0) {
                return;
            }
            circle[i] = c;
        }

        /* We will loop, possibly for all of the remaining characters. */

        for (;;) {
            j = offset;
            b = true;

            /* Compare the circle buffer with the to string. */

            for (i = 0; i < length; i += 1) {
                if (circle[j] != to.charAt(i)) {
                    b = false;
                    break;
                }
                j += 1;
                if (j >= length) {
                    j -= length;
                }
            }

            /* If we exit the loop with b intact, then victory is ours. */

            if (b) {
                return;
            }

            /* Get the next character. If there isn't one, then defeat is ours. */

            c = next();
            if (c == 0) {
                return;
            }
            /*
             * Shove the character in the circle buffer and advance the
             * circle offset. The offset is mod n.
             */
            circle[offset] = c;
            offset += 1;
            if (offset >= length) {
                offset -= length;
            }
        }
    }
}
