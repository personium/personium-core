/**
 * Personium
 * Copyright 2018-2022 Personium Project Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.personium.core.stream.impl.kafka;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import io.personium.core.event.PersoniumEvent;

/**
 * Deserializer for PersoniumEvent.
 */
public class PersoniumEventDeserializer implements Deserializer<PersoniumEvent> {
    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    }

    /**
     * Constructor.
     */
    public PersoniumEventDeserializer() {
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public PersoniumEvent deserialize(String topic, byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        PersoniumEvent event;
        try {
            event = objectMapper.readValue(bytes, PersoniumEvent.class);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing to PersoniumEvent message", e);
        }

        return event;
    }

    @Override
    public void close() {
    }

}
