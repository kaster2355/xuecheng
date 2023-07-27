package com.kaster.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kaster.xuecheng.ucenter.mapper.XcUserMapper;
import com.kaster.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.kaster.xuecheng.ucenter.model.po.XcUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class UserServiceImpl implements UserDetailsService {

    @Autowired
    private XcUserMapper xcUserMapper;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {

        // s是AuthParamDto的json
        AuthParamsDto authParamsDto = null;

        try {
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        } catch (Exception e) {
            throw new RuntimeException("认证请求参数不符合规定");
        }
        String username = authParamsDto.getUsername();
        // 用s查询数据库
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));

        // 不存在返回null
        if (xcUser == null) {
            return null;
        }

        // 存在拿到正确密码封装给框架比对
        String[] authorities = {"test"};
        String password = xcUser.getPassword();
        xcUser.setPassword(null);

        String jsonString = JSON.toJSONString(xcUser);
        return User.withUsername(jsonString).authorities(authorities).password(password).build();

    }
}
