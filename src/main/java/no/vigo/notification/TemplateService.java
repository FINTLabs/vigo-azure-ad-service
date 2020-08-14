package no.vigo.notification;

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

    public String render(String firstName, String registrationUrl, User owner, String template) {
        Context context = new Context();

        context.setVariable("consultantFullname", firstName);
        context.setVariable("registrationUrl", registrationUrl);
        context.setVariable("ownerFullname", owner.displayName);
        context.setVariable("ownerEmail", owner.mail);
        context.setVariable("ownerMobile", owner.mobilePhone);
        return templateEngine.process(template, context);
    }
}
