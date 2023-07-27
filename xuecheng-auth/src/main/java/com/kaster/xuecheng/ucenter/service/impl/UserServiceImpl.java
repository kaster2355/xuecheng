package com.kaster.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kaster.xuecheng.ucenter.mapper.XcUserMapper;
import com.kaster.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.kaster.xuecheng.ucenter.model.dto.XcUserExt;
import com.kaster.xuecheng.ucenter.model.po.XcUser;
import com.kaster.xuecheng.ucenter.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class UserServiceImpl implements UserDetailsService {

    @Autowired
    private XcUserMapper xcUserMapper;

    @Autowired
    ApplicationContext applicationContext;


    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {

        // s是AuthParamDto的json
        AuthParamsDto authParamsDto = null;

        try {
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        } catch (Exception e) {
            throw new RuntimeException("认证请求参数不符合规定");
        }

        String authType = authParamsDto.getAuthType();
        AuthService authService = applicationContext.getBean(authType + "_authservice", AuthService.class);
        XcUserExt userExt = authService.execute(authParamsDto);

        return getUserPrincipal(userExt);
    }

    public UserDetails getUserPrincipal(XcUserExt user) {
        //用户权限,如果不加报Cannot pass a null GrantedAuthority collection
        String[] authorities = {"p1"};
        String password = user.getPassword();
        //为了安全在令牌中不放密码
        user.setPassword(null);
        //将user对象转json
        String userString = JSON.toJSONString(user);
        //创建UserDetails对象
        return User.withUsername(userString).password(password).authorities(authorities).build();
    }

}
