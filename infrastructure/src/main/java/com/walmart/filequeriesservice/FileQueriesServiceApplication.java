package com.walmart.filequeriesservice;

import com.walmart.filequeriesservice.infrastructure.GatewayConfigurationModule;
import com.walmart.filequeriesservice.infrastructure.GatewayStack;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

@SpringBootApplication
public class FileQueriesServiceApplication {

	public static void main(final String[] args) {

		final ConfigurableApplicationContext application = SpringApplication.run(FileQueriesServiceApplication.class, args);
		final GatewayConfigurationModule module = application.getBean(GatewayConfigurationModule.class);

		final App app = new App();
		new GatewayStack(app, "GatewayStack", StackProps.builder().build(), module);
		app.synth();
	}

}
