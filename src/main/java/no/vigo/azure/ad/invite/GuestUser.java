package no.vigo.azure.ad.invite;

import lombok.Data;

@Data
public class GuestUser {

    private String id;
    private String firstName;
    private String lastName;
    private String mobile;
    private String title;
    private String organisation;
}
