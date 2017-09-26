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

package com.flow.platform.api.task;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.node.Yml;
import com.flow.platform.api.domain.envs.FlowEnvs;
import com.flow.platform.api.domain.envs.FlowEnvs.YmlStatusValue;
import com.flow.platform.api.service.GitService;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.util.ExceptionUtil;
import com.flow.platform.util.Logger;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Task to clone from git repo and create or update yml content of node
 * then write status and error message to
 *
 *   - FLOW_YML_STATUS
 *   - FLOW_YML_ERROR_MSG
 *
 * @author yang
 */
public class UpdateNodeYmlTask implements Runnable {

    private final static Logger LOGGER = new Logger(UpdateNodeYmlTask.class);

    private final Flow flow;

    private final NodeService nodeService;

    private final GitService gitService;

    private final Consumer<Yml> callback;

    public UpdateNodeYmlTask(Flow flow,
                             NodeService nodeService,
                             GitService gitService,
                             Consumer<Yml> callback) {
        this.flow = flow;
        this.nodeService = nodeService;
        this.gitService = gitService;
        this.callback = callback;
    }

    @Override
    public void run() {
        String yml;
        try {
            yml = gitService.clone(flow, AppConfig.DEFAULT_YML_FILE, new GitProgressListener());
        } catch (Throwable e) {
            // check yml status is running since exception will be throw if manual stop the git clone thread
            if (YmlStatusValue.isLoadingStatus(flow.getEnv(FlowEnvs.FLOW_YML_STATUS))) {
                Throwable rootCause = ExceptionUtil.findRootCause(e);
                LOGGER.error("Unable to clone from git repo", rootCause);
                nodeService.updateYmlState(flow, YmlStatusValue.ERROR, rootCause.getMessage());
            }
            return;
        }

        try {
            nodeService.createOrUpdate(flow.getPath(), yml);
        } catch (Throwable e) {
            LOGGER.warn("Fail to create or update yml in node");
        }

        LOGGER.trace("Node %s FLOW_YML_STATUS is: %s", flow.getName(), flow.getEnv(FlowEnvs.FLOW_YML_STATUS));

        // call consumer
        if (callback != null) {
            callback.accept(new Yml(flow.getPath(), yml));
        }
    }

    private class GitProgressListener implements GitService.ProgressListener {

        @Override
        public void onStart() {

        }

        @Override
        public void onStartTask(String task) {
            LOGGER.debug("Task start: %s", task);
        }

        @Override
        public void onProgressing(String task, int total, int progress) {
            if (!Objects.equals(flow.getEnv(FlowEnvs.FLOW_YML_STATUS), YmlStatusValue.GIT_LOADING.value())) {
                nodeService.updateYmlState(flow, YmlStatusValue.GIT_LOADING, null);
            }
        }

        @Override
        public void onFinishTask(String task) {
            LOGGER.debug("Task finish: %s", task);
        }

        @Override
        public void onFinish() {
            nodeService.updateYmlState(flow, YmlStatusValue.GIT_LOADED, null);
        }
    }
}