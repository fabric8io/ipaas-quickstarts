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
                                .withImage("fabric8/lets-chat:latest")
                                    .addNewPort()
                                    .withContainerPort(5000)
                                    .withProtocol("TCP")
                                .endPort()
                                .addNewEnv()
                                    .withName("LCB_DATABASE_URI")
                                    .withValue("mongodb://127.0.0.1:27017/letschat")
                                .endEnv()
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
