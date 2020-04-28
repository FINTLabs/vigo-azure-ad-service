package no.vigo.azure.ad.invite;

import lombok.Data;

@Data
public class UserInvitation {
    private String email;
    private String firstName;
    private String lastName;
    private String applicationUrl;
    private String owner;
    private boolean sendInvite = true;
}
