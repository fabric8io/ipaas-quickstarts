package io.fabric8.app.hubotslack;

import io.fabric8.kubernetes.generator.annotation.KubernetesProvider;
import io.fabric8.openshift.api.model.template.Template;
import io.fabric8.openshift.api.model.template.TemplateBuilder;

public class HubotSlackModelProcessor {

    private static final String NAME = "hubot-slack";

    @KubernetesProvider
    public Template create() {
        return new TemplateBuilder()
                .withNewMetadata()
                    .withName(NAME)
                    .addToLabels("name", NAME)
                .endMetadata()
                .addNewParameter().withName("HUBOT_SLACK_TOKEN")
                    .withDescription("The token for Hubot to login to Slack").endParameter()
                .addNewReplicationControllerObject()
                .withNewMetadata()
                        .withName(NAME)
                        .addToLabels("name", NAME)
                    .endMetadata()
                .withNewSpec()
                    .addToSelector("name", NAME)
                    .withReplicas(1)
                    .withNewTemplate()
                        .withNewMetadata()
                            .withName(NAME)
                            .addToLabels("name", NAME)
                        .endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withName(NAME)
                                .withImage("fabric8/hubot-slack")
                                    .addNewPort()
                                    .withContainerPort(5000)
                                    .withProtocol("TCP")
                                .endPort()
                                .addNewEnv().withName("HUBOT_SLACK_TOKEN").withValue("${HUBOT_SLACK_TOKEN}").endEnv()
                            .endContainer()
                .endSpec()
                    .endTemplate()
                .endSpec()
                .endReplicationControllerObject()

                .addNewServiceObject()
                    .withNewMetadata()
                .withName(NAME)
                    .endMetadata()
                .withNewSpec()
                    .addNewPort()
                .withPort(80)
                        .withNewTargetPort(5000)
                .endPort()
                    .addToSelector("name", NAME)
                .endSpec()
                .endServiceObject()

                .build();
    }
}
