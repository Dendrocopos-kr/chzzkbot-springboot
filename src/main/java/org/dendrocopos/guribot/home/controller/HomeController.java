package org.dendrocopos.guribot.home.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.http.WebSocket;


@Controller
public class HomeController {

    @GetMapping(value = "/")
    public String home(){
        return "index.html";
    }

}
