/*
 * Copyright (c) 2020 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.exadel.frs.core.trainservice.dto;

import com.exadel.frs.commonservice.dto.FindFacesResultDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.File;
import java.io.IOException;
import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.multipart.MultipartFile;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.aspectj.util.FileUtil.copyFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(NON_NULL)
public class HashMapVerifyResponseDto extends FaceProcessResponse {
    private List<HashMap<String, MultipleFindFacesResponse>> result;

    @Override
    public HashMapVerifyResponseDto prepareResponse(ProcessImageParams processImageParams) {
        return this;
    }

    public void addResult(final HashMap<String, MultipleFindFacesResponse> resultObject) {
        if (this.result == null) {
            this.result = new ArrayList<>();
        }
        this.result.add(resultObject);
    }

    public void handleFileResponse(final MultipartFile file, final String folderPath) {
        if (this.result == null) {
            this.result = new ArrayList<>();
        }
        String folderName = folderPath + "/" + file.getOriginalFilename();
        File folder = new File(folderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        for (HashMap<String, MultipleFindFacesResponse> resultObject : this.result) {
            for (String key : resultObject.keySet()) {
                MultipleFindFacesResponse response = resultObject.get(key);
                if (response == null) {
                    continue;
                }

                File targetFile = new File(response.getFileName());

                if (targetFile.exists()) {
                    try {
                        copyFile(targetFile, new File(folderName + "/" + targetFile.getName()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    System.out.println("File " + targetFile.getName() + " copied to " + folderName);
                }
            }
        }
    }
}
