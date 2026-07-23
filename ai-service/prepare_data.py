"""
数据准备脚本：将 d:\学习\data1\病虫害\images\ 数据集划分为训练集和验证集
"""
import os
import random
import shutil
from pathlib import Path


def prepare_dataset(
    source_dir: str,
    target_dir: str,
    train_ratio: float = 0.8,
    seed: int = 42,
    exclude_annotated: bool = True,
):
    """
    将源数据集划分为训练集和验证集
    
    Args:
        source_dir: 源数据目录（如 d:\学习\data1\病虫害\images）
        target_dir: 目标数据目录
        train_ratio: 训练集比例
        seed: 随机种子
        exclude_annotated: 是否排除标注后的图片文件夹
    """
    random.seed(seed)
    
    source_path = Path(source_dir)
    target_path = Path(target_dir)
    
    train_path = target_path / "train"
    val_path = target_path / "val"
    
    # 清空目标目录
    if target_path.exists():
        shutil.rmtree(target_path)
    
    train_path.mkdir(parents=True, exist_ok=True)
    val_path.mkdir(parents=True, exist_ok=True)
    
    # 统计信息
    total_images = 0
    train_count = 0
    val_count = 0
    class_stats = {}
    
    # 遍历每个类别文件夹
    for class_dir in sorted(source_path.iterdir()):
        if not class_dir.is_dir():
            continue
            
        class_name = class_dir.name
        
        # 排除标注后的文件夹
        if exclude_annotated and class_name.endswith("_annotated"):
            continue
        
        # 获取该类别下的所有图片
        image_files = []
        for ext in ["*.jpg", "*.jpeg", "*.png", "*.JPG", "*.JPEG", "*.PNG"]:
            image_files.extend(class_dir.glob(ext))
        
        if not image_files:
            print(f"跳过空文件夹: {class_name}")
            continue
        
        # 随机打乱
        random.shuffle(image_files)
        
        # 划分训练集和验证集
        split_idx = int(len(image_files) * train_ratio)
        train_files = image_files[:split_idx]
        val_files = image_files[split_idx:]
        
        # 创建目标文件夹
        (train_path / class_name).mkdir(exist_ok=True)
        (val_path / class_name).mkdir(exist_ok=True)
        
        # 复制文件
        for f in train_files:
            shutil.copy2(f, train_path / class_name / f.name)
            train_count += 1
        
        for f in val_files:
            shutil.copy2(f, val_path / class_name / f.name)
            val_count += 1
        
        total_images += len(image_files)
        class_stats[class_name] = {
            "total": len(image_files),
            "train": len(train_files),
            "val": len(val_files),
        }
        
        print(f"类别 [{class_name}]: {len(image_files)} 张图片 -> 训练集 {len(train_files)}, 验证集 {len(val_files)}")
    
    print("\n" + "=" * 70)
    print("数据集划分完成！")
    print("=" * 70)
    print(f"总图片数: {total_images}")
    print(f"训练集图片数: {train_count}")
    print(f"验证集图片数: {val_count}")
    print(f"类别数量: {len(class_stats)}")
    print(f"训练集目录: {train_path}")
    print(f"验证集目录: {val_path}")
    
    return class_stats


if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="划分数据集")
    parser.add_argument(
        "--source-dir",
        type=str,
        default=r"d:\学习\data1\病虫害\images",
        help="源数据目录",
    )
    parser.add_argument(
        "--target-dir",
        type=str,
        default=r"d:\下载\-\ai-service\data",
        help="目标数据目录",
    )
    parser.add_argument(
        "--train-ratio",
        type=float,
        default=0.8,
        help="训练集比例",
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=42,
        help="随机种子",
    )
    
    args = parser.parse_args()
    
    prepare_dataset(
        source_dir=args.source_dir,
        target_dir=args.target_dir,
        train_ratio=args.train_ratio,
        seed=args.seed,
    )