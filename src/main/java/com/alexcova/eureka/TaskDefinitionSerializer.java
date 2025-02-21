package com.alexcova.eureka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import software.amazon.awssdk.services.ecs.model.TaskDefinition;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskDefinitionSerializer {

    private static Map<String, Object> toMap(Object object) {

        if (object == null)
            return null;

        Map<String, Object> map = new HashMap<>();

        try {

            for (Field declaredField : object.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(declaredField.getModifiers()))
                    continue;

                declaredField.setAccessible(true);

                if (declaredField.getType().getName().startsWith("java")) {

                    var value = declaredField.get(object);

                    if (value instanceof List<?> list) {
                        if(!list.isEmpty() && !list.get(0).getClass().getName().startsWith("java")) {

                            var listMap = new ArrayList<>();

                            for (Object o : list) {
                                listMap.add(toMap(o));
                            }

                            value = listMap;
                        }
                    }

                    map.put(declaredField.getName(), value);
                } else {
                    map.put(declaredField.getName(), toMap(declaredField.get(object)));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return map;
    }

    public static String serialize(TaskDefinition definition) {
        Map<String, Object> map = toMap(definition);

        try {
            return new ObjectMapper()
                    .findAndRegisterModules()
                    .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                    .writerWithDefaultPrettyPrinter().writeValueAsString(map);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
