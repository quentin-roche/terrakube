package org.terrakube.api.plugin.logs;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.terrakube.api.plugin.state.model.logs.LogsRequest;
import org.terrakube.api.plugin.state.model.workspace.tags.TagDataList;

@AllArgsConstructor
@RestController
@Slf4j
@RequestMapping("/logs")
public class LogsController {
    private final LogsService logsService;

    @Transactional
    @PostMapping(produces = "application/vnd.api+json", value = "{jobId}/setup-consumer-groups")
    public ResponseEntity<Void> setupConsumerGroups(@PathVariable("jobId") String jobId) {
        logsService.setupConsumerGroup(jobId);
        return ResponseEntity.ok().build();
    }

    @Transactional
    @PostMapping(produces = "application/vnd.api+json", value = "{jobId}")
    public ResponseEntity<Void> appendLogs(@PathVariable("jobId") String jobId, @RequestBody LogsRequest logsRequest
    ) {
        logsService.appendLogs(jobId, logsRequest.getData());
        return ResponseEntity.ok().build();
    }
}
