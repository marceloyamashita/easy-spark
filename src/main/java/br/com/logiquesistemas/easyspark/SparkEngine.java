package br.com.logiquesistemas.easyspark;

import br.com.logiquesistemas.easyspark.annotations.Controller;
import br.com.logiquesistemas.easyspark.annotations.Delete;
import br.com.logiquesistemas.easyspark.annotations.Post;
import br.com.logiquesistemas.easyspark.annotations.Put;
import br.com.logiquesistemas.easyspark.core.DefaultPathResolver;
import br.com.logiquesistemas.easyspark.core.InvocationHandle;
import br.com.logiquesistemas.easyspark.core.PathResolver;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.*;
import spark.template.freemarker.FreeMarkerEngine;

import java.lang.reflect.Method;
import java.util.Set;

import static spark.Spark.staticFileLocation;

/**
 * Manage Spark framework.
 * <p>
 * Created by gustavo on 26/04/2016.
 */
public class SparkEngine {

    private static Logger logger = LoggerFactory.getLogger(SparkEngine.class);
    public String staticFileLocation;
    public String templateLocation;
    private String basePackage;
    private int port;
    private String defaultEncoding;

    public SparkEngine(int port, String defaultEncoding, String staticFileLocation, String templateLocation, String basePackage) {
        this.port = port;
        this.defaultEncoding = defaultEncoding;
        this.staticFileLocation = staticFileLocation;
        this.templateLocation = templateLocation;
        this.basePackage = basePackage;
    }

    /**
     * Spark framework initialization.
     */
    public void setUp() {
        FreeMarkerEngine freeMarkerEngine = new FreeMarkerEngine();
        Configuration freeMarkerConfiguration = new Configuration(Configuration.getVersion());
        freeMarkerConfiguration.setOutputEncoding(defaultEncoding);
        freeMarkerConfiguration.setDefaultEncoding(defaultEncoding);
        freeMarkerConfiguration.setTemplateLoader(new ClassTemplateLoader(SparkEngine.class, templateLocation));
        freeMarkerEngine.setConfiguration(freeMarkerConfiguration);
        staticFileLocation(staticFileLocation);
        Spark.port(port);
        addRoutes(freeMarkerEngine);
        Spark.init();
        Spark.awaitInitialization();
    }

    private void addRoutes(TemplateEngine freeMarkerEngine) {
        Reflections reflections = new Reflections(basePackage);
        Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(Controller.class);
        for (Class<?> controller : controllers) {
            registerController(controller, freeMarkerEngine);
        }
    }

    private void registerController(Class<?> controller, TemplateEngine freeMarkerEngine) {
        Method[] methods = controller.getDeclaredMethods();
        for (Method method : methods) {
            registerSparkRoute(controller, method, freeMarkerEngine);
        }
    }

    private void registerSparkRoute(Class<?> controller, Method method, TemplateEngine freeMarkerEngine) {
        PathResolver defaultPathResolver = new DefaultPathResolver();
        String path = defaultPathResolver.resolvePath(controller, method);

        boolean isModelAndView = method.getReturnType().isAssignableFrom(ModelAndView.class);

        Route route = (request, response) -> new InvocationHandle(controller, method).execute(request, response);
        TemplateViewRoute templateViewRoute = (request, response) -> new InvocationHandle(controller, method).executeModelAndView(request, response);

        if (method.isAnnotationPresent(Post.class)) {
            logger.info("Registering a POST path {} --> {}::{}", path, controller.getSimpleName(), method.getName());
            if (isModelAndView) {
                Spark.post(path, templateViewRoute, freeMarkerEngine);
            } else {
                Spark.post(path, route);
            }
        } else if (method.isAnnotationPresent(Delete.class)) {
            logger.info("Registering a DELETE path {} --> {}::{}", path, controller.getSimpleName(), method.getName());
            if (isModelAndView) {
                Spark.delete(path, templateViewRoute, freeMarkerEngine);
            } else {
                Spark.delete(path, route);
            }
        } else if (method.isAnnotationPresent(Put.class)) {
            logger.info("Registering a PUT path {} --> {}::{}", path, controller.getSimpleName(), method.getName());
            if (isModelAndView) {
                Spark.put(path, templateViewRoute, freeMarkerEngine);
            } else {
                Spark.put(path, route);
            }
        } else {
            logger.info("Registering a GET path {} --> {}::{}", path, controller.getSimpleName(), method.getName());
            if (isModelAndView) {
                Spark.get(path, templateViewRoute, freeMarkerEngine);
            } else {
                Spark.get(path, route);
            }
        }
    }


    public static class Builder {

        public String staticFileLocation = "/public";
        public String templateLocation = "/public/templates";
        public String basePackage = "br.com";
        private int port = 4567;
        private String defaultEncoding = "UTF-8";

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        /**
         * Define the default encoding to FreeMarker. Default: UTF-8
         *
         * @param defaultEncoding encoding.
         * @return Spark engine builder
         */
        public Builder withDefaultEncoding(String defaultEncoding) {
            this.defaultEncoding = defaultEncoding;
            return this;
        }

        /**
         * Define the path to static file location (css,images,js, etc). Default: /public/
         *
         * @param staticFileLocation path to static file location.
         * @return Spark engine builder
         */
        public Builder withStaticFileLocation(String staticFileLocation) {
            this.staticFileLocation = staticFileLocation;
            return this;
        }

        /**
         * Define the base path to find freemarker templates. Default: /public/templates
         *
         * @param templateLocation path to templates
         * @return Spark engine builder
         */
        public Builder withTemplateLocation(String templateLocation) {
            this.templateLocation = templateLocation;
            return this;
        }

        /**
         * Define the base pakage to process annotated classes, ex: "org.hibernate". Default: br.com
         *
         * @param basePackage base package
         * @return Spark engine builder
         */
        public Builder withBasePackage(String basePackage) {
            this.basePackage = basePackage;
            return this;
        }

        public SparkEngine build() {
            return new SparkEngine(port, defaultEncoding, staticFileLocation, templateLocation, basePackage);
        }
    }

}
