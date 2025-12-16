package ru.itmo.zavar.brainstop.common.api.infra;

import java.io.InputStream;

public interface VideoObjectStorage {

    void uploadProcessed(String key, InputStream is, long size) throws Exception;

    InputStream openProcessed(String key, long offset, long length) throws Exception;

    InputStream downloadProcessed(String key) throws Exception;

    String uploadThumbnail(String key, InputStream is, long size) throws Exception;

    InputStream openThumbnail(String key) throws Exception;

    void deleteProcessed(String key) throws Exception;

    String uploadAvatar(InputStream is, long size, String contentType, Long userId) throws Exception;

    InputStream downloadAvatar(String objectName) throws Exception;
}
