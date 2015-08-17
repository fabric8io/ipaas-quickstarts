package io.fabric8.app.taiga;

import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
import io.fabric8.kubernetes.generator.annotation.KubernetesModelProcessor;
import io.fabric8.kubernetes.generator.annotation.KubernetesProvider;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.api.model.TemplateBuilder;

public class TaigaModelProcessor {

final String NAME = "taiga";

    @KubernetesProvider
    public Template create() {
        return new TemplateBuilder()
                .withNewMetadata()
                .withName(NAME)
                .addToLabels("provider", "fabric8")
                .addToLabels("component", NAME)
                .endMetadata()

                .addNewServiceObject()
                .withNewMetadata()
                .withName("taiga")
                .addToLabels("provider", "fabric8")
                .addToLabels("component", NAME)
                .endMetadata()
                .withNewSpec()
                .withType("LoadBalancer")
                .addNewPort()
                .withPort(80)
                .withNewTargetPort(80)
                .endPort()
                .addToSelector("provider", "fabric8")
                .addToSelector("component", NAME)
                .endSpec()
                .endServiceObject()

                .addNewServiceObject()
                .withNewMetadata()
                .withName("taigaback")
                .addToLabels("provider", "fabric8")
                .addToLabels("component", NAME)
                .endMetadata()
                .withNewSpec()
                .addNewPort()
                .withPort(8000)
                .withNewTargetPort(8000)
                .endPort()
                .addToSelector("provider", "fabric8")
                .addToSelector("component", NAME)
                .endSpec()
                .endServiceObject()

                .addNewReplicationControllerObject()
                .withNewMetadata()
                .withName(NAME)
                .addToLabels("provider", "fabric8")
                .addToLabels("component", NAME)
                .endMetadata()
                .withNewSpec()
                .addToSelector("provider", "fabric8")
                .addToSelector("component", NAME)
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .withName(NAME)
                .addToLabels("provider", "fabric8")
                .addToLabels("component", NAME)
                .endMetadata()
                .withNewSpec()

                .addNewContainer()
                .withName("taiga-postgres")
                .withImage("postgres")
                .addNewEnv().withName("POSTGRES_PASSWORD").withValue("password").endEnv()
                .addNewEnv().withName("POSTGRES_USER").withValue("taiga").endEnv()
                .addNewVolumeMount().withName("taiga-data").withMountPath("/var/lib/postgresql/data/").endVolumeMount()
                .endContainer()

                .addNewContainer()
                .withName("taiga-backend")
                .withImage("fabric8/taiga-back")
                .addNewPort()
                .withContainerPort(8000)
                .withProtocol("TCP")
                .endPort()
                .addNewEnv().withName("SECRET_KEY").withValue("xyz").endEnv()
                .addNewEnv().withName("POSTGRES_PORT_5432_TCP_ADDR").withValue("127.0.0.1").endEnv()
                .addNewEnv().withName("POSTGRES_ENV_POSTGRES_USER").withValue("taiga").endEnv()
                .addNewEnv().withName("POSTGRES_ENV_POSTGRES_PASSWORD").withValue("password").endEnv()
                .addNewVolumeMount().withName("taiga-data").withMountPath("/var/lib/postgresql/data").endVolumeMount()
                .addNewVolumeMount().withName("taiga-static").withMountPath("/usr/local/taiga/static").endVolumeMount()
                .addNewVolumeMount().withName("taiga-media").withMountPath("/usr/local/taiga/media").endVolumeMount()
                .endContainer()

                .addNewContainer()
                .withName("taiga-frontend")
                .withImage("fabric8/taiga-front")
                .addNewPort()
                .withContainerPort(80)
                .withProtocol("TCP")
                .endPort()
                .addNewVolumeMount().withName("taiga-data").withMountPath("/var/lib/postgresql/data").endVolumeMount()
                .addNewVolumeMount().withName("taiga-static").withMountPath("/usr/local/taiga/static").endVolumeMount()
                .addNewVolumeMount().withName("taiga-media").withMountPath("/usr/local/taiga/media").endVolumeMount()
                .endContainer()

                .addNewVolume().withName("taiga-data").withEmptyDir(new EmptyDirVolumeSource()).endVolume()
                .addNewVolume().withName("taiga-static").withEmptyDir(new EmptyDirVolumeSource()).endVolume()
                .addNewVolume().withName("taiga-media").withEmptyDir(new EmptyDirVolumeSource()).endVolume()

                .endSpec()
                .endTemplate()
                .endSpec().endReplicationControllerObject()
                .build();
    }
}
