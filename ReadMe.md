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

* [Common :: CrcModel example](./crayfish-examples-common-crc/ReadMe.md)

```
mvn -pl ./crayfish-examples-common-crc exec:java
```
