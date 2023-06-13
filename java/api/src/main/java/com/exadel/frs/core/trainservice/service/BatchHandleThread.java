package com.exadel.frs.core.trainservice.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public class BatchHandleThread implements Runnable {

    private final ZipService zipService;
    private final List<MultipartFile> batch;
    private final MultipartFile file;
    private final String apiKey;

    public BatchHandleThread(final ZipService zipService, final List<MultipartFile> batch, final MultipartFile file, final String apiKey) {
        this.zipService = zipService;
        this.batch = batch;
        this.file = file;
        this.apiKey = apiKey;
    }

    @Override
    public void run() {
        try {
            System.out.println("Thread " + Thread.currentThread().getId() + " is running");
            zipService.verify(this.file, this.batch, this.apiKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
