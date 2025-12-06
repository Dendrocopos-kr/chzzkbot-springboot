package org.dendrocopos.chzzkbot.front.controller.permitAll;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
    @GetMapping("/")
    public String showMainPage(Model model) {
        model.addAttribute("currentPage", "home");
        return "index";
    }

    @GetMapping("/about")
    public String showAboutPage(Model model) {
        model.addAttribute("currentPage", "about");
        return "common/about";
    }

    @GetMapping("/contact")
    public String showContactPage(Model model) {
        model.addAttribute("currentPage", "contact");
        return "common/contact";
    }

    @GetMapping("/rules")
    public String showRulesPage(Model model) {
        model.addAttribute("currentPage", "rules");
        return "common/rules";
    }

    @GetMapping(value = "/test")
    public String testPage() {
        return "test/html_new/stat_public_251029";
    }

    @GetMapping(value = "/gis_layer")
    public String gisLayer() {
        return "test/html_new/gis_layer";
    }
}