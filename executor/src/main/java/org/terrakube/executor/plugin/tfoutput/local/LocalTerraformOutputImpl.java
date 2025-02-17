package org.terrakube.executor.plugin.tfoutput.local;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.terrakube.client.model.organization.job.TfOutputRequest;
import org.terrakube.executor.plugin.tfoutput.TerraformOutput;
import org.terrakube.client.TerrakubeClient;

@Builder
@Getter
@Setter
@Slf4j
public class LocalTerraformOutputImpl implements TerraformOutput {

    private static final String LOCAL_OUTPUT_DIRECTORY = "/.terraform-spring-boot/local/output/%s/%s/%s.tfoutput";

    @NonNull
    TerrakubeClient terrakubeClient;

    @Override
    public String save(String organizationId, String jobId, String stepId, String output, String outputError) {
        log.info("Uploading output for org: {}, job: {}, step: {}", organizationId, jobId, stepId);

        TfOutputRequest tfOutputRequest = new TfOutputRequest();
        tfOutputRequest.setData(output + outputError);
        return terrakubeClient.uploadOutput(tfOutputRequest, organizationId, jobId, stepId).getData().getUrl();
    }
}
