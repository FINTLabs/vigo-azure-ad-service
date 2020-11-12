package no.vigo.azure.ad;

import com.microsoft.graph.models.extensions.User;
import lombok.Data;

import java.util.function.Consumer;

@Data
public class AzureUserResponse {
    private User user;
    private int responseCode;

    public boolean notExists() {
        return user == null && responseCode == 404;
    }

    public void withUser(Consumer<User> consumer) {
        if (responseCode == 200) {
            consumer.accept(user);
        }
    }

    public boolean hasError() {
        return responseCode != 200;
    }
}
