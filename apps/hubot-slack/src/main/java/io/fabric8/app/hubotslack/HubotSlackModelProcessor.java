package io.fabric8.app.hubotslack;

import io.fabric8.kubernetes.generator.annotation.KubernetesProvider;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.api.model.TemplateBuilder;

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
                .addNewParameter().withName("HUBOT_SLACK_GRAFANA_HOST")
                    .withValue("http://grafana2.default.svc.cluster.local")
                    .withDescription("Host for your Grafana 2.0 install, e.g. 'http://play.grafana.org'").endParameter()
                .addNewParameter().withName("HUBOT_SLACK_GRAFANA_API_KEY")
                    .withDescription("API key for a particular user").endParameter()
                .addNewParameter().withName("HUBOT_JENKINS_URL")
                    .withValue("http://jenkins.default.svc.cluster.local")
                    .withDescription("The URL for the Jenkins CI server").endParameter()
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
                                    .withContainerPort(8080)
                                    .withProtocol("TCP")
                                .endPort()
                                .addNewEnv().withName("HUBOT_SLACK_TOKEN").withValue("${HUBOT_SLACK_TOKEN}").endEnv()
                                .addNewEnv().withName("HUBOT_GRAFANA_HOST").withValue("${HUBOT_SLACK_GRAFANA_HOST}").endEnv()
                                .addNewEnv().withName("HUBOT_GRAFANA_API_KEY").withValue("${HUBOT_SLACK_GRAFANA_API_KEY}").endEnv()
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
                .withType("LoadBalancer")
                    .addNewPort()
                .withPort(80)
                        .withNewTargetPort(8080)
                .endPort()
                    .addToSelector("name", NAME)
                .endSpec()
                .endServiceObject()

                .build();
    }
}
