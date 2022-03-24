provider "aws" {
  region                      = "us-east-1"
  access_key                  = "123"
  secret_key                  = "xyz"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true
  s3_use_path_style           = true

  endpoints {
    apigateway     = "http://localhost:4566"
    cloudformation = "http://localhost:4566"
    cloudwatch     = "http://localhost:4566"
    dynamodb       = "http://localhost:4566"
    ec2            = "http://localhost:4566"
    es             = "http://localhost:4566"
    elasticache    = "http://localhost:4566"
    firehose       = "http://localhost:4566"
    iam            = "http://localhost:4566"
    kinesis        = "http://localhost:4566"
    lambda         = "http://localhost:4566"
    rds            = "http://localhost:4566"
    redshift       = "http://localhost:4566"
    route53        = "http://localhost:4566"
    s3             = "http://localhost:4566"
    secretsmanager = "http://localhost:4566"
    ses            = "http://localhost:4566"
    sns            = "http://localhost:4566"
    sqs            = "http://localhost:4566"
    ssm            = "http://localhost:4566"
    stepfunctions  = "http://localhost:4566"
    sts            = "http://localhost:4566"
  }
}

// DYNAMODB TABLE
resource "aws_dynamodb_table" "PersonDetails" {
  name           = "PersonDetails"
  read_capacity  = "20"
  write_capacity = "20"
  hash_key       = "Id"

  attribute {
    name = "Id"
    type = "N"
  }
}

// CREATE ROLE FOR LAMBDA
/*variable "role_name" {
  type = string
}*/

resource "aws_iam_role" "iam_for_lambda" {
  name = "test-role"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": "1"
    }
  ]
}
EOF
}

//CREATE LAMBDA FUNCTION
/*variable "function_name" {
  type = string
}*/

resource "aws_lambda_function" "tf_lambda" {
  filename         = "../../target/localstackdemo-1.0-SNAPSHOT.jar"
  function_name    = "lambda-1"
  role             = aws_iam_role.iam_for_lambda.arn
  handler          = "com.gaurav.localstack.demo.lambda.S3EventHandler"
  source_code_hash = filebase64sha256("../../target/localstackdemo-1.0-SNAPSHOT.jar")
  timeout          = 30
  memory_size      = 1024
  environment {
    variables = {
      PROFILE    = "localstack"
      AWS_REGION = "us-east-1"
    }
  }

  runtime = "java11"
}


// CREATE S3 BUCKET
/*variable "bucket_name" {
  type = string
}*/
resource "aws_s3_bucket" "tf_bucket" {
  bucket = "test-bucket"

  tags = {
    Name        = "test-bucket"
    Environment = "Dev"
  }
}
resource "aws_s3_bucket_acl" "tf_bucket_acl" {
  bucket = aws_s3_bucket.tf_bucket.id
  acl    = "public-read"
}
resource "aws_s3_bucket_cors_configuration" "tf_bucket_cors_configuration" {
  bucket = aws_s3_bucket.tf_bucket.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "PUT", "POST"]
    allowed_origins = ["*"]
    expose_headers  = ["ETag", "x-amz-version-id"]
    max_age_seconds = 3000
  }
}
resource "aws_s3_bucket_versioning" "tf_bucket_versioning" {
  bucket = aws_s3_bucket.tf_bucket.id
  versioning_configuration {
    status = "Enabled"
  }
}

// CREATE EVENT NOTIFICATION
resource "aws_s3_bucket_notification" "bucket_notification" {
  bucket = aws_s3_bucket.tf_bucket.id
  lambda_function {
    lambda_function_arn = aws_lambda_function.tf_lambda.arn
    events = ["s3:ObjectCreated:*"]
  }
}

// CREATE SQS
/*variable "sqs_name" {
  type = string
}*/
resource "aws_sqs_queue" "tf_sqs" {
  name = "test-sqs"
}

//CREATE LAMBDA FUNCTION
/*variable "function2_name" {
  type = string
}*/

resource "aws_lambda_function" "tf_lambda2" {
  filename         = "../../target/localstackdemo-1.0-SNAPSHOT.jar"
  function_name    = "lambda-2"
  role             = aws_iam_role.iam_for_lambda.arn
  handler          = "com.gaurav.localstack.demo.lambda.SQSEventHandler"
  source_code_hash = filebase64sha256("../../target/localstackdemo-1.0-SNAPSHOT.jar")
  timeout          = 30
  memory_size      = 1024
  environment {
    variables = {
      PROFILE    = "localstack"
      AWS_REGION = "us-east-1"
    }
  }

  runtime = "java11"
}

// CREATE EVENT TRIGGER
resource "aws_lambda_event_source_mapping" "tf_lambda_trigger" {
  event_source_arn = aws_sqs_queue.tf_sqs.arn
  function_name    = aws_lambda_function.tf_lambda2.function_name
  batch_size       = 1
}

// CREATE 2nd S3 Bucket
resource "aws_s3_bucket" "tf_bucket_1" {
  bucket = "sqsbucket"

  tags = {
    Name        = "sqsbucket"
    Environment = "Dev"
  }
}
resource "aws_s3_bucket_acl" "tf_bucket_acl_1" {
  bucket = aws_s3_bucket.tf_bucket_1.id
  acl    = "public-read"
}
resource "aws_s3_bucket_cors_configuration" "tf_bucket_cors_configuration_1" {
  bucket = aws_s3_bucket.tf_bucket_1.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "PUT", "POST"]
    allowed_origins = ["*"]
    expose_headers  = ["ETag", "x-amz-version-id"]
    max_age_seconds = 3000
  }
}
resource "aws_s3_bucket_versioning" "tf_bucket_versioning_1" {
  bucket = aws_s3_bucket.tf_bucket_1.id
  versioning_configuration {
    status = "Enabled"
  }
}