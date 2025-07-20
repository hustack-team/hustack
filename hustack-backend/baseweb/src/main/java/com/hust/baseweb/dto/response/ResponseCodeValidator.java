package com.hust.baseweb.dto.response;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class ResponseCodeValidator {

    @PostConstruct
    public void validateUniqueCodes() {
        List<Class<? extends BaseResponseCode>> responseCodeEnums = List.of(
            SuccessCode.class,
            ErrorCode.class
        );

        Set<Integer> usedCodes = new HashSet<>();
        Map<Integer, String> codeSources = new HashMap<>();
        List<String> duplicates = new ArrayList<>();

        for (Class<? extends BaseResponseCode> enumClass : responseCodeEnums) {
            if (!enumClass.isEnum()) {
                continue;
            }

            for (BaseResponseCode code : enumClass.getEnumConstants()) {
                int value = code.getCode();
                String location = enumClass.getSimpleName() + "." + ((Enum<?>) code).name();

                if (!usedCodes.add(value)) {
                    String previous = codeSources.get(value);
                    duplicates.add("Duplicate code " + value + " found in: " + location + " AND " + previous);
                } else {
                    codeSources.put(value, location);
                }
            }
        }

        if (!duplicates.isEmpty()) {
            log.error("Duplicate response codes detected");
            duplicates.forEach(log::error);
            throw new IllegalStateException("Startup aborted due to duplicate response codes.");
        }
    }
}
