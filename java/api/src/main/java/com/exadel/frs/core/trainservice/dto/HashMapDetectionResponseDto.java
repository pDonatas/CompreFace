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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.exadel.frs.commonservice.dto.FindFacesResultDto;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(NON_NULL)
public class HashMapDetectionResponseDto extends FaceProcessResponse {
    private List<HashMap<String, List<FindFacesResultDto>>> result;

    @Override
    public HashMapDetectionResponseDto prepareResponse(ProcessImageParams processImageParams) {
        return this;
    }

    public void addResult(final HashMap<String, List<FindFacesResultDto>> resultObject) {
        if (this.result == null) {
            this.result = new ArrayList<>();
        }
        this.result.add(resultObject);
    }

    public List<String> getImagesWithLimitedFaces(final Integer limit) {
        List<String> imagesWithLimitedFaces = new ArrayList<>();

        for (HashMap<String, List<FindFacesResultDto>> resultObject : this.result) {
            for (List<FindFacesResultDto> result : resultObject.values()) {
                if (result == null) {
                    continue;
                }

                if (result.size() == limit) {
                    imagesWithLimitedFaces.add(resultObject.keySet().iterator().next());
                }
            }
        }

        return imagesWithLimitedFaces;
    }

    public List<String> getImagesWithNoFaces() {
        List<String> imagesWithNoFaces = new ArrayList<>();

        for (HashMap<String, List<FindFacesResultDto>> resultObject : this.result) {
            for (List<FindFacesResultDto> result : resultObject.values()) {
                if (result == null) {
                    imagesWithNoFaces.add(resultObject.keySet().iterator().next());
                }
            }
        }

        return imagesWithNoFaces;
    }
}
