package org.terrakube.api.plugin.logs;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.terrakube.api.plugin.state.model.logs.Log;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class LogsService {

    RedisTemplate redisTemplate;

    public void appendLogs(List<Log> logs) {
        for (Log log : logs) {
            redisTemplate.opsForStream().add(log.getJobId(), log.toStrMap());
        }
    }

    public void setupConsumerGroups(String jobId) {
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
