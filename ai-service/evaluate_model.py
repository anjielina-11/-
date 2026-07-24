"""
模型评测脚本：计算 ResNet50 在验证集上的准确率、精确率、召回率、F1 和混淆矩阵
"""
import torch
import torch.nn as nn
from torch.utils.data import DataLoader
from torchvision import datasets, transforms, models
import json
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
BATCH_SIZE = 16
MODEL_PATH = os.path.join(BASE_DIR, 'best_model.pth')
CLASS_IDX_PATH = os.path.join(BASE_DIR, 'class_to_idx.pth')
VAL_DIR = os.path.join(BASE_DIR, 'data', 'val')
REPORT_PATH = os.path.join(BASE_DIR, 'model_evaluation_report.json')


def evaluate():
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"使用设备: {device}")

    # 加载类别映射
    class_to_idx = torch.load(CLASS_IDX_PATH)
    idx_to_class = {v: k for k, v in class_to_idx.items()}
    num_classes = len(class_to_idx)
    print(f"类别数: {num_classes}")

    # 数据预处理
    transform = transforms.Compose([
        transforms.Resize((224, 224)),
        transforms.ToTensor(),
        transforms.Normalize([0.485, 0.456, 0.406], [0.229, 0.224, 0.225])
    ])

    # 加载验证集
    val_dataset = datasets.ImageFolder(root=VAL_DIR, transform=transform)
    val_loader = DataLoader(val_dataset, batch_size=BATCH_SIZE, shuffle=False, num_workers=2)
    print(f"验证集样本数: {len(val_dataset)}")

    # 加载模型
    model = models.resnet50(weights=None)
    model.fc = nn.Linear(model.fc.in_features, num_classes)
    model.load_state_dict(torch.load(MODEL_PATH, map_location=device))
    model = model.to(device)
    model.eval()

    # 推理
    all_preds = []
    all_labels = []

    with torch.no_grad():
        for images, labels in val_loader:
            images = images.to(device)
            outputs = model(images)
            _, preds = torch.max(outputs, 1)
            all_preds.extend(preds.cpu().tolist())
            all_labels.extend(labels.tolist())

    # 计算指标
    from sklearn.metrics import (accuracy_score, precision_score, recall_score,
                                  f1_score, confusion_matrix, classification_report)

    accuracy = accuracy_score(all_labels, all_preds)
    precision = precision_score(all_labels, all_preds, average='weighted', zero_division=0)
    recall = recall_score(all_labels, all_preds, average='weighted', zero_division=0)
    f1 = f1_score(all_labels, all_preds, average='weighted', zero_division=0)
    cm = confusion_matrix(all_labels, all_preds)

    # 各类别指标
    per_class = {}
    for i, name in sorted(idx_to_class.items()):
        tp = cm[i, i]
        fp = cm[:, i].sum() - tp
        fn = cm[i, :].sum() - tp
        support = int(cm[i, :].sum())
        prec = tp / (tp + fp) if (tp + fp) > 0 else 0
        rec = tp / (tp + fn) if (tp + fn) > 0 else 0
        f1_c = 2 * prec * rec / (prec + rec) if (prec + rec) > 0 else 0
        per_class[name] = {
            "precision": round(prec, 4),
            "recall": round(rec, 4),
            "f1_score": round(f1_c, 4),
            "support": support
        }

    report = {
        "model": "ResNet50",
        "num_classes": num_classes,
        "val_samples": len(val_dataset),
        "overall_metrics": {
            "accuracy": round(accuracy, 4),
            "precision_weighted": round(precision, 4),
            "recall_weighted": round(recall, 4),
            "f1_score_weighted": round(f1, 4)
        },
        "per_class": per_class,
        "confusion_matrix": cm.tolist(),
        "class_names": [idx_to_class[i] for i in range(num_classes)]
    }

    # 输出和保存
    print("\n========== 模型评测报告 ==========")
    print(f"准确率 (Accuracy):  {accuracy:.4f}")
    print(f"精确率 (Precision): {precision:.4f}")
    print(f"召回率 (Recall):    {recall:.4f}")
    print(f"F1分数 (F1-Score):  {f1:.4f}")
    print(f"\n各类别指标:")
    print(f"{'病害名称':<35} {'Prec':<8} {'Recall':<8} {'F1':<8} {'样本':<6}")
    print("-" * 70)
    for name, metrics in per_class.items():
        print(f"{name:<35} {metrics['precision']:<8.4f} {metrics['recall']:<8.4f} "
              f"{metrics['f1_score']:<8.4f} {metrics['support']:<6}")

    with open(REPORT_PATH, 'w', encoding='utf-8') as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    print(f"\n报告已保存至: {REPORT_PATH}")

    return report


if __name__ == '__main__':
    evaluate()
