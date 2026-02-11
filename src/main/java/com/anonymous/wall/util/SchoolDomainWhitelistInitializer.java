package com.anonymous.wall.util;

import com.anonymous.wall.service.SchoolService;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Initializer to set up SchoolDomainWhitelist with SchoolService on startup
 */
@Singleton
public class SchoolDomainWhitelistInitializer implements ApplicationEventListener<StartupEvent> {

    @Inject
    private SchoolService schoolService;

    @Override
    public void onApplicationEvent(StartupEvent event) {
        SchoolDomainWhitelist.initialize(schoolService);
    }
}
