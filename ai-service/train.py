import argparse
from pathlib import Path

import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader
from torchvision import models

import sys
sys.path.insert(0, "src")
from src.services.data_loader import get_data_loaders


def create_model(num_classes: int):
    """
    创建 ResNet50 模型并替换最后一层全连接层

    Args:
        num_classes: 分类数量

    Returns:
        修改后的模型
    """
    model = models.resnet50(weights=models.ResNet50_Weights.DEFAULT)

    in_features = model.fc.in_features
    model.fc = nn.Linear(in_features, num_classes)

    return model


def train_one_epoch(
    model: nn.Module,
    dataloader: DataLoader,
    criterion: nn.Module,
    optimizer: optim.Optimizer,
    device: torch.device,
):
    """
    训练一个 epoch

    Args:
        model: 模型
        dataloader: 训练数据加载器
        criterion: 损失函数
        optimizer: 优化器
        device: 计算设备

    Returns:
        float: 平均训练损失
    """
    model.train()
    running_loss = 0.0
    correct = 0
    total = 0

    for images, labels in dataloader:
        images, labels = images.to(device), labels.to(device)

        optimizer.zero_grad()
        outputs = model(images)
        loss = criterion(outputs, labels)
        loss.backward()
        optimizer.step()

        running_loss += loss.item() * images.size(0)
        _, predicted = torch.max(outputs, 1)
        total += labels.size(0)
        correct += (predicted == labels).sum().item()

    avg_loss = running_loss / total
    accuracy = 100.0 * correct / total
    return avg_loss, accuracy


@torch.no_grad()
def validate(
    model: nn.Module,
    dataloader: DataLoader,
    criterion: nn.Module,
    device: torch.device,
):
    """
    验证函数

    Args:
        model: 模型
        dataloader: 验证数据加载器
        criterion: 损失函数
        device: 计算设备

    Returns:
        tuple: (平均验证损失, 验证准确率)
    """
    model.eval()
    running_loss = 0.0
    correct = 0
    total = 0

    for images, labels in dataloader:
        images, labels = images.to(device), labels.to(device)

        outputs = model(images)
        loss = criterion(outputs, labels)

        running_loss += loss.item() * images.size(0)
        _, predicted = torch.max(outputs, 1)
        total += labels.size(0)
        correct += (predicted == labels).sum().item()

    avg_loss = running_loss / total
    accuracy = 100.0 * correct / total
    return avg_loss, accuracy


def train(args):
    """
    完整的训练流程

    Args:
        args: 命令行参数
    """
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"使用设备: {device}")

    train_loader, val_loader, class_names = get_data_loaders(
        train_dir=args.train_dir,
        val_dir=args.val_dir,
        batch_size=args.batch_size,
        num_workers=args.num_workers,
    )

    print(f"类别数量: {len(class_names)}")
    print(f"类别: {class_names}")

    model = create_model(num_classes=len(class_names))
    model = model.to(device)

    criterion = nn.CrossEntropyLoss()
    optimizer = optim.Adam(model.parameters(), lr=args.learning_rate)

    best_val_acc = 0.0
    best_model_path = Path(args.save_path)
    best_model_path.parent.mkdir(parents=True, exist_ok=True)

    print(f"\n开始训练, 共 {args.epochs} 个 epoch")
    print("=" * 70)

    for epoch in range(1, args.epochs + 1):
        train_loss, train_acc = train_one_epoch(
            model, train_loader, criterion, optimizer, device
        )
        val_loss, val_acc = validate(
            model, val_loader, criterion, device
        )

        print(
            f"Epoch [{epoch}/{args.epochs}] "
            f"训练损失: {train_loss:.4f} | "
            f"训练准确率: {train_acc:.2f}% | "
            f"验证损失: {val_loss:.4f} | "
            f"验证准确率: {val_acc:.2f}%",
        )

        if val_acc > best_val_acc:
            best_val_acc = val_acc
            torch.save(model.state_dict(), best_model_path)
            class_to_idx = {cls: idx for idx, cls in enumerate(class_names)}
            torch.save(class_to_idx, Path(args.save_path).parent / "class_to_idx.pth")
            print(f"  ★ 新的最佳模型，验证准确率: {val_acc:.2f}%，已保存至 {best_model_path}")

    print("=" * 70)
    print(f"训练完成！最佳验证准确率: {best_val_acc:.2f}%")
    print(f"最佳模型已保存至: {best_model_path}")


def main():
    parser = argparse.ArgumentParser(description="农作物病害识别模型训练")
    parser.add_argument(
        "--train-dir",
        type=str,
        default="data/train",
        help="训练集目录路径",
    )
    parser.add_argument(
        "--val-dir",
        type=str,
        default="data/val",
        help="验证集目录路径",
    )
    parser.add_argument(
        "--epochs",
        type=int,
        default=10,
        help="训练轮数",
    )
    parser.add_argument(
        "--batch-size",
        type=int,
        default=32,
        help="批处理大小",
    )
    parser.add_argument(
        "--learning-rate",
        type=float,
        default=0.001,
        help="学习率",
    )
    parser.add_argument(
        "--num-workers",
        type=int,
        default=4,
        help="数据加载线程数",
    )
    parser.add_argument(
        "--save-path",
        type=str,
        default="best_model.pth",
        help="最佳模型保存路径",
    )

    args = parser.parse_args()
    train(args)


if __name__ == "__main__":
    main()
