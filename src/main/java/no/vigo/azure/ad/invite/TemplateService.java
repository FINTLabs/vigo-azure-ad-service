package no.vigo.azure.ad.invite;

import com.microsoft.graph.models.extensions.User;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


@Service
public class TemplateService {

    private final TemplateEngine templateEngine;

    public TemplateService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String render(String fornavn, String registrationUrl, User owner) {
        Context context = new Context();

        context.setVariable("consultantFullname", fornavn);
        context.setVariable("registrationUrl", registrationUrl);
        context.setVariable("ownerFullname", owner.displayName);
        context.setVariable("ownerEmail", owner.mail);
        context.setVariable("ownerMobile", owner.mobilePhone);
        return templateEngine.process("email-template", context);
    }
}
