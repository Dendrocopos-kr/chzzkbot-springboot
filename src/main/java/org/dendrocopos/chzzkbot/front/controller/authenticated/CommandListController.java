package org.dendrocopos.chzzkbot.front.controller.authenticated;

import lombok.AllArgsConstructor;
import org.dendrocopos.chzzkbot.chzzk.entity.CommandMessageEntity;
import org.dendrocopos.chzzkbot.chzzk.repository.CommandMessageRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@AllArgsConstructor
public class CommandListController {
    private final CommandMessageRepository commandMessageRepository;

    @GetMapping(value = "/commandList")
    public String CommandList(Model model){
        List<CommandMessageEntity> data = commandMessageRepository.findAll();
        model.addAttribute("data", data);


        return "html/authenticated/command_list"; // 이 부분은 thymeleaf 템플릿 파일 이름과 동일해야 합니다.

    }
}
