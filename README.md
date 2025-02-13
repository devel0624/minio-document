# Minio, Object Storage 기본 사용법

- Ref : [https://docs.aws.amazon.com/AmazonS3/latest/API/Type_API_Reference.html](https://docs.aws.amazon.com/AmazonS3/latest/API/Type_API_Reference.html)

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

#### 버킷 생성

```http
PUT /{{bucket-name}} HTTP/1.1
Host: {{minio-server}}
```

#### 버킷 삭제

### Object

#### 객체 생성

#### 객체 조회

#### 객체 삭제

### Multipart 

#### Multipart 업로드 생성

#### Multipart 업로드

#### Multipart 병합


