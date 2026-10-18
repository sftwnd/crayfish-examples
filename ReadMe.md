# CrayFish projects examples

## Alarms

### Alarms TimeRange

* [ITimeRange multithreaded performance example](./crayfish-examples-alarms/crayfish-examples-alarms-timerange/ReadMe.md)

```home-run
mvn -pl ./crayfish-examples-alarms/crayfish-examples-alarms-timerange exec:java
```

### Alarms Service

#### Alarms TimeRange Service

* [AlarmTimeRangeService parallel add elements example](./crayfish-examples-alarms/crayfish-examples-alarms-service/ReadMe.md)

```home-run
mvn -pl ./crayfish-examples-alarms/crayfish-examples-alarms-service -P alarm-service-parallel-add exec:java
```

* [AlarmTimeRangeService performance example](./crayfish-examples-alarms/crayfish-examples-alarms-service/ReadMe.md)

```home-run
mvn -pl ./crayfish-examples-alarms/crayfish-examples-alarms-service -P alarm-service-performance exec:java
```

## Common

### Common CRC examples

* [Common :: CRC examples](./crayfish-examples-common-crc/ReadMe.md)

| Example | Profile |
|---|---|
| CrcModel | (none) |
| Modbus RTU frame | `crc-modbus` |
| Parallel CRC of a large file | `crc-parallel` |
| Resume and append | `crc-resume` |
| Find an unknown CRC | `crc-find` |

```shell
mvn -pl ./crayfish-examples-common-crc -P <profile> exec:java
```
