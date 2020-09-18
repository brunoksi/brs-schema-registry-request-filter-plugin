[Download Plugin](https://github.com/brunoksi/brs-schema-registry-request-filter-plugin/releases/download/v1.0.0/schema-registry-request-filter-plugin-1.0.0.jar)

# Contexto de utilização

De uma maneira sucinta, o schema-registry é o guardião do contrato de comunicação entre o produtor e consumidor dos tópicos no apache kafka. Acredito que a segurança, organização e governança dos dados são essenciais para o sucesso do negócio, em cenários empresárias é muito importante que os schemas sejam governados e tenham sua nomenclatura/taxinomia, padronizados, isso garante organização e reutilização dos schemas/eventos, consequentemente menos retrabalho por parte dos times de desenvolvimento. Até a versão 5.2 do schema-registry não era possível por padrão bloquear a criação de schemas dinamicamente, isso era de responsabilidade dos produtores e consumidores, era necessário definir a propriedade auto-create-schema como false explicitamente. Isso é um problema para quem quer governar os schemas. Uma alternativa para poder impedir os produtores e consumidores de criar schemas automaticamente, era utilizar plugins de segurança pagos, onde é possível bloquear acessos de criação e outros recursos através de acls. Pensando nisso resolvi desenvolver um plugin e disponibiliza-lo sobre uma licença open source. 

# Plugin para bloquear o acesso qualquer recurso que não seja de leitura dos schemas com exceção do usuário administrador

O puglin tem o objetivo de bloquear o acesso qualquer recurso que não seja de leitura dos schemas, além de oferecer a possibilidade de ter um usuário administrador que tenha acesso aos demais recursos como criar, deletar ou atualizar as informações dos schemas. O usuário administrador deve ser armazenado em um keystore específico para o puglin, o cliente no momento da requisição deve informar o mesmo certificado, o puglin é capaz de extrair o usuário (Principal) e procurar sua correspondência no keystore do puglin, quando o match é realizado os acessos as funções administrativas são liberadas. 

## Como configurar o plugin

Para usar o puglin, primeiro copiar o [extension jar](https://github.com/brunoksi/brs-schema-registry-request-filter-plugin/releases/download/v1.0.0/schema-registry-request-filter-plugin-1.0.0.jar) para /usr/share/java/schema-registry. 

O próximo passo é habilitar a autenticação tls no schema registry adicionando algumas configurações no arquivo /etc/schema-registry/schema-registry.properties: caso não saiba como fazer isso, acesse https://github.com/confluentinc/schema-registry

HAbilitando a autenticação TLS no schema-registry
```
ssl.client.auth=true
ssl.truststore.location=<your_truststore_file_location>
ssl.truststore.password=<your_truststore_password>
ssl.keystore.location=<your_keystore_file_location>
ssl.keystore.password=<your_keystore_password>
ssl.key.password=<your_keystore_password>

```

Habilitando o puglin. lembre-se, que o certificado referente ao usuário admin deve ser adicionado no keystore do plugin. 
```
resource.extension.class=SchemaRegistryRequestFilterResourceExtension
resource.ssl.keystore.admin=<your_keystore_resoucer_file_location>
resource.ssl.keystore.admin.password=<your_keystore_resoucer_password>
```

Agora podemos iniciar o schema registry e tentar registrar um subject, para isso é necessário informar um cerificado de cliente para comunicação com schema registry, uma vez que o mesmo está protegido por autenticação e criptografia tls 

```
$ curl  -XPOST --key ./some.pem --cert ./some.pem  -H "Content-Type: application/vnd.schemaregistry.v1+json" --data '{
  "schema": "{\"type\":\"record\",\"name\":\"pagamento\",\"namespace\":\"br.com.brs.examples.clients.pagamentoAvro\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"valor\",\"type\":\"double\"}]}"
}' https://<your_schema_registry_server>:8082/subjects/pagamentoAvro-value/versions

{"error_code":42205,"message":"The user informed by the certificate does not have permission to perform this resource"}
```

Caso queria realizar tarefas administrativas como cadastrar um schema (subject), o certificado informado pelo cliente deve ser o mesmo certificado armazenado na keystore especifico do pluglin 

```
$ curl  -XPOST --key ./admin.pem --cert ./admin.pem  -H "Content-Type: application/vnd.schemaregistry.v1+json" --data '{
  "schema": "{\"type\":\"record\",\"name\":\"pagamento\",\"namespace\":\"br.com.brs.examples.clients.pagamentoAvro\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"valor\",\"type\":\"double\"}]}"
}' https://<your_schema_registry_server>:8082/subjects/pagamentoAvro-value/versions
{"status": 201, "pagamentoAvro-value"}
```

