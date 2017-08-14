/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.api.dao;

import com.flow.platform.api.domain.Flow;
import com.flow.platform.exception.IllegalStatusException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.stereotype.Repository;

/**
 * @author yh@firim
 */
@Repository(value = "flowDao")
public class FlowDaoImpl extends AbstractBaseDao<String, Flow> implements FlowDao {

    @Override
    Class<Flow> getEntityClass() {
        return Flow.class;
    }

    @Override
    String getKeyName() {
        return "path";
    }

    @Override
    public Path workspace(Path base, Flow flow) {
        Path flowWorkingPath = Paths.get(base.toString(), flow.getName());
        try {
            return Files.createDirectories(flowWorkingPath);
        } catch (FileAlreadyExistsException e) {
            return flowWorkingPath;
        } catch (Throwable e) {
            throw new IllegalStatusException("Unable to get flow working path");
        }
    }
}