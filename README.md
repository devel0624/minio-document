# Minio, Object Storage 기본 사용법

- Ref : [https://docs.aws.amazon.com/AmazonS3/latest/API/API_Operations.html](https://docs.aws.amazon.com/AmazonS3/latest/API/API_Operations.html)

## 1. Web UI 를 사용하여 Key 발급

1. [http://storage.java21.net/](http://storage.java21.net/) 에 접속
2. 팀별로 제공된 ID / PW 를 이용하여 콘솔에 로그인
3. [http://storage.java21.net/access-keys](http://storage.java21.net/access-keys) Access key 관리 화면에서 키를 생성. 시크릿키는 분실시 재발급 받아야하므로 유의
4. 아래의 안내에 따라 API 동작을 확인

## 2. 기본 API

### 2-1. Authorization Header

minio API 는 AWS Signature v4 방식의 Authorization 헤더를 포함해야한다.
[https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-authenticating-requests.html](https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-authenticating-requests.html)
[https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_sigv-create-signed-request.html](https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_sigv-create-signed-request.html)

#### 2-1-1. Example Http Request
```http
GET /path HTTP/1.1
Host: ${host}
X-Amz-Content-Sha256: ${sha256-encoded-content}
X-Amz-Date: ${yyyymmddThhmmssZ}
Authorization: ${algorithm} Credential=${access-key}/${yyyymmdd}/${region}/${service}/aws4_request, Signature=${signature}
Content-Length: 0
```

#### 2-1-2. Create a canonical request
Arrange the contents of your request (host, action, headers, etc.) into a standard canonical format. The canonical request is one of the inputs used to create the string to sign. For details on creating the canonical request, see Elements of an AWS API request signature.


[예시로 사용될 요청 #2-1-1-example-http-request](#2-1-1-example-http-request) 을 토대로 아래의 규칙에 따라 표준 요청을 생성

```http
<HTTP_METHOD>
<Canonical_URI>
<Canonical_Query_String>
<Canonical_Headers>
<Signed_Headers>
<PAYLOAD_HASH>
```

```http
GET
/path

host:${host}
x-amz-content-sha256:${sha256-encoded-content}
x-amz-date:${yyyymmddThhmmssZ}

host;x-amz-content-sha256;x-amz-date
${sha256-encoded-content}
```

#### 2-1-3. Create a hash of the canonical request
Hash the canonical request using the same algorithm that you used to create the hash of the payload. The hash of the canonical request is a string of lowercase hexadecimal characters.

##### Shell Command
```bash
echo -n "GET
/path

host:${host}
x-amz-content-sha256:${sha256-encoded-content}
x-amz-date:${yyyymmddThhmmssZ}

${signed-headers}
${sha256-encoded-content}" | openssl dgst -sha256
```

###### input
```bash
echo -n "GET
/path

host:localhost:9000
x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
x-amz-date:20250213T153743Z

host;x-amz-content-sha256;x-amz-date
e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855" | openssl dgst -sha256
```

###### Output 
```sh
SHA2-256(stdin)= 8a1f34ed8e85c864e9244474cdcfe0db5822c412624a7107702d03722accd699
```

#### 2-1-4. Create a string to sign
Create a string to sign with the canonical request and extra information such as the algorithm, request date, credential scope, and the hash of the canonical request.

##### Overview
```md
Algorithm\n
RequestDateTime\n
CredentialScope\n
HashedCanonicalRequest
```

HashedCanonicalRequest is [#### 2-1-3 .Shell Command Output](#2-1-3-create-a-hash-of-the-canonical-request)

##### StringToSign

```md
${algorithm}
${yyyymmddThhmmssZ}
${yyyymmdd}/${region}/${service}/aws4_request
${hashed-canonical-request}
```

###### Example
```md
AWS4-HMAC-SHA256
20250213T153743Z
20250213/us-east-1/s3/aws4_request
68be3dc9f79e99c515f03f9039f11df4da1fd83d39dd885134cb7cda45e08700
```

#### 2-1-5. Derive a signing key
Use the secret access key to derive the key used to sign the request.

##### Overview

```md
DateKey = HMAC-SHA256("AWS4"+"<SecretAccessKey>", "<YYYYMMDD>")
DateRegionKey = HMAC-SHA256(<DateKey>, "<aws-region>")
DateRegionServiceKey = HMAC-SHA256(<DateRegionKey>, "<aws-service>")
SigningKey = HMAC-SHA256(<DateRegionServiceKey>, "aws4_request")
```

##### DateKey

###### Shell Command
```bash
echo -n "${yyyymmdd}" | openssl mac -digest sha256 -macopt key:AWS4${secret-key} HMAC
```

###### Input
```bash
echo -n "20250213" | openssl mac -digest sha256 -macopt key:AWS4MySecret HMAC
```

###### Output

```sh
2366BFC7DF66AFBE5FE0E0AD69FA99C21B6DEA1CB3FF580541BF919BC63E5C8C
```

##### DateRegionKey 

###### Shell Command
```bash
echo -n "${region}" | openssl mac -digest sha256 -macopt hexkey:${date-key} HMAC
```

###### Input
```sh
echo -n "us-east-1" | openssl mac -digest sha256 -macopt hexkey:2366BFC7DF66AFBE5FE0E0AD69FA99C21B6DEA1CB3FF580541BF919BC63E5C8C HMAC
```

###### Output
```sh
E1CD5FE5F51BF1645524B5A9DABD7D2A90A2CE98336C281196EE42B54C8578C7
```

##### DateRegionServiceKey

###### Shell Command
```bash
echo -n "${service}" | openssl mac -digest sha256 -macopt hexkey:${date-region-key} HMAC
```

###### Input
```sh
echo -n "s3" | openssl mac -digest sha256 -macopt hexkey:E1CD5FE5F51BF1645524B5A9DABD7D2A90A2CE98336C281196EE42B54C8578C7 HMAC
```

###### Output
```sh
A71C65C4BFE431D13B270D7C84AB09D40AA85F7DB0C964F8F80B9A8FB0D7D3D7
```

##### SigningKey

###### Shell Command
```bash
echo -n "aws4_request" | openssl mac -digest sha256 -macopt hexkey:${date-region-service-key} HMAC
```

###### Input
```sh
echo -n "aws4_request" | openssl mac -digest sha256 -macopt hexkey:A71C65C4BFE431D13B270D7C84AB09D40AA85F7DB0C964F8F80B9A8FB0D7D3D7 HMAC
```
###### Output
```sh
270D7180AB6395D7C559B527D3A36A91ED658BE5D75205288A98C7E5227D0E86
```

#### 2-1-6. Calculate the signature
Perform a keyed hash operation on the string to sign using the derived signing key as the hash key.

##### Signature

```md
signature = hash(SigningKey, string-to-sign)
```

###### Shell Command
```bash
echo -n "${string-to-sign}" | openssl mac -digest sha256 -macopt hexkey:${signing-key} HMAC
```

string-to-sign is [#### 2-1-4 .StringToSign](#2-1-4-create-a-string-to-sign)

###### Input

```sh
echo -n "AWS4-HMAC-SHA256
20250213T153743Z
20250213/us-east-1/s3/aws4_request
68be3dc9f79e99c515f03f9039f11df4da1fd83d39dd885134cb7cda45e08700" | openssl mac -digest sha256 -macopt hexkey:270D7180AB6395D7C559B527D3A36A91ED658BE5D75205288A98C7E5227D0E86 HMAC
```

###### Output
```sh
8C37E474CA5F33DEB259D4B7F91056DD7976F83EE08533FEDC49621E6B67C489
```

#### 2-1-7. Add the signature to the request
Add the calculated signature to an HTTP header or to the query string of the request.

Ref: [#### 2-1-1. Example Http Request](#2-1-1-example-http-request)

```http
GET /path HTTP/1.1
Host: ${host}
X-Amz-Content-Sha256: ${sha256-encoded-content}
X-Amz-Date: ${yyyymmddThhmmssZ}
Authorization: ${algorithm} Credential=${access-key}/${yyyymmdd}/${region}/${service}/aws4_request, Signature=${signature}
Content-Length: 0
```

```http
GET /path HTTP/1.1
Host: localhost:9000
X-Amz-Content-Sha256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
X-Amz-Date: 20250213T153743Z
Authorization: AWS4-HMAC-SHA256 Credential=MyAccess/20250213/us-east-1/s3/aws4_request, Signature=8C37E474CA5F33DEB259D4B7F91056DD7976F83EE08533FEDC49621E6B67C489
Content-Length: 0
```

### Bucket

#### Basic Path
```md
/{{bucket-name}}
```

#### 버킷 생성

```http
PUT /{{bucket-name}} HTTP/1.1
```

버킷을 생성한다. 성공시 200 OK, 이미 존재할 경우 409 Conflict

##### Example 

###### Request 

```http
PUT /miniouser HTTP/1.1
Host: storage.java21.net:8000
Authorization: AWS4-HMAC-SHA256 Credential=jH83zk4JzMss13efvpUY/20250217/us-east-1/s3/aws4_request, SignedHeaders=host;x-amz-content-sha256;x-amz-date, Signature=a139901ddd9893e5d1bcea839ba0c2ed176754fcb18f57510ec229e6a882eb00
X-Amz-Content-Sha256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
X-Amz-Date: 20250217T022442Z
```

###### Response

```http
HTTP/1.1 200 OK
Accept-Ranges: bytes
Content-Length: 0
Location: /miniouser
Server: MinIO
Strict-Transport-Security: max-age=31536000; includeSubDomains
Vary: Origin
Vary: Accept-Encoding
X-Amz-Id-2: dd9025bab4ad464b049177c95eb6ebf374d3b3fd1af9251148b658df7ac2e3e8
X-Amz-Request-Id: 1824DDB3EC8EC73B
X-Content-Type-Options: nosniff
X-Ratelimit-Limit: 36620
X-Ratelimit-Remaining: 36620
X-Xss-Protection: 1; mode=block
Date: Mon, 17 Feb 2025 02:25:20 GMT
```

#### 버킷 삭제

```http
DELETE /{{bucket-name}} HTTP/1.1
```

버킷을 삭제한다.

##### Example 

###### Request 
```http
DELETE /miniouser HTTP/1.1
Host: storage.java21.net:8000
Authorization: AWS4-HMAC-SHA256 Credential=jH83zk4JzMss13efvpUY/20250217/us-east-1/s3/aws4_request, SignedHeaders=host;x-amz-content-sha256;x-amz-date, Signature=f741997440bb802b63bc0c16b778e17b1df044390f96265f33e328e971bf5f40
X-Amz-Content-Sha256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
X-Amz-Date: 20250217T022548Z
```
###### Response
```http
HTTP/1.1 204 No Content
Accept-Ranges: bytes
Server: MinIO
Strict-Transport-Security: max-age=31536000; includeSubDomains
Vary: Origin
Vary: Accept-Encoding
X-Amz-Id-2: dd9025bab4ad464b049177c95eb6ebf374d3b3fd1af9251148b658df7ac2e3e8
X-Amz-Request-Id: 1824DDBDC3D0BC6E
X-Content-Type-Options: nosniff
X-Ratelimit-Limit: 36620
X-Ratelimit-Remaining: 36620
X-Xss-Protection: 1; mode=block
Date: Mon, 17 Feb 2025 02:26:03 GMT
```

### Object

#### Basic Path
```md
/{{bucket-name}}/{{resource-name}}
```

#### 객체 생성

```http
PUT /{{bucket-name}}/{{resource-name}} HTTP/1.1
```

객체를 생성한다. 이미 같은 이름으로 존재하면 덮어씌운다.

##### Example 

###### Request 
```http
PUT /miniouser/sample-image.jpg HTTP/1.1
Host: storage.java21.net:8000
Authorization: AWS4-HMAC-SHA256 Credential=jH83zk4JzMss13efvpUY/20250217/us-east-1/s3/aws4_request, SignedHeaders=host;x-amz-content-sha256;x-amz-date, Signature=4cbe71cd75685dc73b9433f7d329c71c478bcba3a856ec60a40dd72c7a8c3050
X-Amz-Content-Sha256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
X-Amz-Date: 20250217T022708Z
Content-Type: text/plain
Content-Length: 22

"<file contents here>"
```
###### Response
```http
HTTP/1.1 200 OK
Accept-Ranges: bytes  
Content-Length: 0  
ETag: "8f737670c91d6197959132e3b1928696"  
Server: MinIO  
Strict-Transport-Security: max-age=31536000; includeSubDomains  
Vary: Origin  
Vary: Accept-Encoding  
X-Amz-Id-2: dd9025bab4ad464b049177c95eb6ebf374d3b3fd1af9251148b658df7ac2e3e8  
X-Amz-Request-Id: 1824DDD54262CC98  
X-Content-Type-Options: nosniff  
X-Ratelimit-Limit: 36620  
X-Ratelimit-Remaining: 36620  
X-Xss-Protection: 1; mode=block  
Date: Mon, 17 Feb 2025 02:27:44 GMT  

```

#### 객체 조회

```http
GET /{{bucket-name}}/{{resource-name}} HTTP/1.1
```

객체를 조회한다.

##### Example 

###### Request 
```http
GET /miniouser/sample-image.jpg HTTP/1.1
Host: storage.java21.net:8000
Authorization: AWS4-HMAC-SHA256 Credential=jH83zk4JzMss13efvpUY/20250217/us-east-1/s3/aws4_request, SignedHeaders=host;x-amz-content-sha256;x-amz-date, Signature=814a0b5f0fe7817250debf1175eccac2a3048fb09600c718da1177e2182032dd
X-Amz-Content-Sha256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
X-Amz-Date: 20250217T022823Z
Content-Length: 0

```
###### Response
```http
HTTP/1.1 200 OK
Accept-Ranges: bytes
Cache-Control: no-cache
Content-Length: 82972
Content-Type: image/jpeg
ETag: "8f737670c91d6197959132e3b1928696"
Last-Modified: Mon, 17 Feb 2025 06:24:19 GMT
Server: MinIO
Strict-Transport-Security: max-age=31536000; includeSubDomains
Vary: Origin
Vary: Accept-Encoding
X-Amz-Id-2: dd9025bab4ad464b049177c95eb6ebf374d3b3fd1af9251148b658df7ac2e3e8
X-Amz-Request-Id: 1824EABFE0918A39
X-Content-Type-Options: nosniff
X-Ratelimit-Limit: 36620
X-Ratelimit-Remaining: 36620
X-Xss-Protection: 1; mode=block
Date: Mon, 17 Feb 2025 06:24:25 GMT

```

#### 객체 삭제

```http
DELETE /{{bucket-name}}/{{resource-name}} HTTP/1.1
```

객체를 삭제한다.

##### Example 

###### Request 
```http
DELETE /miniouser/sample-image.jpg HTTP/1.1
Host: storage.java21.net:8000
Authorization: AWS4-HMAC-SHA256 Credential=jH83zk4JzMss13efvpUY/20250217/us-east-1/s3/aws4_request, SignedHeaders=host;x-amz-content-sha256;x-amz-date, Signature=74a8b4f3826df4941b98a9e2271ef418473ee39571304aac7e395faed9213480
X-Amz-Content-Sha256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
X-Amz-Date: 20250217T023052Z
Content-Length: 0

```
###### Response
```http
HTTP/1.1 204 No Content
Accept-Ranges: bytes
Server: MinIO
Strict-Transport-Security: max-age=31536000; includeSubDomains
Vary: Origin
Vary: Accept-Encoding
X-Amz-Id-2: dd9025bab4ad464b049177c95eb6ebf374d3b3fd1af9251148b658df7ac2e3e8
X-Amz-Request-Id: 1824DE050066BCE0
X-Content-Type-Options: nosniff
X-Ratelimit-Limit: 36620
X-Ratelimit-Remaining: 36620
X-Xss-Protection: 1; mode=block
Date: Mon, 17 Feb 2025 02:31:09 GMT

```


### Multipart 

#### Basic Path
```md
/{{bucket-name}}/{{resource-name}} HTTP/1.1
```

#### Multipart 업로드 생성

```http
POST /{{bucket-name}}/{{resource-name}}?uploads HTTP/1.1
```

대용량 파일 업로드를 위해 멀티 파트 업로드 요청을 생성한다.
응답으로 돌아오는 업로드 ID 를 이용하여, 이후 API 들을 처리한다.

##### Example 

###### Request 
```http
POST /miniouser/sample-multipart.jpg?uploads HTTP/1.1
Host: storage.java21.net:8000
Authorization: AWS4-HMAC-SHA256 Credential=jH83zk4JzMss13efvpUY/20250217/us-east-1/s3/aws4_request, SignedHeaders=host;x-amz-content-sha256;x-amz-date, Signature=ae8c4e376a83082054a29521a0e10599d226f097295ceb25d7287564a3558318
X-Amz-Content-Sha256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
X-Amz-Date: 20250217T044539Z
```
###### Response
```http
HTTP/1.1 200 OK
Accept-Ranges: bytes
Content-Length: 352
Content-Type: application/xml
Server: MinIO
Strict-Transport-Security: max-age=31536000; includeSubDomains
Vary: Origin
Vary: Accept-Encoding
X-Amz-Id-2: dd9025bab4ad464b049177c95eb6ebf374d3b3fd1af9251148b658df7ac2e3e8
X-Amz-Request-Id: 1824E55F1CDFBB41
X-Content-Type-Options: nosniff
X-Ratelimit-Limit: 36620
X-Ratelimit-Remaining: 36620
X-Xss-Protection: 1; mode=block
Date: Mon, 17 Feb 2025 04:45:52 GMT

<?xml version="1.0" encoding="UTF-8"?>
<InitiateMultipartUploadResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
    <Bucket>miniouser</Bucket>
    <Key>sample-multipart.jpg</Key>
    <UploadId>ZTliMTNjYWEtNDY4MS00MmI3LTlkZjMtOWJjODUwMTAyZGY4LjlkYTU4YTliLTE2YTItNDhkZi1hNzM2LWY4MzAwNjUyZDc2ZXgxNzM5NzY3NTUyNzQyNjQyMjg5</UploadId>
</InitiateMultipartUploadResult>
```

#### Multipart 업로드

```http
PUT /{{bucket-name}}/{{resource-name}}?partNumber={{part-number}}&uploadId={{uploadId}} HTTP/1.1
```

업로드 생성에서 받은 ID 를 이용하여 각 파트를 업로드한다.
partNumber 는 각 파트들이 나열될 순서이다.
아래에서는 3개의 부분으로 나눠진 멀티 파트 업로드 요청 예시이며 각 응답의 ETag 를 이용하여 최종적으로 멀티 파트 업로드를 완료한다.

##### Example 

###### Request 
```http
PUT /miniouser/sample-multipart.jpg?partNumber=1&uploadId=ZTliMTNjYWEtNDY4MS00MmI3LTlkZjMtOWJjODUwMTAyZGY4LjlkYTU4YTliLTE2YTItNDhkZi1hNzM2LWY4MzAwNjUyZDc2ZXgxNzM5NzY3NTUyNzQyNjQyMjg5 HTTP/1.1
Host: storage.java21.net:8000
Content-Type: text/plain
Content-Length: 22

"<file contents here>"
```
###### Response
```http
HTTP/1.1 200 OK
Accept-Ranges: bytes
Content-Length: 0
ETag: "47e9d83d3324f156a04e32d819e08449"
Server: MinIO
Strict-Transport-Security: max-age=31536000; includeSubDomains
Vary: Origin
Vary: Accept-Encoding
X-Amz-Id-2: dd9025bab4ad464b049177c95eb6ebf374d3b3fd1af9251148b658df7ac2e3e8
X-Amz-Request-Id: 1824E7654EA2B823
X-Content-Type-Options: nosniff
X-Ratelimit-Limit: 36620
X-Ratelimit-Remaining: 36620
X-Xss-Protection: 1; mode=block
Date: Mon, 17 Feb 2025 05:22:58 GMT

```
###### Request 
```http
PUT /miniouser/sample-multipart.jpg?partNumber=2&uploadId=ZTliMTNjYWEtNDY4MS00MmI3LTlkZjMtOWJjODUwMTAyZGY4LjlkYTU4YTliLTE2YTItNDhkZi1hNzM2LWY4MzAwNjUyZDc2ZXgxNzM5NzY3NTUyNzQyNjQyMjg5 HTTP/1.1
Host: storage.java21.net:8000
Content-Type: text/plain
Content-Length: 22

"<file contents here>"
```
###### Response
```http
HTTP/1.1 200 OK
Accept-Ranges: bytes
Content-Length: 0
ETag: "4eb85cfa7eab3bd2cf039ea5fe47bc6d"
Server: MinIO
Strict-Transport-Security: max-age=31536000; includeSubDomains
Vary: Origin
Vary: Accept-Encoding
X-Amz-Id-2: dd9025bab4ad464b049177c95eb6ebf374d3b3fd1af9251148b658df7ac2e3e8
X-Amz-Request-Id: 1824E7768F0675FF
X-Content-Type-Options: nosniff
X-Ratelimit-Limit: 36620
X-Ratelimit-Remaining: 36620
X-Xss-Protection: 1; mode=block
Date: Mon, 17 Feb 2025 05:24:12 GMT

```
###### Request 
```http
PUT /miniouser/sample-multipart.jpg?partNumber=3&uploadId=ZTliMTNjYWEtNDY4MS00MmI3LTlkZjMtOWJjODUwMTAyZGY4LjlkYTU4YTliLTE2YTItNDhkZi1hNzM2LWY4MzAwNjUyZDc2ZXgxNzM5NzY3NTUyNzQyNjQyMjg5 HTTP/1.1
Host: storage.java21.net:8000
Content-Type: text/plain
Content-Length: 22

"<file contents here>"
```
###### Response
```http
HTTP/1.1 200 OK
Accept-Ranges: bytes
Content-Length: 0
ETag: "d41d8cd98f00b204e9800998ecf8427e"
Server: MinIO
Strict-Transport-Security: max-age=31536000; includeSubDomains
Vary: Origin
Vary: Accept-Encoding
X-Amz-Id-2: dd9025bab4ad464b049177c95eb6ebf374d3b3fd1af9251148b658df7ac2e3e8
X-Amz-Request-Id: 1824E77BA29317A4
X-Content-Type-Options: nosniff
X-Ratelimit-Limit: 36620
X-Ratelimit-Remaining: 36620
X-Xss-Protection: 1; mode=block
Date: Mon, 17 Feb 2025 05:24:34 GMT

```

#### Multipart 병합

```http
POST /{{bucket-name}}/{{resource-name}}?uploadId={{uploadId}} HTTP/1.1
```

멀티 파트 업로드를 병합하여 하나의 이미지로 만들어 업로드를 완료한다.
이때 요청 바디에 각 파트 별로 반환되었던 ETag 를 지정해주어야한다.
조회는 일반 객체 조회와 동일

##### Example 

###### Request 
```http
POST /miniouser/sample-multipart.jpg?uploadId=ZTliMTNjYWEtNDY4MS00MmI3LTlkZjMtOWJjODUwMTAyZGY4LjlkYTU4YTliLTE2YTItNDhkZi1hNzM2LWY4MzAwNjUyZDc2ZXgxNzM5NzY3NTUyNzQyNjQyMjg5 HTTP/1.1
Host: storage.java21.net:8000
Authorization: AWS4-HMAC-SHA256 Credential=jH83zk4JzMss13efvpUY/20250217/us-east-1/s3/aws4_request, SignedHeaders=host;x-amz-content-sha256;x-amz-date, Signature=2166a64b3441f3237634247476eb7f043d6080c2818e161c9aee3bc1af7a4983
X-Amz-Content-Sha256: 27aae9654a8903987aafa42a678582a19f430d22788b3e9630f496e78a5442ab
X-Amz-Date: 20250217T052715Z
Content-Length: 394

<CompleteMultipartUpload>
    <Part>
        <PartNumber>1</PartNumber>
        <ETag>"47e9d83d3324f156a04e32d819e08449"</ETag>
    </Part>
    <Part>
        <PartNumber>2</PartNumber>
        <ETag>"4eb85cfa7eab3bd2cf039ea5fe47bc6d"</ETag>
    </Part>
    <Part>
        <PartNumber>3</PartNumber>
        <ETag>"d41d8cd98f00b204e9800998ecf8427e"</ETag>
    </Part>
</CompleteMultipartUpload>
```
###### Response
```http
HTTP/1.1 200 OK
Accept-Ranges: bytes
Content-Length: 346
Content-Type: application/xml
ETag: "c559b266d481dee4b5d3acb00ee64600-3"
Server: MinIO
Strict-Transport-Security: max-age=31536000; includeSubDomains
Vary: Origin
Vary: Accept-Encoding
X-Amz-Id-2: dd9025bab4ad464b049177c95eb6ebf374d3b3fd1af9251148b658df7ac2e3e8
X-Amz-Request-Id: 1824E7A5789012D6
X-Content-Type-Options: nosniff
X-Ratelimit-Limit: 36620
X-Ratelimit-Remaining: 36620
X-Xss-Protection: 1; mode=block
Date: Mon, 17 Feb 2025 05:27:33 GMT

<?xml version="1.0" encoding="UTF-8"?>
<CompleteMultipartUploadResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
    <Location>http://storage.java21.net:8000/miniouser/sample-multipart.jpg</Location>
    <Bucket>miniouser</Bucket>
    <Key>sample-multipart.jpg</Key>
    <ETag>&#34;c559b266d481dee4b5d3acb00ee64600-3&#34;</ETag>
</CompleteMultipartUploadResult>
```


#### Access Policy 

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:*"
            ],
            "Resource": [
                "arn:aws:s3:::${aws:username}/*"
            ]
        }
    ]
}
```