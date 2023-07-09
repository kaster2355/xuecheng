package com.kaster.xuecheng.base.exception;

public class XuechengException extends RuntimeException{
    private String errMessage;

    public XuechengException() {
        super();
    }

    public XuechengException(String errMessage) {
        super(errMessage);
        this.errMessage = errMessage;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public static void cast(CommonError commonError){
        throw new XuechengException(commonError.getErrMessage());
    }
    public static void cast(String errMessage){
        throw new XuechengException(errMessage);
    }

}
