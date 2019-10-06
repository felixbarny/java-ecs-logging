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

import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class JsonUtilsTest {

    @Test
    void quoteAsString() {
        assertQuoteEquals("", "");
        assertQuoteEquals("foo", "foo");
        assertQuoteEquals("foo\nbar", "foo\\nbar");
        String randomString = RandomString.make(JsonUtils.CHAR_BUFFER_SIZE * 2);
        assertQuoteEquals(randomString, randomString);
        assertQuoteEquals("\u0000", "\\u0000");
        assertQuoteEquals(times("\u0000", JsonUtils.CHAR_BUFFER_SIZE), times("\\u0000", JsonUtils.CHAR_BUFFER_SIZE));
    }

    private static String times(CharSequence cs, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            sb.append(cs);
        }
        return sb.toString();
    }

    private void assertQuoteEquals(String input, String expected) {
        StringBuilder sb = new StringBuilder();
        JsonUtils.quoteAsString(input, sb);
        assertThat(sb.toString()).isEqualTo(expected);
    }
}
