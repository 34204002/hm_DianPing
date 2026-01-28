package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;

/**
 * 正则工具类，用于校验字符串格式
 * @author 虎哥
 */
public class RegexUtils {
    /**
     * 是否是有效手机格式
     * @param phone 要校验的手机号
     * @return true:符合，false：不符合
     */
    public static boolean isPhoneValid(String phone){
        return match(phone, RegexPatterns.PHONE_REGEX);
    }
    
    /**
     * 是否是无效手机格式
     * @param phone 要校验的手机号
     * @return true:不符合，false：符合
     */
    public static boolean isPhoneInvalid(String phone){
        return !isPhoneValid(phone);
    }
    
    /**
     * 是否是有效邮箱格式
     * @param email 要校验的邮箱
     * @return true:符合，false：不符合
     */
    public static boolean isEmailValid(String email){
        return match(email, RegexPatterns.EMAIL_REGEX);
    }
    
    /**
     * 是否是无效邮箱格式
     * @param email 要校验的邮箱
     * @return true:不符合，false：符合
     */
    public static boolean isEmailInvalid(String email){
        return !isEmailValid(email);
    }

    /**
     * 是否是有效验证码格式
     * @param code 要校验的验证码
     * @return true:符合，false：不符合
     */
    public static boolean isCodeValid(String code){
        return match(code, RegexPatterns.VERIFY_CODE_REGEX);
    }
    
    /**
     * 是否是无效验证码格式
     * @param code 要校验的验证码
     * @return true:不符合，false：符合
     */
    public static boolean isCodeInvalid(String code){
        return !isCodeValid(code);
    }
    
    /**
     * 是否是有效密码格式
     * @param password 要校验的密码
     * @return true:符合，false：不符合
     */
    public static boolean isPasswordValid(String password){
        return match(password, RegexPatterns.PASSWORD_REGEX);
    }
    
    /**
     * 校验字符串是否符合正则格式
     * @param str 待校验字符串
     * @param regex 正则表达式
     * @return true:符合，false：不符合
     */
    public static boolean match(String str, String regex){
        if (StrUtil.isBlank(str)) {
            return false;
        }
        return str.matches(regex);
    }
}
