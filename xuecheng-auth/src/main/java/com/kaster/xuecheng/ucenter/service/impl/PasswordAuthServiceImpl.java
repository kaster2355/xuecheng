package com.kaster.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kaster.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.kaster.xuecheng.ucenter.mapper.XcUserMapper;
import com.kaster.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.kaster.xuecheng.ucenter.model.dto.XcUserExt;
import com.kaster.xuecheng.ucenter.model.po.XcUser;
import com.kaster.xuecheng.ucenter.service.AuthService;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {

    @Autowired
    XcUserMapper xcUserMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    CheckCodeClient checkCodeClient;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {

        // 远程调用验证码服务校验
        String checkcode = authParamsDto.getCheckcode();
        String checkcodekey = authParamsDto.getCheckcodekey();

        if (Strings.isEmpty(checkcodekey) || Strings.isEmpty(checkcode)){
            throw new RuntimeException("请输入验证码");
        }

        Boolean verify = checkCodeClient.verify(checkcodekey, checkcode);
        if (verify == null || !verify){
            throw new RuntimeException("验证码输入错误");
        }

        //账号
        String username = authParamsDto.getUsername();
        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if(user==null){
            //返回空表示用户不存在
            throw new RuntimeException("账号不存在");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(user,xcUserExt);
        //校验密码
        //取出数据库存储的正确密码
        String passwordDb  =user.getPassword();
        String passwordForm = authParamsDto.getPassword();
        boolean matches = passwordEncoder.matches(passwordForm, passwordDb);
        if(!matches){
            throw new RuntimeException("账号或密码错误");
        }
        return xcUserExt;
    }

}
