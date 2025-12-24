package com.example.readapp;

        import java.io.BufferedReader;
        import java.io.File;
        import java.io.FileReader;
        import java.io.IOException;
        import java.util.ArrayList;
        import java.util.List;
        import java.util.regex.Matcher;
        import java.util.regex.Pattern;

        import java.io.*;
        import java.util.ArrayList;
        import java.util.List;
        import java.util.regex.*;

public class BookParser {

    /**
     * 解析结果包装类
     */
    public static class ParseResult {
        public final List<ChapterBean> chapters;
        public final int totalWords;
        public final int chapterCount;

        public ParseResult(List<ChapterBean> chapters, int totalWords, int chapterCount) {
            this.chapters = chapters;
            this.totalWords = totalWords;
            this.chapterCount = chapterCount;
        }
    }
    // 支持中文（第X章）和数字（第1章）格式
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
            "^第([\\u4e00-\\u9fa5零一二三四五六七八九十百千万0-9]+)章\\s*(.*)$"
    );

    public static ParseResult parseTxt(File file) throws IOException {
        List<ChapterBean> chapters = new ArrayList<>();
        int totalWords = 0;
        int chapterCount = 0;
        StringBuilder chapterContent = new StringBuilder();
        String currentTitle = null;
        int currentChapterNumber = 1;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = CHAPTER_PATTERN.matcher(line);
                if (matcher.find()) {
                    if (currentTitle != null) {
                        // 处理已累积的章节内容
                        ChapterBean chapter = createChapter(
                                currentChapterNumber++,
                                currentTitle,
                                chapterContent
                        );
                        chapters.add(chapter);
                        totalWords += chapter.getWordCount();
                        chapterCount++;
                    }
                    currentTitle = matcher.group(2).trim();
                    chapterContent.setLength(0); // 重置内容缓存
                } else if (currentTitle != null) {
                    chapterContent.append(line).append("\n");

                    // 每处理500行刷新一次内存
                    if ((chapterContent.length() % 500) == 0) {
                        chapterContent.trimToSize();
                    }
                }
            }

            // 处理最后一章
            if (currentTitle != null) {
                ChapterBean chapter = createChapter(
                        currentChapterNumber,
                        currentTitle,
                        chapterContent
                );
                chapters.add(chapter);
                totalWords += chapter.getWordCount();
                chapterCount++;
            }
        }
        return new ParseResult(chapters, totalWords, chapterCount);
    }


    /**
     * 创建章节对象并计算字数
     */
    private static ChapterBean createChapter(int number, String title, StringBuilder content) {
        String chapterText = content.toString().trim();
        ChapterBean chapter = new ChapterBean();
        chapter.setChapterNumber(number);
        chapter.setChapterTitle(title.isEmpty() ? "第"+number+"章" : title);
        chapter.setContent(chapterText);
        chapter.setWordCount(countWords(chapterText)); // 计算字数
        return chapter;
    }

    /**
     * 高效字数统计方法（支持大文本）
     * @param text 需要统计的文本
     * @return 总字数（中文按字，英文按词）
     */
    public static int countWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int chineseCount = 0;
        int englishWordCount = 0;
        boolean inEnglishWord = false;

        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                inEnglishWord = false;
                continue;
            }

            // 统计中文字（CJK统一表意文字）
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                chineseCount++;
                inEnglishWord = false;
            }
            // 统计英文单词（字母或撇号开头）
            else if (Character.isLetter(c) || c == '\'') {
                if (!inEnglishWord) {
                    englishWordCount++;
                    inEnglishWord = true;
                }
            } else {
                inEnglishWord = false;
            }
        }
        return chineseCount + englishWordCount;
    }

    private static void addChapter(List<ChapterBean> chapters, int number,
                                   String title, StringBuilder content) {
        ChapterBean chapter = new ChapterBean();
        chapter.setChapterNumber(number);
        chapter.setChapterTitle(title);
        chapter.setContent(content.toString().trim());
        chapters.add(chapter);
    }
}