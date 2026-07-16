package com.better.CommuteMate.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    public Object getMyPage(Long userId) {

        return null;
    }

    public Object getMyPublishedFaqs(
            Long userId,
            int page
    ) {

        return null;
    }

    public Object getMyDraftFaqs(
            Long userId,
            int page
    ) {

        return null;
    }
}
