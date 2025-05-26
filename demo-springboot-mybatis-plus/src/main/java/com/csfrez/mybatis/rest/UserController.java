package com.csfrez.mybatis.rest;

import com.csfrez.mybatis.dao.entity.User;
import com.csfrez.mybatis.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author csfrez
 * @since 2025-05-26
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public ResponseEntity<List<User>> list() {
        return ResponseEntity.ok(userService.list());
    }

    @PostMapping("/add")
    public ResponseEntity<User> add(@RequestBody User user) {
        userService.save(user);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/update")
    public ResponseEntity<User> update(@RequestBody User user) {
        userService.updateById(user);
        return ResponseEntity.ok(user);
    }

}
