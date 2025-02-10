package org.terrakube.api.plugin.logs;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.terrakube.api.plugin.state.model.logs.Log;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class LogsService {

    RedisTemplate redisTemplate;

    public void appendLogs(String jobId, List<Log> logs) {
        log.info("Appending logs for job: {}", jobId);
        for (Log log : logs) {
            redisTemplate.opsForStream().add(jobId, log.toStrMap());
        }
    }

    public void setupConsumerGroup(String jobId) {
        StreamInfo.XInfoGroups xInfoGroups = redisTemplate.opsForStream().groups(jobId);
        try {
            redisTemplate.opsForStream().createGroup(jobId, "CLI");
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
        try {
            redisTemplate.opsForStream().createGroup(jobId, "UI");
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }
}
