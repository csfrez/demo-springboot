package com.csfrez.mybatis.service;

import com.csfrez.mybatis.dao.entity.User;
import com.csfrez.mybatis.dao.mapper.UserMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
*
* @author csfrez
* @since 2025-05-26
*/
@Service
public class UserService extends ServiceImpl<UserMapper, User> {

}