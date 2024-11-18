package org.dendrocopos.chzzkbot.front.controller.permitAll;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class LoginController {

    @GetMapping(value = "/login")
    public String login() {
        return "html/permitAll/login";
    }
}
