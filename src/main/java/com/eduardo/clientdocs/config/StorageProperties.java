package com.eduardo.clientdocs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String type;
    private Local local = new Local();
    private S3 s3 = new S3();

    public String getType() {
        return type;
    }

    public Local getLocal() {
        return local;
    }

    public S3 getS3() {
        return s3;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public void setS3(S3 s3) {
        this.s3 = s3;
    }

    public static class Local {

        private String rootFolder;
        private String bucketName;

        public String getRootFolder() {
            return rootFolder;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setRootFolder(String rootFolder) {
            this.rootFolder = rootFolder;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }
    }

    public static class S3 {

        private String bucketName;
        private String region;

        public String getBucketName() {
            return bucketName;
        }

        public String getRegion() {
            return region;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }
}