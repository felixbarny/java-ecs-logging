/*-
 * #%L
 * Java ECS logging
 * %%
 * Copyright (C) 2019 Elastic and contributors
 * %%
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * #L%
 */
package co.elastic.logging;

/**
 * This class is based on com.fasterxml.jackson.core.io.CharTypes,
 * under Apache License 2.0
 */
public final class JsonUtils {

    /*
     * A single char can become max 6 chars long, for example "\u0000"
     * we want to write to the buffer 512 times before flushing to the StringBuilder.
     * The advantage is that we can reduce the bounds checks and ensuring capacity
     * to only once every 512 chars as opposed to doing it for every single char.
     */
    static final int CHAR_BUFFER_SIZE = 512 * 6;
    private final static char[] HC = "0123456789ABCDEF".toCharArray();

    /**
     * Lookup table used for determining which output characters in
     * 7-bit ASCII range need to be quoted.
     */
    private final static int[] sOutputEscapes128;
    private final static ThreadLocal<char[]> charBufferThreadLocal = new ThreadLocal<char[]>();

    static {
        int[] table = new int[128];
        // Control chars need generic escape sequence
        for (int i = 0; i < 32; ++i) {
            // 04-Mar-2011, tatu: Used to use "-(i + 1)", replaced with constant
            table[i] = -1;
        }
        // Others (and some within that range too) have explicit shorter sequences
        table['"'] = '"';
        table['\\'] = '\\';
        // Escaping of slash is optional, so let's not add it
        table[0x08] = 'b';
        table[0x09] = 't';
        table[0x0C] = 'f';
        table[0x0A] = 'n';
        table[0x0D] = 'r';
        sOutputEscapes128 = table;
    }

    private static char[] getCharBuffer() {
        char[] charBuffer = charBufferThreadLocal.get();
        if (charBuffer == null) {

            charBuffer = new char[CHAR_BUFFER_SIZE];
            charBufferThreadLocal.set(charBuffer);
        }
        return charBuffer;
    }

    public static void quoteAsString(CharSequence content, StringBuilder sb) {
        char[] buf = getCharBuffer();
        int bufPos = 0;
        final int[] escCodes = sOutputEscapes128;
        final int escLen = escCodes.length;
        for (int i = 0, len = content.length(); i < len; ++i) {
            // flush every 512 iterations
            // the char buffer can at most contain 512 * 6 chars at this point
            if (i > 0 && (i & 511) == 0) {
                sb.append(buf, 0, bufPos);
                bufPos = 0;
            }
            char c = content.charAt(i);
            if (c >= escLen || escCodes[c] == 0) {
                buf[bufPos++] = c;
                continue;
            }
            buf[bufPos++] = '\\';
            int escCode = escCodes[c];
            if (escCode < 0) { // generic quoting (hex value)
                // The only negative value sOutputEscapes128 returns
                // is CharacterEscapes.ESCAPE_STANDARD, which mean
                // appendQuotes should encode using the Unicode encoding;
                // not sure if this is the right way to encode for
                // CharacterEscapes.ESCAPE_CUSTOM or other (future)
                // CharacterEscapes.ESCAPE_XXX values.

                // We know that it has to fit in just 2 hex chars
                buf[bufPos++] = 'u';
                buf[bufPos++] = '0';
                buf[bufPos++] = '0';
                int value = c;  // widening
                buf[bufPos++] = HC[value >> 4];
                buf[bufPos++] = HC[value & 0xF];
            } else { // "named", i.e. prepend with slash
                buf[bufPos++] = (char) escCode;
            }
        }
        sb.append(buf, 0, bufPos);
    }

}

