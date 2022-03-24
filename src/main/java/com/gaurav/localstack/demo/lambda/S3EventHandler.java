package com.gaurav.localstack.demo.lambda;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.gaurav.localstack.demo.model.PersonDetailsVO;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class S3EventHandler implements RequestHandler<S3Event, String> {

    @Override
    public String handleRequest(S3Event event, Context context) {
        Map<String, String> environment = System.getenv();
        context.getLogger().log("Received event: " + event);

        // Get the object from the event and show its content type
        String bucket = event.getRecords().get(0).getS3().getBucket().getName();
        String key = event.getRecords().get(0).getS3().getObject().getKey();

        context.getLogger().log("Bucket: " + bucket);

        context.getLogger().log("Key: " + key);

        S3Object response = getAmazonS3(environment, context).getObject(new GetObjectRequest(bucket, key));
        InputStream objectContent = response.getObjectContent();
        ObjectMapper mapper = new ObjectMapper();
        try {
            CollectionType typeReference = TypeFactory.defaultInstance().constructCollectionType(List.class,
                    PersonDetailsVO.class);
            List<PersonDetailsVO> PersonDetailsVO = mapper.readValue(objectContent, typeReference);
            context.getLogger().log(PersonDetailsVO.toString());
            saveToDynamo(PersonDetailsVO, environment, context);
        } catch (IOException e) {
            e.printStackTrace();
            context.getLogger().log(e.getMessage());
        }
        String contentType = response.getObjectMetadata().getContentType();
        context.getLogger().log("CONTENT TYPE: " + contentType);
        return contentType;
    }

    private void saveToDynamo(List<PersonDetailsVO> PersonDetailsVO, Map<String, String> environment, Context context) {
        AmazonDynamoDB client = getAmazonDynamoDB(environment, context);
        DynamoDBMapper mapper = new DynamoDBMapper(client);
        mapper.batchSave(PersonDetailsVO);
    }

    private AmazonS3 getAmazonS3(Map<String, String> environment, Context context) {
        String profile = environment.get("PROFILE");
        context.getLogger().log("Profile is: " + profile);
        if ("localstack".equals(profile)) {
            BasicAWSCredentials creds = new BasicAWSCredentials("123", "xyz");
            AmazonS3ClientBuilder standard = AmazonS3ClientBuilder.standard();
            String s3Endpoint = environment.get("LOCALSTACK_HOSTNAME");

            System.out.println("LOCALSTACK_HOSTNAME " +s3Endpoint);

            EndpointConfiguration configuration = new EndpointConfiguration("http://" + s3Endpoint + ":4566",
                    "us-east-1");
            standard.withEndpointConfiguration(configuration);
            standard.withPathStyleAccessEnabled(true);
            standard.withCredentials(new AWSStaticCredentialsProvider(creds));
            return standard.build();
        }
        context.getLogger().log(" Using AWS Environment");
        return AmazonS3ClientBuilder.standard().build();
    }

    private AmazonDynamoDB getAmazonDynamoDB(Map<String, String> environment,Context context) {
        String profile = environment.get("PROFILE");
        context.getLogger().log("Profile is: " + profile);
        if ("localstack".equals(profile)) {
            BasicAWSCredentials creds = new BasicAWSCredentials("123", "xyz");
            String dynamoEndPoint = environment.get("LOCALSTACK_HOSTNAME");
            System.out.println("LOCALSTACK_HOSTNAME " +dynamoEndPoint);
            AmazonDynamoDBClientBuilder standard = AmazonDynamoDBClientBuilder.standard();
            EndpointConfiguration configuration = new EndpointConfiguration("http://" + dynamoEndPoint + ":4566",
                    "us-east-1");
            standard.withEndpointConfiguration(configuration);
            standard.withCredentials(new AWSStaticCredentialsProvider(creds));
            return standard.build();
        }
        context.getLogger().log(" Using AWS Environment");
        return AmazonDynamoDBClientBuilder.standard().build();
    }
}