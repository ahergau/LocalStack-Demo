package com.gaurav.localstack.demo.lambda;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.File;
import java.util.Map;
import java.util.UUID;

public class SQSEventHandler implements RequestHandler<SQSEvent, Void> {

    @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {
        Map<String, String> environment = System.getenv();
        try {
            for (SQSEvent.SQSMessage message : sqsEvent.getRecords()) {
                String msgBody = message.getBody();
                context.getLogger().log("Lambda SQSEventHandler invoked . . . ");
                context.getLogger().log("Received message : " + msgBody);
                saveToS3(environment, context, msgBody);
            }
        } catch (Exception ex) {
            context.getLogger().log("Exception : " + ex);
            throw ex;
        }
        return null;
    }

    private void saveToS3(Map<String, String> environment, Context context, String message){
        AmazonS3 amazonS3 = getAmazonS3(environment, context);
        amazonS3.putObject("sqsbucket", UUID.randomUUID().toString(), message);
    }

    private AmazonS3 getAmazonS3(Map<String, String> environment, Context context) {
        String profile = environment.get("PROFILE");
        context.getLogger().log("Profile is: " + profile);
        if ("localstack".equals(profile)) {
            BasicAWSCredentials creds = new BasicAWSCredentials("123", "xyz");
            AmazonS3ClientBuilder standard = AmazonS3ClientBuilder.standard();
            String s3Endpoint = environment.get("LOCALSTACK_HOSTNAME");

            System.out.println("LOCALSTACK_HOSTNAME " +s3Endpoint);

            AwsClientBuilder.EndpointConfiguration configuration = new AwsClientBuilder.EndpointConfiguration("http://" + s3Endpoint + ":4566",
                    "us-east-1");
            standard.withEndpointConfiguration(configuration);
            standard.withPathStyleAccessEnabled(true);
            standard.withCredentials(new AWSStaticCredentialsProvider(creds));
            return standard.build();
        }
        context.getLogger().log(" Using AWS Environment");
        return AmazonS3ClientBuilder.standard().build();
    }
}
