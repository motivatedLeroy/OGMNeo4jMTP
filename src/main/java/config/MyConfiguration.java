package config;

/**
 * Created by Felix Hambrecht on 05.07.2016.
 */
import org.neo4j.ogm.authentication.Credentials;
import org.neo4j.ogm.authentication.UsernamePasswordCredentials;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.annotation.*;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
class MyConfiguration extends Neo4jConfiguration {

    //Ging gestern nicht, da falsche Version!!!
    Credentials t = new UsernamePasswordCredentials("as","as");

    @Bean
    public SessionFactory getSessionFactory(){
        return new SessionFactory(getConfiguration(), "entities");
    }
    @Bean
    private org.neo4j.ogm.config.Configuration getConfiguration() {
        org.neo4j.ogm.config.Configuration config = new org.neo4j.ogm.config.Configuration();
        config
                .driverConfiguration()
                .setDriverClassName("org.neo4j.ogm.drivers.http.driver.HttpDriver")
                .setURI("http://neo4j:decard66@localhost:7474");
        return config;
    }




}
