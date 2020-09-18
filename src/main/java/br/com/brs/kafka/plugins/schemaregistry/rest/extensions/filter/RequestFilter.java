/*
 * Copyright 2020 brunoksi.
 *
*/

package br.com.brs.kafka.plugins.schemaregistry.rest.extensions.filter;

import br.com.brs.kafka.plugins.schemaregistry.rest.extensions.config.SchemaRegistryAditionalPluginConfigs;
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryConfig;
import io.confluent.kafka.schemaregistry.rest.exceptions.Errors;
import io.confluent.kafka.schemaregistry.rest.resources.ConfigResource;
import io.confluent.kafka.schemaregistry.rest.resources.SubjectVersionsResource;
import io.confluent.kafka.schemaregistry.rest.resources.SubjectsResource;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;


@Priority(Priorities.AUTHORIZATION)
public class RequestFilter implements ContainerRequestFilter {

    private static final Set<ResourceActionKey> subjectWriteActions =
        new HashSet<>();

    private SchemaRegistryConfig schemaRegistryConfig = null;

    private static KeyStore keystore;
    @Context
    ResourceInfo resourceInfo;

    @Context
    UriInfo uriInfo;

    @Context
    HttpServletRequest httpServletRequest;

    private static final String X509_CERTIFICATE_ATTRIBUTE = "javax.servlet.request.X509Certificate";

    static {
        initializeSchemaRegistrySubjectWriteActions();
    }

    private static void initializeSchemaRegistrySubjectWriteActions() {
        subjectWriteActions.add(
            new ResourceActionKey(SubjectVersionsResource.class, "POST"));
        subjectWriteActions.add(
            new ResourceActionKey(SubjectVersionsResource.class, "DELETE"));
        subjectWriteActions.add(
            new ResourceActionKey(SubjectsResource.class, "DELETE"));
        subjectWriteActions.add(
            new ResourceActionKey(ConfigResource.class, "PUT"));
    }

    public RequestFilter(SchemaRegistryAditionalPluginConfigs schemaRegistryConfig) {
        this.schemaRegistryConfig = schemaRegistryConfig;
        RequestFilter.loadKeystorePlugin(this.schemaRegistryConfig.getString("resource.ssl.keystore.admin"),  this.schemaRegistryConfig.getPassword("resource.ssl.keystore.admin.password").value());
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Class resource = resourceInfo.getResourceClass();
        String restMethod = requestContext.getMethod();
        X509Certificate[] certificateChain = (X509Certificate[])  httpServletRequest.getAttribute(X509_CERTIFICATE_ATTRIBUTE);
        boolean isOwnerPrincipal = false;
        try {
            isOwnerPrincipal = findOwnerPrincipalFromKeystore(certificateChain[0].getSubjectDN());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

     if (! isOwnerPrincipal) {
            ResourceActionKey key = new ResourceActionKey(resource, restMethod);
            if (subjectWriteActions.contains(key)) {
                throw Errors.operationNotPermittedException("The user informed by the certificate does not have permission to perform this resource");
            }
     }
    }

    private static void loadKeystorePlugin(String keystoreLocation, String keyStorePassWord){
        try {
        FileInputStream is = null;
        is = new FileInputStream(keystoreLocation);
        keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(is, keyStorePassWord.toCharArray());
        is.close();
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | RuntimeException e) {
            e.printStackTrace();
        }
    }

    private boolean findOwnerPrincipalFromKeystore(Principal clientPrincipalDN) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {

        try {

            Enumeration e = keystore.aliases();
            for (; e.hasMoreElements();) {
                String alias = (String) e.nextElement();
                java.security.cert.Certificate cert = keystore.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    X509Certificate x509cert = (X509Certificate) cert;
                    // Get subject
                    Principal serverPrincipal = x509cert.getSubjectDN();
                    if (serverPrincipal.getName().equals(clientPrincipalDN.getName())){
                        return true;
                    }
                }
            }
            return false;

        } catch (KeyStoreException |  RuntimeException e) {
            e.printStackTrace();
            return false;
        }
 }

    private static class ResourceActionKey {

        private final Class resourceClass;
        private final String restMethod;

        public ResourceActionKey(Class resourceClass, String restMethod) {
            this.resourceClass = resourceClass;
            this.restMethod = restMethod;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ResourceActionKey that = (ResourceActionKey) o;
            if (!resourceClass.equals(that.resourceClass)) {
                return false;
            }
            return restMethod.equals(that.restMethod);
        }

        @Override
        public int hashCode() {
            int result = resourceClass.hashCode();
            result = 31 * result + restMethod.hashCode();
            return result;
        }
    }
}
