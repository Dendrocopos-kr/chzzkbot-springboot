package org.dendrocopos.chzzkbot.home.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
@Slf4j
public class HomeController {

    @GetMapping("/admin")
    public String admin() {
        log.info("admin:{}", "home");
        return "html/admin";
    }
}
