"""云农智诊完整 API 集成测试。

运行：ai-service/.venv/Scripts/python.exe -m pytest tests/integration/test_api.py -q
"""
import json
import os
import time
import uuid
from datetime import date, timedelta
from pathlib import Path

import requests
from PIL import Image


BASE_URL = os.getenv("BASE_URL", "http://localhost").rstrip("/")
PROJECT_ROOT = Path(__file__).resolve().parents[2]
SAMPLE_IMAGE = (
    PROJECT_ROOT
    / "ai-service"
    / "data"
    / "train"
    / "rice_blast"
    / "rice_blast_extra_04.jpg"
)


def login(username="admin", password="admin123"):
    response = requests.post(
        f"{BASE_URL}/api/v1/auth/login",
        json={"username": username, "password": password},
        timeout=15,
    )
    response.raise_for_status()
    body = response.json()
    assert body["code"] == 0, body
    return body["data"]


def auth_headers(username="admin", password="admin123"):
    return {"Authorization": f"Bearer {login(username, password)['token']}"}


def assert_ok(response):
    response.raise_for_status()
    body = response.json()
    assert body["code"] == 0, body
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
    data = assert_ok(
        requests.get(
            f"{BASE_URL}/api/v1/diagnosis",
            headers=auth_headers(),
            timeout=15,
        )
    )
    assert "list" in data


def test_ai_disease_list():
    response = requests.get(f"{BASE_URL}/ai/api/v1/diagnosis/diseases", timeout=30)
    assert response.status_code == 200
    assert len(response.json()["diseases"]) == 18


def test_published_knowledge_syncs_to_rag_and_archive_removes_it():
    headers = auth_headers()
    suffix = uuid.uuid4().hex[:10]
    title = f"E2E唯一知识-{suffix}"
    marker = f"连续降雨后检查叶片紫色病斑特征码{suffix}"

    document = assert_ok(
        requests.post(
            f"{BASE_URL}/api/v1/knowledge/documents",
            headers=headers,
            json={
                "title": title,
                "category": "disease",
                "content": marker,
                "tags": '["E2E","连续降雨"]',
                "status": "published",
            },
            timeout=60,
        )
    )

    retrieve = requests.post(
        f"{BASE_URL}/ai/api/v1/rag/retrieve",
        json={"query": marker, "top_k": 10},
        timeout=60,
    )
    retrieve.raise_for_status()
    assert retrieve.json()["success"] is True
    assert marker in json.dumps(retrieve.json(), ensure_ascii=False)

    archived = assert_ok(
        requests.post(
            f"{BASE_URL}/api/v1/knowledge/documents/{document['id']}/archive",
            headers=headers,
            timeout=60,
        )
    )
    assert archived["status"] == "archived"

    retrieve_after_archive = requests.post(
        f"{BASE_URL}/ai/api/v1/rag/retrieve",
        json={"query": marker, "top_k": 10},
        timeout=60,
    )
    retrieve_after_archive.raise_for_status()
    assert marker not in json.dumps(retrieve_after_archive.json(), ensure_ascii=False)


def test_model_runtime_can_query_and_deploy_registered_model():
    headers = auth_headers()
    models = assert_ok(
        requests.get(
            f"{BASE_URL}/api/v1/model-versions?size=50",
            headers=headers,
            timeout=30,
        )
    )["list"]
    model = next(
        item
        for item in models
        if item.get("modelPath") == "/app/best_model.pth"
        and item.get("numClasses") == 18
    )

    runtime = assert_ok(
        requests.get(
            f"{BASE_URL}/api/v1/model-versions/runtime",
            headers=headers,
            timeout=60,
        )
    )
    assert runtime["loaded"] is True
    assert runtime["model_path"] == "/app/best_model.pth"
    assert runtime["num_classes"] == 18

    deployed = assert_ok(
        requests.post(
            f"{BASE_URL}/api/v1/model-versions/{model['id']}/deploy",
            headers=headers,
            timeout=90,
        )
    )
    assert deployed["status"] == "deployed"

    runtime_after_deploy = assert_ok(
        requests.get(
            f"{BASE_URL}/api/v1/model-versions/runtime",
            headers=headers,
            timeout=60,
        )
    )
    assert runtime_after_deploy["model_id"] == model["id"]
    assert runtime_after_deploy["version"] == model["version"]
    assert runtime_after_deploy["loaded"] is True


def test_diagnosis_agent_review_and_farming_task_complete_workflow(tmp_path):
    farmer_headers = auth_headers("farmer", "farmer123")
    admin_headers = auth_headers()
    suffix = uuid.uuid4().hex[:10]

    farm = assert_ok(
        requests.post(
            f"{BASE_URL}/api/v1/farms",
            headers=farmer_headers,
            json={"name": f"E2E农场-{suffix}", "areaMu": 12.5},
            timeout=30,
        )
    )
    forecast = assert_ok(
        requests.post(
            f"{BASE_URL}/api/v1/weather/fetch",
            headers=admin_headers,
            params={"farmId": farm["id"]},
            timeout=60,
        )
    )
    assert len(forecast["records"]) == 7

    field = assert_ok(
        requests.post(
            f"{BASE_URL}/api/v1/farms/{farm['id']}/fields",
            headers=farmer_headers,
            json={"name": f"E2E地块-{suffix}", "areaMu": 3.2, "soilType": "壤土"},
            timeout=30,
        )
    )
    crops = assert_ok(
        requests.get(
            f"{BASE_URL}/api/v1/crops?size=100",
            headers=farmer_headers,
            timeout=30,
        )
    )["list"]
    assert crops

    cycle = assert_ok(
        requests.post(
            f"{BASE_URL}/api/v1/planting-cycles",
            headers=farmer_headers,
            json={
                "fieldId": field["id"],
                "cropId": crops[0]["id"],
                "plantingDate": str(date.today() - timedelta(days=35)),
                "expectedHarvestDate": str(date.today() + timedelta(days=80)),
                "growthStage": "分蘖期",
                "areaMu": 3.2,
                "remark": "E2E完整流程",
            },
            timeout=30,
        )
    )

    unique_image = tmp_path / f"diagnosis-{suffix}.jpg"
    image = Image.open(SAMPLE_IMAGE).convert("RGB")
    for index, value in enumerate(bytes.fromhex(suffix)):
        image.paste(
            (value, 255 - value, (value * 37) % 256),
            (index * 12, 0, (index + 1) * 12, 12),
        )
    image.save(unique_image, format="JPEG", quality=95)
    with unique_image.open("rb") as image_file:
        upload = assert_ok(
            requests.post(
                f"{BASE_URL}/api/v1/diagnosis/upload",
                headers=farmer_headers,
                files={"file": (unique_image.name, image_file, "image/jpeg")},
                data={"cycleId": cycle["id"], "description": f"E2E-{suffix}"},
                timeout=60,
            )
        )

    diagnosis_id = upload["diagnosisId"]
    diagnosis = None
    for _ in range(40):
        diagnosis = assert_ok(
            requests.get(
                f"{BASE_URL}/api/v1/diagnosis/{diagnosis_id}",
                headers=farmer_headers,
                timeout=30,
            )
        )
        if diagnosis.get("aiResult") or diagnosis.get("reviewStatus") == "failed":
            break
        time.sleep(1.5)

    assert diagnosis is not None
    assert diagnosis["reviewStatus"] in {"pending", "pending_review"}, diagnosis
    assert diagnosis["aiResult"], diagnosis
    ai_result = json.loads(diagnosis["aiResult"])
    assert set(ai_result) >= {
        "treatment",
        "citations",
        "contextSummary",
        "agentTrace",
    }
    assert ai_result["contextSummary"], ai_result
    assert ai_result["agentTrace"], ai_result

    visible_result = assert_ok(
        requests.get(
            f"{BASE_URL}/api/v1/diagnosis/result/{diagnosis_id}",
            headers=farmer_headers,
            timeout=30,
        )
    )
    assert visible_result["contextSummary"] == ai_result["contextSummary"]
    assert visible_result["contextSummary"]["growth_stage_label"] == "分蘖期"
    assert visible_result["contextSummary"]["weather_days"] == 7
    assert len(visible_result["agentTrace"]) == 4

    image_response = requests.get(
        f"{BASE_URL}/api/v1/diagnosis/{diagnosis_id}/image",
        headers=farmer_headers,
        timeout=30,
    )
    image_response.raise_for_status()
    assert image_response.headers["content-type"].startswith("image/")
    assert len(image_response.content) > 100

    reviewed = assert_ok(
        requests.post(
            f"{BASE_URL}/api/v1/diagnosis/{diagnosis_id}/review",
            headers=admin_headers,
            params={"status": "approved", "comment": "E2E验收通过"},
            timeout=30,
        )
    )
    assert reviewed["reviewStatus"] == "approved"

    tasks = assert_ok(
        requests.get(
            f"{BASE_URL}/api/v1/tasks?size=100",
            headers=farmer_headers,
            timeout=30,
        )
    )["list"]
    created_task = next(item for item in tasks if item.get("diagnosisId") == diagnosis_id)
    assert created_task["cycleId"] == cycle["id"]
    assert created_task["status"] == "pending"
    assert created_task["taskType"] == "treatment"
