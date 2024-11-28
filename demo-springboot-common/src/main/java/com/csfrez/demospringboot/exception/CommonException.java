package com.csfrez.demospringboot.exception;

/**
 * @author
 * @date 2024/11/28 16:10
 * @email
 */
public class CommonException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CommonException(){
        super();
    }

    public CommonException(String message){
        super(message);
    }
}
