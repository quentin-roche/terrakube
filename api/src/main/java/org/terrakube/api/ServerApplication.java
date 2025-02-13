package org.terrakube.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.orange.common.springboot.autoconfigure.proxy.NetworkProxyAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "org.terrakube.api.repository")
@EnableAsync
@EnableCaching
@EnableScheduling
@ImportAutoConfiguration(NetworkProxyAutoConfiguration.class)
public class ServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServerApplication.class, args);
	}

}

