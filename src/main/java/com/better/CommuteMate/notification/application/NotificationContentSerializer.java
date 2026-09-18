package com.better.CommuteMate.notification.application;

import com.better.CommuteMate.notification.application.dtos.NotificationChangeItem;
import com.better.CommuteMate.notification.controller.dtos.NotificationListResponse;

import java.util.List;

public interface NotificationContentSerializer {

    String serialize(List<NotificationChangeItem> items);

    List<NotificationListResponse.ContentItem> parse(String content);
}
