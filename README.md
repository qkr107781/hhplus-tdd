## "같은 사용자가 동시에 충전할 경우, 해당 요청 모두 정상적으로 반영되어야 한다."

<br>

--------------------------------------
- 위의 요구사항을 충족하기 위해 아래와 같은 테스트를 작성하려고 합니다.
- `"한명의 유저가 1,000 포인트씩 100번의 포인트 충전을 동시 요청했을때 모두 정상적으로 반영되어야 한다."`
- 테스트 후 해당 유저 잔여 포인트는 요청이 유실되지 않고 모두 처리되서 1,000,000 포인트여야 합니다. 

<br>

## 🛠테스트 코드 구현 사용 클래스

> ### ExcutorService
>> - Executors.newFixedThreadPool(int threadCount)를 사용하여 고정된 크기의 스레드 풀을 생성
>> - 입력된 threadCount 만큼 동시에 충전 요청

> ### CyclicBarrier
>> - await() 메소드로 모든 스레드가 barrier에 도착할때 까지 기다렸다가 거의 동시에 실행 시켜 최악의 동시성 시나리오 유발
>> - 모든 스레드가 동시에 실행될 수 있도록 제어

> ### CountDownLatch
>> - 하나 이상의 스레드가 어떤 작업을 완료할 때까지 다른 스레드(여기서는 메인 스레드)를 기다리게 할 때 사용
>> - 최종 결과 값을 확인하기 전에 모든 스레드가 종료됐는지 확인하기 위함

--------------------------------------

<br>

- 여러 동시성 제어 방법 중 ReentarntLock과 ConcurrentHashMap을 선택했습니다.
- 유저가 한명일 때는 ReentrantLock만으로도 구현이 가능하나 더 나아가서 
- 여러명이 동시에 100번씩 충전한다고 하면 ConcurrentHashMap과 함께 사용할때 더 좋을거라고 생각했습니다.

<br>

### 🛠프로덕션 코드 구현 사용 클래스

- lock? 멀티스레드(Multi-thread) 또는 멀티프로세스(Multi-process) 환경에서 공유 자원(Shared Resource)에 대한 동시 접근을 제어하여 데이터의 일관성과 무결성을 보장하는 메커니즘을 총칭하는 용어

<br>	

> ### ReentrantLock - 한명의 유저 동시성 제어 구현
>> - **동작 원리**
>>>  - lock(): 어떠한 스레드도 lock이 점유되어 있지 않다면 lock 획득
>>>  - unlock(): lock 반환
>>>  - tryLock(): lock을 즉시 획득할 수 있으면 true 반환 후 lock 획득, 아닌 경우 false 반환 후 종료 처리
>>>  - tryLock(long timeout, TimeUnit unit): 특정 시간 동안 대기해보고도 lock 획득 안되면 종료 처리

<br>	

>> - **동작 순서**
>>>  1. lock() 메소드를 통해 명시적으로 lock 회득 요청
>>>  2. lock이 어떤 스레드에도 점유되어 있지 않으면 lock 획득 -> 이미 점유되어 있다면 blocking 상태로 앞에 lock 반환 타이밍까지 대기
>>>  3. 로직 수행
>>>  4. unlock() 메소드를 통해 명시적으로 lock 반환
		
<br>	
	
>> - **장점**
>>> - lock 획득 후 내부 로직 수행 중 다시 lock을 획득하려고 해도 DeadLock에 빠지지 않고 획득횟수만 증가함
>>> - 명시적으로 lock을 획득/반환하기 때문에 개발자가 제어하기 수월함
>>> - new ReentrantLock(true)로 생성하여 First In First Out 보장 가능

<br>	

>> - **단점**
>>> - 명시적인 것이 장점 일 수도 있지만, 개발자의 실수로 lock을 반환하지 않으면 뒤에 스레드들은 계속 대기해야됨
>>> - new ReentrantLock(true)는 FIFO가 보장되지만 스레드들을 별도의 큐로 관리해야해서 성능 저하가 발생할 수 있음

<br>	

> ### ConcurrentHashMap - 여러명의 유저 동시성 제어 구현
> - **동작 원리** 
>> - put(): 실행 시 해당 key 값의 Bucket에 lock 획하여 다른 스레드가 동시에 변경하는 것을 방지함
>>> - -> 메소드 전체가 아닌 특정 소스 블럭에 syncronized 선언 되어 있음
>>> - -> syncronized 선언된 부분 소스 블럭에서 value 변경 시켜 Thread-Safe하게 동작
>> - get(): syncronized가 선언되어 있지 않고 여러 Thread가 동시에 접근 할 수 있음 -> 이때 요청 key 값의 최신 value를 가져감
>> - computeIfAbsent(): 요청 key 값에 대해 value가 없을때만 입력하고 있으면 있는 값을 반환함, put()과 동일하게 해당 key 값의 Bucket에 lock 획득

<br>	

> - **동작 순서**
>> 1. Compare and Swap으로 현재 요청온 Bucket 체크 
>>> - -> 비어 있다면 별다른 동작 없이 값 입력
>>> - -> 비어 있지 않다면 무한 loop를 통해 삽입될 Bucket 상태 체크
>>> - -> 상태 체크 후 이미 Node가 존재하면 2.실행
>> 2. syncornized 선언된 코드 블럭(lock 획득)에서 새로운 Node(여기서는 같은 key로 들어온 요청)로 교체

<br>	

> - **장점**
>> - Map 전체가 아닌 특정 key에 대해 세분화된 lock을 획득하기 때문에 병렬 처리(Multi Thread)에 유리함 -> lock을 세분화에서 관리하기 때문에 성능에 큰 이점이 있음

<br>			

> - **단점**
>> - 해당 객체 메소드가 실행되는 동안에만 Thread-Safe가 보장되서(내부의 동시성) 다른 외부 자원(데이터베이스, 파일, 다른 객체 등)의 상태를 함께 동기화해야 할 때는 단독을 쓰기는 어려울 듯

<br>			

> - **단점을 극복할 방법**
> - **ConcurrentHashMap value로 ReentrantLock 사용**
> - **동작 원리**
>> - key:value -> 유저ID : new ReentrantLock 형태로 Node 세팅하여 유저ID 별로 lock 획득
>>> - 여러명의 유저가 동시에 요청해도 ConcurrentHashMap은 key 값 별로 lock을 획득하기 때문에 여러 유저의 동시적인 요청에 대해서도 병렬 처리 가능(ReentrantLock만 사용한다면 스레드 전체가 하나의 lock을 바라보고 대기해야함)
> - **동작 순서**
>> 1. key 값(유저ID)에 new ReentrantLock computeIfAbsent() 요청
>> 2. 해당 Bucket에 대해 lock 획득 -> 다른 스레드는 해당 key 값의 value를 변경하지 못하게 됨
>> 3. value(ReentrantLock)가 있으면 그대로 반환하고 없으면 새로 생성
>> 4. computeIfAbsent()에서 반환한 ReentrantLock lock()으로 lock 수동 획득
>> 5. 충전 로직 실행
>> 6. ReentrantLock unlock()으로 수동 반환

<br>						

 - Bucket: Map에 데이터를 저장하는 배열의 한칸, 한칸에 여러 Node(key:value)가 들어 있음
 - Compare and Swap: lock을 사용하지 않고 스레드의 상태를 제어하기 때문에 lock을 사용해 스레드 상태를 제어하는 방법에서 발생하는 단점(ex)스레드 컨텍스트 스위칭, DeadLock, 우선순위 역전)을 극복할 수 있음


<br>

--------------------------------------

## ✅ 커밋 메세지 컨벤션
| 타입 | 설명 |
| --- | --- |
| feat | 새로운 기능 |
| fix | 버그 수정 |
| docs | 문서 관련 수정 |
| style | 코드 스타일 변경 (코드 포매팅, 세미콜론 누락 등)기능 수정이 없는 경우 |
| refactor | 리팩토링 |
| chore | 설정, 빌드, 잡일 등 |
| test | 테스트 코드 추가 |
| remove | 파일을 삭제만 한 경우 |
| rename | 파일 혹은 폴더명을 수정만 한 경우 |
| perf | 성능 개선 |
| security | 보안 관련 |
