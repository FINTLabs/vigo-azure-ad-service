package no.vigo;

import no.rogfk.jwt.annotations.EnableJwt;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJwt(issuer = "vigo-iks", maxAgeMinutes = 120)
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }


}
