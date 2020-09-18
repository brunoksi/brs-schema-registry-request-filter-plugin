package br.com.brs.kafka.plugins.schemaregistry.rest.extensions.config;

import io.confluent.kafka.schemaregistry.rest.SchemaRegistryConfig;
import io.confluent.rest.RestConfigException;
import org.apache.kafka.common.config.ConfigDef;

import java.util.Properties;

public class SchemaRegistryAditionalPluginConfigs extends SchemaRegistryConfig {

    public static final String RESOURCE_SSL_KEYSTORE_ADMIN_DOC = "resource.ssl.keystore.admin";
    public static final String RESOURCE_SSL_KEYSTORE_ADMIN_PASSWORD_DOC = "resource.ssl.keystore.admin.password";

    private static final ConfigDef puglinConfigDef = initConfigDef();

    public SchemaRegistryAditionalPluginConfigs(Properties props) throws RestConfigException {
        super(puglinConfigDef,props);
    }

    private static ConfigDef initConfigDef() {
        return baseSchemaRegistryConfigDef()
                .define("resource.ssl.keystore.admin", ConfigDef.Type.STRING, "DEFAULT", ConfigDef.Importance.HIGH, RESOURCE_SSL_KEYSTORE_ADMIN_DOC)
                .define("resource.ssl.keystore.admin.password", ConfigDef.Type.PASSWORD, "", ConfigDef.Importance.HIGH, RESOURCE_SSL_KEYSTORE_ADMIN_PASSWORD_DOC);
    }


}


