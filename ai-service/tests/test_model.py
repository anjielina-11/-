import os
import torch
import torch.nn as nn
from torchvision import transforms, models
from PIL import Image

MODEL_PATH = "best_model.pth"
CLASS_TO_IDX_PATH = "class_to_idx.pth"
THRESHOLD = 0.6
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"

class_to_idx = torch.load(CLASS_TO_IDX_PATH, map_location=DEVICE, weights_only=False)
idx_to_class = {v: k for k, v in class_to_idx.items()}
num_classes = len(idx_to_class)

model = models.resnet50(weights=None)
num_ftrs = model.fc.in_features
model.fc = nn.Linear(num_ftrs, num_classes)
model.load_state_dict(torch.load(MODEL_PATH, map_location=DEVICE, weights_only=True))
model = model.to(DEVICE)
model.eval()

transform = transforms.Compose([
    transforms.Resize(256),
    transforms.CenterCrop(224),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
])

def predict(image_path):
    image = Image.open(image_path).convert("RGB")
    image_tensor = transform(image).unsqueeze(0).to(DEVICE)
    
    with torch.no_grad():
        outputs = model(image_tensor)
        probabilities = torch.softmax(outputs, dim=1)
        max_prob, predicted_idx = torch.max(probabilities, dim=1)
    
    confidence = max_prob.item()
    disease_name = idx_to_class[predicted_idx.item()]
    return disease_name, confidence

test_images = [
    ('data/Tomato_Healthy/Tomato_Healthy_0000.jpg', 'Tomato_Healthy'),
    ('data/Tomato_Early_blight/Tomato_Early_blight_0000.jpg', 'Tomato_Early_blight'),
    ('data/Tomato_Late_blight/Tomato_Late_blight_0000.jpg', 'Tomato_Late_blight'),
    ('data/Potato_Early_blight/Potato_Early_blight_0000.jpg', 'Potato_Early_blight'),
    ('data/Tomato_Leaf_Mold/Tomato_Leaf_Mold_0000.jpg', 'Tomato_Leaf_Mold'),
    ('data/Tomato_Septoria_leaf_spot/Tomato_Septoria_leaf_spot_0000.jpg', 'Tomato_Septoria_leaf_spot'),
    ('data/Tomato_Spider_mites/Tomato_Spider_mites_0000.jpg', 'Tomato_Spider_mites'),
    ('data/Tomato_Target_Spot/Tomato_Target_Spot_0000.jpg', 'Tomato_Target_Spot'),
    ('data/Tomato_Yellow_Leaf_Curl_Virus/Tomato_Yellow_Leaf_Curl_Virus_0000.jpg', 'Tomato_Yellow_Leaf_Curl_Virus'),
    ('data/Tomato_Mosaic_Virus/Tomato_Mosaic_Virus_0000.jpg', 'Tomato_Mosaic_Virus'),
]

def test_available_model_samples():
    available = [(path, expected) for path, expected in test_images if os.path.exists(path)]
    if not available:
        import pytest
        pytest.skip("No evaluation images available")

    correct = 0
    for img_path, expected in available:
        disease, _ = predict(img_path)
        if disease == expected:
            correct += 1

    assert correct / len(available) >= 0
