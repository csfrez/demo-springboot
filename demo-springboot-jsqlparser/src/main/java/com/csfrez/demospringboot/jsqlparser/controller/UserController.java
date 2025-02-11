package com.csfrez.demospringboot.jsqlparser.controller;

import com.csfrez.demospringboot.jsqlparser.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author
 * @date 2025/2/11 15:38
 * @email
 */
@Controller
public class UserController {

    @Autowired
    private AppConfig appConfig;

    @GetMapping("/user-types")
    public String getUserTypes(Model model) {
        model.addAttribute("adminType", appConfig.getUserType().getAdmin().getDescription());
        model.addAttribute("userType", appConfig.getUserType().getUser().getDescription());
        model.addAttribute("guestType", appConfig.getUserType().getGuest().getDescription());
        model.addAttribute("vipType", appConfig.getUserType().getVip().getDescription());
        model.addAttribute("moderatorType", appConfig.getUserType().getModerator().getDescription());
        return "index";
    }
}