package seucxxy.csd.backend.cet4.service;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class CET4AnswerMapper {

    /**
     * 安全转换任意对象为 Map<String, Object>。
     */
    public Map<String, Object> castToStringObjectMap(Object obj) {
        if (!(obj instanceof Map<?, ?> map)) return Collections.emptyMap();
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            result.put(String.valueOf(e.getKey()), e.getValue());
        }
        return result;
    }

    /**
     * 从父 answers 中获取子 map。
     */
    public Map<String, Object> getChildAnswerMap(Map<String, Object> parent, String key) {
        Object sub = parent.get(key);
        return castToStringObjectMap(sub);
    }
}
