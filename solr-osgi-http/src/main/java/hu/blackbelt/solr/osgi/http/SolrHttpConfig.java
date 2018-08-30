package hu.blackbelt.solr.osgi.http;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name="Solr HTTP Configration")
@interface SolrHttpConfig {

    @AttributeDefinition(
            name = "Context root",
            description = "The solr context root"
    )
    String contextRoot() default "/solr";

    @AttributeDefinition(
            name = "Exclude patterns",
            description = "Excluded path from dispatch filter"
    )
    String excludePatterns() default "/partials/.+,/solr.libs/.+,/solr.css/.+,/solr.js/.+,/img/.+,/tpl/.+";

}
