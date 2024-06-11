package org.dendrocopos.chzzkbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
public class GuribotApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
		SpringApplication.run(GuribotApplication.class, args);

	}

}
