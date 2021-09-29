package org.notabarista.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@ComponentScan("org.notabarista")
@EnableSwagger2
public class BeStorageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeStorageServiceApplication.class, args);
    }

}
