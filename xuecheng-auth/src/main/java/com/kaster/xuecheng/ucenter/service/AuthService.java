package com.kaster.xuecheng.ucenter.service;

import com.kaster.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.kaster.xuecheng.ucenter.model.dto.XcUserExt;

public interface AuthService {
    XcUserExt execute(AuthParamsDto authParamsDto);
}
