package com.astrodoorways.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

@Component
@Profile("!test")
public class Runner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    @Autowired
    private Converter converter;

    @Autowired
    private ConfigurableApplicationContext context;

    @Override
    public void run(String... args) throws Exception {
        converter.setReadDirectory(ApplicationProperties.getPropertyAsString(ApplicationProperties.READ_DIRECTORY_PROPERTY));
        converter.setWriteDirectory(ApplicationProperties.getPropertyAsString(ApplicationProperties.WRITE_DIRECTORY_PROPERTY));

        try {
            converter.beginConversion();
        } catch (Exception e) {
            logger.error("something bad has risen to the top of the application", e);
        }

        logger.debug("The converter has finished. If the application is still running, feel free to kill it.");
        context.close();
    }



}
