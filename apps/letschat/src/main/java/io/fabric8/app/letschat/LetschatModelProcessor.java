package io.fabric8.app.letschat;

import io.fabric8.kubernetes.generator.annotation.KubernetesProvider;
import io.fabric8.openshift.api.model.template.Template;
import io.fabric8.openshift.api.model.template.TemplateBuilder;

public class LetschatModelProcessor {

    private static final String NAME = "letschat";

    @KubernetesProvider
    public Template create() {
        return new TemplateBuilder()
                .withNewMetadata()
                    .withName(NAME)
                    .addToLabels("name", NAME)
                .endMetadata()
                .addNewParameter().withName("LETSCHAT_DEFAULT_ROOMS").withValue("fabric8_default")
                .withDescription("Default rooms to create and for hubot to join").endParameter()
                .addNewParameter().withName("LETSCHAT_HUBOT_PASSWORD").withValue("RedHat$1")
                    .withDescription("The password for Hubot to login to Let's Chat").endParameter()
                .addNewParameter().withName("LETSCHAT_HUBOT_TOKEN")
                    .withValue("NTU4MmUwNWU4YzUzZDUwZTAwMmJiZWVhOjllMTJlMDM3YzdjYjNjODZkOGE3MDNlNWZlZDhjOGVjYzA2NDdjMmNkNDAwNzk3Nw==")
                    .withDescription("The token for Hubot to login to Let's Chat").endParameter()
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
                                .withName("letschat-mongodb")
                                .withImage("mongo")
                                .addNewPort()
                                    .withContainerPort(27017)
                                    .withProtocol("TCP")
                                .endPort()
                            .endContainer()
                            .addNewContainer()
                                .withName(NAME)
                                .withImage("fabric8/lets-chat")
                                    .addNewPort()
                                    .withContainerPort(5000)
                                    .withProtocol("TCP")
                                .endPort()
                                .addNewEnv().withName("LETSCHAT_CREATE_HUBOT_USER").withValue("true").endEnv()
                                .addNewEnv().withName("LETSCHAT_HUBOT_TOKEN").withValue("${LETSCHAT_HUBOT_TOKEN}").endEnv()
                                .addNewEnv().withName("LETSCHAT_HUBOT_PASSWORD").withValue("${LETSCHAT_HUBOT_PASSWORD}").endEnv()
                                .addNewEnv().withName("LETSCHAT_HUBOT_USERNAME").withValue("fabric8").endEnv()
                                .addNewEnv().withName("LETSCHAT_HUBOT_EMAIL").withValue("fabric8-admin@googlegroups.com").endEnv()
                                .addNewEnv().withName("LETSCHAT_HUBOT_FIRST_NAME").withValue("fabric8").endEnv()
                                .addNewEnv().withName("LETSCHAT_HUBOT_LAST_NAME").withValue("rocks").endEnv()
                                .addNewEnv().withName("LETSCHAT_HUBOT_DISPLAY_NAME").withValue("fabric8").endEnv()
                                .addNewEnv().withName("LETSCHAT_DEFAULT_ROOMS").withValue("${LETSCHAT_DEFAULT_ROOMS}").endEnv()
                                .addNewEnv().withName("LCB_NOROBOTS").withValue("false").endEnv()
                                .addNewEnv().withName("LCB_FILES_ENABLE").withValue("true").endEnv()
                                .addNewEnv().withName("LCB_DATABASE_URI").withValue("mongodb://127.0.0.1:27017/letschat").endEnv()
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
