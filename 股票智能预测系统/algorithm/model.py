"""
Transformer 时间序列预测模型
基于 PyTorch 实现，使用历史收盘价预测未来价格
"""

import time

import numpy as np
import torch
import torch.nn as nn
from sklearn.preprocessing import MinMaxScaler


class PositionalEncoding(nn.Module):
    """正弦/余弦位置编码"""

    def __init__(self, d_model: int, dropout: float = 0.1, max_len: int = 500):
        super().__init__()
        self.dropout = nn.Dropout(p=dropout)

        pe = torch.zeros(max_len, d_model)
        position = torch.arange(0, max_len, dtype=torch.float).unsqueeze(1)
        div_term = torch.exp(
            torch.arange(0, d_model, 2).float() * (-np.log(10000.0) / d_model)
        )
        pe[:, 0::2] = torch.sin(position * div_term)
        pe[:, 1::2] = torch.cos(position * div_term)
        pe = pe.unsqueeze(0)  # [1, max_len, d_model]
        self.register_buffer("pe", pe)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        # x: [batch, seq_len, d_model]
        x = x + self.pe[:, : x.size(1), :]
        return self.dropout(x)


class TimeSeriesTransformer(nn.Module):
    """
    用于时间序列预测的 Transformer 模型
    输入: [batch, seq_len=20, feature=1]
    输出: [batch, pred_len=5]
    """

    def __init__(
        self,
        d_model: int = 64,
        nhead: int = 4,
        num_encoder_layers: int = 3,
        dim_feedforward: int = 256,
        dropout: float = 0.1,
        input_len: int = 20,
        output_len: int = 5,
    ):
        super().__init__()
        self.input_len = input_len
        self.output_len = output_len
        self.d_model = d_model

        # 输入投影: 1维 → d_model维
        self.input_proj = nn.Linear(1, d_model)

        # 位置编码
        self.pos_encoder = PositionalEncoding(d_model, dropout, max_len=500)

        # Transformer Encoder
        encoder_layer = nn.TransformerEncoderLayer(
            d_model=d_model,
            nhead=nhead,
            dim_feedforward=dim_feedforward,
            dropout=dropout,
            batch_first=True,
        )
        self.transformer_encoder = nn.TransformerEncoder(
            encoder_layer, num_layers=num_encoder_layers
        )

        # 输出投影: 展平后 → 128 → 5
        self.output_proj = nn.Sequential(
            nn.Linear(d_model * input_len, 128),
            nn.ReLU(),
            nn.Dropout(dropout),
            nn.Linear(128, output_len),
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        # x: [batch, seq_len, 1]
        batch_size = x.size(0)

        x = self.input_proj(x)  # [batch, seq_len, d_model]
        x = self.pos_encoder(x)
        x = self.transformer_encoder(x)  # [batch, seq_len, d_model]
        x = x.reshape(batch_size, -1)  # [batch, d_model * seq_len]
        x = self.output_proj(x)  # [batch, output_len]
        return x


class StockPredictor:
    """
    股票预测器：封装数据预处理、模型训练和预测
    """

    def __init__(self):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model: TimeSeriesTransformer | None = None
        self.scaler: MinMaxScaler | None = None
        self.input_len = 20
        self.output_len = 5
        # 模型超参数（供外部读取）
        self.model_config = {
            "d_model": 64,
            "nhead": 4,
            "num_encoder_layers": 3,
            "dim_feedforward": 256,
            "dropout": 0.1,
        }

    def _count_parameters(self) -> dict:
        """统计模型参数量"""
        if self.model is None:
            return {"total": 0, "trainable": 0}
        total = sum(p.numel() for p in self.model.parameters())
        trainable = sum(p.numel() for p in self.model.parameters() if p.requires_grad)
        return {"total": total, "trainable": trainable}

    def _create_sequences(
        self, data: np.ndarray
    ) -> tuple[np.ndarray, np.ndarray]:
        """从价格序列创建训练样本 (滑动窗口)"""
        X, y = [], []
        for i in range(len(data) - self.input_len - self.output_len + 1):
            X.append(data[i : i + self.input_len])
            y.append(
                data[i + self.input_len : i + self.input_len + self.output_len]
            )
        return np.array(X), np.array(y)

    def train(
        self,
        prices: np.ndarray,
        epochs: int = 200,
        lr: float = 0.001,
        verbose: bool = True,
    ) -> dict:
        """
        训练 Transformer 模型

        Args:
            prices: 一维收盘价数组，长度至少需要 > input_len + output_len
            epochs: 训练轮数
            lr: 学习率
            verbose: 是否打印训练信息

        Returns:
            dict: 包含训练损失历史、模型参数、训练配置的完整字典
        """
        t_start = time.time()

        # 数据归一化
        self.scaler = MinMaxScaler(feature_range=(-1, 1))
        prices_scaled = self.scaler.fit_transform(
            prices.reshape(-1, 1)
        ).flatten()

        # 创建训练样本
        X, y = self._create_sequences(prices_scaled)

        if len(X) < 10:
            raise ValueError(
                f"训练数据不足：仅 {len(X)} 个样本，需要至少 10 个。"
                f"请获取更多的历史数据（当前 {len(prices)} 个交易日）"
            )

        # 划分训练集和验证集 (80/20)
        split = int(len(X) * 0.8)
        X_train, X_val = X[:split], X[split:]
        y_train, y_val = y[:split], y[split:]

        # 转为 Tensor
        X_train = torch.tensor(X_train, dtype=torch.float32).unsqueeze(-1)
        y_train = torch.tensor(y_train, dtype=torch.float32)
        X_val = torch.tensor(X_val, dtype=torch.float32).unsqueeze(-1)
        y_val = torch.tensor(y_val, dtype=torch.float32)

        # 初始化模型
        self.model = TimeSeriesTransformer(
            d_model=self.model_config["d_model"],
            nhead=self.model_config["nhead"],
            num_encoder_layers=self.model_config["num_encoder_layers"],
            dim_feedforward=self.model_config["dim_feedforward"],
            dropout=self.model_config["dropout"],
            input_len=self.input_len,
            output_len=self.output_len,
        ).to(self.device)

        # 模型参数量统计
        param_info = self._count_parameters()

        # 训练配置
        criterion = nn.MSELoss()
        optimizer = torch.optim.Adam(self.model.parameters(), lr=lr)
        scheduler = torch.optim.lr_scheduler.ReduceLROnPlateau(
            optimizer, mode="min", factor=0.5, patience=20
        )

        train_losses = []
        val_losses = []

        self.model.train()
        for epoch in range(epochs):
            # 训练
            optimizer.zero_grad()
            outputs = self.model(X_train.to(self.device))
            loss = criterion(outputs, y_train.to(self.device))
            loss.backward()
            torch.nn.utils.clip_grad_norm_(self.model.parameters(), max_norm=1.0)
            optimizer.step()

            train_losses.append(loss.item())

            # 验证
            self.model.eval()
            with torch.no_grad():
                val_outputs = self.model(X_val.to(self.device))
                val_loss = criterion(val_outputs, y_val.to(self.device))
                val_losses.append(val_loss.item())
            self.model.train()

            scheduler.step(val_loss)

            if verbose and (epoch + 1) % 50 == 0:
                print(
                    f"  Epoch {epoch+1}/{epochs} | "
                    f"Train Loss: {loss.item():.6f} | "
                    f"Val Loss: {val_loss.item():.6f}"
                )

        self.model.eval()
        train_time = time.time() - t_start

        return {
            "train_losses": train_losses,
            "val_losses": val_losses,
            "final_train_loss": train_losses[-1],
            "final_val_loss": val_losses[-1],
            "train_samples": len(X_train),
            "val_samples": len(X_val),
            "total_samples": len(X),
            "epochs": epochs,
            "train_time_sec": round(train_time, 2),
            "device": str(self.device),
            "model_config": self.model_config,
            "input_len": self.input_len,
            "output_len": self.output_len,
            "param_count": param_info,
            "optimizer": "Adam",
            "learning_rate": lr,
            "loss_function": "MSE",
            "scheduler": "ReduceLROnPlateau",
            "gradient_clip_norm": 1.0,
            "scaler": "MinMaxScaler(-1, 1)",
            "train_val_split": "80/20",
        }

    def predict(self, last_prices: np.ndarray) -> np.ndarray:
        """
        基于最近的价格序列预测未来价格

        Args:
            last_prices: 最近 input_len 个交易日的收盘价

        Returns:
            np.ndarray: 未来 output_len 个交易日的预测收盘价
        """
        if self.model is None or self.scaler is None:
            raise RuntimeError("模型尚未训练，请先调用 train()")

        # 归一化
        scaled = self.scaler.transform(last_prices.reshape(-1, 1)).flatten()

        # 预测
        x = torch.tensor(scaled, dtype=torch.float32).unsqueeze(0).unsqueeze(-1)
        x = x.to(self.device)

        with torch.no_grad():
            pred_scaled = self.model(x).cpu().numpy().flatten()

        # 反归一化
        predictions = self.scaler.inverse_transform(
            pred_scaled.reshape(-1, 1)
        ).flatten()

        return predictions
