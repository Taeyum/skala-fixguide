import json, sys
from jsonschema import Draft202012Validator as V

import os
D = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'schemas') + '/'
S = {n: json.load(open(D + n + '.schema.json')) for n in
     ('agent-input', 'a1.items', 'a2.items', 'a3.documents')}

fails = []
def check(label, schema, inst, expect_ok):
    errs = sorted(V(schema).iter_errors(inst), key=lambda e: e.path)
    ok = not errs
    mark = 'PASS' if ok == expect_ok else 'FAIL'
    if mark == 'FAIL': fails.append(label)
    detail = '' if ok else ' | ' + errs[0].message[:80]
    print(f'  [{mark}] {label}{"" if expect_ok else " (거부 기대)"}{detail if not expect_ok or not ok else ""}')

print('== 스키마 자체 유효성 (draft 2020-12) ==')
for n, s in S.items():
    V.check_schema(s); print(f'  [PASS] {n}.schema.json')

print('== 문서 내 examples 통과 ==')
for n, s in S.items():
    for i, ex in enumerate(s.get('examples', [])):
        check(f'{n} examples[{i}]', s, ex, True)

print('== LLM 출력 계약 ($defs.modelOutput) ==')
mo = lambda n: {**S[n]['$defs']['modelOutput'], '$defs': S[n]['$defs']}
check('a1 modelOutput 정상', mo('a1.items'),
      {'items': [{'text': 'a'}, {'text': 'b'}, {'text': 'c'}]}, True)
check('a2 modelOutput 정상', mo('a2.items'),
      {'items': [{'text': 'a'}, {'text': 'b'}]}, True)
check('a3 modelOutput 정상', mo('a3.documents'),
      {'documents': [{'type': 'WORK_PERMIT', 'name': '작업허가서 초안', 'content': 'x'},
                     {'type': 'RISK_ASSESSMENT', 'name': '위험성평가서 초안', 'content': 'y'}]}, True)

print('== 역예제 (거부되어야 함) ==')
check('a1 항목 1건 (min 2 미달)', S['a1.items'],
      {'items': [{'itemId': 'i-01', 'text': 'a', 'edited': False}]}, False)
check('a1 itemId 형식 오류(item-1)', S['a1.items'],
      {'items': [{'itemId': 'item-1', 'text': 'a', 'edited': False}] * 3}, False)
check('a2 itemId 누락', S['a2.items'],
      {'items': [{'text': 'a', 'edited': False}, {'text': 'b', 'edited': False}]}, False)
check('a2 text 200자 초과', S['a2.items'],
      {'items': [{'itemId': 'i-01', 'text': 'x' * 201, 'edited': False},
                 {'itemId': 'i-02', 'text': 'b', 'edited': False}]}, False)
check('a3 문서 1건만', S['a3.documents'],
      {'documents': [{'docId': 'd-01', 'type': 'WORK_PERMIT', 'name': 'n', 'content': 'c', 'edited': False}]}, False)
check('a3 WORK_PERMIT 2건 (유형 중복)', S['a3.documents'],
      {'documents': [{'docId': 'd-01', 'type': 'WORK_PERMIT', 'name': 'n', 'content': 'c', 'edited': False},
                     {'docId': 'd-02', 'type': 'WORK_PERMIT', 'name': 'n', 'content': 'c', 'edited': False}]}, False)
check('a3 허용되지 않는 type', S['a3.documents'],
      {'documents': [{'docId': 'd-01', 'type': 'LOTO', 'name': 'n', 'content': 'c', 'edited': False},
                     {'docId': 'd-02', 'type': 'RISK_ASSESSMENT', 'name': 'n', 'content': 'c', 'edited': False}]}, False)
check('a1 스키마 밖 필드(model_note) 주입', S['a1.items'],
      {'items': [{'itemId': 'i-01', 'text': 'a', 'edited': False, 'model_note': 'hi'}] * 3}, False)

base = json.loads(json.dumps(S['agent-input']['examples'][0]))
check('input FILTER 인데 substanceType 없음', S['agent-input'],
      {**base, 'productType': 'FILTER', 'specJson': {'pressureRating': '3000 psi'}}, False)
check('input FITTING_TUBE material 누락', S['agent-input'],
      {**base, 'productType': 'FITTING_TUBE', 'specJson': {'connectionStandard': '1/4 in VCR'}}, False)
check('input requestNo 형식 오류', S['agent-input'], {**base, 'requestNo': 'WR-1'}, False)
check('input productType 미허용 값', S['agent-input'], {**base, 'productType': 'PUMP'}, False)
check('input FILTER + substanceType 정상', S['agent-input'],
      {**base, 'productType': 'FILTER', 'specJson': {'substanceType': 'HF'}}, True)

print('== MockAgentEngine 실제 payload 호환 ==')
mock = {
 'a1.items': {'items':[{'itemId':'i-01','text':'규격 적합: pressureRating 3000 psi 기준 (Mock)','edited':False},
                       {'itemId':'i-02','text':'대체 호환: 동급 사양 확인 필요 (Mock)','edited':False}]},
 'a2.items': {'items':[{'itemId':'i-01','text':'산업안전보건기준에 관한 규칙 제38조 — NH3 취급 설비 작업계획 대상 (Mock)','edited':False},
                       {'itemId':'i-02','text':'고압가스 안전관리법 시행규칙 — 운전 압력 기준 검토 필요 (Mock)','edited':False}]},
 'a3.documents': {'documents':[{'docId':'d-01','type':'WORK_PERMIT','name':'작업허가서 초안','content':'… (Mock)','edited':False},
                               {'docId':'d-02','type':'RISK_ASSESSMENT','name':'위험성평가서 초안','content':'… (Mock)','edited':False}]},
}
for n, inst in mock.items():
    check(f'MockAgentEngine {n} payload', S[n], inst, True)

print('\n실패:', fails if fails else '없음')
sys.exit(1 if fails else 0)
