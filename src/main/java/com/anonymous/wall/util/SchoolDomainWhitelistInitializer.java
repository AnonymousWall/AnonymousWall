package com.anonymous.wall.util;

import com.anonymous.wall.service.SchoolDomainService;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Initializer to set up SchoolDomainWhitelist with SchoolDomainService on startup
 */
@Singleton
public class SchoolDomainWhitelistInitializer implements ApplicationEventListener<StartupEvent> {

    @Inject
    private SchoolDomainService schoolDomainService;

    @Override
    public void onApplicationEvent(StartupEvent event) {
        SchoolDomainWhitelist.initialize(schoolDomainService);
    }
}
