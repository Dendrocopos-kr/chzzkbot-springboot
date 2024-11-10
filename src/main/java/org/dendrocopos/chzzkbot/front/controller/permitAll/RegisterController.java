package org.dendrocopos.chzzkbot.front.controller.permitAll;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class RegisterController {

    @RequestMapping(value = "/register")
    public String login() {
        return "html/permitAll/register_form";
    }
}
