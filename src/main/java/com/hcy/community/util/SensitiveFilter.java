package com.hcy.community.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class SensitiveFilter {
    
    
    // 替换符
    private static final String REPLACEMENT = "***";
    
    // 根节点
    private TrieNode rootNode = new TrieNode();
    
    //@PostConstruct注解的方法在项目启动的时候执行这个方法，也可以理解为在spring容器启动的时候执行，可作为一些数据的常规化加载，比如数据字典之类的
    @PostConstruct
    public void init() {
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ) {
            String keyword;
            while ((keyword = reader.readLine()) != null) {
                // 添加到前缀树
                this.addKeyword(keyword);
            }
        } catch (IOException e) {
            log.error("加载敏感词文件失败: " + e.getMessage());
        }
    }
    
    // 将一个敏感词添加到前缀树中
    private void addKeyword(String keyword) {
        TrieNode tempNode = rootNode;
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);
            
            if (subNode == null) {
                // 初始化子节点
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);
            }
            
            // 指向子节点,进入下一轮循环
            tempNode = subNode;
            
            // 设置结束标识
            if (i == keyword.length() - 1) {
                tempNode.setKeywordEnd(true);
            }
        }
    }
    
    /**
     * 过滤敏感词
     *
     * @param text 待过滤的文本
     * @return 过滤后的文本
     */
    public String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        
        // 指针1
        TrieNode tempNode = rootNode;
        // 指针2
        int begin = 0;
        // 指针3
        int position = 0;
        // 结果
        StringBuilder sb = new StringBuilder();
        
        while (position < text.length()) {
            char c = text.charAt(position);
            
            // 跳过特殊符号
            if (isSymbol(c) && position != text.length() - 1) {
                // 若指针1处于根节点,将此符号计入结果,让指针2向下走一步
                if (tempNode == rootNode) {
                    sb.append(c);
                    begin++;
                }
                // 无论符号在开头或中间,指针3都向下走一步
                position++;
                continue;
            }
            
            // 检查下级节点
            tempNode = tempNode.getSubNode(c);
            if (tempNode == null) {// 以begin开头的字符串不是敏感词
                sb.append(text.charAt(begin));
                // 进入下一个位置
                position = ++begin;
                // 重新指向根节点
                tempNode = rootNode;
            } else if (tempNode.isKeywordEnd()) {
                // 发现敏感词,将begin~position字符串替换掉
                sb.append(REPLACEMENT);
                // 进入下一个位置
                begin = ++position;//begin和position重合
                // 重新指向根节点
                tempNode = rootNode;
            } else {
                // 如果找到了敏感字符，但是又没结束，因此继续检查下一个字符
                // 如果当前 begin 字符到 position 末尾字符没有识别出敏感词，那么就从 begin 的下一个开始进行查找
                if (position < text.length() - 1) {
                    position++;
                } else {
                    // 这里还是指向 begin，并没有加 1，因为下一步循环就进入到 tempNode == null 判断里面了
                    // 因此 begin 和 position 都会加 1，同时上一个字符也会被加入到sb中
                    position = begin;
                }
            }
        }
        
        // 将最后一批字符计入结果
        sb.append(text.substring(begin));
        
        return sb.toString();
        
        /*
            以fabc为例
            在最后一个else里面加上判断（这个意思是如果从begin开始匹配敏感词到字符串末尾还没匹配结束，说明不是敏感词，
            因此我们要跳过这个字符，然后此时 position 等于 begin。为什么begin没有先加1呢？因为此时tempNode是指向 c 节点的，
            然后下一轮循环我们在 c 中查找是否存在 begin/position 位置子节点，没有找到，那么就会进入 tempNode == null 这个循环了，
            然后就会把 begin 位置的字符加入到 sb 中，同时 begin 和 position 都加 1，所以成功跳过了 f
            if (position < text.length() - 1) {
                position++;
            } else {
                position = begin;
            }
            
            ☆f☆a☆b☆c☆ 这样会导致最后一个符号时由于position++导致数组越界，因此我在判断是否为符号的时候加上一个判断条件：position != text.length()-1
            这样子就不会有这个问题了，而且最后一个空白字符也会输出，不会被屏蔽，只会屏蔽敏感词开始到结束中间的内容
         */
    }
    
    // 判断是否为符号
    private boolean isSymbol(Character c) {
        // 0x2E80~0x9FFF 是东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }
    
    // 前缀树(内部类)
    private class TrieNode {
        
        // 关键词结束标识
        private boolean isKeywordEnd = false;
        
        // 子节点(key是下级字符,value是下级节点)
        private Map<Character, TrieNode> subNodes = new HashMap<>();
        
        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }
        
        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }
        
        // 添加子节点
        public void addSubNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }
        
        // 获取子节点
        public TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }
        
    }
    
}
