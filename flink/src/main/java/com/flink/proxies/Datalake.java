package com.flink.proxies;

import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.file.datalake.*;
import com.flink.app.Image;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Base64;

public class Datalake {
    DataLakeServiceClient dataLakeServiceClient;
    String containerName;

    public DataLakeServiceClient getDataLakeServiceClient() {
        return dataLakeServiceClient;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public void setDataLakeServiceClient(DataLakeServiceClient dataLakeServiceClient) {
        this.dataLakeServiceClient = dataLakeServiceClient;
    }

    public Datalake(String accountKey, String accountName, String containerName) {
        StorageSharedKeyCredential sharedKeyCredential =
                new StorageSharedKeyCredential(accountName, accountKey);

        DataLakeServiceClientBuilder builder = new DataLakeServiceClientBuilder();

        builder.credential(sharedKeyCredential);
        builder.endpoint("https://" + accountName + ".blob.core.windows.net");

        this.dataLakeServiceClient = builder.buildClient();
        this.containerName = containerName;
    }

    public Datalake() {}

    public DataLakeDirectoryClient CreateDirectory
            (String directoryName){

        DataLakeFileSystemClient fileSystemClient =
                dataLakeServiceClient.getFileSystemClient(containerName);

        return fileSystemClient.createDirectory(directoryName, true);
    }

    public DataLakeDirectoryClient CreateSubDirectory
            (DataLakeDirectoryClient directoryClient, String subDirectoryName){

        return directoryClient.createSubdirectory(subDirectoryName, true);
    }

    public String UploadFile(DataLakeDirectoryClient directoryClient, Image image)
            throws FileNotFoundException {
        DataLakeFileClient fileClient = directoryClient.createFile(image.getName());

        byte[] data = Base64.getDecoder().decode(image.getBuffer());
        InputStream is = new ByteArrayInputStream(data);

        long fileSize = data.length;

        fileClient.append(is, 0, fileSize);
        String url = fileClient.getFileUrl();

        fileClient.flush(fileSize);
        return fileClient.getFileUrl().replaceAll(".dfs.", ".blob.");
    }

}
