package com.sankuai.sjst.scm.exception;

import com.sankuai.sjst.scm.exception.error.BasicErrorCode;

public class ExtensionException extends BaseException {
    
    private static final long serialVersionUID = 1L;
    
    public ExtensionException(String errMessage){
        super(errMessage);
        this.setErrCode(BasicErrorCode.COLA_ERROR);
    }
    
    public ExtensionException(String errMessage, Throwable e) {
        super(errMessage, e);
        this.setErrCode(BasicErrorCode.COLA_ERROR);
    }
}