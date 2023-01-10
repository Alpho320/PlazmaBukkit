package io.papermc.paper.configuration;

public abstract class ConfigurationPart { // Plazma - package -> public

    public static abstract class Post extends ConfigurationPart {

        public abstract void postProcess();
    }

}
