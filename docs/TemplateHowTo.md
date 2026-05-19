# 🗂️ Spring Boot 프로젝트 템플릿

Spring Boot 기반 백엔드 프로젝트를 빠르게 시작할 수 있는 팀 공용 템플릿입니다.
이슈/PR 템플릿, 코드 포맷팅 자동화, OpenAPI 스펙 자동 생성, CodeRabbit AI 코드 리뷰가 사전 구성되어 있습니다.

---

## 🚀 템플릿 사용법

### 1. 레포지토리 생성

1. 이 레포지토리 상단의 **Use this template** 버튼 클릭
2. 새 레포지토리 이름 및 공개 여부 설정 후 생성

### 2. 브랜치 구성

기본 워크플로우는 `main`, `dev` 브랜치를 기준으로 동작합니다.

```bash
git checkout -b dev
git push origin dev
```

### 3. GitHub Pages 설정 (REST Docs 배포용)

1. 레포지토리 **Settings → Pages** 로 이동
2. Source를 **GitHub Actions** 로 설정

`dev` 또는 `main` 브랜치에 푸시하면 REST Docs가 자동으로 GitHub Pages에 배포됩니다.

---

## 📦 포함된 구성 요소

| 구성 요소                      | 설명                                                                                          |
|------------------------------|-----------------------------------------------------------------------------------------------|
| Issue 템플릿                  | 작업 설명, 목적, 세부 항목, 관련 이슈를 포함한 이슈 양식                                      |
| PR 템플릿                     | 관련 이슈, 작업 내용, 변경 이유, 리뷰 포인트를 포함한 PR 양식                                 |
| Spotless (Google Java Format) | 수동 실행(`workflow_dispatch`)으로 코드 포맷 검사                                             |
| OpenAPI 스펙 자동 생성         | `main`, `dev` 브랜치 푸시 시 `docs/openapi.yaml` 자동 생성 및 커밋                            |
| CodeRabbit AI 코드 리뷰        | PR 생성 시 DDD/MSA 아키텍처 규칙 기반 자동 AI 리뷰 (한국어, `.coderabbit.yaml` 사전 구성)     |

---

## 🧹 코드 포맷팅 (Spotless)

이 프로젝트는 일관된 Java 코드 스타일을 유지하기 위해 **Spotless(Google Java Format)** 를 사용합니다.
GitHub Actions에서 수동(`workflow_dispatch`)으로 실행하거나 로컬에서 직접 명령어를 실행해 포맷을 검사할 수 있습니다.



```bash
# 포맷 자동 정렬 적용 (커밋 전 필수)
./gradlew spotlessApply

# 포맷 준수 여부 검사 (CI 검증 명령어)
./gradlew spotlessCheck
```
만약 에러가 날 시 build.gradle 파일에 다음 코드를 추가해 주세요.
반드시 해당 명령어 사용시 자바 21 이하로 설정되어 있어야 합니다. 그 이상 버전은 지원하지 않는 구글 포맷 버전입니다.

```groovy
plugins {
id 'com.diffplug.spotless' version '6.25.0'// 추가 
}

// 파일 맨 아래에 다음 추가 
spotless {
    java {
        googleJavaFormat('1.24.0')
        target 'src/**/*.java'
    }
}
```
---

## 📄 REST Docs 자동 배포

`dev` 또는 `main` 브랜치에 푸시하면 GitHub Actions가 자동으로 다음을 수행합니다.

1. JDK 21 환경에서 `./gradlew asciidoctor` 빌드
2. 생성된 문서(`build/docs/asciidoc`)를 GitHub Pages에 배포

> **참고:** 빌드 시 Redis(7) 서비스가 필요합니다. 테스트에서 Redis를 사용하는 경우 별도 설정 없이 CI에서 자동으로 실행됩니다.

---

## 🤖 CodeRabbit AI 코드 리뷰

이 프로젝트는 PR 생성 시 **CodeRabbit**이 자동으로 AI 코드 리뷰를 수행합니다.
DDD/MSA 아키텍처 규칙에 맞춰 도메인 레이어 침투, Aggregate 경계 위반, 레이어 의존성 방향 등을 검토합니다.

### 초기 설정

1. [coderabbit.ai](https://coderabbit.ai) 에서 GitHub 계정으로 로그인
2. **Add Repository** 에서 이 레포지토리 선택 후 GitHub App 설치

> 설정 파일(`.coderabbit.yaml`)은 레포지토리 루트에 사전 구성되어 있습니다. 별도 설정 없이 바로 동작합니다.

### 리뷰 범위

| 레이어 | 주요 검토 항목 |
|--------|---------------|
| `domain/` | Aggregate 경계, Value Object 불변성, Domain Event 네이밍, 인프라 의존성 침투 감지 |
| `application/` | 오케스트레이션 책임 분리, CQRS, 트랜잭션 경계 |
| `infrastructure/` | Repository 구현, JPA/Domain Entity 분리, ACL 적용 |
| `interfaces/` | Domain 객체 직접 노출 금지, 서비스 간 통신 패턴 |
| 전체 `*.java` | Java 21 최신 문법 활용, Spring Boot 3.x 방식, MSA 회복탄력성 패턴 |