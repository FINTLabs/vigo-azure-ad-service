package no.vigo.azure.ad.invite;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.auth.Credentials;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

@Slf4j
@Configuration
public class Config {

    @Bean
    public Credentials credentials(
            @Value("${fint.invite.mail.serviceaccount:serviceaccount.json}") Path serviceAccount,
            @Value("${fint.orgmonitor.gmail.scopes:https://www.googleapis.com/auth/gmail.send}") String[] scopes,
            @Value("${fint.orgmonitor.gmail.delegate:frode@fintlabs.no}") String delegate
    ) throws IOException {
        InputStream in = Files.newInputStream(serviceAccount);
        return ServiceAccountCredentials.fromStream(in).createScoped(scopes).createDelegated(delegate);
    }

    @Bean
    public HttpTransport httpTransport() throws GeneralSecurityException, IOException {
        return GoogleNetHttpTransport.newTrustedTransport();
    }

    @Bean
    public JsonFactory jsonFactory() {
        return JacksonFactory.getDefaultInstance();
    }

    @Bean
    public Gmail gmail(
            com.google.api.client.http.HttpTransport transport,
            com.google.api.client.json.JsonFactory jsonFactory,
            Credentials credentials,
            @Value("${fint.orgmonitor.application-name:JADA}") String applicationName
    ) {
        return new Gmail.Builder(transport, jsonFactory, new HttpCredentialsAdapter(credentials))
                .setApplicationName(applicationName)
                .build();
    }

    @Bean("sender")
    public String sender(@Value("${fint.orgmonitor.sender:no-reply@fintlabs.no}") String sender) { return sender; }


}