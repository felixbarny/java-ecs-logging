# Log4j2 ECS Layout

## Step 0: build the project
Check out the project and build with `mvn clean install`.
Once this project is available on maven central, this step is no longer needed.

## Step 1: add dependency

Add a dependency to your application
```xml
<dependency>
    <groupId>co.elastic.logging</groupId>
    <artifactId>log4j2-ecs-layout</artifactId>
    <version>${java-ecs-logging.version}</version>
</dependency>
```

## Step 2: use the `EcsLayout`

Instead of the usual `<PatternLayout/>`, use `<EcsLayout serviceName="my-app"/>`.

## Example
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="DEBUG">
    <Appenders>
        <Console name="LogToConsole" target="SYSTEM_OUT">
            <EcsLayout serviceName="my-app"/>
        </Console>
        <File name="LogToFile" fileName="logs/app.log">
            <EcsLayout serviceName="my-app"/>
        </File>
    </Appenders>
    <Loggers>
        <Root level="info">
            <AppenderRef ref="LogToFile"/>
            <AppenderRef ref="LogToConsole"/>
        </Root>
    </Loggers>
</Configuration>
```

## Layout Parameters

|Parameter name   |Type   |Default|Description|
|-----------------|-------|-------|-----------|
|serviceName      |String |       |Sets the `service.name` field so you can filter your logs by a particular service |
|includeMarkers   |boolean|`false`|Log [Markers](https://logging.apache.org/log4j/2.0/manual/markers.html) as `tags` |
|stackTraceAsArray|boolean|`false`|Serializes the `error.stack_trace` as a JSON array where each element is in a new line to improve readability. Note that this requires a slightly more complex [Filebeat configuration](../README.md#when-stacktraceasarray-is-enabled).|

To include any custom field in the output, use following syntax:

```xml
  <EcsLayout>
    <KeyValuePair key="additionalField1" value="constant value"/>
    <KeyValuePair key="additionalField2" value="$${ctx:key}"/>
  </EcsLayout>
```

Custom fields are included in the order they are declared. The values support [lookups](https://logging.apache.org/log4j/2.x/manual/lookups.html).