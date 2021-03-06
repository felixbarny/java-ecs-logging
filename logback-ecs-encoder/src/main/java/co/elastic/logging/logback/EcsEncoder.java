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
package co.elastic.logging.logback;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.EncoderBase;
import co.elastic.logging.EcsJsonSerializer;
import co.elastic.logging.JsonUtils;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

public class EcsEncoder extends EncoderBase<ILoggingEvent> {

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private String serviceName;
    private ThrowableProxyConverter throwableProxyConverter;
    private Set<String> topLevelLabels = new HashSet<String>(EcsJsonSerializer.DEFAULT_TOP_LEVEL_LABELS);

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public void start() {
        super.start();
        throwableProxyConverter = new ThrowableProxyConverter();
        throwableProxyConverter.start();
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        StringBuilder builder = new StringBuilder();
        EcsJsonSerializer.serializeObjectStart(builder, event.getTimeStamp());
        EcsJsonSerializer.serializeLogLevel(builder, event.getLevel().toString());
        EcsJsonSerializer.serializeFormattedMessage(builder, event.getFormattedMessage(), null);
        serializeException(event, builder);
        EcsJsonSerializer.serializeServiceName(builder, serviceName);
        EcsJsonSerializer.serializeThreadName(builder, event.getThreadName());
        EcsJsonSerializer.serializeLoggerName(builder, event.getLoggerName());
        EcsJsonSerializer.serializeLabels(builder, event.getMDCPropertyMap(), topLevelLabels);
        EcsJsonSerializer.serializeObjectEnd(builder);
        // all these allocations kinda hurt
        return builder.toString().getBytes(UTF_8);
    }

    private void serializeException(ILoggingEvent event, StringBuilder builder) {
        if (event.getThrowableProxy() != null) {
            // remove `", `
            builder.setLength(builder.length() - 3);
            builder.append("\\n");
            JsonUtils.quoteAsString(throwableProxyConverter.convert(event), builder);
            builder.append("\",");
        }
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
