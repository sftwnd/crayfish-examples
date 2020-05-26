package com.github.sftwnd.crayfish.examples.common.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.sftwnd.crayfish.common.format.formatter.TemporalFormatter;
import com.github.sftwnd.crayfish.common.json.IJsonMapper;
import com.github.sftwnd.crayfish.common.json.IJsonZonedMapper;
import com.github.sftwnd.crayfish.common.json.JsonMapper;
import com.github.sftwnd.crayfish.common.json.JsonZonedMapper;
import com.github.sftwnd.crayfish.common.json.deserialize.JsonInstantDeserializer;
import com.github.sftwnd.crayfish.common.json.deserialize.JsonZonedDateTimeDeserializer;
import com.github.sftwnd.crayfish.common.json.serialize.JsonZonedDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class JsonMapperExample {

    public static void main(String[] args) throws IOException {
        String DATE_TIME = "2020-01-13T13:14:15";
        IJsonMapper mapper = new JsonMapper();
        logger.info("Date time: {}", DATE_TIME);
        logger.info("Parse pojo by default mapper: POJO[dateTime={}]", mapper.parseObject("{\"dateTime\":\""+DATE_TIME+"\"}", POJO.class).dateTime);
        ZoneId zoneId = ZoneId.systemDefault();
        mapper = new JsonMapper(zoneId);
        logger.info("Parse pojo by mapper(zoneId:{}): POJO[dateTime={}]", zoneId, mapper.parseObject("{\"dateTime\":\""+DATE_TIME+"\"}", POJO.class).dateTime);
        logger.info("Parse pojo by mapper(zoneId:{}): POJO[zoneDateTime={}]", zoneId, mapper.parseObject("{\"zonedDateTime\":\""+DATE_TIME+"\"}", POJO.class).zonedDateTime);
        String str = "{\"dateTime\":\""+DATE_TIME+"\", \"zonedDateTime\":\""+DATE_TIME+"\"}";
        POJO pojo = mapper.parseObject(str, POJO.class);
        logger.info("Parse string '{}' by mapper(zoneId:{}): {}", str, zoneId, pojo);
        logger.info("Format pojo back to string by mapper(zoneId:{}): {}", zoneId, mapper.formatObject(pojo));
        zoneId = ZoneId.of("Asia/Novosibirsk");
        IJsonZonedMapper zonedMapper = new JsonZonedMapper();
        logger.info("Parse(zoneId:{}) pojo by zonedMapper: POJO[dateTime={}]", zoneId, zonedMapper.parseObject(zoneId, "{\"dateTime\":\""+DATE_TIME+"\"}", POJO.class).dateTime);
        logger.info("Parse(zoneId:{}) pojo by zonedMapper: POJO[zoneDateTime={}]", zoneId, zonedMapper.parseObject(zoneId, "{\"zonedDateTime\":\""+DATE_TIME+"\"}", POJO.class).zonedDateTime);
        pojo = zonedMapper.parseObject(zoneId, str, POJO.class);
        logger.info("Parse(zoneId:{}) string '{}' by zonedMapper: {}", zoneId, str, pojo);
        zoneId = ZoneId.of("Europe/Kaliningrad");
        logger.info("Format(zoneId:{}) pojo back to string by zonedMapper: {}", zoneId, zonedMapper.formatObject(zoneId, pojo));
    }

    static class JsonFullZonedDateTimeSerializer extends JsonZonedDateTimeSerializer {
        protected @Nonnull
        TemporalFormatter constructSerializer() {
            return new TemporalFormatter(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        }
    }

    static class JsonNoZoneInstantSerializer extends JsonZonedDateTimeSerializer {
        protected @Nonnull
        TemporalFormatter constructSerializer() {
            return new TemporalFormatter(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    static class POJO {
        @JsonDeserialize(using = JsonInstantDeserializer.class)
        @JsonSerialize(using = JsonNoZoneInstantSerializer.class)
        Instant dateTime;
        @JsonDeserialize(using = JsonZonedDateTimeDeserializer.class)
        @JsonSerialize(using = JsonFullZonedDateTimeSerializer.class)
        ZonedDateTime zonedDateTime;
    }

}
