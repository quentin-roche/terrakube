package org.terrakube.executor.service.logs;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.terrakube.client.TerrakubeClient;
import org.terrakube.client.model.organization.job.Log;
import org.terrakube.client.model.organization.job.LogsRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@AllArgsConstructor
@ConditionalOnProperty(name = "org.executor.log-though-api", havingValue = "true", matchIfMissing = false)
public class LogsServiceHttp implements ProcessLogs {
    private TerrakubeClient terrakubeClient;
    private final ConcurrentMap<Integer, ConcurrentLinkedQueue<Log>> jobLogQueues = new ConcurrentHashMap<>();

    @Override
    public void setupConsumerGroups(String jobId) {
        terrakubeClient.setupConsumerGroups(jobId);
    }

    public void sendLogs(Integer jobId, String stepId, int lineNumber, String output) {
        Log log = new Log();
        log.setJobId(jobId);
        log.setStepId(stepId);
        log.setLineNumber(lineNumber);
        log.setOutput(output);

        ConcurrentLinkedQueue<Log> logQueue = jobLogQueues.computeIfAbsent(jobId, k -> new ConcurrentLinkedQueue<>());
        logQueue.add(log);
    }

    @Scheduled(fixedDelay = 5000)  // Send logs every 5 seconds
    public void sendBatchedLogs() {
        if (jobLogQueues.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, ConcurrentLinkedQueue<Log>> entry : jobLogQueues.entrySet()) {
            Integer jobId = entry.getKey();
            ConcurrentLinkedQueue<Log> queue = entry.getValue();

            if (queue.isEmpty()) {
                continue;
            }

            List<Log> batch = new ArrayList<>();
            while (!queue.isEmpty()) {
                batch.add(queue.poll());
            }

            LogsRequest logsRequest = new LogsRequest();
            logsRequest.setData(batch);

            try {
                terrakubeClient.appendLogs(logsRequest, String.valueOf(jobId));
            } catch (Exception e) {
                log.error("Failed to send logs, retrying...");
            }
        }
    }
}