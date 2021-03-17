/*-
 * #%L
 * Java ECS logging
 * %%
 * Copyright (C) 2019 - 2020 Elastic and contributors
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
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.encoder.EncoderBase;
import co.elastic.logging.AdditionalField;
import co.elastic.logging.DataStreamFieldSanitizer;
import co.elastic.logging.EcsJsonSerializer;
import org.slf4j.Marker;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EcsEncoder extends EncoderBase<ILoggingEvent> {

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private final List<AdditionalField> additionalFields = new ArrayList<AdditionalField>();
    private boolean stackTraceAsArray = false;
    private String serviceName;
    private String dataset;
    private String dataStreamNamespace;
    private boolean includeMarkers = false;
    private ThrowableProxyConverter throwableProxyConverter;
    private boolean includeOrigin;
    private OutputStream os;

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public void start() {
        super.start();
        throwableProxyConverter = new ThrowableProxyConverter();
        throwableProxyConverter.start();
        setDataset(EcsJsonSerializer.computeDataset(dataset, serviceName));
    }

    /**
     * This method has been removed in logback 1.2.
     * To make this lib backwards compatible with logback 1.1 we have implement this method.
     */
    public void init(OutputStream os) {
        this.os = os;
    }

    /**
     * This method has been removed in logback 1.2.
     * To make this lib backwards compatible with logback 1.1 we have implement this method.
     */
    public void doEncode(ILoggingEvent event) throws IOException {
        os.write(encode(event));
    }

    /**
     * This method has been removed in logback 1.2.
     * To make this lib backwards compatible with logback 1.1 we have implement this method.
     */
    public void close() throws IOException {
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        StringBuilder builder = new StringBuilder();
        EcsJsonSerializer.serializeObjectStart(builder, event.getTimeStamp());
        EcsJsonSerializer.serializeLogLevel(builder, event.getLevel().toString());
        EcsJsonSerializer.serializeFormattedMessage(builder, event.getFormattedMessage());
        EcsJsonSerializer.serializeEcsVersion(builder);
        serializeMarkers(event, builder);
        EcsJsonSerializer.serializeServiceName(builder, serviceName);
        EcsJsonSerializer.serializeDataset(builder, dataset);
        EcsJsonSerializer.serializeNamespace(builder, dataStreamNamespace);
        EcsJsonSerializer.serializeThreadName(builder, event.getThreadName());
        EcsJsonSerializer.serializeLoggerName(builder, event.getLoggerName());
        EcsJsonSerializer.serializeAdditionalFields(builder, additionalFields);
        EcsJsonSerializer.serializeMDC(builder, event.getMDCPropertyMap());
        if (includeOrigin) {
            StackTraceElement[] callerData = event.getCallerData();
            if (callerData != null && callerData.length > 0) {
                EcsJsonSerializer.serializeOrigin(builder, callerData[0]);
            }
        }
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy instanceof ThrowableProxy) {
            EcsJsonSerializer.serializeException(builder, ((ThrowableProxy) throwableProxy).getThrowable(), stackTraceAsArray);
        } else if (throwableProxy != null) {
            EcsJsonSerializer.serializeException(builder, throwableProxy.getClassName(), throwableProxy.getMessage(), throwableProxyConverter.convert(event), stackTraceAsArray);
        }
        EcsJsonSerializer.serializeObjectEnd(builder);
        // all these allocations kinda hurt
        return builder.toString().getBytes(UTF_8);
    }

    private void serializeMarkers(ILoggingEvent event, StringBuilder builder) {
        Marker marker = event.getMarker();
        if (includeMarkers && marker != null) {
            EcsJsonSerializer.serializeTagStart(builder);
            serializeMarker(builder, marker);
            EcsJsonSerializer.serializeTagEnd(builder);
        }
    }

    private void serializeMarker(StringBuilder builder, Marker marker) {
        if (marker != null) {
            EcsJsonSerializer.serializeSingleTag(builder, marker.getName());
            Iterator<Marker> it = marker.iterator();
            while (it.hasNext()) {
                serializeMarker(builder, it.next());
            }
        }
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setIncludeMarkers(boolean includeMarkers) {
        this.includeMarkers = includeMarkers;
    }

    public void setStackTraceAsArray(boolean stackTraceAsArray) {
        this.stackTraceAsArray = stackTraceAsArray;
    }

    public void setIncludeOrigin(boolean includeOrigin) {
        this.includeOrigin = includeOrigin;
    }

    public void addAdditionalField(AdditionalField pair) {
        this.additionalFields.add(pair);
    }

    public void setEventDataset(String dataset) {
        setDataset(dataset);
    }

    public void setDataStreamDataset(String dataset) {
        setDataset(dataset);
    }

    private void setDataset(String dataset) {
        this.dataset = DataStreamFieldSanitizer.sanitizeDataStreamDataset(dataset);
    }

    public void setDataStreamNamespace(String dataStreamNamespace) {
        this.dataStreamNamespace = DataStreamFieldSanitizer.sanitizeDataStreamNamespace(dataStreamNamespace);
    }
}
