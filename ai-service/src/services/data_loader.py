from pathlib import Path

import torch
from torch.utils.data import DataLoader
from torchvision import datasets, transforms


def get_transforms():
    """
    构建图像预处理转换管道

    Returns:
        transforms.Compose: 预处理转换组合
    """
    return transforms.Compose([
        transforms.Resize(256),
        transforms.CenterCrop(224),
        transforms.ToTensor(),
        transforms.Normalize(
            mean=[0.485, 0.456, 0.406],
            std=[0.229, 0.224, 0.225],
        ),
    ])


def get_data_loaders(
    train_dir: str | Path,
    val_dir: str | Path,
    batch_size: int = 32,
    num_workers: int = 4,
):
    """
    创建训练集和验证集的 DataLoader

    Args:
        train_dir: 训练集目录路径
        val_dir: 验证集目录路径
        batch_size: 批处理大小
        num_workers: 数据加载线程数

    Returns:
        tuple: (train_loader, val_loader, class_names)
    """
    transform = get_transforms()

    train_dataset = datasets.ImageFolder(root=train_dir, transform=transform)
    val_dataset = datasets.ImageFolder(root=val_dir, transform=transform)

    train_loader = DataLoader(
        train_dataset,
        batch_size=batch_size,
        shuffle=True,
        num_workers=num_workers,
        pin_memory=True,
    )

    val_loader = DataLoader(
        val_dataset,
        batch_size=batch_size,
        shuffle=False,
        num_workers=num_workers,
        pin_memory=True,
    )

    class_names = train_dataset.classes

    return train_loader, val_loader, class_names


def print_dataset_info(train_loader: DataLoader, val_loader: DataLoader, class_names: list):
    """
    打印数据集信息和类别映射

    Args:
        train_loader: 训练集 DataLoader
        val_loader: 验证集 DataLoader
        class_names: 类别名称列表
    """
    train_size = len(train_loader.dataset)
    val_size = len(val_loader.dataset)
    train_batches = len(train_loader)
    val_batches = len(val_loader)
    batch_size = train_loader.batch_size

    print("=" * 60)
    print("数据集信息")
    print("=" * 60)
    print(f"训练集样本数: {train_size}")
    print(f"验证集样本数: {val_size}")
    print(f"Batch Size:   {batch_size}")
    print(f"训练集 Batch 数: {train_batches} (ceil({train_size}/{batch_size}))")
    print(f"验证集 Batch 数: {val_batches} (ceil({val_size}/{batch_size}))")
    print(f"类别数量:     {len(class_names)}")
    print("-" * 60)
    print("类别名称与索引映射:")
    print("-" * 60)
    for idx, name in enumerate(class_names):
        print(f"  [{idx:2d}] {name}")
    print("=" * 60)


if __name__ == "__main__":
    train_loader, val_loader, class_names = get_data_loaders(
        train_dir="data/train",
        val_dir="data/val",
    )
    print_dataset_info(train_loader, val_loader, class_names)
