"""统一入口接口集成测试。运行: pytest tests/integration/test_api.py -q"""
import os

import requests


BASE_URL = os.getenv("BASE_URL", "http://localhost").rstrip("/")


def login(username="admin", password="admin123"):
    response = requests.post(
        f"{BASE_URL}/api/v1/auth/login",
        json={"username": username, "password": password},
        timeout=15,
    )
    response.raise_for_status()
    body = response.json()
    assert body["code"] == 0
    return body["data"]


def test_backend_health():
    response = requests.get(f"{BASE_URL}/actuator/health", timeout=15)
    assert response.status_code == 200
    assert response.json()["status"] == "UP"


def test_ai_service_health():
    response = requests.get(f"{BASE_URL}/ai/health", timeout=30)
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "healthy"
    assert body["model_loaded"] is True
    assert body["vector_db_ready"] is True


def test_login_returns_nested_user_contract():
    data = login()
    assert data["token"]
    assert data["user"]["role"] == "admin"


def test_wrong_password_returns_business_error():
    response = requests.post(
        f"{BASE_URL}/api/v1/auth/login",
        json={"username": "admin", "password": "wrong_password"},
        timeout=15,
    )
    assert response.status_code == 200
    assert response.json()["code"] != 0


def test_unauthenticated_diagnosis_list_is_rejected():
    response = requests.get(f"{BASE_URL}/api/v1/diagnosis", timeout=15)
    assert response.status_code in {401, 403}


def test_authenticated_diagnosis_list():
    token = login()["token"]
    response = requests.get(
        f"{BASE_URL}/api/v1/diagnosis",
        headers={"Authorization": f"Bearer {token}"},
        timeout=15,
    )
    assert response.status_code == 200
    body = response.json()
    assert body["code"] == 0
    assert "list" in body["data"]


def test_ai_disease_list():
    response = requests.get(f"{BASE_URL}/ai/api/v1/diagnosis/diseases", timeout=30)
    assert response.status_code == 200
    assert len(response.json()["diseases"]) == 18
