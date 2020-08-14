package no.vigo.provisioning.qlik;

import com.google.gson.annotations.Expose;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Data
@Builder
public class QlikUser {
    private String countyNumber;
    private String nin;
    @Expose
    private String firstName;
    @Expose
    private String lastName;
    @Expose
    private String email;
    private String mobile;
    private Boolean qlikViewRead;
    private Boolean qlikViewDeveloper;
    private Boolean qlikSenseDeveloper;
    private Boolean qlikSenseRead;
    private Boolean nPrintDeveloper;
    private Boolean nPrintRead;

    public String getAzureADUPN() {
        return UriUtils.encode(
                String.format(
                        "%s#EXT#@vigoiks.onmicrosoft.com",
                        email.replace("@", "_")
                ),
                StandardCharsets.UTF_8.toString()
        );
    }

    public String getFullname() {
        return String.format("%s %s", firstName, lastName);
    }
}
