"""课程验收级 API 性能冒烟测试。默认执行 60 次请求、并发 6。"""
from __future__ import annotations

import argparse
import json
import math
import os
import statistics
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

import requests


def percentile(values: list[float], p: float) -> float:
    ordered = sorted(values)
    index = max(0, math.ceil(len(ordered) * p) - 1)
    return ordered[index]


def login(base_url: str) -> str:
    response = requests.post(
        f"{base_url}/api/v1/auth/login",
        json={
            "username": os.getenv("PERF_USERNAME", "admin"),
            "password": os.getenv("PERF_PASSWORD", "admin123"),
        },
        timeout=15,
    )
    response.raise_for_status()
    body = response.json()
    if body.get("code") != 0:
        raise RuntimeError(f"登录失败: {body}")
    return body["data"]["token"]


def request_once(url: str, headers: dict[str, str]) -> tuple[bool, float, int]:
    started = time.perf_counter()
    try:
        response = requests.get(url, headers=headers, timeout=20)
        elapsed = (time.perf_counter() - started) * 1000
        return response.ok, elapsed, response.status_code
    except requests.RequestException:
        elapsed = (time.perf_counter() - started) * 1000
        return False, elapsed, 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default=os.getenv("BASE_URL", "http://localhost"))
    parser.add_argument("--requests", type=int, default=60)
    parser.add_argument("--concurrency", type=int, default=6)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()

    base_url = args.base_url.rstrip("/")
    token = login(base_url)
    headers = {"Authorization": f"Bearer {token}"}
    endpoints = [
        f"{base_url}/actuator/health",
        f"{base_url}/api/v1/diagnosis?page=1&size=10",
        f"{base_url}/api/v1/model-versions/runtime",
    ]

    started = time.perf_counter()
    results: list[tuple[bool, float, int]] = []
    with ThreadPoolExecutor(max_workers=args.concurrency) as executor:
        futures = [
            executor.submit(request_once, endpoints[i % len(endpoints)], headers)
            for i in range(args.requests)
        ]
        for future in as_completed(futures):
            results.append(future.result())
    total_seconds = time.perf_counter() - started

    latencies = [elapsed for _, elapsed, _ in results]
    successes = sum(1 for ok, _, _ in results if ok)
    report = {
        "scope": "course-acceptance-smoke-test",
        "base_url": base_url,
        "requests": len(results),
        "concurrency": args.concurrency,
        "successes": successes,
        "failures": len(results) - successes,
        "success_rate": round(successes / len(results) * 100, 2),
        "throughput_rps": round(len(results) / total_seconds, 2),
        "latency_ms": {
            "min": round(min(latencies), 2),
            "median": round(statistics.median(latencies), 2),
            "p95": round(percentile(latencies, 0.95), 2),
            "max": round(max(latencies), 2),
        },
        "status_codes": {
            str(code): sum(1 for _, _, current in results if current == code)
            for code in sorted({code for _, _, code in results})
        },
    }
    rendered = json.dumps(report, ensure_ascii=False, indent=2)
    print(rendered)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered + "\n", encoding="utf-8")
    return 0 if successes == len(results) else 1


if __name__ == "__main__":
    raise SystemExit(main())