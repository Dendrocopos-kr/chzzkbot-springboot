package org.dendrocopos.chzzkbot.home.controller;


import lombok.AllArgsConstructor;
import org.dendrocopos.chzzkbot.chzzk.chatentity.CommandMessageEntity;
import org.dendrocopos.chzzkbot.chzzk.repository.CommandMessageRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;


@Controller
@AllArgsConstructor
public class MainController {
  private final CommandMessageRepository commandMessageRepository;

  @RequestMapping("/")
  public String showMainPage(Model model) {
    List<CommandMessageEntity> data = commandMessageRepository.findAll();
    model.addAttribute("data", data);
    
    return "main"; // 이 부분은 thymeleaf 템플릿 파일 이름과 동일해야 합니다. 
  }
}