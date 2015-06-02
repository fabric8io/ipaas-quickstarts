package io.fabric8.app.gerrit;

import io.fabric8.kubernetes.generator.annotation.KubernetesModelProcessor;
import io.fabric8.openshift.api.model.template.TemplateBuilder;

@KubernetesModelProcessor
public class GerritModelProcessor {

    public void onList(TemplateBuilder builder) {
        builder.addNewServiceObject()
                .withNewMetadata()
                  .withName("gerrit-http-service")
                  .addToLabels("component", "gerrit")
                  .addToLabels("provider", "fabric8")
                .endMetadata()
                .withNewSpec()
                  .addNewPort()
                    .withProtocol("TCP")
                    .withPort(80)
                      .withNewTargetPort(8080)
                    .endPort()
                    .addToSelector("component", "gerrit")
                    .addToSelector("provider", "fabric8")
                .endSpec()
                .endServiceObject()
                // Second service
                .addNewServiceObject()
                .withNewMetadata()
                  .withName("gerrit-ssh-service")
                  .addToLabels("component", "gerrit")
                  .addToLabels("provider", "fabric8")
                .endMetadata()
                .withNewSpec()
                  .addNewPort()
                    .withProtocol("TCP")
                    .withPort(29418)
                      .withNewTargetPort(29418)
                    .endPort()
                    .addToSelector("component", "gerrit")
                    .addToSelector("provider", "fabric8")
                .endSpec()
                .endServiceObject()
                .build();
    }
}
