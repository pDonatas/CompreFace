package com.exadel.frs.core.trainservice.service;

import static com.exadel.frs.commonservice.system.global.Constants.DET_PROB_THRESHOLD;
import static com.exadel.frs.core.trainservice.system.global.Constants.SOURCE_IMAGE;
import static com.exadel.frs.core.trainservice.system.global.Constants.TARGET_IMAGE;
import com.exadel.frs.core.trainservice.dto.HashMapDetectionResponseDto;
import com.exadel.frs.core.trainservice.dto.HashMapVerifyResponseDto;
import com.exadel.frs.core.trainservice.dto.ProcessImageParams;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZipService {

    private final EmbeddingsProcessService verificationService;
    private final FaceDetectionProcessServiceImpl detectionService;

    public void unzip(final MultipartFile zipFile, String destDir) {
        File dir = new File(destDir);
        if(!dir.exists()) {
            dir.mkdirs();
        }
        FileInputStream fis;
        byte[] buffer = new byte[1024];
        try {
            fis = (FileInputStream) zipFile.getInputStream();
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            while(ze != null){
                String fileName = ze.getName();
                File newFile = new File(destDir + File.separator + fileName);
                System.out.println("Unzipping to "+newFile.getAbsolutePath());
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashMapVerifyResponseDto[] handleFiles(final String path, final String apiKey) {
        List<MultipartFile> listOfFiles = new ArrayList<>();
        try {
            Files.find(
                         Paths.get(path),
                         Integer.MAX_VALUE,
                         (filePath, fileAttr) -> fileAttr.isRegularFile()
                 )
                 .forEach(file -> {
                     listOfFiles.add(convertFileToMultiPart(file));
                 });
        } catch (IOException e) {
            log.error("Error while reading files from directory: {}", e.getMessage());
        }

        var files = new HashMapVerifyResponseDto[listOfFiles.size()];
        int i = 0;
        for (MultipartFile file : listOfFiles) {
            files[i++] = verify(file, listOfFiles, apiKey);
        }

        return files;
    }

    public List<MultipartFile> getFiles(final String path) throws IOException {
        List<MultipartFile> listOfFiles = new ArrayList<>();
        Files.find(
                     Paths.get(path),
                     Integer.MAX_VALUE,
                     (filePath, fileAttr) -> fileAttr.isRegularFile()
             )
             .forEach(file -> {
                 listOfFiles.add(convertFileToMultiPart(file));
             });

        return listOfFiles;
    }

    public void writeToFile(final List<String> files, final String extracted, final String file) {
        try {
            var path = Paths.get(extracted + "/" + file);
            Files.write(path, files, StandardCharsets.UTF_8, Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
        } catch (IOException e) {
            System.out.println("Error while writing to file: " + e.getMessage());
        }
    }

    public List<String> readFromFile(final String extracted, final String file) {
        try {
            var path = Paths.get(extracted + "/" + file);
            return Files.readAllLines(path);
        } catch (IOException e) {
            log.error("Error while reading from file: {}", e.getMessage());
        }

        return null;
    }

    public MultipartFile convertFileToMultiPart(final Path path) {
        try {
            File file = path.toFile();
            FileInputStream input = new FileInputStream(file);

            return new MockMultipartFile("file", file.getName(), "text/plain",
                    IOUtils.toByteArray(input));
        } catch (IOException e) {
            System.out.println("Exception => " + e.getLocalizedMessage());
        }

        return null;
    }

    public List<List<String>> detect(List<MultipartFile> listOfFiles, Integer threshold) {
        var processableImages = new ProcessImageParams[listOfFiles.size()];
        var i = 0;
        for (var file : listOfFiles) {
            var processImageParams = ProcessImageParams
                    .builder()
                    .file(file)
                    .build();

            processableImages[i++] = processImageParams;
        }

        return detectionService.getImagesWithLimitedAndNoFaces(processableImages, threshold);
    }

    public HashMapVerifyResponseDto verify(MultipartFile file, List<MultipartFile> listOfFiles, String apiKey) {
        var processableImages = new ProcessImageParams[listOfFiles.size()];
        var i = 0;
        for (var sourceFile : listOfFiles) {
            Map<String, Object> fileMap = Map.of(
                    SOURCE_IMAGE, sourceFile,
                    TARGET_IMAGE, file
            );

            ProcessImageParams processImageParams = ProcessImageParams
                    .builder()
                    .apiKey(apiKey)
                    .file(fileMap)
                    .build();

            processableImages[i++] = processImageParams;
        }

        return (HashMapVerifyResponseDto) verificationService.processImages(processableImages);
    }

    public void handleZipFile(final MultipartFile zipFile, final String apiKey) {
        try {
            var unzipThread = new Thread(() -> {
                try {
                    unzip(zipFile, "extracted");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            unzipThread.start();
            unzipThread.join();

            if (!unzipThread.isAlive()) {
                var files = getFiles("/extracted");
                int maxFilesPerBatch = 300;
                int batchCount = (int) Math.ceil((double) files.size() / maxFilesPerBatch);
                var threads = new Thread[batchCount];

                // Step 1: Get all photos with only one face
                for (int j = 0; j < batchCount; j++) {
                    final int threadId = j;
                    threads[j] = new Thread(() -> {
                        System.out.println("Processing batch " + (threadId + 1) + " of " + batchCount);
                        int start = threadId * maxFilesPerBatch;
                        int end = Math.min(start + maxFilesPerBatch, files.size());
                        List<MultipartFile> batch = files.subList(start, end);
                        List<List<String>> faces = detect(batch, 1);
                        List<String> oneFace = faces.get(0);
                        List<String> noFaces = faces.get(1);
                        createFiles(oneFace, "/home/one_face");
                        createFiles(noFaces, "/home/no_face");
                        System.out.println("Batch " + (threadId + 1) + " of " + batchCount + " processed");
                    });
                }

                for (int j = 0; j < batchCount; j++) {
                    threads[j].start();
                    threads[j].join();
                }

                System.out.println("Analyzing faces...");
                List<MultipartFile> oneFace = getFiles("/home/one_face");
                List<MultipartFile> noFace = getFiles("/home/no_face");
                System.out.println("Found " + oneFace.size() + " faces");
                System.out.println("Found " + noFace.size() + " files without faces");

                List<MultipartFile> filteredFiles = new ArrayList<>();
                for (var file : files) {
                    if (!noFace.contains(file)) {
                        filteredFiles.add(file);
                    }
                }

                System.out.println("Found " + filteredFiles.size() + " files with faces");
                int filteredBatchCount = (int) Math.ceil((double) filteredFiles.size() / maxFilesPerBatch);

                int threadLimit = 6;
                ExecutorService executor = Executors.newFixedThreadPool(threadLimit);
                List<Future<Double>> futures = new ArrayList<>();

                for (int i = 0; i < oneFace.size(); i++) {
                    var file = oneFace.get(i);
                    executor.execute(() -> {
                        System.out.println("Starting thread");
                        for (int j = 0; j < filteredBatchCount; j++) {
                            int start = j * maxFilesPerBatch;
                            int end = Math.min(start + maxFilesPerBatch, filteredFiles.size());
                            List<MultipartFile> batch = filteredFiles.subList(start, end);
                            HashMapVerifyResponseDto response = verify(file, batch, apiKey);
                            futures.add(
                                    CompletableFuture.supplyAsync(() -> response.handleFileResponse(file, "/home/verified"), executor)
                            );

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                System.out.println("Thread interrupted");
                            }
                        }
                    });
                }

                try {
                    executor.shutdown();
                    executor.awaitTermination(10, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    System.out.println("InterruptedException occurred while waiting for threads to finish: " + e.getMessage());
                    e.printStackTrace();
                }
                System.out.println("All threads have finished executing.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createFiles(final List<String> files, final String path) {
        final Path pathToDirectory = Paths.get(path);
        if (!Files.exists(pathToDirectory)) {
            try {
                Files.createDirectories(pathToDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (String file : files) {
            try {
                Files.createFile(Paths.get(path + "/" + file));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
