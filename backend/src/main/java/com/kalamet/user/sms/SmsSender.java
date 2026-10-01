package com.kalamet.user.sms;

/** Delivers login codes. Selected by {@code kalamet.sms.provider}. */
public interface SmsSender {

    void sendLoginCode(String mobile, String code);
}
