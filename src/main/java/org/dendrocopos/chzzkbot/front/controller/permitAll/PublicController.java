package org.dendrocopos.chzzkbot.front.controller.permitAll;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/public")
public class PublicController {

    @GetMapping(value = "/register")
    public String showRegister() {
        return "/html/permitAll/register_form";
    }

    @GetMapping(value = "/rules")
    public String showRulesPage() {
        return "/html/permitAll/rules";
    }
}
