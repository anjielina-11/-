import torch
import torch.nn as nn
from torchvision import transforms, models
from PIL import Image
from io import BytesIO
from core.config import settings


class UnknownDiseaseError(Exception):
    def __init__(self, message="未知病害，需要人工审核"):
        super().__init__(message)
        self.message = message


class DiseaseClassifier:
    def __init__(self, model_path=None, class_to_idx_path=None, num_classes=None, threshold=None, device=None):
        self.model_path = model_path if model_path else settings.MODEL_PATH
        self.class_to_idx_path = class_to_idx_path if class_to_idx_path else settings.CLASS_TO_IDX_PATH
        self.threshold = threshold if threshold else settings.CONFIDENCE_THRESHOLD
        self.device = device if device else ("cuda" if torch.cuda.is_available() else "cpu")
        
        self._load_class_mapping()
        
        if num_classes is not None:
            self.num_classes = num_classes
        else:
            self.num_classes = len(self.idx_to_class)
        
        self._load_model()
        
        self.transform = transforms.Compose([
            transforms.Resize(256),
            transforms.CenterCrop(224),
            transforms.ToTensor(),
            transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
        ])
    
    def _load_class_mapping(self):
        class_to_idx = torch.load(self.class_to_idx_path, map_location=self.device, weights_only=False)
        self.idx_to_class = {v: k for k, v in class_to_idx.items()}
        self.class_names = [self.idx_to_class[i] for i in range(len(self.idx_to_class))]
    
    def _load_model(self):
        self.model = models.resnet50(weights=None)
        num_ftrs = self.model.fc.in_features
        self.model.fc = nn.Linear(num_ftrs, self.num_classes)
        
        self.model.load_state_dict(torch.load(self.model_path, map_location=self.device, weights_only=True))
        self.model = self.model.to(self.device)
        self.model.eval()
    
    def predict(self, image_path):
        image = Image.open(image_path).convert("RGB")
        image_tensor = self.transform(image).unsqueeze(0).to(self.device)
        
        with torch.no_grad():
            outputs = self.model(image_tensor)
            probabilities = torch.softmax(outputs, dim=1)
            max_prob, predicted_idx = torch.max(probabilities, dim=1)
        
        confidence = max_prob.item()
        disease_name = self.idx_to_class[predicted_idx.item()]
        
        if confidence < self.threshold:
            raise UnknownDiseaseError(f"置信度 {confidence:.4f} 低于阈值 {self.threshold}，未知病害，需要人工审核")
        
        return disease_name, confidence

    def predict_from_bytes(self, image_bytes):
        image = Image.open(BytesIO(image_bytes)).convert("RGB")
        image_tensor = self.transform(image).unsqueeze(0).to(self.device)
        
        with torch.no_grad():
            outputs = self.model(image_tensor)
            probabilities = torch.softmax(outputs, dim=1)
            max_prob, predicted_idx = torch.max(probabilities, dim=1)
        
        confidence = max_prob.item()
        disease_name = self.idx_to_class[predicted_idx.item()]
        
        if confidence < self.threshold:
            raise UnknownDiseaseError(f"置信度 {confidence:.4f} 低于阈值 {self.threshold}，未知病害，需要人工审核")
        
        return disease_name, confidence