package org.terrakube.executor.plugin.tfstate.api;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.TextStringBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.terrakube.client.TerrakubeClient;
import org.terrakube.client.model.generic.Resource;
import org.terrakube.client.model.state.CreateStateVersion;
import org.terrakube.client.model.state.CreateStateVersionAttributes;
import org.terrakube.client.model.state.CreateStateVersionRelationships;
import org.terrakube.client.model.state.CreateStateVersionRequest;
import org.terrakube.client.model.state.RunData;
import org.terrakube.executor.plugin.tfstate.TerraformState;
import org.terrakube.executor.plugin.tfstate.TerraformStatePathService;
import org.terrakube.executor.service.mode.TerraformJob;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.stream.Stream;

@Slf4j
@Builder
@Getter
@Setter
public class ApiTerraformStateImpl implements TerraformState {
    private static final String TERRAFORM_PLAN_FILE = "terraformLibrary.tfPlan";
    private static final String BACKEND_FILE_NAME = "api_backend_override.tf";
    private static final String STATE_FILE_NAME = "api.tfstate";

    @NonNull
    TerrakubeClient terrakubeClient;

    @NonNull
    TerraformStatePathService terraformStatePathService;


    @Value("${org.terrakube.api.url}")
    private String apiUrl;

    private final static String URL_PLAN = "%s/organization/%s/workspace/%s/jobId/%s/step/%s/terraform.tfstate";

    @Override
    public String getBackendStateFile(String organizationId, String workspaceId, File workingDirectory, String terraformVersion) {
        try {
            log.info("Downloading state file");
            String backendStatePath =
                    FilenameUtils.separatorsToSystem(
                            String.join(File.separator, Stream.of(workingDirectory.getAbsolutePath(), STATE_FILE_NAME)
                                    .toArray(String[]::new)));

            feign.Response.Body res = terrakubeClient.getCurrentState(organizationId, workspaceId).body();

            if (res == null) {
                log.warn("No state file found. This is probably the first run.");
            } else {
                Files.copy(res.asInputStream(), Paths.get(backendStatePath), StandardCopyOption.REPLACE_EXISTING);
            }

            TextStringBuilder localBackendHcl = new TextStringBuilder();
            localBackendHcl.appendln("terraform {");
            localBackendHcl.appendln("  backend \"local\" {");
            localBackendHcl.appendln("    path                  = \"" + backendStatePath + "\"");
            localBackendHcl.appendln("  }");
            localBackendHcl.appendln("}");

            File localBackendFile = new File(
                    FilenameUtils.separatorsToSystem(
                            String.join(File.separator, Stream.of(workingDirectory.getAbsolutePath(), BACKEND_FILE_NAME)
                                    .toArray(String[]::new))));

            log.info("Creating Local Backend File: {}", localBackendFile.getAbsolutePath());
            FileUtils.writeStringToFile(localBackendFile, localBackendHcl.toString(), Charset.defaultCharset());
            return BACKEND_FILE_NAME;
        } catch (IOException e) {
            log.error("Failed to download state file from terrakube API: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String saveTerraformPlan(String organizationId, String workspaceId, String jobId, String stepId,
                                    File workingDirectory) {
        log.info("Saving plan file to terrakube API");

        Path localPlanPath = Paths.get(workingDirectory.getAbsolutePath() + "/" + TERRAFORM_PLAN_FILE);
        File localPlanFile = localPlanPath.toFile();

        if (localPlanFile.exists()) {
            try {
                byte[] planBytes = Files.readAllBytes(localPlanFile.toPath());
                log.info(String.format("bytes: 0x%02X 0x%02X 0x%02X 0x%02X 0x%02X 0x%02X 0x%02X 0x%02X", planBytes[0], planBytes[1], planBytes[2], planBytes[3], planBytes[4], planBytes[5], planBytes[6], planBytes[7]));
                return terrakubeClient.uploadPlanState(planBytes, organizationId, workspaceId, jobId, stepId).getData().getPath();
            } catch (Exception e) {
                log.error("Failed to upload plan file to terrakube API: {}", e.getMessage());
                return null;
            }

        } else {
            log.warn("Terraform plan file not found");
            return null;
        }
    }

    @Override
    public boolean downloadTerraformPlan(String organizationId, String workspaceId, String jobId, String stepId,
                                         File workingDirectory) {
        log.info("Downloading plan file from terrakube API");
        String localPlanPath = workingDirectory.getAbsolutePath() + "/" + TERRAFORM_PLAN_FILE;
        try {

            feign.Response.Body res = terrakubeClient.getPlanState(organizationId, workspaceId, jobId, stepId).body();

            if (res == null) {
                log.info("No plan file found");
                return false;
            }
            Files.copy(res.asInputStream(), Paths.get(localPlanPath), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            log.error("Failed to download state file from terrakube API: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void saveStateJson(TerraformJob terraformJob, String applyJSON, String rawState) {
        log.info("Saving state json to terrakube API");
        // Attributes
        CreateStateVersionAttributes stateAttributes = new CreateStateVersionAttributes();
        stateAttributes.setJsonState(Base64.getEncoder().encodeToString(applyJSON.getBytes()));
        stateAttributes.setState(Base64.getEncoder().encodeToString(rawState.getBytes()));

        // Relationship
        Resource runResource = new Resource();
        runResource.setType("runs");
        runResource.setId(terraformJob.getJobId());
        RunData runData = new RunData();
        runData.setData(runResource);
        CreateStateVersionRelationships stateRelationships = new CreateStateVersionRelationships();
        stateRelationships.setRun(new RunData());

        CreateStateVersion createStateVersion = new CreateStateVersion();
        createStateVersion.setType("state-versions");
        createStateVersion.setAttributes(stateAttributes);
        createStateVersion.setRelationships(stateRelationships);

        CreateStateVersionRequest createStateVersionRequest = new CreateStateVersionRequest();
        createStateVersionRequest.setData(createStateVersion);

        terrakubeClient.createWorkspaceStateVersion(
            createStateVersionRequest,
            terraformJob.getWorkspaceId()
        );
    }
}
