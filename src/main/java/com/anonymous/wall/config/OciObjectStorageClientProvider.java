package com.anonymous.wall.config;

import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import io.micronaut.context.annotation.Requires;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

@Singleton
@Requires(env = "prod")
public class OciObjectStorageClientProvider {

    private final ObjectStorageClient client;

    public OciObjectStorageClientProvider() {
        this.client = ObjectStorageClient.builder()
                .build(InstancePrincipalsAuthenticationDetailsProvider.builder().build());
    }

    public ObjectStorageClient getClient() {
        return client;
    }

    @PreDestroy
    void destroy() {
        client.close();
    }
}