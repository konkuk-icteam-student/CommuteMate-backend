package com.better.CommuteMate.notification.application;

import com.better.CommuteMate.notification.application.dtos.NotificationChangeItem;

import java.util.List;

public interface NotificationContentSerializer {

    String serialize(List<NotificationChangeItem> items);
}
