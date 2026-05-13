package com.auraspark.note.core.service.test.impl;

import com.auraspark.note.core.entity.TestUser;
import com.auraspark.note.core.mapper.TestUserMapper;
import com.auraspark.note.core.service.test.TestService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TestServiceImpl implements TestService {

    private final TestUserMapper testUserMapper;

    public TestServiceImpl(TestUserMapper testUserMapper) {
        this.testUserMapper = testUserMapper;
    }

    @Override
    public List<TestUser> getTestUser() {
        return testUserMapper.selectList(null);
    }
}

