## 🌳 Git 브랜치 규칙

### Branch Strategy
main
<br>>ㅤdevelop
ㅤ<br>ㅤ>>ㅤfeature/~
ㅤ<br>ㅤ>>ㅤfeature/~
ㅤ<br>ㅤ>>ㅤfeature/~

### Branch Rule
main
- 배포 가능한 코드
- 직접 Push 금지
- develop에서 Merge
develop
- 개발 통합 브랜치
- feature 브랜치만 Merge
feature
- 기능 단위 개발
ex) feature/fe-menu, feature/be-order, feature/be-payment

### Workflow
develop Pull
<br>↓
<br>feature 생성
<br>↓
<br>개발
<br>↓
<br>Commit
<br>↓
<br>Push
<br>↓
<br>PR 생성
<br>↓
<br>Review
<br>↓
<br>develop Merge
<br>↓
<br>feature 삭제

### Commit Convention
feat: 기능 추가
<br>fix: 버그 수정
<br>refactor: 리팩토링
<br>docs: 문서 수정
<br>style: 코드 스타일
<br>test: 테스트
<br>chore: 설정 변경

### Commit Message

```text
<type>: <변경 내용>
```

예시:

```text
feat: 주문 생성 API 구현
fix: 결제 취소 시 상태 변경 오류 수정
chore: 로컬 MySQL 및 JPA 환경 설정
docs: 로컬 DB 실행 방법 추가
```

### Pull Request

PR 제목은 커밋 메시지와 같은 형식을 사용합니다.

```text
<type>: <작업 내용>
```

PR 본문 템플릿:

```markdown
## 작업 내용

- 구현하거나 변경한 내용을 작성합니다.

## 테스트

- 수행한 테스트와 결과를 작성합니다.

## 참고

- 리뷰에 필요한 사항이나 후속 작업을 작성합니다.
```

로컬 DB 설정 PR 예시:

```text
chore: 로컬 MySQL 및 JPA 환경 설정
```

<br><br>

## 🚨 프로젝트 규칙

### 개발 규칙
- 모든 기능은 feature/* 브랜치에서 개발한다.
- main 브랜치 직접 Push 금지
- develop 브랜치 직접 Push 금지 (PR을 통해 Merge)
- 기능 개발 전 최신 develop을 Pull한다.
- Merge는 Squash and Merge를 사용한다.
  
### 완료 기준 (Definition of Done)
기능 구현 완료
API 연동 완료
예외 처리 완료
로컬 테스트 완료
PR 생성 및 리뷰 완료
develop 브랜치에 Merge 완료

<br><br>

