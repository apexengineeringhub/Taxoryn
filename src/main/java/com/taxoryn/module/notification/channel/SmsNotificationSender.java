package com.taxoryn.module.notification.channel;

public interface SmsNotificationSender {

    boolean sendSms(String phoneNumber, String message);

    String getProviderName();
}
