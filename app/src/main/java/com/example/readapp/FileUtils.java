package com.example.readapp;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtils {
    private static final String AVATAR_DIR = "avatars";

    // 保存图片到私有目录
    public static String saveAvatarToPrivateStorage(Context context, Uri sourceUri) {
        try {
            // 创建目标目录
            File avatarDir = new File(context.getFilesDir(), AVATAR_DIR);
            if (!avatarDir.exists()) {
                avatarDir.mkdirs();
            }

            // 生成唯一文件名
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "avatar_" + timeStamp + ".jpg";
            File destFile = new File(avatarDir, fileName);

            // 复制文件
            try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
                 FileOutputStream outputStream = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }

            return AVATAR_DIR + File.separator + fileName;
        } catch (Exception e) {
            Log.e("FileUtils", "保存头像失败", e);
            return null;
        }
    }

        public static void copy(InputStream input, OutputStream output) throws IOException {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }
    public static void copyStreamToFile(InputStream input, File outputFile) throws IOException {
        try (OutputStream output = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    public static String saveCoverToPrivateStorage(Context context, Uri uri) throws IOException {
        // 创建唯一文件名
        String fileName = "cover_" + System.currentTimeMillis() + ".jpg";
        File outputFile = new File(context.getFilesDir(), fileName);

        try (InputStream is = context.getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
        return outputFile.getAbsolutePath();
    }

    /**
     * 保存书籍文件到私有目录
     * @param context 上下文
     * @param uri 文件Uri
     * @return 保存后的文件路径
     */
    public static String saveBookToPrivateStorage(Context context, Uri uri) throws IOException {
        // 创建唯一文件名
        String fileName = "book_" + System.currentTimeMillis() + ".txt";
        File outputFile = new File(context.getFilesDir(), fileName);

        try (InputStream is = context.getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
        return outputFile.getAbsolutePath();
    }

    public static String getFilePathFromUri(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        File file = new File(context.getFilesDir(), "temp_" + System.currentTimeMillis());
        try (OutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        }
        return file.getAbsolutePath();
    }
}