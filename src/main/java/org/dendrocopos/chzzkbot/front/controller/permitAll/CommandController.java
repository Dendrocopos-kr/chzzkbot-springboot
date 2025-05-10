package org.dendrocopos.chzzkbot.front.controller.permitAll;


import lombok.RequiredArgsConstructor;
import org.dendrocopos.chzzkbot.chzzk.chatentity.CommandMessageEntity;
import org.dendrocopos.chzzkbot.chzzk.repository.CommandMessageRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;


@Controller
@RequestMapping("/commands")
@RequiredArgsConstructor
public class CommandController {
    
    private final CommandMessageRepository commandMessageRepository;
    
    @GetMapping
    public String showCommands(Model model) {
        List<CommandMessageEntity> commands = commandMessageRepository.findAll();
        model.addAttribute("currentPage", "commands");  // 현재 페이지 표시
        model.addAttribute("data", commands);
        return "common/commands";
    }
}