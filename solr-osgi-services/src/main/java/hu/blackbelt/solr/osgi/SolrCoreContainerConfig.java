package hu.blackbelt.solr.osgi;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name="Solr container manager")
@interface SolrCoreContainerConfig {

    @AttributeDefinition(
            name = "Node name",
            description = "The name of the node"
    )
    String nodeName() default "osgiEmbeddedSolr";

    /* Environmental parameters */
    @AttributeDefinition(
            name = "Solr Home",
            description = "The solr  directory which contains solr base files"
    )
    String solrHome() default "/tmp/solr";

    /*
    @AttributeDefinition(
            name = "Solr Installation Directory",
            description = "The directory where solr is installed"
    )
    String solrInstallDir();

    */

    @AttributeDefinition(
            name = "Run ZooKeeper",
            description = "Causes Solr to run an embedded version of ZooKeeper. Set to the address of ZooKeeper on this node; this allows us to " +
                    "know who you are in the list of addresses in the zkHost connect string. Use -DzkRun (with no value) to get the default value." +
                    "Defaults to localhost:<hostPort+1000>. Same as solr -DzkRun parameter"
    )
    String zkRun() default "";

    @AttributeDefinition(
            name = "ZooKeeper host connect",
            description = "The host address for ZooKeeper. Usually this is a comma-separated list of addresses to each node in your ZooKeeper " +
                    "ensemble. Same as solr -DzkHost parameter."
    )
    String zkHost() default "";


    @AttributeDefinition(
            name = "ZooKeeper client timeout",
            description = "The time a client is allowed to not talk to ZooKeeper before its session expires. Defaults to 15000." +
                    "Same as solr -DzkClientTimeout parameter"
    )
    int zkClientTimeout() default 15000;

    /* Solr.xml parameters */
    @AttributeDefinition(
            name = "Custom AdminHandler",
            description = "If used, this attribute should be set to the FQN (Fully qualified name) of a class that inherits from " +
                    "CoreAdminHandler. For example, <str name=\"adminHandler\">com.myorg.MyAdminHandler</str> would configure the custom admin " +
                    "handler (MyAdminHandler) to handle admin requests. If this attribute isn’t set, Solr uses the default admin handler, " +
                    "org.apache.solr.handler.admin.CoreAdminHandler. For more information on this parameter, " +
                    "see the Solr Wiki at http://wiki.apache.org/solr/CoreAdmin#cores."
    )
    String adminHandler() default "";

    @AttributeDefinition(
            name = "Custom CollectionHandler",
            description = "As above, for custom CollectionsHandler implementations."
    )
    String collectionsHandler() default "";

    @AttributeDefinition(
            name = "Custom InfoHanddler",
            description = "As above, for custom InfoHandler implementations."
    )
    String infoHandler() default "";

    @AttributeDefinition(
            name = "Core load threads",
            description = "Specifies the number of threads that will be assigned to load cores in parallel."
    )
    int coreLoadThreads() default -1;

    @AttributeDefinition(
            name = "Core root directory",
            description = "The root of the core discovery tree, defaults to $SOLR_HOME."
    )
    String coreRootDirectory() default "";

    @AttributeDefinition(
            name = "Shared lib path",
            description = "Specifies the path to a common library directory that will be shared across all cores. " +
                    "Any JAR files in this directory will be added to the search path for Solr plugins. " +
                    "This path is relative to $SOLR_HOME. Custom handlers may be placed in this directory."
    )
    String sharedLib() default "lib";

    @AttributeDefinition(
            name = "Shared schema",
            description = "This attribute, when set to true, ensures that the multiple cores pointing to the same Schema resource " +
                    "file will be referring to the same IndexSchema Object. Sharing the IndexSchema Object makes loading the core faster. " +
                    "If you use this feature, make sure that no core-specific property is used in your Schema file."
    )
    boolean shareSchema() default false;

    @AttributeDefinition(
            name = "Transient cache size",
            description = "Defines how many cores with transient=true that can be loaded before swapping the least recently used core for " +
                    "a new core."
    )
    int transientCacheSize() default -1;

    @AttributeDefinition(
            name = "Config Set Base Dir",
            description = "The directory under which configsets for Solr cores can be found. Defaults to $SOLR_HOME/configsets."
    )
    String configSetBaseDir() default "";


    /*
      Solr Cloud prameters
      This section is ignored unless theSolr instance is started with either -DzkRun or -DzkHost
     */

    @AttributeDefinition(
            name = "SolrCloud - distributed update connection time ",
            description = "Used to set the underlying \"connTimeout\" for intra-cluster updates."
    )
    int solrcloud_distribUpdateConnTimeout() default -1;

    @AttributeDefinition(
            name = "SolrCloud - distributed socket connection time ",
            description = "Used to set the underlying \"socketTimeout\" for intra-cluster updates."
    )
    int solrcloud_distribUpdateSoTimeout() default  -1;

    @AttributeDefinition(
            name = "SolrCloud - host",
            description = "The hostname Solr uses to access cores."
    )
    String solrcloud_host() default "";


    @AttributeDefinition(
            name = "SolrCloud - url context path",
            description = "The url context path."
    )
    String solrcloud_hostContext() default "";

    @AttributeDefinition(
            name = "SolrCloud - host port",
            description = "The port Solr uses to access cores. In the default solr.xml file, this is set to ${jetty.port:8983}, " +
                    "which will use the Solr port defined in Jetty, and otherwise fall back to 8983."
    )
    int solrcloud_hostPort() default -1;

    @AttributeDefinition(
            name = "SolrCloud - leader vote wait",
            description = "When SolrCloud is starting up, how long each Solr node will wait for all known replicas for that shard to " +
                    "be found before assuming that any nodes that haven’t reported are down."
    )
    int solrcloud_leaderVoteWait() default -1;

    @AttributeDefinition(
            name = "SolrCloud - leader conflict resolve wait",
            description = "When trying to elect a leader for a shard, this property sets the maximum time a replica will wait to see " +
                    "conflicting state information to be resolved; temporary conflicts in state information can occur when doing rolling " +
                    "restarts, especially when the node hosting the Overseer is restarted. Typically, the default value of 180000 (ms) is " +
                    "sufficient for conflicts to be resolved; you may need to increase this value if you have hundreds or thousands of " +
                    "small collections in SolrCloud."
    )
    int solrcloud_leaderConflictResolveWait() default  -1;

    @AttributeDefinition(
            name = "SolrCloud - zkClient timeout",
            description = "A timeout for connection to a ZooKeeper server. It is used with SolrCloud."
    )
    int solrcloud_zkClientTimeout() default  -1;


    @AttributeDefinition(
            name = "SolrCloud - zkHost",
            description = "In SolrCloud mode, the URL of the ZooKeeper host that Solr should use for cluster state information."
    )
    String solrcloud_zkHost() default "";

    @AttributeDefinition(
            name = "SolrCloud - generic core node names",
            description = "If TRUE, node names are not based on the address of the node, but on a generic name that identifies the core. " +
                    "When a different machine takes over serving that core things will be much easier to understand."
    )
    boolean solrcloud_genericCoreNodeNames() default true;

    @AttributeDefinition(
            name = "SolrCloud - zkCredentialsProvider",
            description = "Optional parameters that can be specified if you are using ZooKeeper Access Control."
    )
    String solrcloud_zkCredentialsProvider() default "";

    @AttributeDefinition(
            name = "SolrCloud - zkACLProvider",
            description = "Optional parameters that can be specified if you are using ZooKeeper Access Control."
    )
    String solrcloud_zkACLProvider() default "";

    /*
    Custom shard handlers can be defined in solr.xml if you wish to create a custom shard handler.

    <shardHandlerFactory name="ShardHandlerFactory" class="qualified.class.name">
    Since this is a custom shard handler, sub-elements are specific to the implementation. The default and only shard handler provided by
    Solr is the HttpShardHandlerFactory in which case, the following sub-elements can be specified:
    */
    @AttributeDefinition(
            name = "Shard Handler Factory - Name",
            description = "Name of hard handler factory"
    )
    String shardHandleFactoryName() default "shardHandlerFactory";


    @AttributeDefinition(
            name = "Shard Handler Factory - Class",
            description = "Since this is a custom shard handler, sub-elements are specific to the implementation. The default and only shard " +
                    "handler provided by Solr is the HttpShardHandlerFactory"
    )
    String shardHandleFactoryClass() default "HttpShardHandlerFactory";


    @AttributeDefinition(
            name = "Default Shard Handler Factory - Socket timeout",
            description = "The read timeout for intra-cluster query and administrative requests. The default is the same as the distribUpdateSoTimeout specified in the <solrcloud> section."
    )
    int http_shardHandler_socketTimeout() default -1;


    @AttributeDefinition(
            name = "Default Shard Handler Factory - Connection timeout",
            description = "The connection timeout for intra-cluster query and administrative requests. Defaults to the distribUpdateConnTimeout specified in the <solrcloud> section"
    )
    int http_shardHandler_connTimeout() default -1;


    @AttributeDefinition(
            name = "Default Shard Handler Factory - URL Scheme",
            description = "URL scheme to be used in distributed search"
    )
    String http_shardHandler_urlScheme() default "";

    @AttributeDefinition(
            name = "Default Shard Handler Factory - Max connection per host",
            description = "Maximum connections allowed per host. Defaults to 20"
    )
    int http_shardHandler_maxConnectionsPerHost() default 20;

    @AttributeDefinition(
            name = "Default Shard Handler Factory - Max connections",
            description = "Maximum total connections allowed. Defaults to 10000"
    )
    int http_shardHandler_maxConnections() default  10000;

    @AttributeDefinition(
            name = "Default Shard Handler Factory - Core pool size",
            description = "The initial core size of the threadpool servicing requests. Default is 0."
    )
    int http_shardHandler_corePoolSize() default  0;

    @AttributeDefinition(
            name = "Default Shard Handler Factory - Maximum pool size",
            description = "The maximum size of the threadpool servicing requests. Default is unlimited."
    )
    int http_shardHandler_maximumPoolSize() default -1;

    @AttributeDefinition(
            name = "Default Shard Handler Factory - Max thread idle time",
            description = "The amount of time in seconds that idle threads persist for in the queue, before being killed. Default is 5 seconds."
    )
    int http_shardHandler_maxThreadIdleTime() default 5;

    @AttributeDefinition(
            name = "Default Shard Handler Factory - Size of queue",
            description = "If the threadpool uses a backing queue, what is its maximum size to use direct handoff. Default is to use a SynchronousQueue."
    )
    int http_shardHandler_sizeOfQueue() default -1;

    @AttributeDefinition(
            name = "Default Shard Handler Factory - fairness policy",
            description = "A boolean to configure if the threadpool favours fairness over throughput. Default is false to favor throughput."
    )
    boolean http_shardHandler_fairnessPolicy() default false;
}
