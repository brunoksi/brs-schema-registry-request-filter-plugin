/*
 * Copyright 2020 brunoksi.
 *
*/

package br.com.brs.kafka.plugins.schemaregistry.rest.extensions;

import br.com.brs.kafka.plugins.schemaregistry.rest.extensions.config.SchemaRegistryAditionalPluginConfigs;
import br.com.brs.kafka.plugins.schemaregistry.rest.extensions.filter.RequestFilter;
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryConfig;
import io.confluent.kafka.schemaregistry.rest.extensions.SchemaRegistryResourceExtension;
import io.confluent.kafka.schemaregistry.storage.SchemaRegistry;
import io.confluent.rest.RestConfigException;

import javax.ws.rs.core.Configurable;
import java.io.IOException;

public class SchemaRegistryRequestFilterResourceExtension
    implements SchemaRegistryResourceExtension {

    @Override
    public void register(Configurable<?> configurable,SchemaRegistryConfig schemaRegistryConfig,SchemaRegistry schemaRegistry) {
        SchemaRegistryAditionalPluginConfigs schemaRegistryAditionalPluginConfigs = null;
        try {
            schemaRegistryAditionalPluginConfigs = new SchemaRegistryAditionalPluginConfigs(schemaRegistryConfig.originalProperties());
            configurable.register(new RequestFilter(schemaRegistryAditionalPluginConfigs));
        } catch (RestConfigException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
    }
}
