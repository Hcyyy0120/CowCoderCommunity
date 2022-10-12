package com.hcy.community;

import com.hcy.community.util.SensitiveFilter;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = CommunityApplication.class)
@SpringBootTest
public class SensitiveTest {
    
    @Autowired
    private SensitiveFilter sensitiveFilter;
    
    @Test
    public void testSensitive() {
        String text = "这里可以$赌$博，可以嫖娼，可以吸毒，可以开票，哈哈哈！";
        String filter = sensitiveFilter.filter(text);
        System.out.println(filter);
    }
}
