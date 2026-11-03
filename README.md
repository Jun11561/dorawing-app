# 두로잉: 오늘의 기억을 그리다

65세 이상 어르신을 위한 치매 예방 안드로이드 앱입니다.
자가진단(KDSQ-C 방식)으로 내 상태를 살펴보고, 두뇌 훈련 게임으로 매일 기억력, 계산력, 주의력, 언어력을 연습합니다.
기록이 평소와 달라지면 알려 주고, 원하면 보호자와 결과를 나눌 수 있습니다.

## 실행 방법
1. Android Studio에서 이 폴더를 연다
2. Gradle Sync가 끝나면 에뮬레이터나 폰을 골라 Run
3. 명령줄: `gradlew assembleDebug` (빌드), `gradlew testDebugUnitTest` (단위 테스트)

디버그 빌드의 홈 화면 맨 아래 "디버그 메뉴"에서 가상 기록을 넣거나 게임 화면을 바로 열 수 있습니다.

## 보호자 연결 확인 (폰 2대)
`app/google-services.json`을 넣고 `firestore.rules`를 Firebase 콘솔 Firestore 규칙에 붙여 넣은 뒤 두 폰에 설치합니다.
어르신 폰: 설정 > 보호자 연결 > 어르신용 > 동의 > 6자리 연결 번호. 보호자 폰: 처음 실행에서 보호자용 > 연결 번호와 이름, 어르신 호칭 입력.
어르신 폰에서 연결하기를 누르고 두뇌 훈련을 1세트 하면 보호자 화면에 정답률과 출석이 보입니다.

## 폴더별 담당
| 폴더 / 파일 | 내용 | 담당 |
|---|---|---|
| `db/` | Room Entity, Dao, AppDatabase | 강준묵 |
| `game/` | GameRecorder, GameStats, Difficulty, ChangeCheck | 강준묵 |
| `game/GameType.kt` | 게임 종류 목록 | 허시온 |
| `util/` | Speaker(음성 안내), Prefs, TopBar, 날짜 함수 | 강준묵 |
| `ui/home`, `ui/diagnosis`, `ui/record`, `ui/guardian`, `ui/settings` | 홈, 자가진단, 내 기록, 보호자 모드, 설정 | 강준묵 |
| `ui/debug` (src/debug) | 디버그 메뉴 | 강준묵 |
| `ui/game/` | 게임 공통 틀, GameLauncher, 게임 선택, 각 게임, 결과 화면 | 허시온 |
| `strings_game.xml`, `colors_game.xml`, `dimens_game.xml`, `activity_game_*.xml`, `game_` 리소스 | 게임 리소스 | 허시온 |
| 그 밖의 리소스, Manifest(게임 화면 구역 제외), Gradle | 공용 설정 | 강준묵 |
