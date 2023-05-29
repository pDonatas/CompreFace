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
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(NON_NULL)
public class MultipleFindFacesResponse extends FaceProcessResponse {

    @JsonProperty(value = "plugins_versions")
    private PluginsVersionsDto pluginsVersions;
    private List<FaceMatch> result;
    private String fileName;

    @Override
    public MultipleFindFacesResponse prepareResponse(ProcessImageParams processImageParams) {
        if (this.getFileName() == null) {
            this.setFileName(processImageParams.getFile().toString());
        }

        if (this.getResult()==null || this.getResult().isEmpty()){
            return this;
        }

        String facePlugins = processImageParams.getFacePlugins();
        if (isEmpty(facePlugins) || !facePlugins.contains(CALCULATOR)) {
            this.getResult().forEach(r -> r.setEmbedding(null));
        }

        if (Boolean.FALSE.equals(processImageParams.getStatus())) {
            this.setPluginsVersions(null);
            this.getResult().forEach(r -> r.setExecutionTime(null));
        }

        return this;
    }

    public static HashMapVerifyResponseDto  buildResponse(final MultipleFindFacesResponse[] responses) {
        HashMapVerifyResponseDto result = new HashMapVerifyResponseDto();

        for (var response : responses) {
            var resultObject = new HashMap<String, List<MultipleFindFacesResponse>>();
            resultObject.put(response.getFileName(), List.of(response));
            result.addResult(resultObject);
        }

        return result;
    }
}
