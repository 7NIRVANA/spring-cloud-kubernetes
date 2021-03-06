/*
 * Copyright (C) 2016 to the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.fabric8.spring.cloud.kubernetes.reload;

import javax.annotation.PostConstruct;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.spring.cloud.kubernetes.config.ConfigMapPropertySource;
import io.fabric8.spring.cloud.kubernetes.config.ConfigMapPropertySourceLocator;
import io.fabric8.spring.cloud.kubernetes.config.SecretsPropertySource;
import io.fabric8.spring.cloud.kubernetes.config.SecretsPropertySourceLocator;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * A change detector that periodically retrieves secrets and configmaps and fire a reload when something changes.
 */
public class PollingConfigurationChangeDetector extends ConfigurationChangeDetector {

    private ConfigMapPropertySourceLocator configMapPropertySourceLocator;

    private SecretsPropertySourceLocator secretsPropertySourceLocator;

    public PollingConfigurationChangeDetector(AbstractEnvironment environment,
                                              ConfigReloadProperties properties,
                                              KubernetesClient kubernetesClient,
                                              ConfigurationUpdateStrategy strategy,
                                              ConfigMapPropertySourceLocator configMapPropertySourceLocator,
                                              SecretsPropertySourceLocator secretsPropertySourceLocator) {
        super(environment, properties, kubernetesClient, strategy);

        this.configMapPropertySourceLocator = configMapPropertySourceLocator;
        this.secretsPropertySourceLocator = secretsPropertySourceLocator;
    }

    @PostConstruct
    public void init() {
        log.info("Kubernetes polling configuration change detector activated");
    }

    @Scheduled(initialDelayString = "${spring.cloud.kubernetes.reload.period:15000}", fixedDelayString = "${spring.cloud.kubernetes.reload.period:15000}")
    public void executeCycle() {

        boolean changedConfigMap = false;
        if (properties.isMonitoringConfigMaps()) {
            MapPropertySource currentConfigMapSource = findPropertySource(ConfigMapPropertySource.class);
            if (currentConfigMapSource != null) {
                MapPropertySource newConfigMapSource = configMapPropertySourceLocator.locate(environment);
                changedConfigMap = changed(currentConfigMapSource, newConfigMapSource);
            }
        }

        boolean changedSecrets = false;
        if (properties.isMonitoringSecrets()) {
            MapPropertySource currentSecretSource = findPropertySource(SecretsPropertySource.class);
            if (currentSecretSource != null) {
                MapPropertySource newSecretSource = secretsPropertySourceLocator.locate(environment);
                changedSecrets = changed(currentSecretSource, newSecretSource);
            }
        }

        if (changedConfigMap || changedSecrets) {
            reloadProperties();
        }
    }

}
