package no.vigo.azure.ad.invite.token;

import lombok.Data;
import lombok.EqualsAndHashCode;
import no.rogfk.jwt.claims.DefaultClaim;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserToken extends DefaultClaim {
    private String id;
}