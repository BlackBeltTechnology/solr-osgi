package hu.blackbelt.solr.osgi;

/*-
 * #%L
 * Solr OSGi services
 * %%
 * Copyright (C) 2018 - 2023 BlackBelt Technology
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.common.base.Strings;

public final class SolrXmlGenerator {

    public static String getSolrXml(SolrCoreContainerConfig config) {
        return "<solr>\n"
                        + getAdminHandler(config)
                        + getCollectionHandler(config)
                        + getInfoHandler(config)
                        + getCoreLoadThreads(config)
                        + getCoreRootDirectory(config)
                        + getSharedLib(config)
                        + getShareSchema(config)
                        + getTransientCacheSize(config)
                        + getConfigSetBaseDir(config)
                        + getSolrCloudConfig(config)
                        + getShardHandlerFactory(config)
                        + "</solr>\n";
    }

    private static String getAdminHandler(SolrCoreContainerConfig config) {
        if (!Strings.isNullOrEmpty(config.adminHandler())) {
             return String.format("  <str name=\"adminHandler\">%s</str>\n", config.adminHandler());
        } else {
            return "";
        }
    }

    private static String getCollectionHandler(SolrCoreContainerConfig config) {
        if (!Strings.isNullOrEmpty(config.collectionsHandler())) {
            return String.format("  <str name=\"collectionHandler\">%s</str>\n", config.collectionsHandler());
        } else {
            return "";
        }
    }

    private static String getInfoHandler(SolrCoreContainerConfig config) {
        if (!Strings.isNullOrEmpty(config.infoHandler())) {
            return String.format("  <str name=\"collectionHandler\">%s</str>\n", config.infoHandler());
        } else {
            return "";
        }
    }

    private static String getCoreLoadThreads(SolrCoreContainerConfig config) {
        if (config.coreLoadThreads() != -1) {
            return String.format("  <int name=\"coreLoadThreads\">%d</int>\n", config.coreLoadThreads());
        } else {
            return "";
        }
    }

    private static String getCoreRootDirectory(SolrCoreContainerConfig config) {
        if (!Strings.isNullOrEmpty(config.coreRootDirectory())) {
            return String.format("  <str name=\"coreRootDirectory\">%s</str>\n", config.coreRootDirectory());
        } else {
            return "";
        }
    }

    private static String getSharedLib(SolrCoreContainerConfig config) {
        if (!Strings.isNullOrEmpty(config.coreRootDirectory())) {
            return String.format("  <str name=\"sharedLib\">%s</str>\n", config.sharedLib());
        } else {
            return "";
        }
    }

    private static String getShareSchema(SolrCoreContainerConfig config) {
        if (config.shareSchema()) {
            return String.format("  <bool name=\"sharedLib\">%b</bool>\n", config.shareSchema());
        } else {
            return "";
        }
    }

    private static String getTransientCacheSize(SolrCoreContainerConfig config) {
        if (config.transientCacheSize() != -1) {
            return String.format("  <int name=\"transientCacheSize\">%d</int>\n", config.transientCacheSize());
        } else {
            return "";
        }
    }

    private static String getConfigSetBaseDir(SolrCoreContainerConfig config) {
        if (!Strings.isNullOrEmpty(config.configSetBaseDir())) {
            return String.format("  <str name=\"configSetBaseDir\">%s</str>\n", config.configSetBaseDir());
        } else {
            return "";
        }
    }


    private static String getSolrCloudConfig(SolrCoreContainerConfig config) {
        if (!Strings.isNullOrEmpty(config.zkRun()) || !Strings.isNullOrEmpty(config.zkHost())) {
            return  "  <solrcloud>\n"
                    + getSolrCloudDistribUpdateConnTimeout(config)
                    + getSolrCloudDistribUpdateSoTimeout(config)
                    + getSolrCloudHost(config)
                    + getSolrCloudHostContext(config)
                    + getSolrCloudHostPort(config)
                    + getSolrCloudLeaderVoteWait(config)
                    + getSolrCloudLeaderConflictResolveWait(config)
                    + getSolrCloudZkClientTimeout(config)
                    + getSolrCloudZkHost(config)
                    + getSolrCloudGenericCoreNodeNames(config)
                    + getSolrCloudZkCredentialsProvider(config)
                    + getSolrCloudZkACLProvider(config)
                    + getShardHandleFactoryClass(config)
                    + getShardHandlerFactory(config)
                    + "  </solrcloud>\n";
        } else {
            return "";
        }
    }


    private static String getSolrCloudDistribUpdateConnTimeout(SolrCoreContainerConfig config) {
        if (config.solrcloud_distribUpdateConnTimeout() != -1) {
            return String.format("    <int name=\"distribUpdateConnTimeout\">%d</int>\n", config.solrcloud_distribUpdateConnTimeout());
        } else {
            return "";
        }
    }

    private static String getSolrCloudDistribUpdateSoTimeout(SolrCoreContainerConfig config) {
        if (config.solrcloud_distribUpdateSoTimeout() != -1) {
            return String.format("    <int name=\"distribUpdateSoTimeout\">%d</int>\n", config.solrcloud_distribUpdateSoTimeout());
        } else {
            return "";
        }
    }

    private static String getSolrCloudHost(SolrCoreContainerConfig config) {
        if (!Strings.isNullOrEmpty(config.solrcloud_host())) {
            return String.format("    <str name=\"host\">%s</str>\n", config.solrcloud_host());
        } else {
            return "";
        }
    }

    private static String getSolrCloudHostContext(SolrCoreContainerConfig config) {
        if (!Strings.isNullOrEmpty(config.solrcloud_hostContext())) {
            return String.format("    <str name=\"hostContext\">%s</str>\n", config.solrcloud_hostContext());
        } else {
            return "";
        }
    }

    private static String getSolrCloudHostPort(SolrCoreContainerConfig config) {
        if (config.solrcloud_hostPort() != -1) {
            return String.format("    <int name=\"hostPort\">%d</int>\n", config.solrcloud_hostPort());
        } else {
            return "";
        }
    }


    private static String getSolrCloudLeaderVoteWait(SolrCoreContainerConfig config) {
        if (config.solrcloud_leaderVoteWait() != -1) {
            return String.format("    <int name=\"leaderVoteWait\">%d</int>\n", config.solrcloud_leaderVoteWait());
        } else {
            return "";
        }
    }

    private static String getSolrCloudLeaderConflictResolveWait(SolrCoreContainerConfig config) {
        if (config.solrcloud_leaderConflictResolveWait() != -1) {
            return String.format("    <int name=\"leaderConflictResolveWait\">%d</int>\n", config.solrcloud_leaderConflictResolveWait());
        } else {
            return "";
        }
    }

    private static String getSolrCloudZkClientTimeout(SolrCoreContainerConfig config) {
        if (config.solrcloud_zkClientTimeout() != -1) {
            return String.format("    <int name=\"zkClientTimeout\">%d</int>\n", config.solrcloud_zkClientTimeout());
        } else {
            return "";
        }
    }

    private static String getSolrCloudZkHost(SolrCoreContainerConfig config) {
        if (!Strings.isNullOrEmpty(config.solrcloud_zkHost())) {
            return String.format("    <str name=\"zkHost\">%s</str>\n", config.solrcloud_zkHost());
        } else {
            return "";
        }
    }

    private static String getSolrCloudGenericCoreNodeNames(SolrCoreContainerConfig config) {
        if (config.solrcloud_genericCoreNodeNames() != true) {
            return String.format("    <bool name=\"genericCoreNodeNames\">%b</bool>\n", config.solrcloud_genericCoreNodeNames());
        } else {
            return "";
        }
    }

    private static String getSolrCloudZkCredentialsProvider(SolrCoreContainerConfig config) {
        if (!Strings.isNullOrEmpty(config.solrcloud_zkCredentialsProvider())) {
            return String.format("    <str name=\"zkCredentialsProvider\">%s</str>\n", config.solrcloud_zkCredentialsProvider());
        } else {
            return "";
        }
    }

    private static String getSolrCloudZkACLProvider(SolrCoreContainerConfig config) {
        if (!Strings.isNullOrEmpty(config.solrcloud_zkACLProvider())) {
            return String.format("    <str name=\"zkACLProvider\">%s</str>\n", config.solrcloud_zkACLProvider());
        } else {
            return "";
        }
    }

    private static String getShardHandleFactoryClass(SolrCoreContainerConfig config) {
        if (!Strings.isNullOrEmpty(config.shardHandleFactoryClass())) {
            return String.format("  <str name=\"shardHandleFactoryClass\">%s</str>\n", config.shardHandleFactoryClass());
        } else {
            return "";
        }
    }

    private static String getShardHandlerFactory(SolrCoreContainerConfig config) {
        if (config.shardHandleFactoryClass().equals("HttpShardHandlerFactory")) {
            return String.format("<shardHandlerFactory name=\"%s\"\n"
                    + "    class=\"%s\">"
                    + getHttpShardHandlerSocketTimeout(config)
                    + getHttpShardHandlerConnTimeout(config)
                    + getHttpShardHandlerUrlScheme(config)
                    + getHttpShardHandlerMaxConnectionsPerHost(config)
                    + getHttpShardHandlerMaxConnections(config)
                    + getHttpShardHandlerCorePoolSize(config)
                    + getHttpShardHandlerMaxThreadIdleTime(config)
                    + getHttpShardHandlerSizeOfQueue(config)
                    + getHttpShardHandlerFairnessPolicy(config)
                    + "</shardHandlerFactory>\n", config.shardHandleFactoryName(), config.shardHandleFactoryClass());
        } else {
            return "";
        }
    }

    private static String getHttpShardHandlerSocketTimeout(SolrCoreContainerConfig config) {
        if (config.http_shardHandler_socketTimeout() != -1) {
            return String.format("    <int name=\"socketTimeout\">%d</int>\n", config.http_shardHandler_socketTimeout());
        } else {
            return "";
        }
    }

    private static String getHttpShardHandlerConnTimeout(SolrCoreContainerConfig config) {
        if (config.http_shardHandler_connTimeout() != -1) {
            return String.format("    <int name=\"connTimeout\">%d</int>\n", config.http_shardHandler_connTimeout());
        } else {
            return "";
        }
    }

    private static String getHttpShardHandlerUrlScheme(SolrCoreContainerConfig config) {
        if (!Strings.isNullOrEmpty(config.http_shardHandler_urlScheme())) {
            return String.format("  <str name=\"urlScheme\">%s</str>\n", config.http_shardHandler_urlScheme());
        } else {
            return "";
        }
    }

    private static String getHttpShardHandlerMaxConnectionsPerHost(SolrCoreContainerConfig config) {
        if (config.http_shardHandler_maxConnectionsPerHost() != -1) {
            return String.format("    <int name=\"maxConnectionsPerHost\">%d</int>\n", config.http_shardHandler_maxConnectionsPerHost());
        } else {
            return "";
        }
    }

    private static String getHttpShardHandlerMaxConnections(SolrCoreContainerConfig config) {
        if (config.http_shardHandler_maxConnections() != -1) {
            return String.format("    <int name=\"maxConnections\">%d</int>\n", config.http_shardHandler_maxConnections());
        } else {
            return "";
        }
    }

    private static String getHttpShardHandlerCorePoolSize(SolrCoreContainerConfig config) {
        if (config.http_shardHandler_corePoolSize() != -1) {
            return String.format("    <int name=\"corePoolSize\">%d</int>\n", config.http_shardHandler_corePoolSize());
        } else {
            return "";
        }
    }

    private static String getHttpShardHandlerMaxThreadIdleTime(SolrCoreContainerConfig config) {
        if (config.http_shardHandler_maxThreadIdleTime() != -1) {
            return String.format("    <int name=\"maxThreadIdleTime\">%d</int>\n", config.http_shardHandler_maxThreadIdleTime());
        } else {
            return "";
        }
    }

    private static String getHttpShardHandlerSizeOfQueue(SolrCoreContainerConfig config) {
        if (config.http_shardHandler_sizeOfQueue() != -1) {
            return String.format("    <int name=\"sizeOfQueue\">%d</int>\n", config.http_shardHandler_sizeOfQueue());
        } else {
            return "";
        }
    }

    private static String getHttpShardHandlerFairnessPolicy(SolrCoreContainerConfig config) {
        if (config.http_shardHandler_fairnessPolicy()) {
            return String.format("    <bool name=\"fairnessPolicy\">%b</bool>\n", config.http_shardHandler_fairnessPolicy());
        } else {
            return "";
        }
    }

}
