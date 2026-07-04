# 학습 문서 (Learning)

ai-repo를 소재로 한 **개념 학습용 튜토리얼** 모음입니다. 실제 실행/운영 문서는 [Getting Started](../GETTING-STARTED.md)와 [development/](../development/)를, 결정 근거는 [ADR](../adr/README.md)를 참고하세요. 이 폴더의 글은 "왜 이렇게 동작하는가"를 초심자에게 친절히 설명하는 데 초점을 둡니다.

## 목록

| 문서 | 무엇을 배우나 | 관련 |
| --- | --- | --- |
| [elk-stack.md](elk-stack.md) | ELK 로깅 스택(Filebeat→Logstash→Elasticsearch→Kibana) 4-tier 아키텍처, grok 파싱, 역색인, PLG(Loki)와의 비교, Kibana 실습 | opt-in · [ADR-0066](../adr/0066-elk-logging-stack.md) · [가이드](../development/elk-local-logging.md) |
