package sk.posam.fsa.foodrescue.mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

final class ApiDateTimeMapper {

    private ApiDateTimeMapper() {
    }

    static OffsetDateTime toUtcOffsetDateTime(LocalDateTime value) {
        if (value == null) {
            return null;
        }

        return value.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .toOffsetDateTime();
    }
}
