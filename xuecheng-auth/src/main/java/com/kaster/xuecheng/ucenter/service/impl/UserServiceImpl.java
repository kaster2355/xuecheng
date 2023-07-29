package com.kaster.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.kaster.xuecheng.ucenter.mapper.XcMenuMapper;
import com.kaster.xuecheng.ucenter.mapper.XcUserMapper;
import com.kaster.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.kaster.xuecheng.ucenter.model.dto.XcUserExt;
import com.kaster.xuecheng.ucenter.model.po.XcMenu;
import com.kaster.xuecheng.ucenter.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserServiceImpl implements UserDetailsService {

    @Autowired
    private XcUserMapper xcUserMapper;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    XcMenuMapper xcMenuMapper;


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

        String[] authorities = {"p1"};
        //查询用户权限
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId(user.getId());
        if (xcMenus.size()>0){
            authorities = xcMenus.stream().map(XcMenu::getCode).toArray(String[]::new);
        }

        String password = user.getPassword();
        //为了安全在令牌中不放密码
        user.setPassword(null);
        //将user对象转json
        String userString = JSON.toJSONString(user);
        //创建UserDetails对象
        return User.withUsername(userString).password(password).authorities(authorities).build();
    }

}
